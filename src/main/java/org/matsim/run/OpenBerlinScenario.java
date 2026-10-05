package org.matsim.run;

import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.InLoopAscCalibration;
import org.matsim.analysis.PointElasticityStatsModule;
import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.bicycle.*;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaTechnology;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.HbefaUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanInheritanceConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
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

	@CommandLine.Option(names = "--scoring-model",
		description = "Choice model parameterization: 'published' uses the scoring values from the config file unchanged; " +
			"'reestimated' overrides them in code with the re-estimated specification (branch michaz/choicemodel).",
		defaultValue = "published")
	private ScoringModel scoringModel;

	@CommandLine.Option(names = "--car-cost-factor",
		description = "Scales car monetary costs (fuel monetaryDistanceRate incl. ride, and daily cost). " +
			"Applied after --scoring-model, for price elasticity experiments.",
		defaultValue = "1.0")
	private double carCostFactor;

	@CommandLine.Option(names = "--pt-cost-factor",
		description = "Scales the pt daily cost. Applied after --scoring-model, for price elasticity experiments.",
		defaultValue = "1.0")
	private double ptCostFactor;

	@CommandLine.Option(names = "--trip-error-scale",
		description = "Standard deviation of the frozen per-trip normal error term, overriding the value the scoring " +
			"model implies (published 0.495, re-estimated 2.213). E.g. 0.703 is the published model with its units " +
			"slip corrected: sqrt(pi^2/6/3.32) instead of pi^2/6/3.32.")
	private Double tripErrorScale;

	@CommandLine.Option(names = "--bike-speed-offset",
		description = "Add this many km/h to the bike vehicle type's maximum velocity. For the bike speed " +
			"elasticity experiment; run it with the constants fixed.",
		defaultValue = "0")
	private double bikeSpeedOffset;

	@CommandLine.Option(names = "--taste-variations",
		description = "'published' applies the per-person mode constant deviations the population carries " +
			"(tasteVariations.variationsOf as configured); 'none' empties variationsOf so the persons' draws are " +
			"ignored and everybody gets the config constants. The income exponent is unaffected.",
		defaultValue = "published")
	private TasteVariations tasteVariations;

	@CommandLine.Option(names = "--in-loop-asc-calibration",
		description = "Calibrate mode ASCs in-loop against SrV shares (Newton-conditioned logit update, " +
			"self-determined convergence; see InLoopAscCalibration).",
		defaultValue = "false")
	private boolean inLoopAscCalibration;

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

	public enum ScoringModel {published, reestimated}

	public enum TasteVariations {published, none}

	public OpenBerlinScenario() {
	}

	public OpenBerlinScenario(Config config) {
		super(config);
	}

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

		if (scoringModel == ScoringModel.reestimated) {
			// Re-estimated choice model (branch michaz/choicemodel; smoke-scale k=9/1pct estimates
			// of 2026-08-21, pending k=70 / 10pct re-estimation). Structure: mode-specific time
			// offsets against performing=6, estimated at trip level (trip_model.py); full
			// unperceived daily costs; ChangeExpBeta supplying the plan-level logit kernel;
			// situational error scale in BerlinScoringModule; no income exponent.
			// Constants are estimation results (plan_model.py, EC+ride_s spec), meant as
			// starting values for ASC calibration.
			ScoringConfigGroup scoring = config.scoring();
			scoring.setPerforming_utils_hr(6.0);
			// pair-consistent with the pt time offset (estimated jointly)
			scoring.setUtilityOfLineSwitch(-0.318692);

			// positive marginalUtilityOfTraveling = travel time partly usable; net cost per hour
			// is performing minus the value (walk -7.13, pt -1.84, car -2.01, bike -3.91, ride -4.02)
			scoring.getOrCreateModeParams(TransportMode.walk)
				.setMarginalUtilityOfTraveling(-1.129768);
			scoring.getOrCreateModeParams(TransportMode.pt)
				.setMarginalUtilityOfTraveling(4.162085)
				.setDailyMonetaryConstant(-3.00)
				.setConstant(-2.932627);
			scoring.getOrCreateModeParams(TransportMode.car)
				.setMarginalUtilityOfTraveling(3.987524)
				.setDailyMonetaryConstant(-14.30)
				.setConstant(-1.664753);
			scoring.getOrCreateModeParams(TransportMode.bike)
				.setMarginalUtilityOfTraveling(2.085135)
				.setConstant(-2.275658);
			scoring.getOrCreateModeParams(TransportMode.ride)
				// 2x car time cost convention collapsed: 2 * 3.987524 - performing
				.setMarginalUtilityOfTraveling(1.975048)
				.setConstant(-7.049106);
			// bus submode penalty dropped in re-estimated spec
			scoring.getOrCreateModeParams("bus").setConstant(0);

			TasteVariationsConfigParameterSet tasteVariations = scoring.getScoringParameters(null).getTasteVariationsParams();
			if (tasteVariations != null) {
				// no income effect identifiable in the choice data
				tasteVariations.setIncomeExponent(0);
				// variationsOf stays as configured (constant): the persisted person attributes are
				// re-interpreted to ride-only in prepareScenario (see there).
			}

			// positive marginalUtilityOfTraveling values are deliberate
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

			// ChangeExpBeta supplies the estimation's residual plan-level Gumbel kernel (scale 1
			// matches the estimation's normalization); frozen error components live in the scores.
			planSelector = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta;
		}

		if (tasteVariations == TasteVariations.none) {
			TasteVariationsConfigParameterSet tv = config.scoring().getScoringParameters(null).getTasteVariationsParams();
			if (tv != null) {
				// an empty set makes IndividualPersonScoringParameters skip the persons' draws (income scaling stays)
				tv.setVariationsOf(Set.of());
				log.info("Taste variations switched off: every person scores with the config constants.");
			}
		}

		// Price experiment factors, applied after model selection so they work in both arms.
		if (carCostFactor != 1.0) {
			ScoringConfigGroup.ModeParams car = config.scoring().getOrCreateModeParams(TransportMode.car);
			car.setMonetaryDistanceRate(car.getMonetaryDistanceRate() * carCostFactor);
			car.setDailyMonetaryConstant(car.getDailyMonetaryConstant() * carCostFactor);
			// ride uses the same vehicle's fuel
			ScoringConfigGroup.ModeParams ride = config.scoring().getOrCreateModeParams(TransportMode.ride);
			ride.setMonetaryDistanceRate(ride.getMonetaryDistanceRate() * carCostFactor);
			log.info("Applied car cost factor {}: monetaryDistanceRate={}, dailyMonetaryConstant={}",
				carCostFactor, car.getMonetaryDistanceRate(), car.getDailyMonetaryConstant());
		}
		if (ptCostFactor != 1.0) {
			ScoringConfigGroup.ModeParams pt = config.scoring().getOrCreateModeParams(TransportMode.pt);
			pt.setDailyMonetaryConstant(pt.getDailyMonetaryConstant() * ptCostFactor);
			log.info("Applied pt cost factor {}: dailyMonetaryConstant={}", ptCostFactor, pt.getDailyMonetaryConstant());
		}

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

		// Need to switch to warning for best score
		// best score is used because the pseudo random error term are added explicitly in the scoring
		if (planSelector.equals(DefaultPlanStrategiesModule.DefaultSelector.BestScore)) {
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		}

		// Bicycle config must be present
		ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);

		// Add emissions configuration
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
		eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
		eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
		eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
		eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		if (scoringModel == ScoringModel.reestimated) {
			// Re-interpret the persisted taste variations for the re-estimated model: person-level
			// variation remains only for ride (household driver availability proxy). The persisted
			// draws are kept per person (identities and common-random-number comparability preserved)
			// and rescaled from the published sd 2.861008 to the re-estimated sd 2.919387. All other
			// modes' deltas are dropped: their variance is carried by the situational error component
			// (BerlinScoringModule) in this specification.
			double rideSdScale = 2.919387 / 2.861008;
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Map<String, Map<ModeUtilityParameters.Type, Double>> variations = PersonUtils.getModeTasteVariations(person);
				if (variations == null)
					continue;
				Map<ModeUtilityParameters.Type, Double> ride = variations.get(TransportMode.ride);
				Map<ModeUtilityParameters.Type, Double> scaledRide = new LinkedHashMap<>();
				if (ride != null) {
					ride.forEach((type, value) -> scaledRide.put(type,
						type == ModeUtilityParameters.Type.constant ? value * rideSdScale : value));
				} else {
					scaledRide.put(ModeUtilityParameters.Type.constant, 0.0);
				}
				Map<String, Map<ModeUtilityParameters.Type, Double>> rideOnly = new LinkedHashMap<>();
				rideOnly.put(TransportMode.ride, scaledRide);
				PersonUtils.setModeTasteVariations(person, rideOnly);
			}
		}

		// add hbefa link attributes.
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());

		if (bikeSpeedOffset != 0) {
			VehicleType bike = scenario.getVehicles().getVehicleTypes().get(Id.createVehicleTypeId(TransportMode.bike));
			double v = bike.getMaximumVelocity() + bikeSpeedOffset / 3.6;
			log.info("Bike speed offset {} km/h: maximum velocity {} -> {} m/s", bikeSpeedOffset, bike.getMaximumVelocity(), v);
			bike.setMaximumVelocity(v);
		}

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

