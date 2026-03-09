package org.matsim.run.deparking;

import com.google.inject.Injector;
import com.google.inject.Provider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControllerListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.dsim.ExecutionContext;
import org.matsim.run.policies.autofrei.RunAutofreiPolicyDeparking;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for ParkingCostHistory. Focuses on cost retrieval and cost updates based on occupancy.
 * The test assumes 7.5m parking spots, which are set as an attribute on the links in the test network.
 */
class ParkingCostTrackerTest {
	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void cost_singleLink_returnsCorrectBin() {
		// Setup: 1 link, 3 time bins (each 3600s), initialCosts = [1.0, 2.0, 3.0]
		Map<Id<Link>, Integer> linkIndexMap = Map.of(Id.createLinkId("1"), 0);
		double[][] initialCosts = new double[][]{{1.0, 2.0, 3.0}};
		Network network = createNetworkWithLink("1", 100.0);

		ParkingCostTracker history = new ParkingCostTracker(
			linkIndexMap, initialCosts, null, 3600, null, network, 1
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

		ParkingCostTracker history = new ParkingCostTracker(
			linkIndexMap, costs, null, 3600, null, network, 1
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

		ParkingCostTracker history = new ParkingCostTracker(
			linkIndexMap, costs, mockAnalyzer, 3600, approach, network, 1
		);

		// Simulate iteration end
		history.notifyIterationEnds(new org.matsim.core.controler.events.IterationEndsEvent(new MockMatsimTestServices(utils.getOutputDirectory()), 0, false));

		double availableSpots = length / parkingSpotLength; //75/7.5 = 10
		double relativeOccupancy = occupancy / availableSpots; //5/10 = 0.5
		double newCost = approach.newParkingCost(relativeOccupancy, initialCost);
		Assertions.assertEquals(3.75, newCost);
		Assertions.assertEquals(newCost, history.cost(linkId, 0), 0.001);
	}

	@Test
	void notifyIterationEnds_weightedOccupancy_partialBins() {
		double initialCost = 5.0;
		double length = 75.0;

		Id<Link> linkId = Id.createLinkId("1");
		Map<Id<Link>, Integer> linkIndexMap = Map.of(linkId, 0);
		double[][] costs = new double[][]{{initialCost}};
		Network network = createNetworkWithLink("1", length); // 10 available spots

		// Bin 0 [0,3600): occupancy is 2 for the first half and 8 for the second half.
		ParkingAnalyzer mockAnalyzer = new ParkingAnalyzer() {
			@Override
			public List<OccupancyEntry> occupancy(int iteration, Id<Link> link, double from, double to) {
				return List.of(
					new OccupancyEntry(from, from + 1800, 2),
					new OccupancyEntry(from + 1800, to, 8)
				);
			}
		};

		DeParkingApproach approach = new InverseLinearDeParkingApproach();
		ParkingCostTracker history = new ParkingCostTracker(
			linkIndexMap, costs, mockAnalyzer, 3600, approach, network, 1
		);

		history.notifyIterationEnds(new org.matsim.core.controler.events.IterationEndsEvent(new MockMatsimTestServices(utils.getOutputDirectory()), 0, false));

		// weightedOccupancy = (1800*2 + 1800*8) / 3600 = 5
		// relativeOccupancy = 5 / 10 = 0.5
		// newCost = approach.newParkingCost(0.5, 5.0) = 3.75
		Assertions.assertEquals(3.75, history.cost(linkId, 0), 0.001);
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
			link.getAttributes().putAttribute(RunAutofreiPolicyDeparking.PARKING_SPOTS_ATTR, entry.getValue() / 7.5); // 7.5m per parking spot
			network.addLink(link);
			i++;
		}

		return network;
	}

	private record MockMatsimTestServices(String testDir) implements MatsimServices {

		@Override
		public IterationStopWatch getStopwatch() {
			return null;
		}

		@Override
		public TravelTime getLinkTravelTimes() {
			return null;
		}

		@Override
		public Provider<TripRouter> getTripRouterProvider() {
			return null;
		}

		@Override
		public TravelDisutility createTravelDisutilityCalculator() {
			return null;
		}

		@Override
		public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
			return null;
		}

		@Override
		public ScoringFunctionFactory getScoringFunctionFactory() {
			return null;
		}

		@Override
		public Config getConfig() {
			return null;
		}

		@Override
		public Scenario getScenario() {
			return null;
		}

		@Override
		public EventsManager getEvents() {
			return null;
		}

		@Override
		public Injector getInjector() {
			return null;
		}

		@Override
		public CalcLinkStats getLinkStats() {
			return null;
		}

		@Override
		public VolumesAnalyzer getVolumes() {
			return null;
		}

		@Override
		public ScoreStats getScoreStats() {
			return null;
		}

		@Override
		public TravelDisutilityFactory getTravelDisutilityFactory() {
			return null;
		}

		@Override
		public StrategyManager getStrategyManager() {
			return null;
		}

		@Override
		public OutputDirectoryHierarchy getControllerIO() {
			return null;
		}

		@Override
		public OutputDirectoryHierarchy getControlerIO() {
			return new OutputDirectoryHierarchy(testDir, OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles, ControllerConfigGroup.CompressionType.none);
		}

		@Override
		public ExecutionContext getSimulationContext() {
			return null;
		}

		@Override
		public void addControllerListener(ControllerListener controllerListener) {

		}

		@Override
		public Integer getIterationNumber() {
			return 0;
		}
	}
}
