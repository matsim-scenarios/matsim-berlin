package org.matsim.run.policies.autofrei;

import com.github.luben.zstd.ZstdInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.deparking.DeParkingModule;
import org.matsim.run.deparking.DeparkingConfigGroup;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class RunAutofreiPolicyDeparking extends RunAutofreiPolicy {
	public static final String PARKING_SPOTS_ATTR = "parkingSpots";

	@CommandLine.Option(names = "--parking-spots")
	private String parkingSpotsFile;

	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiPolicyDeparking.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);
		ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		addParkingSpots(scenario);
		replaceLowestScorePlanByWalkPlan(scenario);
//		scenario.getPopulation().getPersons().entrySet().removeIf(p -> !p.getKey().equals(Id.createPersonId("goodsTraffic_re_vkz.0053_4_80")) &&
//			!p.getKey().equals(Id.createPersonId("berlin_12c8d407")));
	}

	private static void replaceLowestScorePlanByWalkPlan(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			// remove plan with lowest score from unselected plans
			Plan selectedPlan = person.getSelectedPlan();
			List<? extends Plan> unselectedPlans = person.getPlans().stream().filter(p -> p != selectedPlan).toList();
			Plan min = Collections.min(unselectedPlans, Comparator.comparing(Plan::getScore));
			person.removePlan(min);

			// create a new walk plan based on the selected plan
			Plan newPlan = PopulationUtils.createPlan(person);
			List<Activity> activities = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			for (int i = 0; i < activities.size()-1; i++) {
				newPlan.addActivity(activities.get(i));
				newPlan.addLeg(PopulationUtils.createLeg(TransportMode.walk));
			}
			newPlan.addActivity(activities.getLast());

			person.addPlan(newPlan);
		}
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		controler.addOverridingModule(new DeParkingModule());
	}

	private void addParkingSpots(Scenario scenario) {
		Network network = scenario.getNetwork();

		// read csv with parking spots
		// header is linkId,from_time,to_time,length,occupancy,initial
		// for each linkId, get the corresponding link from the network and add an entry in attributes with the occupancy value.
		var links = new HashSet<>(network.getLinks().keySet());

		// assert parkingSpotsFile ends with zst
		if (!this.parkingSpotsFile.endsWith(".zst")) {
			throw new RuntimeException("parkingSpotsFile must end with .zst");
		}

		try (ZstdInputStream zstdInputStream = new ZstdInputStream(Files.newInputStream(Path.of(this.parkingSpotsFile)));
			 Reader r = new InputStreamReader(zstdInputStream)) {
			for (CSVRecord record : CSVFormat.Builder.create().setHeader("linkId", "from_time", "to_time", "length", "occupancy", "initial")
				.setSkipHeaderRecord(true).build().parse(r)) {
				Id<Link> linkId = Id.createLinkId(record.get("linkId"));

				Link link = network.getLinks().get(linkId);
				if (link == null) {
					throw new RuntimeException("Unable to find link with id " + linkId);
				}
				double occupancy = Double.parseDouble(record.get("occupancy"));

				if(occupancy <= 0) {
					throw new RuntimeException("Occupancy for link with id " + linkId + " is <= " +  occupancy + " This strongly suggests that there is an error in your parking spot file.");
				}

				links.remove(linkId);
				link.getAttributes().putAttribute(PARKING_SPOTS_ATTR, occupancy);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// For the links with no entry in the csv, set parkingSpots based on link length
		for (Id<Link> linkId : links) {
			Link link = network.getLinks().get(linkId);
			double parkingSpots = link.getLength() / 7.5 * scenario.getConfig().qsim().getFlowCapFactor(); // assume 7.5m per parking spot
			link.getAttributes().putAttribute(PARKING_SPOTS_ATTR, parkingSpots);
		}
	}
}
