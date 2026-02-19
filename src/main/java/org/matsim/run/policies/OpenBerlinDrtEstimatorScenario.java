package org.matsim.run.policies;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.estimator.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.legacy.run.drt.OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier;
import org.matsim.legacy.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.matsim.run.OpenBerlinDrtScenario.prepareNetworkAndTransitScheduleForDrt;

/**
 * Berlin scenario including estimated drt.
 * This class uses the changes made in OpenBerlinDrtScenario and changes some of them by copying them, see comments in some methods.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinDrtEstimatorScenario extends OpenBerlinScenario {
	Logger log = LogManager.getLogger(OpenBerlinDrtEstimatorScenario.class);

	@CommandLine.Option(names = "--typ-wt", description = "typical waiting time (base)", defaultValue = "300")
	protected double typicalWaitTime;

	@CommandLine.Option(names = "--wt-std", description = "waiting time standard deviation", defaultValue = "0.3")
	protected double waitTimeStd;

	//		ride time alpha + beta for pooled drt service from below paper. See Table 1.
	//	https://api-depositonce.tu-berlin.de/server/api/core/bitstreams/82f8e8b5-7c7c-4bf2-a636-5b8b1ab7fe1d/content
	@CommandLine.Option(names = "--ride-time-alpha", description = "ride time estimator alpha", defaultValue = "1.5")
	protected double rideTimeAlpha;

	@CommandLine.Option(names = "--ride-time-beta", description = "ride time estimator beta", defaultValue = "360")
	protected double rideTimeBeta;

	@CommandLine.Option(names = "--ride-time-std", description = "ride duration standard deviation", defaultValue = "0.3")
	protected double rideTimeStd;

	@CommandLine.Option(names = "--intermodal", defaultValue = "DRT_REGULAR_AND_INTERMODAL", description = "INTERMODAL_DRT_ONLY: Drt can only be used for access/egress to PT. DRT_REGULAR_AND_INTERMODAL: Drt used for intermodal access/egress to PT and as separate mode.")
	private DrtIntermodalityHandling intermodal;

	@CommandLine.Option(names = "--drt-fare", description = "Daily drt fare to be charged for drt trips. Default = -3Eu := same as PT", defaultValue = "-3.0")
	private double drtFare;

	@CommandLine.Option(names = "--drt-config",
		defaultValue = "input/v" + OpenBerlinScenario.VERSION + "/berlin-v" + OpenBerlinScenario.VERSION + ".drt-config.xml",
		description = "Path to drt (only) config. Should contain only additional stuff to base config. Otherwise overrides.")
	private String drtConfig;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);


		configureDrtInConfig(config, drtConfig, drtFare, intermodal);

		//modify output directory and runId
		config.controller().setOutputDirectory(config.controller().getOutputDirectory() + "-alpha-" + rideTimeAlpha + "-beta-" + rideTimeBeta + "-fare-" + drtFare);
		config.controller().setRunId(config.controller().getRunId() + "-alpha-" + rideTimeAlpha + "-beta-" + rideTimeBeta + "-fare-" + drtFare);

		return config;
	}

//	TODO: we may need the following, but for estimator I do not see why?
//	@Override
//	protected Scenario createScenario(Config config) {
//		Scenario scenario = ScenarioUtils.createScenario(config);
//
//		//if the input plans contain DrtRoutes, this will cause problems later in the DrtRouteFactory
//		//to avoid this, the DrtRouteFactory would have to get set before loading the scenario, just like in Open Berlin v5.x
//		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
//		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
//
//		ScenarioUtils.loadScenario(scenario);
//		return scenario;
//	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		configureDrtInScenario(scenario);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

		configureDrtInController(controler, typicalWaitTime, waitTimeStd, rideTimeAlpha, rideTimeBeta, rideTimeStd, drtFare);
	}

	/**
	 * make all necessary config changes to simulate drt from OpenBerlinDrtScenario (see comment) and OpenBerlinDrtEstimatorScenario.
	 */
	static void configureDrtInConfig(Config config, String drtConfig, double drtFare, DrtIntermodalityHandling intermodal) {
		//		###################### the following is copied from OpenBerlinDrtScenario ##############################################################
//		there is no way around copying it unfortunately:
//		this class relies on changes in OpenBerlinDrtScenario + overrides some of the methods there.
//		for the M2GScenario run classes we want to apply all changes from OpenBerlinDrtScenario as well as this class, so we need static methods
//		which can be called in the M2GScenario classes. If we extract changes in OpenBerlinScenario to static methods, the overridable methods used in this class here
//		also need to be static (we cannot reference a non-static object from a static context). But a static object cannot be overriden.
//		Hence, the copy. -sm0226

		ConfigUtils.loadConfig(config, drtConfig);

		//drt only works with the following sim start time interpretation
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtCfg, config.scoring(), config.routing());

		Set<String> drtModes = new HashSet<>();

		ScoringConfigGroup.ModeParams ptParams = config.scoring().getModes().get(TransportMode.pt);

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			drtModes.add(drtCfg.getMode());

			//copy all scoring params from pt
			ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(drtCfg.getMode());
			modeParams.setConstant(ptParams.getConstant());
			modeParams.setMarginalUtilityOfDistance(ptParams.getMarginalUtilityOfDistance());
			modeParams.setMarginalUtilityOfTraveling(ptParams.getMarginalUtilityOfTraveling());
			modeParams.setDailyUtilityConstant(ptParams.getDailyUtilityConstant());

			//assume that the drt is fully integrated in pt, i.e. fare integration
			modeParams.setMonetaryDistanceRate(ptParams.getMonetaryDistanceRate());
			modeParams.setDailyMonetaryConstant(ptParams.getDailyMonetaryConstant());
			config.scoring().addModeParams(modeParams);
		}

		configureIntermodalTripFareCompensation(config, drtModes, drtFare);

		//include drt in mode-choice and add mode params.
		//by using a Set, it should be assured that they aren't included twice.
		drtModes.addAll(Arrays.asList(config.subtourModeChoice().getModes()));
		config.subtourModeChoice().setModes(drtModes.toArray(String[]::new));

