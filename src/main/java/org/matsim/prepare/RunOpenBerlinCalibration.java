package org.matsim.prepare;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.longDistanceFreightGER.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.network.params.ApplyNetworkParams;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.application.prepare.scenario.CreateScenarioCutOut;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.mediumcompressed.MediumCompressedNetworkRouteFactory;
import org.matsim.core.replanning.choosers.ForceInnovationStrategyChooser;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.prepare.choices.ComputePlanChoices;
import org.matsim.prepare.choices.ComputeTripChoices;
import org.matsim.prepare.counts.CreateCountsFromGeoPortalBerlin;
import org.matsim.prepare.counts.CreateCountsFromVMZ;
import org.matsim.prepare.counts.CreateCountsFromVMZOld;
import org.matsim.prepare.download.DownloadCommuterStatistic;
import org.matsim.prepare.drt.CreateDrtVehicles;
import org.matsim.prepare.facilities.CreateMATSimFacilities;
import org.matsim.prepare.facilities.ExtractFacilityGeoPkg;
import org.matsim.prepare.network.LinkCapacityFromMeasurements;
import org.matsim.prepare.opt.ExtractPlanIndexFromType;
import org.matsim.prepare.opt.RunCountOptimization;
import org.matsim.prepare.opt.SelectPlansFromIndex;
import org.matsim.prepare.population.*;
import org.matsim.prepare.pt.EndlessCircleLineScheduleModifier;
import org.matsim.run.Activities;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.run.scoring.experimental.AdvancedScoringConfigGroup;
import org.matsim.run.scoring.BerlinScoringConfigGroup;
import org.matsim.run.scoring.experimental.AdvancedScoringModule;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;
import org.matsim.smallScaleCommercialTrafficGeneration.prepare.CreateDataDistributionOfStructureData;
import picocli.CommandLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * This scenario class is used for run a MATSim scenario in various stages of the calibration process.
 */
@CommandLine.Command(header = ":: Open Berlin Calibration ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
	CreateLandUseShp.class, CreateBerlinPopulation.class, CreateBrandenburgPopulation.class, MergePopulations.class,
	LookupRegioStaR.class, ExtractFacilityGeoPkg.class, DownSamplePopulation.class, DownloadCommuterStatistic.class,
	RunActitopp.class, CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class,
	CleanNetwork.class, CreateMATSimFacilities.class, InitLocationChoice.class, CreateScenarioCutOut.class,
	CreateCountsFromGeoPortalBerlin.class, CreateCountsFromVMZOld.class, CreateCountsFromVMZ.class, ReprojectNetwork.class, RunActivitySampling.class,
	MergePlans.class, SplitActivityTypesDuration.class, CleanPopulation.class, CleanAttributes.class,
	RescheduleLatePlans.class, SplitWrapAroundActivities.class, EncodeTypicalDuration.class, EndTimeToDuration.class,
	GenerateSmallScaleCommercialTrafficDemand.class, CreateDataDistributionOfStructureData.class, LinkCapacityFromMeasurements.class,
	RunCountOptimization.class, SelectPlansFromIndex.class, ExtractPlanIndexFromType.class, AssignReferencePopulation.class, PrepareTasteVariations.class,
	ExtractRelevantFreightTrips.class, CheckCarAvailability.class, FixSubtourModes.class, ComputeTripChoices.class, ComputePlanChoices.class,
	ApplyNetworkParams.class, SetCarAvailabilityByAge.class, CreateDrtVehicles.class, EndlessCircleLineScheduleModifier.class
})
public class RunOpenBerlinCalibration extends MATSimApplication {

	/**
	 * Scaling factor if all persons use car (~20% share).
	 */
	public static final int CAR_FACTOR = 5;
	/**
	 * Flexible activities, which need to be known for location choice and during generation.
	 * A day can not end on a flexible activity.
	 */
	public static final Set<String> FLEXIBLE_ACTS = Set.of("shop_daily", "shop_other", "shopping", "leisure", "dining");
	private static final Logger log = LogManager.getLogger(RunOpenBerlinCalibration.class);
	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(25, 10, 3, 1);
	@CommandLine.Option(names = "--weight", description = "Strategy weight.", defaultValue = "1")
	private double weight;
	@CommandLine.Option(names = "--population", description = "Path to population.")
	private Path populationPath;

