package org.matsim.synthetic;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.MergePopulations;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.prepare.berlinCounts.CreateCountsFromOpenData;
import org.matsim.prepare.berlinCounts.CreateCountsFromVMZ;
import org.matsim.run.RunOpenBerlinScenario;
import org.matsim.synthetic.download.DownloadCommuterStatistic;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This scenario class is used for run a MATSim scenario in various stages of the calibration process.
 */
@CommandLine.Command(header = ":: Open Berlin Calibration ::", version = RunOpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
		CreateLandUseShp.class, CreateBerlinPopulation.class, CreateBrandenburgPopulation.class, MergePopulations.class,
		LookupRegioStaR.class, ExtractFacilityShp.class, DownSamplePopulation.class, DownloadCommuterStatistic.class,
		AssignCommuters.class, RunActitopp.class, CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class,
		CleanNetwork.class, CreateMATSimFacilities.class, InitLocationChoice.class, FilterRelevantAgents.class,
		CreateCountsFromOpenData.class, CreateCountsFromVMZ.class, ReprojectNetwork.class
})
public class RunOpenBerlinCalibration extends MATSimApplication {

	private static final Logger log = LogManager.getLogger(RunOpenBerlinCalibration.class);

	@CommandLine.Option(names = "--mode", description = "Calibration mode that should be run.")
	private CalibrationMode mode;

	@CommandLine.Option(names = "--weight", description = "Strategy weight for calibration config.", defaultValue = "1")
	private double weight;

	@CommandLine.Option(names = "--population", description = "Path to population")
	private Path populationPath;

	@CommandLine.Option(names = "--no-qsim", description = "Disable QSim and use teleportation only", defaultValue = "false")
	private boolean noQSIM;

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(100, 25, 10, 1);

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinCalibration.class, args);
	}

	public RunOpenBerlinCalibration() {
		super("input/v6.0/berlin-v6.0-base-calib.config.xml");
	}

	@Override
	protected Config prepareConfig(Config config) {

		if (populationPath == null) {
			throw new IllegalArgumentException("Population path is required [--population]");
		}

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		log.info("Running {} calibration {}", mode, populationPath);

		config.plans().setInputFile(populationPath.toString());

		// TODO: typical durations not from data yet
		for (String act : List.of("home", "work", "education", "leisure", "shopping", "other")) {
			config.planCalcScore()
					.addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act).setTypicalDuration(6 * 3600));
		}

		config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));

		if (sample.isSet()) {
			config.qsim().setFlowCapFactor(sample.getSize() / 100d);
			config.qsim().setStorageCapFactor(sample.getSize() / 100d);

			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		}


		if (mode == null)
			throw new IllegalArgumentException("Calibration mode [--mode} not set!");

		if (mode == CalibrationMode.locationChoice) {

			// TODO: increase network capacity factor
			// TODO: set all plans to walk/car
			config.qsim().setFlowCapFactor(config.qsim().getFlowCapFactor() * 10.0);
			config.qsim().setStorageCapFactor(config.qsim().getStorageCapFactor() * 10.0);

			config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY).setWeight(weight));
			config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setWeight(1.0));
			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
			config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

			FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class);

			dccg.setEpsilonScaleFactors("1.0,1.0,1.0");
			dccg.setAlgorithm(FrozenTastesConfigGroup.Algotype.bestResponse);
			dccg.setFlexibleTypes("leisure,shopping,other");
			dccg.setTravelTimeApproximationLevel(FrozenTastesConfigGroup.ApproximationLevel.localRouting);
			dccg.setRandomSeed(2);
			dccg.setDestinationSamplePercent(10.);

		} else if (mode == CalibrationMode.cadyts) {

			config.counts().setInputFile("./berlin-v6.0-car-counts.xml.gz");

		} else
			throw new IllegalStateException("Mode not implemented:" + mode);


		if (noQSIM) {
			log.info("Disabling QSim...");

			// Transit will not work without qsim
			config.transit().setUseTransit(false);

			Collection<String> mainModes = config.qsim().getMainModes();
			config.qsim().setMainModes(Set.of());

		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		if (mode == CalibrationMode.locationChoice) {

			// Location choice is based on car
			for (Person p : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : p.getPlans()) {
					for (Leg leg : TripStructureUtils.getLegs(plan)) {
						leg.setMode("car");
						leg.setRoutingMode("car");
					}
				}
			}
		}
	}

	@Override
	protected void prepareControler(Controler controler) {

		if (mode == CalibrationMode.locationChoice) {
			FrozenTastes.configure(controler);
		} else if (mode == CalibrationMode.cadyts) {

			controler.addOverridingModule(new CadytsCarModule());
			controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
				@Inject
				private CadytsContext cadytsContext;
				@Inject
				ScoringParametersForPerson parameters;

				@Override
				public ScoringFunction createNewScoringFunction(Person person) {
					SumScoringFunction sumScoringFunction = new SumScoringFunction();

					Config config = controler.getConfig();

					final ScoringParameters params = parameters.getScoringParameters(person);
					sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
					sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
					sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

					final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
					scoringFunction.setWeightOfCadytsCorrection(weight * config.planCalcScore().getBrainExpBeta());
					sumScoringFunction.addScoringFunction(scoringFunction);

					return sumScoringFunction;
				}
			});

		}

		if (noQSIM) {

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addTravelTimeBinding(TransportMode.ride).to(FreeSpeedTravelTime.class);
					addTravelTimeBinding(TransportMode.car).to(FreeSpeedTravelTime.class);
					addTravelTimeBinding("freight").to(FreeSpeedTravelTime.class);
				}

			});

		}


	}

	public enum CalibrationMode {
		locationChoice,
		cadyts
	}

}
