package org.matsim.run.deparking;

import org.apache.commons.lang.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.List;
import java.util.Map;

class ParkingCostHistoryTest {

	@Test
	void cost_singleLink_returnsCorrectBin() {
		// Setup: 1 link, 3 time bins (each 3600s), costs = [1.0, 2.0, 3.0]
		Map<Id<Link>, Integer> linkIndexMap = Map.of(Id.createLinkId("1"), 0);
		double[][] costs = new double[][]{{1.0, 2.0, 3.0}};
		Network network = createNetworkWithLink("1", 100.0);

		ParkingCostHistory history = new ParkingCostHistory(
			linkIndexMap, costs, null, 3600, null, network
		);

		// Bin 0: 0-3600s
		Assertions.assertEquals(1.0, history.cost(Id.createLinkId("1"), 0));
		Assertions.assertEquals(1.0, history.cost(Id.createLinkId("1"), 1800));
		Assertions.assertEquals(1.0, history.cost(Id.createLinkId("1"), 3599));

		// Bin 1: 3600-7200s
		Assertions.assertEquals(2.0, history.cost(Id.createLinkId("1"), 3600));
		Assertions.assertEquals(2.0, history.cost(Id.createLinkId("1"), 5000));

		// Bin 2: 7200-10800s
		Assertions.assertEquals(3.0, history.cost(Id.createLinkId("1"), 7200));
		Assertions.assertEquals(3.0, history.cost(Id.createLinkId("1"), 10000));

		// No bin afterwards
		Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> history.cost(Id.createLinkId("1"), 30000));
	}

	@Test
	void cost_multipleLinks_returnsCorrectCosts() {
		// Setup: 2 links, 2 time bins each
		Map<Id<Link>, Integer> linkIndexMap = Map.of(
			Id.createLinkId("A"), 0,
			Id.createLinkId("B"), 1
		);
		double[][] costs = new double[][]{
			{10.0, 20.0},  // Link A
			{30.0, 40.0}   // Link B
		};
		Network network = createNetworkWithLinks(Map.of("A", 100.0, "B", 100.0));

		ParkingCostHistory history = new ParkingCostHistory(
			linkIndexMap, costs, null, 3600, null, network
		);

		Assertions.assertEquals(10.0, history.cost(Id.createLinkId("A"), 0));
		Assertions.assertEquals(20.0, history.cost(Id.createLinkId("A"), 3600));
		Assertions.assertEquals(30.0, history.cost(Id.createLinkId("B"), 0));
		Assertions.assertEquals(40.0, history.cost(Id.createLinkId("B"), 3600));
	}

	@Test
	void notifyIterationEnds_updatesCostsBasedOnOccupancy() {
		int occupancy = 5;
		double initialCost = 5.0;
		double length = 75.0;
		double sample = 1.0;
		double parkingSpotLength = 7.5;

		Id<Link> linkId = Id.createLinkId("1");
		Map<Id<Link>, Integer> linkIndexMap = Map.of(linkId, 0);
		double[][] costs = new double[][]{{initialCost}};
		Network network = createNetworkWithLink("1", length);  // 75m / 7.5m = 10 parking spots

		// Mock ParkingAnalyzer that returns occupancy of 5 for the full bin
		ParkingAnalyzer mockAnalyzer = new ParkingAnalyzer() {
			@Override
			public List<OccupancyEntry> occupancy(int iteration, Id<Link> link, double from, double to) {
				return List.of(new OccupancyEntry(from, to, occupancy));
			}
		};

		// Simple approach: new cost = relative occupancy * 100
		DeParkingApproach approach = new InverseLinearDeParkingApproach();

		ParkingCostHistory history = new ParkingCostHistory(
			linkIndexMap, costs, mockAnalyzer, 3600, approach, network
		);

		// Simulate iteration end
		history.notifyIterationEnds(new org.matsim.core.controler.events.IterationEndsEvent(null, 0, false));

		// Expected: weightedOccupancy = 5, availableSpots = 75/7.5/1.0 = 10
		// relativeOccupancy = 5/10 = 0.5
		// newCost = 0.5 * 5.0 = 2.5
		double availableSpots = length / parkingSpotLength / sample; //75/7.5/1.0 = 10
		double relativeOccupancy = occupancy / availableSpots; //5/10 = 0.5
		double newCost = relativeOccupancy * initialCost; //0.5 * 5 = 2.5
		Assertions.assertEquals(newCost, history.cost(linkId, 0), 0.001);
	}

	@Test
	void notifyIterationEnds_weightedOccupancy_partialBins() {
		throw new NotImplementedException();
		// Test weighted occupancy when occupancy changes mid-bin
		// Bin: 0-3600s
		// Occupancy: 0-1800s = 2 cars, 1800-3600s = 8 cars
		// Weighted = (1800*2 + 1800*8) / 3600 = (3600 + 14400) / 3600 = 5.0

		Id<Link> linkId = Id.createLinkId("1");
		Map<Id<Link>, Integer> linkIndexMap = Map.of(linkId, 0);
		double[][] costs = new double[][]{{0.0}};
		Network network = createNetworkWithLink("1", 75.0);  // 10 parking spots

		ParkingAnalyzer mockAnalyzer = new ParkingAnalyzer() {
			@Override
			public List<OccupancyEntry> occupancy(int iteration, Id<Link> link, double from, double to) {
				return List.of(
					new OccupancyEntry(0, 1800, 2),
					new OccupancyEntry(1800, 3600, 8)
				);
			}
		};

		// Return weighted occupancy directly as cost for easy verification
		DeParkingApproach approach = (relOcc, prevCost) -> relOcc;

		ParkingCostHistory history = new ParkingCostHistory(
			linkIndexMap, costs, mockAnalyzer, 3600, approach, network
		);

		history.notifyIterationEnds(new org.matsim.core.controler.events.IterationEndsEvent(null, 0, false));

		// weightedOccupancy = 5.0, availableSpots = 10, relativeOccupancy = 0.5
		Assertions.assertEquals(0.5, history.cost(linkId, 0), 0.001);
	}

	@Test
	void notifyIterationEnds_respectsSampleSize() {
		throw new NotImplementedException();
		// With sample = 0.25, available spots should be divided by sample
		// 75m link, 7.5m spots => 10 spots at sample=1.0, but 40 spots at sample=0.25

		Id<Link> linkId = Id.createLinkId("1");
		Map<Id<Link>, Integer> linkIndexMap = Map.of(linkId, 0);
		double[][] costs = new double[][]{{0.0}};
		Network network = createNetworkWithLink("1", 75.0);

		ParkingAnalyzer mockAnalyzer = new ParkingAnalyzer() {
			@Override
			public List<OccupancyEntry> occupancy(int iteration, Id<Link> link, double from, double to) {
				return List.of(new OccupancyEntry(from, to, 10));  // 10 cars parked
			}
		};

		DeParkingApproach approach = (relOcc, prevCost) -> relOcc;

		ParkingCostHistory history = new ParkingCostHistory(
			linkIndexMap, costs, mockAnalyzer, 3600, approach, network
		);

		history.notifyIterationEnds(new org.matsim.core.controler.events.IterationEndsEvent(null, 0, false));

		// availableSpots = 75 / 7.5 / 0.25 = 40
		// relativeOccupancy = 10 / 40 = 0.25
		Assertions.assertEquals(0.25, history.cost(linkId, 0), 0.001);
	}

	private Network createNetworkWithLink(String linkId, double length) {
		return createNetworkWithLinks(Map.of(linkId, length));
	}

	private Network createNetworkWithLinks(Map<String, Double> linksWithLengths) {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();

		Node fromNode = factory.createNode(Id.createNodeId("from"), new Coord(0, 0));
		network.addNode(fromNode);

		int i = 0;
		for (Map.Entry<String, Double> entry : linksWithLengths.entrySet()) {
			Node toNode = factory.createNode(Id.createNodeId("to_" + i), new Coord(entry.getValue(), 0));
			network.addNode(toNode);

			Link link = factory.createLink(Id.createLinkId(entry.getKey()), fromNode, toNode);
			link.setLength(entry.getValue());
			network.addLink(link);
			i++;
		}

		return network;
	}
}
