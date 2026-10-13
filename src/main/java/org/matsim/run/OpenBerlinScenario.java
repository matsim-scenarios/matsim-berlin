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
import org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaTechnology;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.scenarioCheckers.Hbefa41ScenarioChecker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.HbefaUtils;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanInheritanceConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.checkers.VspScenarioCheckerImpl;
import org.matsim.dashboard.BerlinDashboardProvider;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
@MATSimApplication.Analysis({AgentWiseComparison.class})
public class OpenBerlinScenario extends MATSimApplication {

	public static final String VERSION = "7.1";
	// yyyy need to clarify if this should be 8.<something> on this branch

	public static final String CRS = "EPSG:25832";

	//	To decrypt hbefa input files set MATSIM_DECRYPTION_PASSWORD as environment variable. ask VSP for access.
	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc" ;
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	private static final String AVERAGE = "average";
	// yyyy if this is a hbefa string, it should go into the corresponding global package

	private static final Logger log = LogManager.getLogger(OpenBerlinScenario.VERSION);

	/**
	 * Default length of the simulation period, as a multiple of 24h. 1.125 (= 27:00) makes the non-wrap-around
	 * overnight scoring clamp the last activity at 27:00 instead of 24:00.
	 */
	public static final double DEFAULT_SIMULATION_PERIOD_IN_DAYS = 1.125;

	@CommandLine.Option(names = "--plan-selector",
		description = "Plan selector to use.",
		defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
	private String planSelector;
	/// (The issue here is that CR has switched this to {@link DefaultSelector#BestScore}, since this is in some sense more consistent with frozen randomness.
	/// It has the problem that plans, especially after innovation switchoff, are rarely selected for re-evaluation if they do not have the best score.
	/// yy Unclear /// how we want to proceed here. kai, oct'26)

	/*@CommandLine.Option(names = "--with-opening-times",
		description = "Give the activity types their opening times. Off by default: the per-activity typical " +
			"durations carry the schedule, so the opening times only distort it.")
	private boolean withOpeningTimes = false; */
		// we think that we do not need this any more after the end of the experiments.
		// yy unclear how much of this we want to keep (as functionality) and how that would need to be documented. kai, oct'26

	@CommandLine.Option(names = "--simulation-period-in-days",
		description = "Length of the simulation period, as a multiple of 24h. Moves the else-branch overnight " +
			"scoring clamp: handleOvernightActivity scores the (non-wrap-around) last activity from its start to " +
			"simulationPeriodInDays * 24h. Preprocessing (reschedule-late-plans, encode-typical-duration) must use " +
			"the same value.")
	private double simulationPeriodInDays = DEFAULT_SIMULATION_PERIOD_IN_DAYS;

	/*@CommandLine.Option(names = "--allow-config-typical-durations",
		description = "Allow person-subpopulation activities without a typicalDuration attribute to score against " +
			"the config typical duration. By default such an activity ABORTS the run. Pass this for populations " +
			"whose typical durations are still encoded in the activity type.")
	private boolean allowConfigTypicalDurations = false; */
	// we think that we do not need this any more after the end of the experiments.
	// yy unclear how much of this we want to keep (as functionality) and how that would need to be documented. kai, oct'26

	@CommandLine.Option(names = "--agent-wise-comparison-base",
		description = "Output directory of a base run. After the run, compare the scores of this run's agents against " +
			"it, see AgentWiseComparison; results go to analysis/agent-wise-comparison. With --post-processing " +
			"post_process_only, the existing output is compared without running.")
	// yy what does "the existing output is compared without running" mean?  I do not understand.  compared with what?  not running what?  kai, oct'26
	private Path agentWiseComparisonBase;

	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {

		// input files may live behind a login (shared-svn); see HttpAuthentication
		HttpAuthentication.installFromEnvironment();

		ConfigUtils.addOrGetModule( config, SimWrapperConfigGroup.class ).setSampleSize( config.qsim().getFlowCapFactor() );

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);

//		still registering the duration-binned types, so populations that predate the typicalDuration attribute keep
//		scoring; for attribute-carrying activities the type's typical duration is only a fallback.
		Activities.addScoringParams(config, true, false);
		// yy this looks like we could/should eventually get rid of it??
		// TODO talk with MZ. How do we pull in the act-specific typical-duration here? // DR20261013
		// this is needed for freight, commercial according to TypicalDurations#forPerson

//		ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class).setAllowConfigTypicalDurations(false);
		// see comment at corresponding config switch (also commented out)
		// When we comment this out we also drop the person-activity-specific typical-durations.
		// Currently we can only use them via BerlinScoringFunctionFactory#52
		// sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters, TypicalDurations.forPerson(config, person), person));
		// as far as I see it is not possible to use the default matsim-scoring when we want to use thee person-specific durations.
		// DR20261013

//		move the else-branch overnight scoring clamp away from 24:00; see --simulation-period-in-days.
		config.scenario().setSimulationPeriodInDays(simulationPeriodInDays);

		//switched this from bestScore to ChangeExpBeta.
		// Required for all calibration strategies
		//TODO use subpoulations default names --> habe ich gemacht. kai
		for (String subpopulation : List.of( SUBPOP_PERSON, SUBPOP_FREIGHT,  SUBPOP_GOODS, SUBPOP_COM_PERSON, SUBPOP_COM_PERSON_SERVICE )) {
			/// yy in the medium term, replace {@link SUBPOP_FREIGHT} by {@link SUBPOP_LONG_DISTANCE_FREIGHT}.
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(planSelector) // setting from cmd line param
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName( DefaultStrategy.ReRoute )
					.setWeight(0.15)
					.setSubpopulation(subpopulation)
			);
		}

