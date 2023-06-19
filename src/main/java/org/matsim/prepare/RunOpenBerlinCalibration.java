package org.matsim.prepare;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastes;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.choosers.ForceInnovationStrategyChooser;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.prepare.counts.CreateCountsFromGeoPortalBerlin;
import org.matsim.prepare.counts.CreateCountsFromVMZ;
import org.matsim.prepare.download.DownloadCommuterStatistic;
import org.matsim.prepare.network.SampleNetwork;
import org.matsim.prepare.opt.RunCountOptimization;
import org.matsim.prepare.opt.SelectPlansFromIndex;
import org.matsim.run.Activities;
import org.matsim.run.RunOpenBerlinScenario;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.smallScaleCommercialTrafficGeneration.CreateSmallScaleCommercialTrafficDemand;
import picocli.CommandLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This scenario class is used for run a MATSim scenario in various stages of the calibration process.
 */
@CommandLine.Command(header = ":: Open Berlin Calibration ::", version = RunOpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
	CreateLandUseShp.class, CreateBerlinPopulation.class, CreateBrandenburgPopulation.class, MergePopulations.class,
	LookupRegioStaR.class, ExtractFacilityShp.class, DownSamplePopulation.class, DownloadCommuterStatistic.class,
	RunActitopp.class, CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class,
	CleanNetwork.class, SampleNetwork.class, CreateMATSimFacilities.class, InitLocationChoice.class, FilterRelevantAgents.class,
	CreateCountsFromGeoPortalBerlin.class, CreateCountsFromVMZ.class, ReprojectNetwork.class, RunActivitySampling.class,
	MergePlans.class, SplitActivityTypesDuration.class, CleanPopulation.class, CleanAttributes.class,
	CreateSmallScaleCommercialTrafficDemand.class, RunCountOptimization.class, SelectPlansFromIndex.class,
	ExtractRelevantFreightTrips.class, CheckCarAvailability.class, FixSubtourModes.class
})
public class RunOpenBerlinCalibration extends MATSimApplication {

	/**
	 * Flexible activities, which need to be known for location choice and during generation.
	 * A day can not end on a flexible activity.
	 */
	static final Set<String> FLEXIBLE_ACTS = Set.of("shop_daily", "shop_other", "leisure", "dining");
	private static final Logger log = LogManager.getLogger(RunOpenBerlinCalibration.class);
	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(100, 25, 10, 1);
	@CommandLine.Option(names = "--mode", description = "Calibration mode that should be run.")
	private CalibrationMode mode;
	@CommandLine.Option(names = "--weight", description = "Strategy weight for calibration config.", defaultValue = "1")
	private double weight;
	@CommandLine.Option(names = "--population", description = "Path to population")
	private Path populationPath;
	@CommandLine.Option(names = "--scale-factor", description = "Scale factor for capacity.", defaultValue = "1")
	private double scaleFactor;

	@CommandLine.Option(names = "--plan-index", description = "Only use one plan with specified index")
	private Integer planIndex;

	public RunOpenBerlinCalibration() {
		super("input/v6.0/berlin-v6.0-base-calib.config.xml");
	}

	public static double roundNumber(double x) {
		return BigDecimal.valueOf(x).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

	@Override
	protected Config prepareConfig(Config config) {

		if (populationPath == null) {
			throw new IllegalArgumentException("Population path is required [--population]");
		}

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		log.info("Running {} calibration {}", mode, populationPath);

		config.plans().setInputFile(populationPath.toString());
		config.controler().setRunId(mode.toString());
		config.planCalcScore().setWriteExperiencedPlans(true);

		// Location choice does not work with the split types
		Activities.addScoringParams(config, mode != CalibrationMode.locationChoice);

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		sw.defaultParams().mapCenter = "13.39,52.51";
		sw.defaultParams().mapZoomLevel = 9.1;

		if (sample.isSet()) {
			double sampleSize = sample.getSize() / 100d;

			config.qsim().setFlowCapFactor(sampleSize);
			config.qsim().setStorageCapFactor(sampleSize);

			// Counts can be scaled with sample size
			config.counts().setCountsScaleFactor(sampleSize);
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));

			sw.defaultParams().sampleSize = String.valueOf(sampleSize);
		}

		config.counts().setInputFile("./berlin-v6.0-counts-car-vmz.xml.gz");

		// Required for all calibration strategies
		for (String subpopulation : List.of("person", "businessTraffic", "businessTraffic_service")) {
			config.strategy().addStrategySettings(
				new StrategyConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);
		}

		if (mode == null)
			throw new IllegalArgumentException("Calibration mode [--mode} not set!");

		if (mode == CalibrationMode.locationChoice) {

			config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings()
				.setStrategyName(FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY)
				.setWeight(weight)
				.setSubpopulation("person")
			);