//		bike does not have HbefaTechnology in vehicle types xml file
		VehicleUtils.setHbefaTechnology(scenario.getVehicles().getVehicleTypes().get(Id.createVehicleTypeId(TransportMode.bike)).getEngineInformation(), AVERAGE);
		log.warn("For vehicle type {}, the HbefaTechnolgy was missing and was set to {}.", TransportMode.bike, AVERAGE);

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

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new SimWrapperModule());

		// Guice-bound providers bypass the SimWrapperListener's defaultDashboards check
		// (only SPI-loaded providers are filtered by it), so honor the setting here.
		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(controler.getConfig(), SimWrapperConfigGroup.class);
		if (sw.getDefaultDashboards() != SimWrapperConfigGroup.DefaultDashboardsMode.disabled) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					BerlinDashboardProvider dashboardProvider = new BerlinDashboardProvider();
					Multibinder.newSetBinder( binder(), DashboardProvider.class ).addBinding().toInstance( dashboardProvider );
				}
			});
		}

		controler.addOverridingModule(new TravelTimeBinding());
		controler.addOverridingModule(new QsimTimingModule());

		double errorScale = tripErrorScale != null ? tripErrorScale
			: scoringModel == ScoringModel.reestimated
			? BerlinScoringModule.PSEUDO_RANDOM_SCALE_REESTIMATED
			: BerlinScoringModule.PSEUDO_RANDOM_SCALE_PUBLISHED;
		log.info("Frozen per-trip error term: normal with sd {}", errorScale);
		controler.addOverridingModule(new BerlinScoringModule(errorScale));

		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		// In-loop point elasticity monitor (open-loop, from plan memories; see class javadoc)
		controler.addOverridingModule(new PointElasticityStatsModule());

		// Always installed: provides the (inert-by-default) score offsets the scoring factories
		// depend on; the calibration listener itself only runs with --in-loop-asc-calibration.
		controler.addOverridingModule(new InLoopAscCalibration(inLoopAscCalibration));
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
				addTravelTimeBinding("freight").to(Key.get(TravelTime.class, Names.named(TransportMode.truck)));
				addTravelDisutilityFactoryBinding("freight").to(Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.truck)));

				bind(BicycleLinkSpeedCalculator.class).to(BicycleLinkSpeedCalculatorDefaultImpl.class);
				bind(BicycleParams.class).to(BicycleParamsDefaultImpl.class);

				// Bike should use free speed travel time
				addTravelTimeBinding(TransportMode.bike).to(BicycleTravelTime.class);
				addTravelDisutilityFactoryBinding(TransportMode.bike).to(OnlyTimeDependentTravelDisutilityFactory.class);
			}
		}
	}

}