	@CommandLine.Option(names = "--scale-factor", description = "Scale factor for capacity to avoid congestions.", defaultValue = "1.5")
	private double scaleFactor;

	@CommandLine.Option(names = "--with-opening-times", description = "Give the activity types their opening times. " +
		"Off by default, matching the scenario: the per-activity typical durations carry the schedule.")
	private boolean withOpeningTimes = false;

	@CommandLine.Option(names = "--simulation-period-in-days", description = "Length of the simulation period, as a " +
		"multiple of 24h. Must match the scenario and the preprocessing.")
	private double simulationPeriodInDays = OpenBerlinScenario.DEFAULT_SIMULATION_PERIOD_IN_DAYS;

	@CommandLine.Option(names = "--allow-config-typical-durations", description = "Allow person-subpopulation " +
		"activities without a typicalDuration attribute to score against the config typical duration. By default " +
		"such an activity ABORTS the run.")
	private boolean allowConfigTypicalDurations = false;

	/**
	 * Round to two digits.
	 */
	public static double roundNumber(double x) {
		return BigDecimal.valueOf(x).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
	}

	/**
	 * Round coordinates to sufficient precision.
	 */
	public static Coord roundCoord(Coord coord) {
		return new Coord(roundNumber(coord.getX()), roundNumber(coord.getY()));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinCalibration.class, args);
	}

	private static Coord getCoord(Scenario scenario, Activity act) {

		if (act.getCoord() != null)
			return act.getCoord();

		if (act.getFacilityId() != null)
			return Objects.requireNonNull(
				scenario.getActivityFacilities().getFacilities().get(act.getFacilityId()),
				() -> "Facility %s not found".formatted(act.getFacilityId())).getCoord();

		return scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord();
	}

	@Override
	@SuppressWarnings("JavaNCSS")
	protected Config prepareConfig(Config config) {

		if (populationPath == null) {
			throw new IllegalArgumentException("Population path is required [--population]");
		}

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.plans().setInputFile(populationPath.toString());
		config.controller().setRunId("cadyts");
		config.scoring().setWriteExperiencedPlans(true);

		Activities.addScoringParams(config, false, withOpeningTimes);

		ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class)
			.setAllowConfigTypicalDurations(allowConfigTypicalDurations);

		config.scenario().setSimulationPeriodInDays(simulationPeriodInDays);

		config.replanningAnnealer().setActivateAnnealingModule(false);
		config.replanning().setFractionOfIterationsToDisableInnovation(0.7);
		config.scoring().setFractionOfIterationsToStartScoreMSA(0.7);


		if (sample.isSet()) {
			config.qsim().setFlowCapFactor(sample.getSample() * (double) CAR_FACTOR);
			config.qsim().setStorageCapFactor(sample.getSample() * (double) CAR_FACTOR);

			// Despite its name, this factor is applied to the simulated flow, not the counts
			config.counts().setCountsScaleFactor(1 / (sample.getSample() * (double) CAR_FACTOR));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		}

		log.info("before: config.qsim().getFlowCapFactor() * scaleFactor = {} * {}", config.qsim().getFlowCapFactor(), scaleFactor);

		// Routes are not relaxed yet, and there should not be too heavy congestion
		// factors are increased to accommodate for more than usual traffic
		config.qsim().setFlowCapFactor(config.qsim().getFlowCapFactor() * scaleFactor);
		config.qsim().setStorageCapFactor(config.qsim().getStorageCapFactor() * scaleFactor);

		log.info("Running with flow and storage capacity: {} / {}", config.qsim().getFlowCapFactor(), config.qsim().getStorageCapFactor());

		config.transit().setUseTransit(false);

		// Only car and ride will be network modes, ride is not simulated on the network though
		config.routing().setNetworkModes(List.of(TransportMode.car, TransportMode.ride));
		config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams(TransportMode.bike)
			.setBeelineDistanceFactor(1.3)
			.setTeleportedModeSpeed(3.1388889)
		);
		config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams(TransportMode.truck)
			.setBeelineDistanceFactor(1.3)
			.setTeleportedModeSpeed(8.3)
		);
		config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams("freight")
			.setBeelineDistanceFactor(1.3)
			.setTeleportedModeSpeed(8.3)
		);

		config.qsim().setMainModes(List.of(TransportMode.car));

		// Required for all calibration strategies
		for (String subpopulation : List.of("person", "commercialPersonTraffic", "commercialPersonTraffic_service", "goodsTraffic")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);
		}

		// Re-route for all populations
		for (String subpopulation : List.of("person", "commercialPersonTraffic", "commercialPersonTraffic_service", "goodsTraffic")) {
			config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
				.setWeight(weight / 8)
				.setSubpopulation(subpopulation)
			);
		}

		// Agents should generally use the faster routes, this is without any mode choice
		config.scoring().getModes().values().forEach(m -> {
			// Only time goes into the score
			m.setMarginalUtilityOfTraveling(-config.scoring().getPerforming_utils_hr());
			m.setConstant(0);
			m.setMarginalUtilityOfDistance(0);
			m.setDailyMonetaryConstant(0);
			m.setDailyUtilityConstant(0);
			m.setMonetaryDistanceRate(0);
		});

		config.controller().setRunId("cadyts");
		config.controller().setOutputDirectory("./output/cadyts-" + scaleFactor);

		// Need to store more plans because of plan types
		config.replanning().setMaxAgentPlanMemorySize(7);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore);

		// Reduce number of threads, to reduce memory usage
		config.global().setNumberOfThreads(Math.min(12, config.global().getNumberOfThreads()));
		config.qsim().setNumberOfThreads(Math.min(12, config.qsim().getNumberOfThreads()));

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (int i = 0; i < person.getPlans().size(); i++) {
				person.getPlans().get(i).setType(String.valueOf(i));
			}
		}

		scenario.getPopulation().getFactory().getRouteFactories()
			.setRouteFactory(NetworkRoute.class, new MediumCompressedNetworkRouteFactory());

		log.info("Converting all agents to car plans.");

		MainModeIdentifier mmi = new DefaultAnalysisMainModeIdentifier();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				final List<PlanElement> planElements = plan.getPlanElements();
				for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
					final List<PlanElement> fullTrip =
						planElements.subList(
							planElements.indexOf(trip.getOriginActivity()) + 1,
							planElements.indexOf(trip.getDestinationActivity()));

					String mode = mmi.identifyMainMode(fullTrip);

					// Already car, nothing to do
					if (Objects.equals(mode, TransportMode.car) ||
						Objects.equals(mode, TransportMode.truck) ||
						Objects.equals(mode, "freight"))
						continue;

					double dist = CoordUtils.calcEuclideanDistance(getCoord(scenario, trip.getOriginActivity()), getCoord(scenario, trip.getDestinationActivity()));

					// short bike and walk trips are not changed
					if (dist <= 350 && (Objects.equals(mode, TransportMode.walk) || Objects.equals(mode, TransportMode.bike)))
						continue;

					// rest of the trips is set to walk if below threshold, car otherwise
					String desiredMode = dist <= 350 ? TransportMode.walk : TransportMode.car;

					fullTrip.clear();
					Leg leg = PopulationUtils.createLeg(desiredMode);
					TripStructureUtils.setRoutingMode(leg, desiredMode);
					fullTrip.add(leg);
				}
			}
		}
	}

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new CadytsCarModule());
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject
			private CadytsContext cadytsContext;

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				Config config = controler.getConfig();

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(30 * config.scoring().getBrainExpBeta());
				sumScoringFunction.addScoringFunction(scoringFunction);

				return sumScoringFunction;
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {
				}).toInstance(new ForceInnovationStrategyChooser<>((int) Math.ceil(1.0 / weight), ForceInnovationStrategyChooser.Permute.yes));
			}
		});

		controler.addOverridingModule(new OpenBerlinScenario.TravelTimeBinding(true));
	}

}
