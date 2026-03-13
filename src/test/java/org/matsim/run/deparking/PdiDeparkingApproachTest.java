package org.matsim.run.deparking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.run.policies.autofrei.RunAutofreiPolicyDeparking;

import java.util.List;
import java.util.Map;

class PdiDeparkingApproachTest {
	@Test
	void newParkingCost_returnsDirectControllerOutputForKnownOccupancy() {
		Config config = createConfig(2.0, 0.25, 0.5, 20.0);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);

		double newCost = approach.newParkingCost(fullBinAnalyzer(15.0), Id.createLinkId("1"), 0, 0.0, 3600.0);

		Assertions.assertEquals(1.375, newCost, 1e-9);
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.0, 7.0, 10.0})
	void newParkingCost_clampsToZero(double occupancy) {
		Config config = createConfig(2.0, 0.25, 0.5, 20.0);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);

		double newCost = approach.newParkingCost(fullBinAnalyzer(occupancy), Id.createLinkId("1"), 0, 0.0, 3600.0);

		Assertions.assertEquals(0.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_clampsToMaxCost() {
		Config config = createConfig(2.0, 0.25, 0.5, 1.0);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);

		double newCost = approach.newParkingCost(fullBinAnalyzer(Double.POSITIVE_INFINITY), Id.createLinkId("1"), 0, 0.0, 3600.0);

		Assertions.assertEquals(1.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_usesConfiguredTargetRelativeOccupancy() {
		Config config = createConfig(2.0, 0.25, 0.5, 20.0);
		ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class).setTargetRelativeOccupancy(1.5);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);

		double newCost = approach.newParkingCost(fullBinAnalyzer(15.0), Id.createLinkId("1"), 0, 0.0, 3600.0);

		Assertions.assertEquals(0.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_accumulatesIntegralAcrossIterationsForSameLinkAndBin() {
		Config config = createConfig(0.0, 1.0, 0.0, 20.0);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);
		ParkingAnalyzer analyzer = fullBinAnalyzer(15.0);
		Id<Link> linkId = Id.createLinkId("1");

		double firstCost = approach.newParkingCost(analyzer, linkId, 0, 0.0, 3600.0);
		double secondCost = approach.newParkingCost(analyzer, linkId, 1, 0.0, 3600.0);

		Assertions.assertEquals(0.5, firstCost, 1e-9);
		Assertions.assertEquals(1.0, secondCost, 1e-9);
	}

	@Test
	void newParkingCost_resetOnZeroClearsIntegralNearTarget() {
		Config config = createConfig(0.0, 1.0, 0.0, 20.0);
		DeparkingConfigGroup deparkingConfigGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		deparkingConfigGroup.setIntegralApproach(DeparkingConfigGroup.IntegralApproach.RESET_ON_ZERO);
		deparkingConfigGroup.setResetOnZeroThreshold(0.1);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);
		Id<Link> linkId = Id.createLinkId("1");

		double firstCost = approach.newParkingCost(fullBinAnalyzer(15.0), linkId, 0, 0.0, 3600.0);
		double resetCost = approach.newParkingCost(fullBinAnalyzer(10.5), linkId, 1, 0.0, 3600.0);

		Assertions.assertEquals(0.5, firstCost, 1e-9);
		Assertions.assertEquals(0.0, resetCost, 1e-9);
	}

	@Test
	void newParkingCost_smoothingDecayShortensIntegralMemory() {
		Config config = createConfig(0.0, 1.0, 0.0, 20.0);
		DeparkingConfigGroup deparkingConfigGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		deparkingConfigGroup.setIntegralApproach(DeparkingConfigGroup.IntegralApproach.SMOOTHING);
		deparkingConfigGroup.setSmoothAlpha(0.5);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);
		Id<Link> linkId = Id.createLinkId("1");

		double firstCost = approach.newParkingCost(fullBinAnalyzer(15.0), linkId, 0, 0.0, 3600.0);
		double secondCost = approach.newParkingCost(fullBinAnalyzer(15.0), linkId, 1, 0.0, 3600.0);
		double thirdCost = approach.newParkingCost(fullBinAnalyzer(10.0), linkId, 2, 0.0, 3600.0);

		Assertions.assertEquals(0.5, firstCost, 1e-9);
		Assertions.assertEquals(0.75, secondCost, 1e-9);
		Assertions.assertEquals(0.375, thirdCost, 1e-9);
	}

	@Test
	void newParkingCost_usesDerivativeWhenOccupancyChanges() {
		Config config = createConfig(0.0, 0.0, 1.0, 20.0);
		Network network = createNetworkWithSpots(Map.of("1", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);
		Id<Link> linkId = Id.createLinkId("1");

		double baselineCost = approach.newParkingCost(fullBinAnalyzer(10.0), linkId, 0, 0.0, 3600.0);
		double changedCost = approach.newParkingCost(fullBinAnalyzer(20.0), linkId, 1, 0.0, 3600.0);
		double repeatedCost = approach.newParkingCost(fullBinAnalyzer(20.0), linkId, 2, 0.0, 3600.0);

		Assertions.assertEquals(0.0, baselineCost, 1e-9);
		Assertions.assertEquals(1.0, changedCost, 1e-9);
		Assertions.assertEquals(0.0, repeatedCost, 1e-9);
	}

	@Test
	void newParkingCost_keepsCachesIsolatedByLinkAndBin() {
		Config config = createConfig(0.0, 1.0, 0.0, 20.0);
		Network network = createNetworkWithSpots(Map.of("A", 10.0, "B", 10.0));
		PdiDeparkingApproach approach = new PdiDeparkingApproach(config, network);
		ParkingAnalyzer analyzer = new ParkingAnalyzer() {
			@Override
			public List<OccupancyEntry> occupancy(int iteration, Id<Link> linkId, double from, double to) {
				return List.of(new OccupancyEntry(from, to, 20.0));
			}
		};

		double firstLinkFirstBin = approach.newParkingCost(analyzer, Id.createLinkId("A"), 0, 0.0, 3600.0);
		double sameLinkSameBinNextIteration = approach.newParkingCost(analyzer, Id.createLinkId("A"), 1, 0.0, 3600.0);
		double otherLinkFirstBin = approach.newParkingCost(analyzer, Id.createLinkId("B"), 1, 0.0, 3600.0);
		double sameLinkOtherBin = approach.newParkingCost(analyzer, Id.createLinkId("A"), 1, 3600.0, 7200.0);

		Assertions.assertEquals(1.0, firstLinkFirstBin, 1e-9);
		Assertions.assertEquals(2.0, sameLinkSameBinNextIteration, 1e-9);
		Assertions.assertEquals(1.0, otherLinkFirstBin, 1e-9);
		Assertions.assertEquals(1.0, sameLinkOtherBin, 1e-9);
	}

	private ParkingAnalyzer fullBinAnalyzer(double occupancy) {
		return new ParkingAnalyzer() {
			@Override
			public List<OccupancyEntry> occupancy(int iteration, Id<Link> linkId, double from, double to) {
				return List.of(new OccupancyEntry(from, to, occupancy));
			}
		};
	}

	private Config createConfig(double kP, double kI, double kD, double maxCost) {
		Config config = ConfigUtils.createConfig();
		DeparkingConfigGroup deparkingConfigGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		deparkingConfigGroup.setK_p(kP);
		deparkingConfigGroup.setK_i(kI);
		deparkingConfigGroup.setK_d(kD);
		deparkingConfigGroup.setMaxCost(maxCost);
		deparkingConfigGroup.setTargetRelativeOccupancy(1.0);
		deparkingConfigGroup.setIntegralApproach(DeparkingConfigGroup.IntegralApproach.ALL);
		deparkingConfigGroup.setSmoothAlpha(0.5);
		deparkingConfigGroup.setResetOnZeroThreshold(0.1);
		return config;
	}

	private Network createNetworkWithSpots(Map<String, Double> linksWithSpots) {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();

		Node fromNode = factory.createNode(Id.createNodeId("from"), new Coord(0, 0));
		network.addNode(fromNode);

		int i = 0;
		for (Map.Entry<String, Double> entry : linksWithSpots.entrySet()) {
			Node toNode = factory.createNode(Id.createNodeId("to_" + i), new Coord(i + 1, 0));
			network.addNode(toNode);

			Link link = factory.createLink(Id.createLinkId(entry.getKey()), fromNode, toNode);
			link.setLength(entry.getValue() * 7.5);
			link.getAttributes().putAttribute(RunAutofreiPolicyDeparking.PARKING_SPOTS_ATTR, entry.getValue());
			network.addLink(link);
			i++;
		}

		return network;
	}
}
