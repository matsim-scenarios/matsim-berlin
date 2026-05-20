package org.matsim.run.policies;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;

import static org.matsim.run.policies.OpenBerlinScooterSharingScenario.*;

/**
 * This is a run class which configures and runs the simulation run(s) to provide output data fpr the siemens game.
 * The runs are divided into policies of 5 policy packages (Massnahmenbuendel): M1-5.
 * Each policy package applies a maximum of 2 policies.
 * Policy package have 3 levels:
 * level 0 := base
 * level 1 := moderate policies
 * level 2 := strong policies
 * With this class we will simulate all possible combinations of policy package-levels.
 */
public class OpenBerlinSiemensScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinSiemensScenario.class);

	@CommandLine.Option(names = "--car-fix-cost", description = "Defines to which value the daily monetary constant for mode car is set. Default = -5.0Eu/d", defaultValue = "-5.0")
	private double carFixCost;
	@CommandLine.Option(names = "--car-distance-cost", description = "Defines to which value the monetary distance rate for mode car is set. Default = -0.000149Eu/m", defaultValue = "-0.000149")
	private double carDistanceCost;
	@CommandLine.Option(names = "--speed-relative-change", description = "provide a value that is bigger than 0.0. Should be < 1.0 for speed reduction and > 1.0 for increase.",
		defaultValue = "1.0")
	private double relativeSpeedChange;
	@CommandLine.Option(names = "--pt-daily-monetary-constant", description = "Defines to which value the daily monetary constant for pt is set. Default = -3.0", defaultValue = "-3.0")
	private double dailyMonetaryConstantPt;
	@CommandLine.Option(names = "--drt", defaultValue = "DRT_NOT_ACTIVE", description = "Switch simulation of drt on/off. DRT_NOT_ACTIVE:= every other drt-related run param will be ignored.")
	private DrtHandling drtHandling;
	//		ride time alpha + beta for pooled drt service from below paper. See Table 1.
	//	https://api-depositonce.tu-berlin.de/server/api/core/bitstreams/82f8e8b5-7c7c-4bf2-a636-5b8b1ab7fe1d/content
	@CommandLine.Option(names = "--ride-time-alpha", description = "ride time estimator alpha", defaultValue = "1.5")
	private double drtRideTimeAlpha;
	@CommandLine.Option(names = "--ride-time-beta", description = "ride time estimator beta", defaultValue = "360")
	private double drtRideTimeBeta;
	@CommandLine.Option(names = "--ride-time-std", description = "ride duration standard deviation", defaultValue = "0.3")
	private double drtRideTimeStd;
	@CommandLine.Option(names = "--drt-intermodal", defaultValue = "DRT_REGULAR_AND_INTERMODAL", description = "INTERMODAL_DRT_ONLY: Drt can only be used for access/egress to PT. DRT_REGULAR_AND_INTERMODAL: Drt used for intermodal access/egress to PT and as separate mode.")
	private OpenBerlinDrtEstimatorScenario.DrtIntermodalityHandling drtIntermodal;
	@CommandLine.Option(names = "--drt-fare", description = "Daily drt fare to be charged for drt trips. Default = -3Eu := same as PT", defaultValue = "-3.0")
	private double drtFare;
	@CommandLine.Option(names = "--drt-config",
		defaultValue = "input/v" + OpenBerlinScenario.VERSION + "/berlin-v" + OpenBerlinScenario.VERSION + ".drt-config.xml",
		description = "Path to drt (only) config. Should contain only additional stuff to base config. Otherwise overrides.")
	private String drtConfig;
	@CommandLine.Option(names = "--typ-wt", description = "typical waiting time (base)", defaultValue = "300")
	private double drtTypicalWaitTime;
	@CommandLine.Option(names = "--wt-std", description = "waiting time standard deviation", defaultValue = "0.3")
	private double drtWaitTimeStd;
	@CommandLine.Option(names = "--max-bike-speed", description = "Defines to which value in km/h the maximum velocity of bikes is set. Default = 10.728 km/h", defaultValue = "10.728")
	private double maxBikeSpeedKmH;
	@CommandLine.Option(names = "--sharing", defaultValue = "SHARING_NOT_ACTIVE", description = "Switch simulation of sharing on/off. SHARING_NOT_ACTIVE:= every other sharing-related run param will be ignored.")
	private SharingHandling sharingHandling;
	@CommandLine.Option(names = "--sharing-service", description = "Path to sharing service xml file with stations and vehicles.")
	private String sharingServiceFile;
	@CommandLine.Option(names = "--intermodal-e-scooter", defaultValue = "E_SCOOTER_REGULAR_AND_INTERMODAL", description = "INTERMODAL_E_SCOOTER_ONLY: eScooter can only be used for access/egress to PT. E_SCOOTER_REGULAR_AND_INTERMODAL: eScooter used for intermodal access/egress to PT and as separate mode.")
	private OpenBerlinScooterSharingScenario.EScooterIntermodalityHandling eScooterIntermodal;
	@CommandLine.Option(names = "--base-fare", description = "Base fare for a sharing trip := fare for unlocking the vehicle. Default = 1Eu. " +
		"Value has to be provided as non negative double.", defaultValue = "1.0")
	private double baseFare;
	@CommandLine.Option(names = "--distance-fare", description = "Distance based fare for a sharing trip [Eu/m]. Default = 0Eu/m. " +
		"Value has to be provided as non negative double.", defaultValue = "0.0")
	private double distanceFare;
	@CommandLine.Option(names = "--time-fare", description = "Time based fare for a sharing trip [Eu/s]. Default = 0.0045Eu/s. " +
		"Value has to be provided as non negative double.", defaultValue = "0.0045")
	private double timeFare;
	@CommandLine.Option(names = "--home-office-pct", description = "", defaultValue = "0.0")
	private double additionalHomeOfficePct;
	@CommandLine.Option(names = "--berlin-shp", description = "Path to shp file for adaption of link capacities. Should be shape of berlin or related.", required = true)
	private String berlinShp;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

