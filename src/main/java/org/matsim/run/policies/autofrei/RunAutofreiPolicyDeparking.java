package org.matsim.run.policies.autofrei;

import com.github.luben.zstd.ZstdInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.controler.Controler;
import org.matsim.run.deparking.DeParkingModule;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

public class RunAutofreiPolicyDeparking extends RunAutofreiPolicy {
	public static final String PARKING_SPOTS_ATTR = "parkingSpots";

	@CommandLine.Option(names = "--parking-spots")
	private String parkingSpotsFile;

	@CommandLine.Option(names = "--write-interval")
	private int writeInterval = -1;

	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiPolicyDeparking.class, args);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		addParkingSpots(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		if (writeInterval > 0) {
			controler.addOverridingModule(new DeParkingModule(writeInterval));
		} else {
			controler.addOverridingModule(new DeParkingModule());
		}
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
				links.remove(linkId);

				Link link = network.getLinks().get(linkId);
				if (link == null) {
					throw new RuntimeException("Unable to find link with id " + linkId);
				}
				link.getAttributes().putAttribute(PARKING_SPOTS_ATTR, Double.parseDouble(record.get("occupancy")));
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