//		##################################################### end of copied lines ###############################################################

//		adapt dailyMonetaryConstant to param drtFare if drtFare != dailyMonetaryConstantPT
		if (drtFare != config.scoring().getModes().get(TransportMode.pt).getDailyMonetaryConstant()) {
			config.scoring().getModes().get(TransportMode.drt).setDailyMonetaryConstant(drtFare);
		}

//		we want to estimate drt, so we do not need the pre-defined vehicles file
		multiModeDrtCfg.getModalElements().forEach(e -> {
			e.vehiclesFile = null;
			e.drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/shp/Berlin_25832.shp";
		});

		if (intermodal == DrtIntermodalityHandling.INTERMODAL_DRT_ONLY) {
			//		remove drt from mode choice
			Set<String> modes = new HashSet<>(Set.of(config.subtourModeChoice().getModes()));
			modes.remove(TransportMode.drt);
			config.subtourModeChoice().setModes(modes.toArray(new String[0]));
		}

		// set to drt estimate and teleport
//		this enables the usage of the DrtEstimator by CL
		for (DrtConfigGroup drtConfigGroup : multiModeDrtCfg.getModalElements()) {
			drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;
		}

		config.removeModule("");
	}

	/**
	 * make all necessary scenario changes to simulate drt from OpenBerlinDrtScenario (see comment) and OpenBerlinDrtEstimatorScenario.
	 */
	static void configureDrtInScenario(Scenario scenario) {
		//		###################### the following is copied from OpenBerlinDrtScenario ##############################################################
//		there is no way around copying it unfortunately:
//		this class relies on changes in OpenBerlinDrtScenario + overrides some of the methods there.
//		for the M2GScenario run classes we want to apply all changes from OpenBerlinDrtScenario as well as this class, so we need static methods
//		which can be called in the M2GScenario classes. If we extract changes in OpenBerlinScenario to static methods, the overridable methods used in this class here
//		also need to be static (we cannot reference a non-static object from a static context). But a static object cannot be overriden.
//		Hence, the copy. -sm0226

		prepareNetworkAndTransitScheduleForDrt(scenario);

//		##################################################### end of copied lines ###############################################################

		//		add drt veh type if not already existing
		Id<VehicleType> drtTypeId = Id.create(TransportMode.drt, VehicleType.class);
		if (!scenario.getVehicles().getVehicleTypes().containsKey(drtTypeId)) {
//			drt veh type = car veh type, but capacity 1 passenger
			VehicleType drtType = VehicleUtils.createVehicleType(drtTypeId);

			VehicleUtils.copyFromTo(scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.car, VehicleType.class)), drtType);
			drtType.setDescription("drt vehicle copied from car vehicle type");
			VehicleCapacity capacity = drtType.getCapacity();
			capacity.setSeats(1);

			scenario.getVehicles().addVehicleType(drtType);

			Vehicle drtDummy = VehicleUtils.createVehicle(Id.createVehicleId("drtDummy"), drtType);
			drtDummy.getAttributes().putAttribute("dvrpMode", TransportMode.drt);
			drtDummy.getAttributes().putAttribute("startLink", "1119935543");
			drtDummy.getAttributes().putAttribute("serviceBeginTime", 0.);
			drtDummy.getAttributes().putAttribute("serviceEndTime", 86400.);

			scenario.getVehicles().addVehicle(drtDummy);
		}
	}

	/**
	 * make all necessary controller changes to simulate drt from OpenBerlinDrtScenario (see comment) and OpenBerlinDrtEstimatorScenario.
	 */
	static void configureDrtInController(Controler controler, double typicalWaitTime, double waitTimeStd, double rideTimeAlpha,
										 double rideTimeBeta, double rideTimeStd, double drtFare) {
		//		###################### the following is copied from OpenBerlinDrtScenario ##############################################################
//		there is no way around copying it unfortunately:
//		this class relies on changes in OpenBerlinDrtScenario + overrides some of the methods there.
//		for the M2GScenario run classes we want to apply all changes from OpenBerlinDrtScenario as well as this class, so we need static methods
//		which can be called in the M2GScenario classes. If we extract changes in OpenBerlinScenario to static methods, the overridable methods used in this class here
//		also need to be static (we cannot reference a non-static object from a static context). But a static object cannot be overriden.
//		Hence, the copy. -sm0226

		// drt + dvrp modules
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.class);
				bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);

			}
		});

		// yyyy there is fareSModule (with S) in config. ?!?!  kai, jul'19
		addIntermodalTripFareCompensatorsModule(controler, drtFare);
		controler.addOverridingModule(new PtIntermodalRoutingModesModule());

