package org.matsim.run.policies;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.estimator.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;

/**
 * This is the scenario class for the second M2G scenario: "motorized hedonism".
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
 * 9) price changes in car mode params
 * All necessary configurations will be made in this class.
 */
public class OpenBerlinM2GMotorizedHedonismScenario extends OpenBerlinScenario {
	private static final double DRT_FARE = -3.0;
//	private static final double REL_ROAD_SPEED_CHANGE = 0.6;
	private static final double DRT_TYP_WAIT_TIME = 300.;
	private static final double DRT_WAIT_TIME_STD = 0.3;
	private static final double DRT_RIDE_TIME_ALPHA = 1.5;
	private static final double DRT_RIDE_TIME_BETA = 0.;
	private static final double DRT_RIDE_TIME_STD = 0.3;
	private static final OpenBerlinDrtEstimatorScenario.DrtIntermodalityHandling DRT_INTERMODALITY_HANDLING = OpenBerlinDrtEstimatorScenario.DrtIntermodalityHandling.INTERMODAL_DRT_ONLY;
//	private static final double BETA_MONEY = 0.5;
	private static final double MAX_BIKE_SPEED = 20.;
	private static final double SHARING_BASE_FARE = 1.0;
	private static final double SHARING_DISTANCE_FARE = 0.0;
	private static final double SHARING_TIME_FARE = 0.0045;
	private static final OpenBerlinScooterSharingScenario.EScooterIntermodalityHandling SHARING_INTERMODALITY_HANDLING = OpenBerlinScooterSharingScenario.EScooterIntermodalityHandling.E_SCOOTER_REGULAR_AND_INTERMODAL;
//	private static final double ADDITIONAL_HOME_OFFICE_PCT = 0.1;
	private static final double REL_ROAD_CAPACITY_CHANGE = 1.25;
	private static final double DAILY_MONETARY_CONSTANT_PT = -4.5;

//	necessary input files have to be provided via cmd line option to avoid relative path problems by defining the paths in this class.
	@CommandLine.Option(names = "--drt-config",
		description = "Path to drt (only) config. Should contain only additional stuff to base config. Otherwise overrides.", required = true)
	private String drtConfig;
	@CommandLine.Option(names = "--sharing-service", description = "Path to sharing service xml file with stations and vehicles.", required = true)
	private String serviceFile;
	@CommandLine.Option(names = "--berlin-shp", description = "Path to shp file for adaption of link capacities. Should be shape of berlin or related.", required = true)
	private String berlinShp;

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
//		drt fare = pt fare = -3; drt intermodal + as own mode
		OpenBerlinDrtEstimatorScenario.configureDrtInConfig(config, drtConfig, DRT_FARE, DRT_INTERMODALITY_HANDLING);
//		4) marginal utility of money
//		no changes in config compared to base case
//		5) bicycle speed
//		max bike speed 20km/h due to improved infrastructure
		OpenBerlinBikeSpeedScenario.assertNoTeleportedBikeParamsInConfig(config, MAX_BIKE_SPEED);
//		6) sharing
//		intermodal sharing + as own mode, base fare 1Eu, time fare 0.0045Eu/s, no distance fare
//		sharing stations with 1000 veh capacity and 10 scooters each
		OpenBerlinScooterSharingScenario.addSharingServiceInConfig(config,
			serviceFile,
			SHARING_BASE_FARE,
			SHARING_DISTANCE_FARE,
			SHARING_TIME_FARE,
			SHARING_INTERMODALITY_HANDLING);
//		7) home office
//		no changes in config compared to base case
//		8) road capacity
//		no changes in config compared to base case
//		9) price changes in car mode params
//		no changes in config compared to base case

		return config;
	}

//	method createScenario needed for DRT simulation only
	@Override
	protected Scenario createScenario(Config config) {
		return OpenBerlinDrtEstimatorScenario.configureDrtInCreateScenario(config);
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) price change in pt
//		no changes in scenario compared to base case
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
		OpenBerlinScooterSharingScenario.copyBikeValuesForSharingInScenario(scenario);
//		add tags to transit stops for intermodality between pt and eScooter sharing
		OpenBerlinScooterSharingScenario.tagIntermodalPtSharingTransitStopsInScenario(scenario, OpenBerlinScooterSharingScenario.STOP_FILTER,
			OpenBerlinScooterSharingScenario.STOP_FILTER_VALUE, berlinShp);
