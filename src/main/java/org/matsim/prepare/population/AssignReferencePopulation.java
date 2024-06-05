package org.matsim.prepare.population;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "assign-reference-population",
	description = "Assigns persons from reference data to a population."
)
public class AssignReferencePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AssignReferencePopulation.class);


	@CommandLine.Option(names = "--population", description = "Input population path.", required = true)
	private String populationPath;

	@CommandLine.Option(names = "--persons", description = "Input persons from survey data, in matsim-python-tools format.", required = true)
	private Path personsPath;

	@CommandLine.Option(names = "--activities", description = "Path to activity table from survey data.", required = true)
	private Path activityPath;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file.", required = true)
	private Path facilityPath;

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--output", description = "Output population path.", required = true)
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp;

	private FacilityIndex facilities;

	private PersonMatcher persons;

	public static void main(String[] args) {
		new AssignReferencePopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			log.error("No shapefile defined. Please specify a shapefile for the zones using the --shp option.");
			return 2;
		}

		if (!Files.exists(activityPath)) {
			log.error("Input activity file does not exist: {}", activityPath);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(populationPath);

		SplittableRandom rnd = new SplittableRandom(0);
		persons = new PersonMatcher("idx", personsPath);
		facilities = new FacilityIndex(facilityPath.toString());

		PlanBuilder planBuilder = new PlanBuilder(shp, facilities, activityPath);

		Long2ObjectMap<List<Person>> homeIndex = planBuilder.createHomeIndex(population);

		// Remove persons without legs, these can not be assigned
		for (List<Person> list : homeIndex.values()) {
			list.removeIf(p -> TripStructureUtils.getLegs(p.getSelectedPlan()).isEmpty());
		}

		RunActivitySampling sampling = new RunActivitySampling(persons, planBuilder.getActivities(), population.getFactory(), 1);

		int i = 0;
		for (Map.Entry<String, CSVRecord> e : ProgressBar.wrap(persons, "Assigning reference population")) {

			CSVRecord p = e.getValue();
			if (!p.get("seq").equals("0"))
				continue;

			if (p.get("mobile_on_day").equals("false"))
				continue;

			long zone = planBuilder.findHomeZone(e.getKey());

			// No home zone known
			if (zone < 0)
				continue;

			List<Person> refPersons = homeIndex.get(zone);

			if (refPersons == null)
				continue;

			Person person = persons.matchEntry(e.getValue(), refPersons, rnd);

			// No persons matched
			if (person == null)
				continue;

			// Create the base daily plan (without locations)
			Coord homeCoord = Attributes.getHomeCoord(person);
			Plan plan = sampling.createPlan(homeCoord, e.getKey());

			boolean success = planBuilder.assignLocationsFromZones(e.getKey(), plan, homeCoord);

			if (success) {
				sampling.copyAttributes(p, person);
				person.getAttributes().putAttribute(Attributes.REF_WEIGHT, p.get("p_weight"));
				person.removePlan(person.getSelectedPlan());
				person.addPlan(plan);
				person.setSelectedPlan(plan);

				String refModes = TripStructureUtils.getLegs(plan).stream().map(Leg::getMode).collect(Collectors.joining("-"));
				person.getAttributes().putAttribute(Attributes.REF_MODES, refModes);

				// remove person that have been used as reference
				refPersons.remove(person);
				i++;
			}
		}

		log.info("Assigned {}/{} reference persons", i, population.getPersons().size());

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
