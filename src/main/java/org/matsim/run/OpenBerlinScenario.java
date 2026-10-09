package org.matsim.run;

import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.AgentWiseComparison;
import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.bicycle.*;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaTechnology;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.HbefaUtils;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanInheritanceConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.dashboard.BerlinDashboardProvider;
import org.matsim.run.scoring.BerlinScoringModule;
import org.matsim.run.scoring.BerlinScoringConfigGroup;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
@MATSimApplication.Analysis({AgentWiseComparison.class})
public class OpenBerlinScenario extends MATSimApplication {

	public static final String VERSION = "7.1";
	public static final String CRS = "EPSG:25832";

	//	To decrypt hbefa input files set MATSIM_DECRYPTION_PASSWORD as environment variable. ask VSP for access.
	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc" ;
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	private static final String AVERAGE = "average";
	private static final Logger log = LogManager.getLogger(OpenBerlinScenario.VERSION);

	/**
	 * Default length of the simulation period, as a multiple of 24h. 1.125 (= 27:00) makes the non-wrap-around
	 * overnight scoring clamp the last activity at 27:00 instead of 24:00.
	 */
	public static final double DEFAULT_SIMULATION_PERIOD_IN_DAYS = 1.125;

	@CommandLine.Option(names = "--plan-selector",
		description = "Plan selector to use.",
		defaultValue = DefaultPlanStrategiesModule.DefaultSelector.BestScore)
	private String planSelector;

	@CommandLine.Option(names = "--with-opening-times",
		description = "Give the activity types their opening times. Off by default: the per-activity typical " +
			"durations carry the schedule, so the opening times only distort it.")
	private boolean withOpeningTimes = false;

	@CommandLine.Option(names = "--simulation-period-in-days",
		description = "Length of the simulation period, as a multiple of 24h. Moves the else-branch overnight " +
			"scoring clamp: handleOvernightActivity scores the (non-wrap-around) last activity from its start to " +
			"simulationPeriodInDays * 24h. Preprocessing (reschedule-late-plans, encode-typical-duration) must use " +
			"the same value.")
	private double simulationPeriodInDays = DEFAULT_SIMULATION_PERIOD_IN_DAYS;

	@CommandLine.Option(names = "--allow-config-typical-durations",
		description = "Allow person-subpopulation activities without a typicalDuration attribute to score against " +
			"the config typical duration. By default such an activity ABORTS the run. Pass this for populations " +
			"whose typical durations are still encoded in the activity type.")
	private boolean allowConfigTypicalDurations = false;

	@CommandLine.Option(names = "--agent-wise-comparison-base",
		description = "Output directory of a base run. After the run, compare the scores of this run's agents against " +
			"it, see AgentWiseComparison; results go to analysis/agent-wise-comparison. With --post-processing " +
			"post_process_only, the existing output is compared without running.")
	private Path agentWiseComparisonBase;

	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {

		// input files may live behind a login (shared-svn); see HttpAuthentication
		HttpAuthentication.installFromEnvironment();

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		sw.setSampleSize(config.qsim().getFlowCapFactor());

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);

//		still registering the duration-binned types, so populations that predate the typicalDuration attribute keep
//		scoring; for attribute-carrying activities the type's typical duration is only a fallback.
		Activities.addScoringParams(config, true, withOpeningTimes);

		ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class)
			.setAllowConfigTypicalDurations(allowConfigTypicalDurations);

//		move the else-branch overnight scoring clamp away from 24:00; see --simulation-period-in-days.
		config.scenario().setSimulationPeriodInDays(simulationPeriodInDays);

		// Required for all calibration strategies
		for (String subpopulation : List.of("person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(planSelector)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(0.15)
					.setSubpopulation(subpopulation)
			);
		}

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

//		write score explanations into person attrs for each person
		config.scoring().setExplainScores(true);

