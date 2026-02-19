package org.matsim.analysis.autofrei;

import com.google.inject.Injector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.deparking.DeParkingModule;
import org.matsim.run.deparking.ParkingAnalyzer;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Map;

class ParkingAnalyzerTest {
	// One person enters traffic on link 1 at time 0, leaves traffic on link 2 at time 1.0.
	@Test
	void onePerson() {
		VehicleEntersTrafficEvent ve = new VehicleEntersTrafficEvent(1., Id.createPersonId("p"), Id.createLinkId("1"), Id.createVehicleId("p"), "car", 1.0);
		VehicleLeavesTrafficEvent vl = new VehicleLeavesTrafficEvent(1., Id.createPersonId("p"), Id.createLinkId("2"), Id.createVehicleId("p"), "car", 1.0);

		ParkingAnalyzer.ParkingEventHandler peh = ParkingAnalyzer.run((em) -> {
			em.processEvent(ve);
			em.processEvent(vl);
		});

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> actual = peh.getOccupancyChangesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> expected = Map.of(
				Id.createLinkId("1"), List.of(new ParkingAnalyzer.OccupancyChange(0., 1.), new ParkingAnalyzer.OccupancyChange(1.0, -1)),
				Id.createLinkId("2"), List.of(new ParkingAnalyzer.OccupancyChange(1., 1))
			);

			Assertions.assertEquals(expected, actual);
		}

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> actual = peh.getOccupancyEntriesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> expected = Map.of(
				Id.createLinkId("1"), List.of(new ParkingAnalyzer.OccupancyEntry(0., 1.0, 1), new ParkingAnalyzer.OccupancyEntry(1., Double.POSITIVE_INFINITY, 0)),
				Id.createLinkId("2"), List.of(new ParkingAnalyzer.OccupancyEntry(0., 1.0, 0), new ParkingAnalyzer.OccupancyEntry(1.0, Double.POSITIVE_INFINITY, 1))
			);

