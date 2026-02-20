package org.matsim.run.deparking;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.run.policies.autofrei.RunAutofreiPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingCostHistory implements IterationEndsListener {
	private static final Logger log = LogManager.getLogger(ParkingCostHistory.class);

	// double array with dimensions [linkIndex][timeBin] to store parking costs for each link and time bin
	private double[][] costs;
	private final Map<Id<Link>, Integer> linkIndexMap = new HashMap<>();
	private final int binSizeInSeconds;
	private final int binCount;
	private final ParkingAnalyzer parkingAnalyzer;
	private final DeParkingApproach deParkingApproach;
	private final Network network;

	// package-private for testing
	ParkingCostHistory(Map<Id<Link>, Integer> linkIndexMap, double[][] costs, ParkingAnalyzer parkingAnalyzer, int binSizeInSeconds, DeParkingApproach deParkingApproach, Network network) {
		this.linkIndexMap.putAll(linkIndexMap);
		this.costs = costs;
		this.binSizeInSeconds = binSizeInSeconds;
		this.binCount = costs[0].length;
		this.parkingAnalyzer = parkingAnalyzer;
		this.deParkingApproach = deParkingApproach;
		this.network = network;
	}

	/// Returns the cost of parking at a given link at a given time in a given iteration.
	public double cost(Id<Link> link, double time) {
		int bin = ((int) time) / binSizeInSeconds;
		return costs[linkIndexMap.get(link)][bin];
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Updating parking costs at the end of iteration {}.", event.getIteration());
		double[][] newCosts = new double[costs.length][binCount];

		// go through all links and calculate new parking costs
		linkIndexMap.forEach((id, linkIndex) -> {
			for (int bin = 0; bin < binCount; bin++) {
				List<ParkingAnalyzer.OccupancyEntry> occupancies = parkingAnalyzer.occupancy(event.getIteration(), id, bin * binSizeInSeconds, (bin + 1) * binSizeInSeconds);
				double weightedOccupancy = occupancies.stream().mapToDouble(o -> (o.toTime() - o.fromTime()) * o.occupancy()).sum() / binSizeInSeconds;
				double availableSpots = (Double) network.getLinks().get(id).getAttributes().getAttribute(RunAutofreiPolicy.PARKING_SPOTS_ATTR);
				double weightedRelativeOccupancy = weightedOccupancy / availableSpots;
				newCosts[linkIndex][bin] = deParkingApproach.newParkingCost(weightedRelativeOccupancy, costs[linkIndex][bin]);
			}
		});

		Path file = Path.of(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parking_costs.csv"));
		try {
			Files.createDirectories(file.getParent());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writeCsv(file);

		// replace old costs with new costs
		costs = newCosts;
	}

	private void writeCsv(Path file) {
		var header = List.of("linkId", "from_time", "to_time", "cost");

		// Use Apache Commons CSV to write the file
		try (var writer = java.nio.file.Files.newBufferedWriter(file);
			 var csvPrinter = org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader(header.toArray(new String[0])).build().print(writer)) {
			for (Id<Link> linkId : network.getLinks().keySet()) {
				double[] costForLink = this.costs[linkIndexMap.get(linkId)];
				for (int bin = 0; bin < binCount; bin++) {
					var row = List.of(
						linkId.toString(),
						String.valueOf(bin * binSizeInSeconds),
						String.valueOf((bin + 1) * binSizeInSeconds),
						String.valueOf(costForLink[bin])
					);
					csvPrinter.printRecord(row);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Factory implements Provider<ParkingCostHistory> {
		private Map<Id<Link>, double[]> initialCosts = new HashMap<>();
		private int binSizeInSeconds = 3600;
		private double parkingSpotLength = 7.5;

		@Inject
		private Network network;

		@Inject
		private Config config;

		@Inject
		private ParkingAnalyzer parkingAnalyzer;

		@Inject
		private DeParkingApproach deParkingApproach;

		public Factory(Map<Id<Link>, double[]> initialCosts, int binSizeInSeconds) {
			this.initialCosts = initialCosts;
			this.binSizeInSeconds = binSizeInSeconds;
		}

		public Factory(Map<Id<Link>, double[]> initialCosts, int binSizeInSeconds, double parkingSpotLength) {
			this.initialCosts = initialCosts;
			this.binSizeInSeconds = binSizeInSeconds;
			this.parkingSpotLength = parkingSpotLength;
		}

		@Override
		public ParkingCostHistory get() {
			int endTime = (int) config.qsim().getEndTime().seconds();
			int binCount = endTime / binSizeInSeconds;

			if (endTime % binSizeInSeconds != 0) {
				throw new IllegalArgumentException("End time must be a multiple of bin size in seconds");
			}

			Map<Id<Link>, Integer> linkIndexMap = new HashMap<>();
			double[][] costs = new double[network.getLinks().size()][binCount];

			// initialize link index map
			int index = 0;
			for (Link link : network.getLinks().values()) {
				linkIndexMap.put(link.getId(), index);
				index++;
			}

			// apply initial costs
			for (Map.Entry<Id<Link>, double[]> entry : initialCosts.entrySet()) {
				Integer idx = linkIndexMap.get(entry.getKey());
				if (idx == null) {
					throw new IllegalArgumentException("Initial costs contain link " + entry.getKey() + " which is not in the network.");
				}
				double[] value = entry.getValue();

				if (value.length != binCount) {
					throw new IllegalArgumentException("Initial costs for link " + entry.getKey() + " have length " + value.length + " but expected " + binCount);
				}

				costs[idx] = value;
			}

			log.info("Created a ParkingCostHistory with {} links and {} time bins.", network.getLinks().size(), binCount);

			return new ParkingCostHistory(linkIndexMap, costs, parkingAnalyzer, binSizeInSeconds, deParkingApproach, network);
		}
	}
}