//		also enable plan inheritance analysis
		PlanInheritanceConfigGroup planInheritanceConfigGroup = ConfigUtils.addOrGetModule(config, PlanInheritanceConfigGroup.class);
		planInheritanceConfigGroup.setEnabled(true);


		applyV64ScoringParameters(config);

		// Overwrite ride scoring params with values derived from the v6.4 car parameters.
		RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);

		// Need to switch to warning for best score
		// best score is used because the pseudo random error term are added explicitly in the scoring
		//not best score but selectExp as we removed the taste variations for now gr 10/26
		//if (planSelector.equals(DefaultPlanStrategiesModule.DefaultSelector.BestScore)) {
		//	config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
	//}

		// Bicycle config must be present --> we simulate the bicycle just as in dresden
		//ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		config.qsim().setMainModes(
			List.of(TransportMode.car, TransportMode.truck, "freight", TransportMode.bike)
		);



		// Add emissions configuration
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
		eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
		eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
		eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
		eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction);

		//vsp consistency check does not know about recent changes to time structure, setting this to warn now
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);




		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		// add hbefa link attributes.
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());

		// Force the update of all bike travel times, otherwise bike speeds would only update once a leg is routed
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Leg leg : TripStructureUtils.getLegs(plan)) {
					if (leg.getMode().equals(TransportMode.bike)) {
						leg.setRoute(null);
						leg.setTravelTimeUndefined();
					}
				}
			}
		}

//		ride does not have engineInformation in vehicle types xml file
		if (scenario.getVehicles().getVehicleTypes().get(Id.createVehicleTypeId(TransportMode.ride)).getEngineInformation().getAttributes().isEmpty()) {
			EngineInformation engineInformation = scenario.getVehicles().getVehicleTypes().get(Id.createVehicleTypeId(TransportMode.ride)).getEngineInformation();
			VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
			VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
			VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);

			log.warn("Engine information for {} were added to the respective vehicle type because they were not present." +
				"The vehicle type will be ignored for emission calculation because it is marked as {}", TransportMode.ride, HbefaVehicleCategory.NON_HBEFA_VEHICLE);
		}

		configureBikeVehicleType(scenario);

