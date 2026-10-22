package org.matsim.analysis;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AgentWiseComparisonTest {

	/** A score explanation as a dev/v7 Berlin run writes it, for a person with two pt trips with bus legs. */
	private static final String EXPLANATION = "actPerforming_util=139.46455724541465;actPerforming_s=91463.0;actWaiting_util=0.0;actWaiting_s=0.0;" +
		"actLateArrival_util=0.0;actLateArrival_s=0.0;actEarlyDeparture_util=0.0;actEarlyDeparture_s=0.0;legs_util=-5.702858634736179; " +
		"legs_0_util=0.0; legs_1_util=-2.1320944381320626; legs_2_util=0.0; legs_3_util=-1.0; legs_4_util=0.0; legs_5_util=0.0; " +
		"legs_6_util=-1.5707641966041161; legs_7_util=0.0; legs_8_util=-1.0; legs_9_util=0.0;trips_util=-0.8609840023859955;" +
		"trip_0=-0.6412409334126301;trip_1=-0.21974306897336535;transit_score=-0.492153;money_util=0.0;money_price=0.0;" +
		"agentStuck_util=0.0;scoreEvents_util=0.0";

	@Test
	void explanation() {
		double[] parts = new double[AgentWiseComparison.Part.values().length];
		Set<String> unknown = new TreeSet<>();

		AgentWiseComparison.addExplanation(EXPLANATION + ";ascCalibrationOffset_util=0.4", parts, unknown);

		assertThat(parts[AgentWiseComparison.Part.ACTS.ordinal()]).isEqualTo(139.46455724541465);
		assertThat(parts[AgentWiseComparison.Part.LEGS.ordinal()]).isEqualTo(-5.702858634736179);
		assertThat(parts[AgentWiseComparison.Part.PSEUDO_RANDOM.ordinal()]).isEqualTo(-0.8609840023859955);
		assertThat(parts[AgentWiseComparison.Part.PT_SUBMODES.ordinal()]).isEqualTo(-0.492153);
		assertThat(parts[AgentWiseComparison.Part.OTHER.ordinal()]).isEqualTo(0.4);
		assertThat(unknown).containsExactly("ascCalibrationOffset_util");
		// the details (seconds, single legs and trips) are not counted a second time
		assertThat(parts[AgentWiseComparison.Part.SCORE.ordinal()])
			.isCloseTo(139.46455724541465 - 5.702858634736179 - 0.8609840023859955 - 0.492153 + 0.4, within(1e-9));
	}

	/**
	 * The split of the leg part has to add up to what the leg scoring of the run computes, for every kind of term:
	 * constants once per mode and trip, daily constants once per mode, line switches, pt waiting, distance.
	 */
	@Test
	void legSplitAddsUpToLegScoring() {
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup.ScoringParameterSet set = config.scoring().getOrCreateScoringParameters(null);
		set.setMarginalUtilityOfMoney(0.4);
		set.setUtilityOfLineSwitch(-1.0);
		set.setMarginalUtlOfWaitingPt_utils_hr(-2.0);
		set.getOrCreateModeParams(TransportMode.car).setConstant(-1.0).setDailyMonetaryConstant(-3.8).setMonetaryDistanceRate(-1.5e-4)
			.setMarginalUtilityOfDistance(-1e-5);
		set.getOrCreateModeParams(TransportMode.pt).setConstant(-1.5).setDailyMonetaryConstant(-0.8).setMarginalUtilityOfTraveling(-0.5)
			.setDailyUtilityConstant(-0.3);
		set.getOrCreateModeParams(TransportMode.walk).setConstant(-0.2).setMarginalUtilityOfTraveling(-1.0);
		set.getOrCreateModeParams(TransportMode.ride).setConstant(-5.0).setMarginalUtilityOfTraveling(-4.0);

		ScoringParameters params = new ScoringParameters.Builder(config.scoring(), set, config.scenario()).build();

		Plan plan = PopulationUtils.createPlan();
		addAct(plan, "home", 0, 7 * 3600);
		addLeg(plan, TransportMode.walk, 7 * 3600, 60, 50);
		addAct(plan, "car interaction", 7 * 3600 + 60, 7 * 3600 + 60);
		addLeg(plan, TransportMode.car, 7 * 3600 + 60, 1200, 15000);
		addAct(plan, "car interaction", 7 * 3600 + 1260, 7 * 3600 + 1260);
		addLeg(plan, TransportMode.walk, 7 * 3600 + 1260, 60, 50);
		addAct(plan, "work", 7 * 3600 + 1320, 16 * 3600);
		addLeg(plan, TransportMode.walk, 16 * 3600, 300, 300);
		addAct(plan, "pt interaction", 16 * 3600 + 300, 16 * 3600 + 300);
		addPtLeg(plan, 16 * 3600 + 300, 1500, 8000, 16 * 3600 + 480);
		addAct(plan, "pt interaction", 16 * 3600 + 1800, 16 * 3600 + 1800);
		addPtLeg(plan, 16 * 3600 + 1800, 900, 5000, 16 * 3600 + 2000);
		addAct(plan, "pt interaction", 16 * 3600 + 2700, 16 * 3600 + 2700);
		addLeg(plan, TransportMode.walk, 16 * 3600 + 2700, 300, 300);
		addAct(plan, "leisure", 16 * 3600 + 3000, 18 * 3600);
		addLeg(plan, TransportMode.ride, 18 * 3600, 900, 6000);
		addAct(plan, "home", 18 * 3600 + 900, Double.NaN);

		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
		Set<String> ptModes = new HashSet<>(Set.of(TransportMode.pt));

		CharyparNagelLegScoring legScoring = new CharyparNagelLegScoring(params, ptModes);
		trips.forEach(legScoring::handleTrip);

		double[] parts = new double[AgentWiseComparison.Part.values().length];
		AgentWiseComparison.splitLegs(trips, params, ptModes, parts);

		double split = parts[AgentWiseComparison.Part.U_TRAV.ordinal()] + parts[AgentWiseComparison.Part.U_DIST.ordinal()]
			+ parts[AgentWiseComparison.Part.LINE_SWITCHES.ordinal()] + parts[AgentWiseComparison.Part.ASCS.ordinal()]
			+ parts[AgentWiseComparison.Part.MONEY_LEGS_SCORE.ordinal()];

		assertThat(split).isCloseTo(legScoring.getScore(), within(1e-9));
		assertThat(parts[AgentWiseComparison.Part.LINE_SWITCHES.ordinal()]).isEqualTo(-1.0);
		// walk constant once per trip (twice for the car trip's access and egress would be wrong), pt and car once, ride once
		assertThat(parts[AgentWiseComparison.Part.ASCS.ordinal()]).isCloseTo(-0.2 - 1.0 - 0.2 - 1.5 - 0.3 - 5.0, within(1e-9));
		assertThat(parts[AgentWiseComparison.Part.MONEY_EUR.ordinal()]).isCloseTo(15000 * -1.5e-4 - 3.8 - 0.8, within(1e-9));
	}

	/**
	 * MUSE is the slope of the activity score in the activity's start time, in utils per hour: performing times typical
	 * over actual duration, and for the evening activity the duration wrapped around with the morning one.
	 */
	@Test
	void muse() {
		Config config = ConfigUtils.createConfig();
		config.scoring().setPerforming_utils_hr(6.0);
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(12 * 3600.));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600.));
		ScoringParameters params = new ScoringParameters.Builder(config.scoring(), config.scoring().getScoringParameters(null), config.scenario()).build();

		Plan plan = PopulationUtils.createPlan();
		addAct(plan, "home", 0, 7 * 3600);
		addLeg(plan, TransportMode.walk, 7 * 3600, 3600, 1000);
		addAct(plan, "work", 8 * 3600, 16 * 3600);
		addLeg(plan, TransportMode.walk, 16 * 3600, 3600, 1000);
		addAct(plan, "home", 17 * 3600, Double.NaN);
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		person.addPlan(plan);

		List<AgentWiseComparison.ActivityMuse> muse = new ArrayList<>();
		AgentWiseComparison.muse(person, plan, List.of(TransportMode.walk, TransportMode.walk), p -> {
			SumScoringFunction sf = new SumScoringFunction();
			sf.addScoringFunction(new CharyparNagelActivityScoring(params));
			return sf;
		}, muse);

		assertThat(muse).hasSize(2);
		assertThat(muse.get(0).type()).isEqualTo("work");
		assertThat(muse.get(0).museH()).isCloseTo(6.0 * 8 / 8, within(1e-3));
		assertThat(muse.get(1).museH()).isCloseTo(6.0 * 12 / 14, within(1e-3));
		assertThat(muse.get(1).arrivingMode()).isEqualTo(TransportMode.walk);
	}

	private static void addAct(Plan plan, String type, double start, double end) {
		Activity act = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId("l"));
		if (start > 0)
			act.setStartTime(start);
		if (!Double.isNaN(end))
			act.setEndTime(end);
		plan.addActivity(act);
	}

	private static Leg addLeg(Plan plan, String mode, double departure, double travelTime, double distance) {
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setDepartureTime(departure);
		leg.setTravelTime(travelTime);
		Route route = RouteUtils.createGenericRouteImpl(Id.createLinkId("l"), Id.createLinkId("l"));
		route.setDistance(distance);
		route.setTravelTime(travelTime);
		leg.setRoute(route);
		plan.addLeg(leg);
		return leg;
	}

	private static void addPtLeg(Plan plan, double departure, double travelTime, double distance, double boarding) {
		Leg leg = PopulationUtils.createLeg(TransportMode.pt);
		leg.setDepartureTime(departure);
		leg.setTravelTime(travelTime);
		Id<Link> link = Id.createLinkId("l");
		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(link, link, null, null, null, null);
		route.setDistance(distance);
		route.setTravelTime(travelTime);
		route.setBoardingTime(boarding);
		leg.setRoute(route);
		plan.addLeg(leg);
	}

}
