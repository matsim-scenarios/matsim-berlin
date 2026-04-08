package org.matsim.prepare.network;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "network-add-parking-spots", description = "Add parking spots to the network.")
public class NetworkAddParkingSpots implements MATSimAppCommand {

	@CommandLine.Option(names = "--network", description = "Path to input network", required = true)
	private Path network;

	@CommandLine.Option(names = "--parking-spots", description = "Path to parking spots csv", required = true)
	private Path parkingSpots;

	@CommandLine.Option(names = "--output", description = "Desired output path", required = true)
	private Path output;

	public static void main(String[] args) {
		new NetworkAddParkingSpots().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Network net = NetworkUtils.readNetwork(network.toString());
		List<ParkingSpotEntry> parkingSpotEntries = readParkingSpots();
		// Add parking spots to network
		parkingSpotEntries.forEach(e -> {
			Id<Link> linkId = Id.createLinkId(e.linkId());
			net.getLinks().get(linkId).getAttributes().putAttribute("onstreet_spots", e.onStreet());
			net.getLinks().get(linkId).getAttributes().putAttribute("offstreet_spots", e.offStreet());
		});

		NetworkUtils.writeNetwork(net, output.toString());
		return 0;
	}

	private List<ParkingSpotEntry> readParkingSpots() throws IOException {
		List<ParkingSpotEntry> parkingSpotEntries = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(parkingSpots)) {
			CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord csvRecord : csvParser) {
				String linkId = csvRecord.get("id");
				double onStreet = catchNa(csvRecord.get("onstreet_spots"));
				double offStreet = catchNa(csvRecord.get("offstreet_spots"));
				parkingSpotEntries.add(new ParkingSpotEntry(linkId, onStreet, offStreet));
			}
		}
		return parkingSpotEntries;
	}

	private Double catchNa(String string) {
		if (string.equals("NA")) {
			return 0.0;
		}
		return Double.parseDouble(string);
	}

	private record ParkingSpotEntry(String linkId, double onStreet, double offStreet) {
	}
}