		// the following are only for the person subpopulation
		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName( DefaultStrategy.TimeAllocationMutator )
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName( DefaultStrategy.SubtourModeChoice )
				.setWeight(0.15)
				.setSubpopulation("person")
		);

//		write score explanations into person attrs for each person
		//there are now two ways to explain scores do we really want both?
		config.scoring().setExplainScores(true);

//		also enable plan inheritance analysis
		PlanInheritanceConfigGroup planInheritanceConfigGroup = ConfigUtils.addOrGetModule(config, PlanInheritanceConfigGroup.class);
		planInheritanceConfigGroup.setEnabled(true);

		// TODO only here for experiments, move to config when done // DR20261013
		applyV64ScoringParameters(config);

		// taste variations income exponent, taste variations config block muss da sein!
		// set income exponent auf 0.6 basierend auf Poznan. 0.3 ist zu low aus CR Diss und 1.0 ist zu hoch.
		TasteVariationsConfigParameterSet tasteVariations = config.scoring()
			.getScoringParameters(null)
			.getOCreateTasteVariationsParams();
		tasteVariations.setIncomeExponent(0.6);
		tasteVariations.setVariationsOf(Set.of());

		// Overwrite ride scoring params with values derived from the v6.4 car parameters.
		RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);

		// Need to switch to warning for best score
		// best score is used because the pseudo random error term are added explicitly in the scoring
		//not best score but selectExp as we removed the taste variations for now gr 10/26
		//if (planSelector.equals(DefaultPlanStrategiesModule.DefaultSelector.BestScore)) {
		//	config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
	//}

		// add bike as network-mode? // DR 20261013

		config.qsim().setMainModes(
			List.of(TransportMode.car, TransportMode.truck, "freight")
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
		//TODO write down error message
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		// yyyyyy there should be a scenario checker that checks if we are separating out the subpopulation scoring.
		// Programmed by Ricardo.  Maybe this is not yet in 8.0-pre, but it should at least exist already in a comment.
		// kai, oct'26

		// add hbefa link attributes.
		OsmHbefaMapping.build().addHbefaMappings(scenario.getNetwork() );

		// Force the update of all bike travel times, otherwise bike speeds would only update once a leg is routed
		// (this is, e.g., an issue if bicycle speeds are changed from one run to the next, e.g. for speed elasticities)
		// Is this really necessary here? Or only for policies? // DR20261013
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

		Id<VehicleType> bikeTypeId = Id.createVehicleTypeId(TransportMode.bike);
		VehicleType bikeType = scenario.getVehicles().getVehicleTypes().get(bikeTypeId);

		if (bikeType == null) {
			bikeType = scenario.getVehicles().getFactory().createVehicleType(bikeTypeId);
			scenario.getVehicles().addVehicleType(bikeType);
		}

		//check if this is really the case in dresden, should be set to zero!
		//bikeType.setAccessTime(1.0);
		//bikeType.setEgressTime(1.0);
		//bikeType.getCapacity().setSeats(0);
		//bikeType.getCapacity().setStandingRoom(0);
		//not set this
		//bikeType.setLength(2.0);
		//not set this
		//bikeType.setWidth(1.0);

		//bikeType.setMaximumVelocity(4.16); --> dresden
		//below from berlin v6.4
		bikeType.setMaximumVelocity(2.98);
		bikeType.setPcuEquivalents(0.2);
		bikeType.setNetworkMode(TransportMode.bike);
		//bikeType.setFlowEfficiencyFactor(1.0);

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

		if (scenario.getVehicles().getVehicleTypes().get(Id.createVehicleTypeId(TransportMode.bike)).getEngineInformation().getAttributes().isEmpty()) {
			EngineInformation engineInformation1 = bikeType.getEngineInformation();
			//TODO check if this is really needed
			VehicleUtils.setHbefaEmissionsConcept(engineInformation1, AVERAGE);
			VehicleUtils.setHbefaSizeClass(engineInformation1, AVERAGE);
			VehicleUtils.setHbefaTechnology(engineInformation1, AVERAGE);
			//this is needed!
			VehicleUtils.setHbefaVehicleCategory(engineInformation1, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
		}

//		for some of the input vehicle types hbefa emissionConcept and technology are swapped. We have to swap them back.
//		hbefa4.1 relies on HbefaTechnology for correct emission calculation, not on HbefaEmissionConcept
//  WARN Hbefa41ScenarioChecker:94 You have configured vehicle types with invalid hbefa emission concept settings. For Hbefa4.1, the only valid emission concept is: average. If you want to analyse air pollution of different engine types, please use param HbefaTechnology.
// FATAL Hbefa41ScenarioChecker:48 found a situation that leads to vsp-abort.  aborting ...
		scenario.addScenarioChecker( new Hbefa41ScenarioChecker());

		// HbefaUtils.checkAndCorrectHbefaTechnologyAndEmissionConcept(scenario);

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

		//TODO think about moving this to libs, is this not just the stop watch??
		controler.addOverridingModule(new QsimTimingModule());

		//We want to use the default scoring again
		//controler.addOverridingModule(new BerlinScoringModule());

		//TODO make default?
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		//TODO inline this?
		controler.addOverridingModule(new TravelTimeBinding());
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
		//ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class);
		ConfigUtils.addOrGetModule(config, PlanInheritanceConfigGroup.class);
		ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

		Controler controler = new Controler(scenario);
		new OpenBerlinScenario().prepareControler(controler);
		return controler;

		// yy the above has to to do with the question if the agentWiseComparison can be set up globally, or separately for
		// every scenario.  For the main user (kn), a global setup would be __far__ easier.  Discussion exists elsewhere.
	}

	/**
	 * Add travel time bindings for ride and freight modes, which are not actually network modes.
	 */
	@Deprecated
	// yyyy my preference would be to do without this class, since it only contains two lines of material, and adds 20
	// lines of syntactic sugar. kai, oct'26
	public static final class TravelTimeBinding extends AbstractModule {


		private final boolean carOnly;

		@Deprecated
		public TravelTimeBinding() {
			this.carOnly = false;
		}

		@Deprecated //try to do without // The carOnly-stuff is needed for RunOpenBerlinCalibration // DR20261013
		public TravelTimeBinding(boolean carOnly) {
			this.carOnly = carOnly;
		}

		@Override
		public void install() {
			addTravelTimeBinding(TransportMode.ride).to(carTravelTime());
			addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());

			if (!carOnly) {
				//TODO take names from vsp contrib --> SubpopulationDefaultNames
				//was soll diese Sonderbehandlung??
				//in the below there is a confusion between mode and vehicle class
				//the idea here is to differintiate up to 3.5 tonnes Pkw and larger then 3.5 tonnes
				//rename "freight" to truck.
				addTravelTimeBinding("freight").to(Key.get(TravelTime.class, Names.named(TransportMode.truck)));
				addTravelDisutilityFactoryBinding("freight").to(Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.truck)));

				//TODO talk with SM. --> Beschluss: Bike nur als routing network mode und nicht als qsim mode und bike contrib aus pom.xml raus. --> removed bike from pom
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
		//this is different from v6.4
		scoring.setPerforming_utils_hr(6.0);
		scoring.setMarginalUtilityOfMoney(1.0);

		//same as in Berlin v6.4

		setV64ModeParams(scoring, TransportMode.car, -5.0, 0.0, -1.49e-4);
		setV64ModeParams(scoring, TransportMode.pt, -3.0, 0.0, 0.0);
		setV64ModeParams(scoring, TransportMode.bike, 0.0, 0.0, 0.0);
		setV64ModeParams(scoring, TransportMode.walk, 0.0, 0.0, 0.0);
		//TODO: Think about scoring of commercial agents...
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