			config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
				.setWeight(weight / 5)
				.setSubpopulation("person")
			);

			// Overwrite these to fix scoring warnings
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("pt interaction").setTypicalDuration(30));

			config.vspExperimental().setAbleToOverwritePtInteractionParams(true);

			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
			config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

			FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class);

			dccg.setEpsilonScaleFactors(FLEXIBLE_ACTS.stream().map(s -> "1.0").collect(Collectors.joining(",")));
			dccg.setAlgorithm(FrozenTastesConfigGroup.Algotype.bestResponse);
			dccg.setFlexibleTypes(String.join(",", FLEXIBLE_ACTS));
			dccg.setTravelTimeApproximationLevel(FrozenTastesConfigGroup.ApproximationLevel.localRouting);
			dccg.setRandomSeed(2);
			dccg.setDestinationSamplePercent(25);

		} else if (mode == CalibrationMode.cadyts) {

			/*
			for (String subpopulation : List.of("person", "businessTraffic", "businessTraffic_service")) {
				config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings()
						.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
						.setWeight(weight)
						.setSubpopulation(subpopulation)
				);
			}

			config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
					.setWeight(weight)
					.setSubpopulation("person")
			);
			 */
			// Set high flow capacity
			config.qsim().setFlowCapFactor(config.qsim().getFlowCapFactor() * 4);
			config.qsim().setStorageCapFactor(config.qsim().getStorageCapFactor() * 4);

			config.timeAllocationMutator().setMutationRange(15 * 60);
			config.timeAllocationMutator().setAffectingDuration(false);

			config.counts().setInputFile("./berlin-v6.0-counts-car-vmz.xml.gz");

			config.controler().setRunId("cadyts");
			config.controler().setOutputDirectory("./output/cadyts-" + scaleFactor);

			// No innovation switch-off needed
			config.planCalcScore().setFractionOfIterationsToStartScoreMSA(1.0);
			config.strategy().setFractionOfIterationsToDisableInnovation(1.0);
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore);

		} else if (mode == CalibrationMode.routeChoice) {

			// Re route for all populations
			for (String subpopulation : List.of("person", "businessTraffic", "businessTraffic_service")) {
				config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(weight)
					.setSubpopulation(subpopulation)
				);
			}

		} else if (mode == CalibrationMode.eval) {

			iterations = 0;
			config.controler().setLastIteration(0);

			config.qsim().setFlowCapFactor(config.qsim().getFlowCapFactor() * scaleFactor);
			config.qsim().setStorageCapFactor(config.qsim().getStorageCapFactor() * scaleFactor);

		} else
			throw new IllegalStateException("Mode not implemented:" + mode);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		/*
		if (mode == CalibrationMode.cadyts)
			// each initial plan needs a separate type, so it won't be removed
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (int i = 0; i < person.getPlans().size(); i++) {
					person.getPlans().get(i).setType(String.valueOf(i));
				}
			}
		 */

		if (planIndex != null) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				List<? extends Plan> plans = person.getPlans();
				Set<Plan> toRemove = new HashSet<>();

				for (int i = 0; i < plans.size(); i++) {
					if (i == planIndex) {
						person.setSelectedPlan(plans.get(i));
					} else
						toRemove.add(plans.get(i));
				}
				toRemove.forEach(person::removePlan);
			}
		}
	}

	@Override
	protected void prepareControler(Controler controler) {

		if (mode == CalibrationMode.locationChoice) {
			FrozenTastes.configure(controler);

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					binder().bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {
					}).toInstance(new ForceInnovationStrategyChooser<>(5, ForceInnovationStrategyChooser.Permute.no));
				}
			});

		} else if (mode == CalibrationMode.cadyts) {

			controler.addOverridingModule(new CadytsCarModule());
			controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
				@Inject
				ScoringParametersForPerson parameters;
				@Inject
				private CadytsContext cadytsContext;

				@Override
				public ScoringFunction createNewScoringFunction(Person person) {
					SumScoringFunction sumScoringFunction = new SumScoringFunction();

					Config config = controler.getConfig();

					final ScoringParameters params = parameters.getScoringParameters(person);

					// Only cadyts is scored
//					sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//					sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
//					sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

					// TODO: change the scoring type in the cadyts module
					// to FLOW VEHH

					final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
					scoringFunction.setWeightOfCadytsCorrection(weight * config.planCalcScore().getBrainExpBeta());
					sumScoringFunction.addScoringFunction(scoringFunction);

					return sumScoringFunction;
				}
			});

		}

		controler.addOverridingModule(new SimWrapperModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				install(new SwissRailRaptorModule());
				bind(AnalysisMainModeIdentifier.class).to(RoutingModeMainModeIdentifier.class);
			}
		});
	}

	@Override
	protected List<MATSimAppCommand> preparePostProcessing(Path outputFolder, String runId) {
		return List.of(
			new CleanPopulation().withArgs(
				"--plans", outputFolder.resolve(runId + ".output_plans.xml.gz").toString(),
				"--output", outputFolder.resolve(runId + ".output_selected_plans.xml.gz").toString(),
				"--remove-unselected-plans"
			)
		);
	}

	/**
	 * Different calibration stages.
	 */
	public enum CalibrationMode {
		eval,
		locationChoice,
		cadyts,
		routeChoice
	}

}