//		M1: engine type (post-processing only); car cost changes
		OpenBerlinCarCostScenario.setCarCostInConfig(config, carFixCost, carDistanceCost);
//		M2: car cost changes; max. allowed speed changes
//		conflict with M1; we only need to set it once; this is done in run script, so commented out here
//		OpenBerlinCarCostScenario.setCarCostInConfig(config, carFixCost, carDistanceCost);
//		M3: pt fare changes; drt
		OpenBerlinPtPricingScenario.setDailyMonetaryConstantPtInConfig(config, dailyMonetaryConstantPt);
		if (drtHandling == DrtHandling.DRT_ACTIVE) {
			OpenBerlinDrtEstimatorScenario.configureDrtInConfig(config, drtConfig, drtFare, drtIntermodal);
		}
//		M4: bike speed changes; sharing
		OpenBerlinBikeSpeedScenario.assertNoTeleportedBikeParamsInConfig(config, maxBikeSpeedKmH);

		if (sharingHandling == SharingHandling.SHARING_ACTIVE) {
			OpenBerlinScooterSharingScenario.addSharingServiceInConfig(config, sharingServiceFile, baseFare, distanceFare, timeFare, eScooterIntermodal);
		}
//		M5: home office changes; drt
//		conflict with M3; we only need to set it once; this is done in run script, so commented out here
//		OpenBerlinDrtEstimatorScenario.configureDrtInConfig(config, drtConfig, drtFare, drtIntermodal);

		return config;
	}