//		for some of the input vehicle types hbefa emissionConcept and technology are swapped. We have to swap them back.
//		hbefa4.1 relies on HbefaTechnology for correct emission calculation, not on HbefaEmissionConcept
		HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario);

		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();
			if (VehicleUtils.getHbefaTechnology(engineInformation).equals("petrol")) {
//				some veh types use technology "petrol" which does not exist. it either is petrol (4S) or petrol (2S). going for 4S here
				VehicleUtils.setHbefaTechnology(engineInformation, HbefaTechnology.PETROL_4S.id);
				log.warn("For vehicle type {} HbefaTechnology was set to 'petrol'. This is not a possible value. It was changed to {}." +
					"Please check class HbefaTechnology for possibles values.", type.getId(), HbefaTechnology.PETROL_4S.id);
			}
		}
	}

	/**
	 * Apply the bike vehicle specification used by the Dresden scenario, expect the maximum velocity this is from berlin v6.4
	 */
	private static void configureBikeVehicleType(Scenario scenario) {
		Id<VehicleType> bikeTypeId = Id.createVehicleTypeId(TransportMode.bike);
		VehicleType bikeType = scenario.getVehicles().getVehicleTypes().get(bikeTypeId);

		if (bikeType == null) {
			bikeType = scenario.getVehicles().getFactory().createVehicleType(bikeTypeId);
			scenario.getVehicles().addVehicleType(bikeType);
		}

		bikeType.setAccessTime(1.0);
		bikeType.setEgressTime(1.0);
		bikeType.getCapacity().setSeats(0);
		bikeType.getCapacity().setStandingRoom(0);
		bikeType.setLength(2.0);
		bikeType.setWidth(1.0);
		//bikeType.setMaximumVelocity(4.16); --> dresden
		//belwo from berlin v6.4
		bikeType.setMaximumVelocity(2.98);
		bikeType.setPcuEquivalents(0.2);
		bikeType.setNetworkMode(TransportMode.bike);
		bikeType.setFlowEfficiencyFactor(1.0);

		EngineInformation engineInformation = bikeType.getEngineInformation();
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
		VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
		VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
		VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
	}

	@Override
	protected void prepareControler(Controler controler) {

		//TODO seperate observers from configurations
		controler.addOverridingModule(new SimWrapperModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				BerlinDashboardProvider dashboardProvider = new BerlinDashboardProvider();
				Multibinder.newSetBinder( binder(), DashboardProvider.class ).addBinding().toInstance( dashboardProvider );
			}
		});

		//TODO inline this?
		controler.addOverridingModule(new TravelTimeBinding());
		//TODO think about moving this to libs, is this not just the stop watch??
		controler.addOverridingModule(new QsimTimingModule());

		//We want to use the default scoring again
		//controler.addOverridingModule(new BerlinScoringModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
			}
		});

		//TODO make default?
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		// ASCs intentionally remain untouched and are handled by the calibration script.
	}

	@Override
	protected List<MATSimAppCommand> preparePostProcessing(Path outputFolder, String runId) {
		if (agentWiseComparisonBase == null)
			return List.of();
		return List.of(new AgentWiseComparison(agentWiseComparisonBase, outputFolder));
	}

	/**
	 * A controler for an existing scenario, set up the way {@link #prepareControler} sets up a run, but not run.
	 * Analyses that recompute parts of the score take their bindings from its injector, so that they use what the run
	 * used instead of a setup of their own that has to be kept in agreement with it.
	 */
	public static Controler prepareControlerForAnalysis(Scenario scenario) {
		// an output config read back by plain ConfigUtils.loadConfig has the groups prepareConfig adds as untyped
		// groups, but the modules are bound against the typed ones
		Config config = scenario.getConfig();
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class);
		ConfigUtils.addOrGetModule(config, PlanInheritanceConfigGroup.class);
		ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

		Controler controler = new Controler(scenario);
		new OpenBerlinScenario().prepareControler(controler);
		return controler;
	}

	/**
	 * Add travel time bindings for ride and freight modes, which are not actually network modes.
	 */
	public static final class TravelTimeBinding extends AbstractModule {

		private final boolean carOnly;

		public TravelTimeBinding() {
			this.carOnly = false;
		}

		public TravelTimeBinding(boolean carOnly) {
			this.carOnly = carOnly;
		}

		@Override
		public void install() {
			addTravelTimeBinding(TransportMode.ride).to(carTravelTime());
			addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());

			if (!carOnly) {
				//take names from vsp contrib --> SubpopulationDefaultNames
				addTravelTimeBinding("freight").to(Key.get(TravelTime.class, Names.named(TransportMode.truck)));
				addTravelDisutilityFactoryBinding("freight").to(Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.truck)));

				//TODO talk with SM. -->
				//bind(BicycleLinkSpeedCalculator.class).to(BicycleLinkSpeedCalculatorDefaultImpl.class);
				//bind(BicycleParams.class).to(BicycleParamsDefaultImpl.class);

				// Bike should use free speed travel time
				//compensate for bug in core library #4981?
				//TODO think about the effect of this
				//addTravelTimeBinding(TransportMode.bike).to(BicycleTravelTime.class);
				//addTravelDisutilityFactoryBinding(TransportMode.bike).to(OnlyTimeDependentTravelDisutilityFactory.class);
				//addTravelTimeBinding(TransportMode.bike).to(BicycleTravelTime.class);
				//addTravelDisutilityFactoryBinding(TransportMode.bike).to(OnlyTimeDependentTravelDisutilityFactory.class);
			}
		}
	}

	/**
	 * Apply the global and mode-specific v6.4 scoring parameters without changing any ASCs.
	 */
	private static void applyV64ScoringParameters(Config config) {
		ScoringConfigGroup scoring = config.scoring();
		scoring.setPerforming_utils_hr(6.88);
		scoring.setMarginalUtilityOfMoney(1.0);

		setV64ModeParams(scoring, TransportMode.car, -5.0, 0.0, -1.49e-4);
		setV64ModeParams(scoring, TransportMode.pt, -3.0, 0.0, 0.0);
		setV64ModeParams(scoring, TransportMode.bike, 0.0, 0.0, 0.0);
		setV64ModeParams(scoring, TransportMode.walk, 0.0, 0.0, 0.0);
		setV64ModeParams(scoring, "freight", 0.0, 0.0, -4.0e-4);
		setV64ModeParams(scoring, TransportMode.truck, 0.0, 0.0, -4.0e-4);
	}

	private static void setV64ModeParams(ScoringConfigGroup scoring, String mode, double dailyMonetaryConstant,
										 double marginalUtilityOfTraveling, double monetaryDistanceRate) {
		ScoringConfigGroup.ModeParams params = scoring.getOrCreateModeParams(mode);
		params.setDailyMonetaryConstant(dailyMonetaryConstant);
		params.setDailyUtilityConstant(0.0);
		params.setMarginalUtilityOfDistance(0.0);
		params.setMarginalUtilityOfTraveling(marginalUtilityOfTraveling);
		params.setMonetaryDistanceRate(monetaryDistanceRate);
	}


}
