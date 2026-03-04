package org.matsim.run.policies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;

import javax.annotation.Nullable;

/**
 * This is the scenario class for the first M2G scenario: "multimodal mass".
 * In this class, all changes regarding the chosen indicators are made.
 * The changes are called as methods from the according scenarios (e.g. OpenBerlinBetaMoneyScenario).
 * For the sake of readability and trying to prevent chaos the order of changes in each method of this class will be:
 * 1) vehicle composition
 * 2) price change in pt
 * 3) drt
 * 4) marginal utility of money
 * 5) bicycle speed
 * 6) sharing
 * 7) home office
 * 8) road capacity
 * 9) changes in maximum allowed speed for motorized vehicles
 * All necessary configurations will be made in this class.
 */
public class OpenBerlinM2GMultimodalMassScenario extends OpenBerlinScenario {
	private static final String DRT_CONFIG = "input/v" + OpenBerlinScenario.VERSION + "/berlin-v" + OpenBerlinScenario.VERSION + ".drt-config.xml";
	private static final double DRT_FARE = -3.0;
	private static final double REL_ROAD_SPEED_CHANGE = 0.6;
	private static final double DRT_TYP_WAIT_TIME = 300.;
	private static final double DRT_WAIT_TIME_STD = 0.3;
	private static final double DRT_RIDE_TIME_ALPHA = 1.5;
	private static final double DRT_RIDE_TIME_BETA = 360.;
	private static final double DRT_RIDE_TIME_STD = 0.3;
	private static final OpenBerlinDrtEstimatorScenario.DrtIntermodalityHandling DRT_INTERMODALITY_HANDLING = OpenBerlinDrtEstimatorScenario.DrtIntermodalityHandling.INTERMODAL_DRT_ONLY;
	private static final double BETA_MONEY = 0.5;
//	this is the "scaled" bike speed after analyzing elasticities of bike speed and bike modal share
	private static final double MAX_BIKE_SPEED = 16.;
//	TODO: which station capacity and number of scooter should we use?
	private static final String SHARING_SERVICE_FILE = "input/v" + OpenBerlinScenario.VERSION + "/berlin-v" + OpenBerlinScenario.VERSION + ".sharing-service-1000-capacity-10-vehicles.xml";
	private static final double SHARING_BASE_FARE = 1.0;
	private static final double SHARING_DISTANCE_FARE = 0.0;
	private static final double SHARING_TIME_FARE = 0.0045;
	private static final OpenBerlinSharingScenario.EScooterIntermodalityHandling SHARING_INTERMODALITY_HANDLING = OpenBerlinSharingScenario.EScooterIntermodalityHandling.INTERMODAL_E_SCOOTER_ONLY;
	private static final double ADDITIONAL_HOME_OFFICE_PCT = 0.1;
	private static final double REL_ROAD_CAPACITY_CHANGE = 0.75;
	private static final String BERLIN_SHP = "input/v" + OpenBerlinScenario.VERSION + "/Berlin_25832.shp";
	private static final double DAILY_MONETARY_CONSTANT_PT = 0.;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) price change in pt
		OpenBerlinPtPricingScenario.setDailyMonetaryConstantPtInConfig(config, DAILY_MONETARY_CONSTANT_PT);
//		3) drt
//		drt fare = pt fare = -3; intermodal drt only
		OpenBerlinDrtEstimatorScenario.configureDrtInConfig(config, DRT_CONFIG, DRT_FARE, DRT_INTERMODALITY_HANDLING);
//		4) marginal utility of money
//		set marginal utility of money to 0.5: everything is/feels less expensive now (default 1.0)
		OpenBerlinBetaMoneyScenario.setBetaMoneyInConfig(config, BETA_MONEY);
//		5) bicycle speed
//		max bike speed 20km/h due to improved infrastructure
		OpenBerlinBikeSpeedScenario.assertNoTeleportedBikeParamsInConfig(config, MAX_BIKE_SPEED);
//		6) sharing
//		intermodal sharing only, base fare 1Eu, time fare 0.0045Eu/s, no distance fare
//		sharing stations with 1000 veh capacity and 10 scooters each
		OpenBerlinSharingScenario.addSharingServiceInConfig(config,
			SHARING_SERVICE_FILE,
			SHARING_BASE_FARE,
			SHARING_DISTANCE_FARE,
			SHARING_TIME_FARE,
			SHARING_INTERMODALITY_HANDLING);
//		7) home office
//		no changes in config compared to base case
//		8) road capacity
//		no changes in config compared to base case
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in config compared to base case

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) price change in pt
//		no changes in controller compared to base case
//		3) drt
//		prepare transit schedule for drt and add dummy drt vehicle
		OpenBerlinDrtEstimatorScenario.configureDrtInScenario(scenario);
//		4) marginal utility of money
//		no changes in scenario compared to base case
//		5) bicycle speed
//		set max bike speed in bike vehicle type
		OpenBerlinBikeSpeedScenario.setMaxBikeSpeedInScenario(scenario, MAX_BIKE_SPEED);
//		6) sharing
//		copy mode constants := tase preferences from bike to eScooter if available
		OpenBerlinSharingScenario.copyBikeModeConstantsForSharingInScenario(scenario);
//		7) home office
//		+10% more home office agents in Berlin aka stay home agents
		OpenBerlinHomeOfficeScenario.addHomeOfficeWorkersInScenario(scenario, ADDITIONAL_HOME_OFFICE_PCT);
//		8) road capacity
//		reduced capacity to 0.075 := more inhabitants in Berlin, so road are more congested
		OpenBerlinRoadCapacitiesScenario.changeLinkCapacitiesInScenario(scenario, REL_ROAD_CAPACITY_CHANGE, BERLIN_SHP);
//		9) changes in maximum allowed speed for motorized vehicles
//		appy relative freespeed change of 0.6 to all Berlin links except motorways
		OpenBerlinRoadSpeedScenario.applyRelativeSpeedChangeToLinksInScenario(scenario, REL_ROAD_SPEED_CHANGE, BERLIN_SHP);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) price change in pt
//		no changes in controller compared to base case
//		3) drt
//		this is: DRT with pooling and drt fare of 3Eu = pt fare
		OpenBerlinDrtEstimatorScenario.configureDrtInController(controler, DRT_TYP_WAIT_TIME, DRT_WAIT_TIME_STD, DRT_RIDE_TIME_ALPHA, DRT_RIDE_TIME_BETA,
			DRT_RIDE_TIME_STD, DRT_FARE);
//		4) marginal utility of money
//		no changes in controller compared to base case
//		5) bicycle speed
//		no changes in controller compared to base case
//		6) sharing
		//		TODO: the bindings here will fail because we have bindings to the same classes with drt.
		OpenBerlinSharingScenario.addSharingModuleAndIntermodalFareCompensationInController(controler);
//		7) home office
//		no changes in controller compared to base case
//		8) road capacity
//		no changes in controller compared to base case
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in controller compared to base case
	}
}