//	method necessary for drt simulation.
	@Override
	protected Scenario createScenario(Config config) {
		if (drtHandling == DrtHandling.DRT_ACTIVE) {
			return OpenBerlinDrtEstimatorScenario.configureDrtInCreateScenario(config);
		} else {
			return ScenarioUtils.loadScenario(config);
		}
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		//		M1: engine type (post-processing only); car cost changes
//		no changes in scenario
		//		M2: car cost changes; max. allowed speed changes
		OpenBerlinRoadSpeedScenario.applyRelativeSpeedChangeToLinksInScenario(scenario, relativeSpeedChange, berlinShp);
		//		M3: pt fare changes; drt
		if (drtHandling == DrtHandling.DRT_ACTIVE) {
			OpenBerlinDrtEstimatorScenario.configureDrtInScenario(scenario);
		}
		//		M4: bike speed changes; sharing
		OpenBerlinBikeSpeedScenario.setMaxBikeSpeedInScenario(scenario, maxBikeSpeedKmH);

		if (sharingHandling == SharingHandling.SHARING_ACTIVE) {
			OpenBerlinScooterSharingScenario.copyBikeValuesForSharingInScenario(scenario);
		}
//		tag intermodal eScooter-pt-stations
		OpenBerlinScooterSharingScenario.tagIntermodalPtSharingTransitStopsInScenario(scenario, STOP_FILTER, STOP_FILTER_VALUE, berlinShp);
		//		M5: home office changes; drt
		OpenBerlinHomeOfficeScenario.addHomeOfficeWorkersInScenario(scenario, additionalHomeOfficePct);
//		conflict with M3; we only need to set it once; this is done in run script, so commented out here
//		OpenBerlinDrtEstimatorScenario.configureDrtInScenario(scenario);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

		//		M1: engine type (post-processing only); car cost changes
//		no changes in controller
		//		M2: car cost changes; max. allowed speed changes
//		no changes in controller
		//		M3: pt fare changes; drt
//		configure cotroller for drt and sharing.
		if (drtHandling == DrtHandling.DRT_ACTIVE || sharingHandling == SharingHandling.SHARING_ACTIVE) {
			configureControllerForDrtAndOrSharing(drtHandling, sharingHandling, controler);
		}
		//		M4: bike speed changes; sharing
//		see above / method configureControllerForDrtAndOrSharing for sharing controller setup
		//		M5: home office changes; drt
//		conflict with M3 because of drt; we only need to set it once; this is done in run script, so we do not configure drt here (again)
	}

	private void configureControllerForDrtAndOrSharing(DrtHandling drtHandling, SharingHandling sharingHandling, Controler controler) {
		if (drtHandling == DrtHandling.DRT_ACTIVE && sharingHandling == SharingHandling.SHARING_NOT_ACTIVE) {
//			drt only
//			here we can use the method from OpenBerlinDrtEstimatorScenario
			OpenBerlinDrtEstimatorScenario.configureDrtInController(controler, drtTypicalWaitTime, drtWaitTimeStd, drtRideTimeAlpha, drtRideTimeBeta, drtRideTimeStd, drtFare);
		} else if (drtHandling == DrtHandling.DRT_NOT_ACTIVE && sharingHandling == SharingHandling.SHARING_ACTIVE) {
//			sharing only
//			here we can use the method from OpenBerlinScooterSharingScenario
			OpenBerlinScooterSharingScenario.addSharingModuleAndIntermodalFareCompensationInController(controler);

		} else if (drtHandling == DrtHandling.DRT_ACTIVE && sharingHandling == SharingHandling.SHARING_ACTIVE) {
//			drt and sharing
			//		do drt controller changes here because we would be binding classes twice for drt and sharing when calling OpenBerlinDrtEstimatorScenario.configureDrtInController as well as OpenBerlinScooterSharingScenario.addSharingModuleAndIntermodalFareCompensationInController
			// drt + dvrp modules
			controler.addOverridingModule(new MultiModeDrtModule());
			controler.addOverridingModule(new DvrpModule());
//		we have to configure the qsim components of drt and sharing at the same time. see below.
//		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

			// yyyy there is fareSModule (with S) in config. ?!?!  kai, jul'19
			OpenBerlinDrtEstimatorScenario.addIntermodalTripFareCompensatorsModule(controler, drtFare);
			controler.addOverridingModule(new PtIntermodalRoutingModesModule());


			for (DrtConfigGroup drtConfigGroup : MultiModeDrtConfigGroup.get(controler.getConfig()).getModalElements()) {
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						DrtEstimatorModule.bindEstimator(binder(), drtConfigGroup.mode).toInstance(
							new DirectTripBasedDrtEstimator.Builder()
//							typical waiting time is set as minimal waiting time. it will only be applied if the typical waiting time of a service area is >= minimal waiting time.
								.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(drtTypicalWaitTime))
								.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, drtWaitTimeStd))
								.setRideDurationEstimator(new ConstantRideDurationEstimator(drtRideTimeAlpha, drtRideTimeBeta))
								.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, drtRideTimeStd))
								.build()
						);
					}
				});
			}

			//		do sharing controller changes here because we would be binding classes twice for drt and sharing when calling OpenBerlinDrtEstimatorScenario.configureDrtInController as well as OpenBerlinScooterSharingScenario.addSharingModuleAndIntermodalFareCompensationInController
			controler.addOverridingModule(new SharingModule());
//			we have to configure the qsim components of drt and sharing at the same time. see below.
//			controler.configureQSimComponents(SharingUtils.configureQSim(ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class)));

//			we configure drt _and_ sharing qsim components here.
			controler.configureQSimComponents(MobilityToGridScenariosUtils.drtAndSharingQSimComponentsConfigurator(ConfigUtils.addOrGetModule(controler.getConfig(),
				SharingConfigGroup.class), MultiModeDrtConfigGroup.get(controler.getConfig())));

//			add intermodal trip compensation when pt is used once in a day for eScooter trips
			OpenBerlinScooterSharingScenario.SharingRefundHandler refundHandler = new OpenBerlinScooterSharingScenario.SharingRefundHandler(TransportMode.pt);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(refundHandler);
					addControlerListenerBinding().toInstance(refundHandler);
					bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtAndSharingRouterAnalysisModeIdentifier.class);
					bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtAndSharingRouterModeIdentifier.class);
//					this is bound in drt and sharing run class separately, we need it only once, so we bind it here once
					bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
				}
			});
		} else {
			log.fatal("Drt and Sharing are both not activated for this run. The code should not end up in this method. Aborting!");
			throw new IllegalStateException("");
		}
	}

	/**
	 * Helper enum to enable/disable drt simulation.
	 */
	private enum DrtHandling {DRT_ACTIVE, DRT_NOT_ACTIVE}

	/**
	 * Helper enum to enable/disable sharing simulation.
	 */
	private enum SharingHandling {SHARING_ACTIVE, SHARING_NOT_ACTIVE}
}
