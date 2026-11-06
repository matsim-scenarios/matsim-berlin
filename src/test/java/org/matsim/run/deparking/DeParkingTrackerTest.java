package org.matsim.run.deparking;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.run.deparking.DeparkingConfigGroup.FirstAndLastParkingCharging;
import static org.matsim.run.deparking.DeparkingConfigGroup.PenaltyEventType;

class DeParkingTrackerTest {
	private static final Id<Person> PERSON_ID = Id.createPersonId("driver");
	private static final Id<Vehicle> VEHICLE_ID = Id.createVehicleId("car");
	private static final Id<Link> PARKING_LINK_ID = Id.createLinkId("parking");
	private static final Id<Link> DEPARTURE_LINK_ID = Id.createLinkId("departure");
	private static final double END_TIME = 10800.0;

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void chargeParking_emitsSelectedEventWithParkingLink(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 2.0);

		park(fixture, 1800.0);
		depart(fixture, 7200.0);

		// 1,5h parking => -3.0
		assertPenalty(fixture, eventType, 7200.0, -3.0, "car parking cost");
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void chargeParking_withZeroRate_emitsNoEvent(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 0.0);

		park(fixture, 1800.0);
		depart(fixture, 7200.0);

		// hourly costs = 0 => no parking penalty
		assertTrue(fixture.events().isEmpty());
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void chargeParking_withZeroDuration_emitsNoEvent(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 2.0);

		park(fixture, 1800.0);
		depart(fixture, 1800.0);

		// 0s parking => no parking penalty
		assertTrue(fixture.events().isEmpty());
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void chargeParking_forUntrackedActivity_emitsNoEvent(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 2.0);

		park(fixture, 1800.0);
		fixture.tracker().handleEvent(new ActivityStartEvent(1801.0, PERSON_ID, PARKING_LINK_ID, null, "ignored interaction", null));
		depart(fixture, 7200.0);

		// Untracked activity => no parking penalty
		assertTrue(fixture.events().isEmpty());
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void firstParking_isChargedAndLabeled(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 2.0);

		fixture.tracker().handleEvent(new VehicleEntersTrafficEvent(5400.0, PERSON_ID, PARKING_LINK_ID, VEHICLE_ID, TransportMode.car, 1.0));

		// first parking 1.5h => -3.0 parking penalty
		assertPenalty(fixture, eventType, 5400.0, -3.0, "car parking cost first");
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void lastParking_isChargedAndLabeledAtSimulationEnd(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 2.0);

		park(fixture, 5400.0);
		fixture.tracker().notifyMobsimAfterSimStep(new MobsimAfterSimStepEvent<>(null, END_TIME - 1.0));

		// No costs applied before end of simulation
		assertTrue(fixture.events().isEmpty());
		fixture.tracker().notifyMobsimAfterSimStep(new MobsimAfterSimStepEvent<>(null, END_TIME));

		// parking 1.5h => -3.0 parking costs
		assertPenalty(fixture, eventType, END_TIME, -3.0, "car parking cost last");
	}

	@ParameterizedTest
	@EnumSource(PenaltyEventType.class)
	void firstAndLastParking_canBeIgnored(PenaltyEventType eventType) {
		Fixture fixture = createFixture(eventType, 2.0);
		ConfigUtils.addOrGetModule(fixture.tracker().config, DeparkingConfigGroup.class)
			.setFirstAndLastParkingCharging(FirstAndLastParkingCharging.IGNORE);

		fixture.tracker().handleEvent(new VehicleEntersTrafficEvent(1800.0, PERSON_ID, PARKING_LINK_ID, VEHICLE_ID, TransportMode.car, 1.0));
		park(fixture, 5400.0);
		fixture.tracker().notifyMobsimAfterSimStep(new MobsimAfterSimStepEvent<>(null, END_TIME));

		assertTrue(fixture.events().isEmpty());
	}

	private Fixture createFixture(PenaltyEventType eventType, double hourlyCost) {
		Config config = ConfigUtils.createConfig(new DeparkingConfigGroup());
		config.qsim().setEndTime(END_TIME);
		ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class).setPenaltyEventType(eventType);

		Network network = NetworkUtils.createNetwork();
		Node from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
		Node to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(100, 0));
		network.addNode(from);
		network.addNode(to);
		network.addLink(network.getFactory().createLink(PARKING_LINK_ID, from, to));
		network.addLink(network.getFactory().createLink(DEPARTURE_LINK_ID, from, to));

		DeParkingTracker tracker = new DeParkingTracker(TransportMode.car, Set.of("ignored"));
		tracker.config = config;
		tracker.network = network;
		tracker.parkingCostTracker = new ParkingCostTracker(
			Map.of(PARKING_LINK_ID, 0, DEPARTURE_LINK_ID, 1),
			new double[][]{{hourlyCost, hourlyCost, hourlyCost}, {99.0, 99.0, 99.0}},
			null, 3600, null, network, config
		);
		List<Event> events = new ArrayList<>();
		tracker.events = EventsUtils.createEventsManager();
		tracker.events.addHandler((BasicEventHandler) events::add);
		return new Fixture(tracker, events);
	}

	private void park(Fixture fixture, double time) {
		fixture.tracker().handleEvent(new VehicleLeavesTrafficEvent(time, PERSON_ID, PARKING_LINK_ID, VEHICLE_ID, TransportMode.car, 1.0));
	}

	private void depart(Fixture fixture, double time) {
		fixture.tracker().handleEvent(new VehicleEntersTrafficEvent(time, PERSON_ID, DEPARTURE_LINK_ID, VEHICLE_ID, TransportMode.car, 1.0));
	}

	private void assertPenalty(Fixture fixture, PenaltyEventType eventType, double time, double amount, String purpose) {
		assertEquals(1, fixture.events().size());
		Event event = fixture.events().getFirst();
		assertEquals(time, event.getTime());
		if (eventType == PenaltyEventType.MONEY) {
			PersonMoneyEvent moneyEvent = assertInstanceOf(PersonMoneyEvent.class, event);
			assertEquals(PERSON_ID, moneyEvent.getPersonId());
			assertEquals(amount, moneyEvent.getAmount(), 1e-9);
			assertEquals(purpose, moneyEvent.getPurpose());
			assertNull(moneyEvent.getTransactionPartner());
			assertEquals(PARKING_LINK_ID.toString(), moneyEvent.getReference());
		} else {
			PersonScoreEvent scoreEvent = assertInstanceOf(PersonScoreEvent.class, event);
			assertEquals(PERSON_ID, scoreEvent.getPersonId());
			assertEquals(amount, scoreEvent.getAmount(), 1e-9);
			assertEquals(purpose + ";linkId=" + PARKING_LINK_ID, scoreEvent.getKind());
		}
	}

	private record Fixture(DeParkingTracker tracker, List<Event> events) {
	}
}