//		##################################################### end of copied lines ###############################################################

		for (DrtConfigGroup drtConfigGroup : MultiModeDrtConfigGroup.get(controler.getConfig()).getModalElements()) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					DrtEstimatorModule.bindEstimator(binder(), drtConfigGroup.mode).toInstance(
						new DirectTripBasedDrtEstimator.Builder()
//							typical waiting time is set as minimal waiting time. it will only be applied if the typical waiting time of a service area is >= minimal waiting time.
							.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(typicalWaitTime))
							.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, waitTimeStd))
							.setRideDurationEstimator(new ConstantRideDurationEstimator(rideTimeAlpha, rideTimeBeta))
							.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, rideTimeStd))
							.build()
					);
				}
			});
		}
	}

	private static void addIntermodalTripFareCompensatorsModule(Controler controler, double drtFare) {
//		we do not need intermodal trip fare compensation when drt has no fare
		if (drtFare != 0.) {
			controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
		}
	}


	private static void configureIntermodalTripFareCompensation(Config config, Set<String> drtModes, double drtFare) {
		//		we do not need intermodal trip fare compensation when drt has no fare
		if (drtFare != 0.) {
			IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);

			//assume that (all) the drt is fully integrated in pt, i.e. fare integration
			IntermodalTripFareCompensatorConfigGroup drtCompensationCfg = new IntermodalTripFareCompensatorConfigGroup();
			drtCompensationCfg.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedAnywhereInTheDay);
//			drtFare is negative, so compensation is -1 * drtFare
			drtCompensationCfg.setCompensationMoneyPerDay(-1 * drtFare);
			drtCompensationCfg.setNonPtModes(ImmutableSet
				.<String>builder()
				.addAll(drtModes)
				.build());
			compensatorsConfig.addParameterSet(drtCompensationCfg);
		}
	}

	/**
	 * Helper enum to enable/disable functionalities.
	 */
	enum DrtIntermodalityHandling {INTERMODAL_DRT_ONLY, DRT_REGULAR_AND_INTERMODAL}
}