			Assertions.assertEquals(expected, actual);
		}
	}

	// Two persons depart at a link and than arrive at another link.
	@Test
	void twoPersons_sameRoute() {
		// Person 1: departs from link1 at t=1, arrives at link2 at t=2
		VehicleEntersTrafficEvent ve1 = new VehicleEntersTrafficEvent(1., Id.createPersonId("p1"), Id.createLinkId("1"), Id.createVehicleId("p1"), "car", 1.0);
		VehicleLeavesTrafficEvent vl1 = new VehicleLeavesTrafficEvent(2., Id.createPersonId("p1"), Id.createLinkId("2"), Id.createVehicleId("p1"), "car", 1.0);

		// Person 2: departs from link1 at t=3, arrives at link2 at t=4
		VehicleEntersTrafficEvent ve2 = new VehicleEntersTrafficEvent(3., Id.createPersonId("p2"), Id.createLinkId("1"), Id.createVehicleId("p2"), "car", 1.0);
		VehicleLeavesTrafficEvent vl2 = new VehicleLeavesTrafficEvent(4., Id.createPersonId("p2"), Id.createLinkId("2"), Id.createVehicleId("p2"), "car", 1.0);

		ParkingAnalyzer.ParkingEventHandler peh = ParkingAnalyzer.run((em) -> {
			em.processEvent(ve1);
			em.processEvent(vl1);
			em.processEvent(ve2);
			em.processEvent(vl2);
		});

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> actual = peh.getOccupancyChangesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> expected = Map.of(
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyChange(0., 2.),  // initial: 2 parked cars
					new ParkingAnalyzer.OccupancyChange(1., -1),  // p1 departs
					new ParkingAnalyzer.OccupancyChange(3., -1)   // p2 departs
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyChange(2., 1),   // p1 arrives
					new ParkingAnalyzer.OccupancyChange(4., 1)    // p2 arrives
				)
			);

			Assertions.assertEquals(expected, actual);
		}

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> actual = peh.getOccupancyEntriesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> expected = Map.of(
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 1., 2),   // initial: 2 parked
					new ParkingAnalyzer.OccupancyEntry(1., 3., 1),   // after p1 departs: 1 parked
					new ParkingAnalyzer.OccupancyEntry(3., Double.POSITIVE_INFINITY, 0)  // after p2 departs: 0 parked
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 2., 0),   // initially: 0 parked
					new ParkingAnalyzer.OccupancyEntry(2., 4., 1),   // after p1 arrives: 1 parked
					new ParkingAnalyzer.OccupancyEntry(4., Double.POSITIVE_INFINITY, 2)  // after p2 arrives: 2 parked
				)
			);

			Assertions.assertEquals(expected, actual);
		}
	}

	// Tests that one link with some first departures and intermediate arrivals is correctly handled.
	@Test
	void threePersons() {
		// link1 is of interest
		// person1 departs at link0 at t=1
		VehicleEntersTrafficEvent ve1 = new VehicleEntersTrafficEvent(1., Id.createPersonId("p1"), Id.createLinkId("0"), Id.createVehicleId("p1"), "car", 1.0);
		// person1 arrives at link1 at t=2
		VehicleLeavesTrafficEvent vl1 = new VehicleLeavesTrafficEvent(2., Id.createPersonId("p1"), Id.createLinkId("1"), Id.createVehicleId("p1"), "car", 1.0);
		// person2 departs at link1 at t=3
		VehicleEntersTrafficEvent ve2 = new VehicleEntersTrafficEvent(3., Id.createPersonId("p2"), Id.createLinkId("1"), Id.createVehicleId("p2"), "car", 1.0);
		// person3 departs at link1 at t=4
		VehicleEntersTrafficEvent ve3 = new VehicleEntersTrafficEvent(4., Id.createPersonId("p3"), Id.createLinkId("1"), Id.createVehicleId("p3"), "car", 1.0);
		// person2 arrives at link2 at t=5
		VehicleLeavesTrafficEvent vl2 = new VehicleLeavesTrafficEvent(5., Id.createPersonId("p2"), Id.createLinkId("2"), Id.createVehicleId("p2"), "car", 1.0);
		// person3 arrives at link3 at t=6
		VehicleLeavesTrafficEvent vl3 = new VehicleLeavesTrafficEvent(6., Id.createPersonId("p3"), Id.createLinkId("3"), Id.createVehicleId("p3"), "car", 1.0);

		ParkingAnalyzer.ParkingEventHandler peh = ParkingAnalyzer.run((em) -> {
			em.processEvent(ve1);
			em.processEvent(vl1);
			em.processEvent(ve2);
			em.processEvent(ve3);
			em.processEvent(vl2);
			em.processEvent(vl3);
		});

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> actual = peh.getOccupancyChangesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> expected = Map.of(
				Id.createLinkId("0"), List.of(
					new ParkingAnalyzer.OccupancyChange(0., 1.),  // initial: 1 parked (p1)
					new ParkingAnalyzer.OccupancyChange(1., -1)   // p1 departs
				),
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyChange(0., 2.),  // initial: 2 parked (p2, p3)
					new ParkingAnalyzer.OccupancyChange(2., 1),   // p1 arrives
					new ParkingAnalyzer.OccupancyChange(3., -1),  // p2 departs
					new ParkingAnalyzer.OccupancyChange(4., -1)   // p3 departs
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyChange(5., 1)    // p2 arrives
				),
				Id.createLinkId("3"), List.of(
					new ParkingAnalyzer.OccupancyChange(6., 1)    // p3 arrives
				)
			);

			Assertions.assertEquals(expected, actual);
		}

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> actual = peh.getOccupancyEntriesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> expected = Map.of(
				Id.createLinkId("0"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 1., 1),   // initial: 1 parked
					new ParkingAnalyzer.OccupancyEntry(1., Double.POSITIVE_INFINITY, 0)  // after p1 departs: 0
				),
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 2., 2),   // initial: 2 parked (p2, p3)
					new ParkingAnalyzer.OccupancyEntry(2., 3., 3),   // after p1 arrives: 3
					new ParkingAnalyzer.OccupancyEntry(3., 4., 2),   // after p2 departs: 2
					new ParkingAnalyzer.OccupancyEntry(4., Double.POSITIVE_INFINITY, 1)  // after p3 departs: 1
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 5., 0),   // initially: 0
					new ParkingAnalyzer.OccupancyEntry(5., Double.POSITIVE_INFINITY, 1)  // after p2 arrives: 1
				),
				Id.createLinkId("3"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 6., 0),   // initially: 0
					new ParkingAnalyzer.OccupancyEntry(6., Double.POSITIVE_INFINITY, 1)  // after p3 arrives: 1
				)
			);

			Assertions.assertEquals(expected, actual);
		}
	}

	// Two persons depart at a link and than arrive at another link with different modes.
	@Test
	void twoPersons_differentModes() {
		// Person 1: uses "car" mode, departs from link1 at t=1, arrives at link2 at t=2
		VehicleEntersTrafficEvent ve1 = new VehicleEntersTrafficEvent(1., Id.createPersonId("p1"), Id.createLinkId("1"), Id.createVehicleId("p1"), "car", 1.0);
		VehicleLeavesTrafficEvent vl1 = new VehicleLeavesTrafficEvent(2., Id.createPersonId("p1"), Id.createLinkId("2"), Id.createVehicleId("p1"), "car", 1.0);

		// Person 2: uses "truck" mode, departs from link1 at t=3, arrives at link2 at t=4
		VehicleEntersTrafficEvent ve2 = new VehicleEntersTrafficEvent(3., Id.createPersonId("p2"), Id.createLinkId("1"), Id.createVehicleId("p2"), "truck", 1.0);
		VehicleLeavesTrafficEvent vl2 = new VehicleLeavesTrafficEvent(4., Id.createPersonId("p2"), Id.createLinkId("2"), Id.createVehicleId("p2"), "truck", 1.0);

		ParkingAnalyzer.ParkingEventHandler peh = ParkingAnalyzer.run((em) -> {
			em.processEvent(ve1);
			em.processEvent(vl1);
			em.processEvent(ve2);
			em.processEvent(vl2);
		});

		{
			// Both modes contribute to occupancy changes on the same links
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> actual = peh.getOccupancyChangesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> expected = Map.of(
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyChange(0., 2.),  // initial: 2 parked (1 car, 1 truck)
					new ParkingAnalyzer.OccupancyChange(1., -1),  // p1 (car) departs
					new ParkingAnalyzer.OccupancyChange(3., -1)   // p2 (truck) departs
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyChange(2., 1),   // p1 (car) arrives
					new ParkingAnalyzer.OccupancyChange(4., 1)    // p2 (truck) arrives
				)
			);

			Assertions.assertEquals(expected, actual);
		}

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> actual = peh.getOccupancyEntriesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> expected = Map.of(
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 1., 2),   // initial: 2 parked
					new ParkingAnalyzer.OccupancyEntry(1., 3., 1),   // after p1 departs: 1
					new ParkingAnalyzer.OccupancyEntry(3., Double.POSITIVE_INFINITY, 0)  // after p2 departs: 0
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 2., 0),   // initially: 0
					new ParkingAnalyzer.OccupancyEntry(2., 4., 1),   // after p1 arrives: 1
					new ParkingAnalyzer.OccupancyEntry(4., Double.POSITIVE_INFINITY, 2)  // after p2 arrives: 2
				)
			);

			Assertions.assertEquals(expected, actual);
		}
	}

	// Tests that simultaneous arrival and departure on the same link are handled correctly.
	@Test
	void sameTimeEnterLeave() {
		// p1 enters l1 at t=1
		VehicleEntersTrafficEvent ve1 = new VehicleEntersTrafficEvent(1., Id.createPersonId("p1"), Id.createLinkId("1"), Id.createVehicleId("p1"), "car", 1.0);
		// p1 leaves l2 at t=2, p2 enters l2 at t=2 (same time)
		VehicleLeavesTrafficEvent vl1 = new VehicleLeavesTrafficEvent(2., Id.createPersonId("p1"), Id.createLinkId("2"), Id.createVehicleId("p1"), "car", 1.0);
		VehicleEntersTrafficEvent ve2 = new VehicleEntersTrafficEvent(2., Id.createPersonId("p2"), Id.createLinkId("2"), Id.createVehicleId("p2"), "car", 1.0);
		// p2 leaves l3 at t=3
		VehicleLeavesTrafficEvent vl2 = new VehicleLeavesTrafficEvent(3., Id.createPersonId("p2"), Id.createLinkId("3"), Id.createVehicleId("p2"), "car", 1.0);

		ParkingAnalyzer.ParkingEventHandler peh = ParkingAnalyzer.run((em) -> {
			em.processEvent(ve1);
			em.processEvent(vl1);
			em.processEvent(ve2);
			em.processEvent(vl2);
		});

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> actual = peh.getOccupancyChangesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyChange>> expected = Map.of(
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyChange(0., 1.),  // initial: 1 parked (p1)
					new ParkingAnalyzer.OccupancyChange(1., -1)   // p1 departs
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyChange(0., 1.),  // initial: 1 parked (p2)
					new ParkingAnalyzer.OccupancyChange(2., 1),   // p1 arrives
					new ParkingAnalyzer.OccupancyChange(2., -1)   // p2 departs (same time as p1 arrives)
				),
				Id.createLinkId("3"), List.of(
					new ParkingAnalyzer.OccupancyChange(3., 1)    // p2 arrives
				)
			);

			Assertions.assertEquals(expected, actual);
		}

		{
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> actual = peh.getOccupancyEntriesByLink();
			Map<Id<Link>, List<ParkingAnalyzer.OccupancyEntry>> expected = Map.of(
				Id.createLinkId("1"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 1., 1),   // initial: 1 parked
					new ParkingAnalyzer.OccupancyEntry(1., Double.POSITIVE_INFINITY, 0)  // after p1 departs: 0
				),
				Id.createLinkId("2"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 2., 1),   // initial: 1 parked (p2)
					new ParkingAnalyzer.OccupancyEntry(2., Double.POSITIVE_INFINITY, 1)  // p1 arrives (+1) and p2 departs (-1): net 0 change
				),
				Id.createLinkId("3"), List.of(
					new ParkingAnalyzer.OccupancyEntry(0., 3., 0),   // initially: 0
					new ParkingAnalyzer.OccupancyEntry(3., Double.POSITIVE_INFINITY, 1)  // after p2 arrives: 1
				)
			);

			Assertions.assertEquals(expected, actual);
		}
	}

	@Test
	void occupancyCalculation_empty() {
		List<ParkingAnalyzer.OccupancyEntry> occupancyEntriesInTimeBin = ParkingAnalyzer.getOccupancyEntriesInTimeBin(0, 200, List.of());
		Assertions.assertEquals(List.of(), occupancyEntriesInTimeBin);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_fullyIncluded() {
		List<ParkingAnalyzer.OccupancyEntry> occupancyEntriesInTimeBin = ParkingAnalyzer.getOccupancyEntriesInTimeBin(0, 200, List.of(new ParkingAnalyzer.OccupancyEntry(1, 199, 42)));
		Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(1, 199, 42)), occupancyEntriesInTimeBin);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_overlapsFrom() {
		// Entry starts before 'from', ends within [from, to] → shrunk to start at 'from'
		// Time bin: [100, 300], Entry: [50, 200] → Expected: [100, 200]
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300,
			List.of(new ParkingAnalyzer.OccupancyEntry(50, 200, 5)));
		Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(100, 200, 5)), result);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_overlapsTo() {
		// Entry starts within [from, to], ends after 'to' → shrunk to end at 'to'
		// Time bin: [100, 300], Entry: [200, 400] → Expected: [200, 300]
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300,
			List.of(new ParkingAnalyzer.OccupancyEntry(200, 400, 7)));
		Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(200, 300, 7)), result);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_overlapsCompletely() {
		// Entry spans beyond both boundaries → shrunk to [from, to]
		// Time bin: [100, 300], Entry: [50, 400] → Expected: [100, 300]
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300,
			List.of(new ParkingAnalyzer.OccupancyEntry(50, 400, 10)));
		Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(100, 300, 10)), result);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_completelyBefore() {
		// Entry ends at or before 'from' → excluded
		// Time bin: [100, 300], Entry: [0, 100] → Expected: empty (toTime <= from)
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300,
			List.of(new ParkingAnalyzer.OccupancyEntry(0, 100, 3)));
		Assertions.assertEquals(List.of(), result);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_completelyAfter() {
		// Entry starts at or after 'to' → excluded
		// Time bin: [100, 300], Entry: [300, 400] → Expected: empty (fromTime >= to)
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300,
			List.of(new ParkingAnalyzer.OccupancyEntry(300, 400, 4)));
		Assertions.assertEquals(List.of(), result);
	}

	@Test
	void occupancyCalculation_oneOccupancyEntry_exactMatch() {
		// Entry exactly matches time bin boundaries
		// Time bin: [100, 300], Entry: [100, 300] → Expected: [100, 300]
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300,
			List.of(new ParkingAnalyzer.OccupancyEntry(100, 300, 8)));
		Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(100, 300, 8)), result);
	}

	@Test
	void occupancyCalculation_multipleEntries_mixedOverlaps() {
		// Multiple entries: some excluded, some included, some shrunk
		// Time bin: [100, 300]
		// Entry 1: [0, 50] → excluded (completely before)
		// Entry 2: [50, 150] → shrunk to [100, 150]
		// Entry 3: [150, 200] → included as-is
		// Entry 4: [250, 350] → shrunk to [250, 300]
		// Entry 5: [400, 500] → excluded (completely after)
		List<ParkingAnalyzer.OccupancyEntry> result = ParkingAnalyzer.getOccupancyEntriesInTimeBin(100, 300, List.of(
			new ParkingAnalyzer.OccupancyEntry(0, 50, 1),
			new ParkingAnalyzer.OccupancyEntry(50, 150, 2),
			new ParkingAnalyzer.OccupancyEntry(150, 200, 3),
			new ParkingAnalyzer.OccupancyEntry(250, 350, 4),
			new ParkingAnalyzer.OccupancyEntry(400, 500, 5)
		));
		Assertions.assertEquals(List.of(
			new ParkingAnalyzer.OccupancyEntry(100, 150, 2),
			new ParkingAnalyzer.OccupancyEntry(150, 200, 3),
			new ParkingAnalyzer.OccupancyEntry(250, 300, 4)
		), result);
	}

	static class IntegrationTest {
		@RegisterExtension
		MatsimTestUtils matsim = new MatsimTestUtils();

		@Test
		void occupancyCalculation() {
			Config config = ConfigUtils.loadConfig("input/v6.4/berlin-v6.4.config.xml");
			config.controller().setOutputDirectory(matsim.getOutputDirectory());
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

			// turn off. It complains about missing scoring types in config, but we are not scoring anything here.
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);

			// prepare network
			Scenario scenario = ScenarioUtils.createScenario(config);
			Node n1 = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("n1"), new Coord());
			Node n2 = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("n2"), new Coord());
			NetworkUtils.createAndAddLink(scenario.getNetwork(), Id.createLinkId("l1"), n1, n2, 0., 0., 0, 0.);

			Controller controller = ControllerUtils.createController(scenario);
			controller.addOverridingModule(new DeParkingModule());

			Injector injector = controller.getInjector();

			ParkingAnalyzer instance = injector.getInstance(ParkingAnalyzer.class);
			EventsManager events = injector.getInstance(EventsManager.class);

			// replay simulation
			instance.notifyIterationStarts(new IterationStartsEvent(null, 0, false));

			VehicleEntersTrafficEvent ve = new VehicleEntersTrafficEvent(1., Id.createPersonId("p"), Id.createLinkId("l1"), Id.createVehicleId("p"), "car", 1.0);
			VehicleLeavesTrafficEvent vl = new VehicleLeavesTrafficEvent(1., Id.createPersonId("p"), Id.createLinkId("l2"), Id.createVehicleId("p"), "car", 1.0);
			events.processEvent(ve);
			events.processEvent(vl);

			instance.notifyAfterMobsim(new AfterMobsimEvent(null, 0, false));

			{
				List<ParkingAnalyzer.OccupancyEntry> occupancy1 = instance.occupancy(0, Id.createLinkId("l1"), 0., 3600.);
				List<ParkingAnalyzer.OccupancyEntry> occupancy2 = instance.occupancy(0, Id.createLinkId("l2"), 0., 3600.);

				Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(0., 1., 1.), new ParkingAnalyzer.OccupancyEntry(1., 3600., 0.)), occupancy1);
				Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(0., 1., 0.), new ParkingAnalyzer.OccupancyEntry(1., 3600., 1.)), occupancy2);
			}

			{
				List<ParkingAnalyzer.OccupancyEntry> occupancy1 = instance.occupancy(0, Id.createLinkId("l1"), 3600., 3600. * 2);
				List<ParkingAnalyzer.OccupancyEntry> occupancy2 = instance.occupancy(0, Id.createLinkId("l2"), 3600., 3600. * 2);
				Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(3600., 7200., 0.)), occupancy1);
				Assertions.assertEquals(List.of(new ParkingAnalyzer.OccupancyEntry(3600., 7200., 1.)), occupancy2);
			}
		}
	}
}