//		7) home office
//		no changes in scenario compared to base case
//		8) road capacity
//		reduced capacity to 0.075 := more inhabitants in Berlin, so road are more congested
		OpenBerlinRoadCapacitiesScenario.changeLinkCapacitiesInScenario(scenario, REL_ROAD_CAPACITY_CHANGE, berlinShp);
//		9) price changes in car mode params
//		no changes in scenario compared to base case
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
//		the following changes are: single passenger DRT and drt fare of 3Eu = pt fare
//		OpenBerlinDrtEstimatorScenario.configureDrtInController(controler, DRT_TYP_WAIT_TIME, DRT_WAIT_TIME_STD, DRT_RIDE_TIME_ALPHA, DRT_RIDE_TIME_BETA,
//			DRT_RIDE_TIME_STD, DRT_FARE);
//
//		do drt controller changes here because we would be binding classes twice for drt and sharing when calling OpenBerlinDrtEstimatorScenario.configureDrtInController as well as OpenBerlinScooterSharingScenario.addSharingModuleAndIntermodalFareCompensationInController
		// drt + dvrp modules
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
//		we have to configure the qsim components of drt and sharing at the same time. see below.
//		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

		// yyyy there is fareSModule (with S) in config. ?!?!  kai, jul'19
		OpenBerlinDrtEstimatorScenario.addIntermodalTripFareCompensatorsModule(controler, DRT_FARE);
		controler.addOverridingModule(new PtIntermodalRoutingModesModule());


		for (DrtConfigGroup drtConfigGroup : MultiModeDrtConfigGroup.get(controler.getConfig()).getModalElements()) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					DrtEstimatorModule.bindEstimator(binder(), drtConfigGroup.mode).toInstance(
						new DirectTripBasedDrtEstimator.Builder()
//							typical waiting time is set as minimal waiting time. it will only be applied if the typical waiting time of a service area is >= minimal waiting time.
							.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(DRT_TYP_WAIT_TIME))
							.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, DRT_WAIT_TIME_STD))
							.setRideDurationEstimator(new ConstantRideDurationEstimator(DRT_RIDE_TIME_ALPHA, DRT_RIDE_TIME_BETA))
							.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, DRT_RIDE_TIME_STD))
							.build()
					);
				}
			});
		}

//		4) marginal utility of money
//		no changes in controller compared to base case
//		5) bicycle speed
//		no changes in controller compared to base case
//		6) sharing
//		OpenBerlinScooterSharingScenario.addSharingModuleAndIntermodalFareCompensationInController(controler);

//		do sharing controller changes here because we would be binding classes twice for drt and sharing when calling OpenBerlinDrtEstimatorScenario.configureDrtInController as well as OpenBerlinScooterSharingScenario.addSharingModuleAndIntermodalFareCompensationInController
		controler.addOverridingModule(new SharingModule());
//		we have to configure the qsim components of drt and sharing at the same time. see below.
//		controler.configureQSimComponents(SharingUtils.configureQSim(ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class)));

//		we configure drt _and_ sharing qsim components here.
		controler.configureQSimComponents(MobilityToGridScenariosUtils.drtAndSharingQSimComponentsConfigurator(ConfigUtils.addOrGetModule(controler.getConfig(),
				SharingConfigGroup.class), MultiModeDrtConfigGroup.get(controler.getConfig())));

//		add intermodal trip compensation when pt is used once in a day for eScooter trips
		OpenBerlinScooterSharingScenario.SharingRefundHandler refundHandler = new OpenBerlinScooterSharingScenario.SharingRefundHandler(TransportMode.pt);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(refundHandler);
				addControlerListenerBinding().toInstance(refundHandler);
				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtAndSharingRouterAnalysisModeIdentifier.class);
				bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtAndSharingRouterModeIdentifier.class);
//				this is bound in drt and sharing run class separately, we need it only once, so we bind it here once
				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
			}
		});
//		7) home office
//		no changes in controller compared to base case
//		8) road capacity
//		no changes in controller compared to base case
//		9) price changes in car mode params
//		no changes in controller compared to base case
	}
}
