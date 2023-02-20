package org.matsim.synthetic;

import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.MergePopulations;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastes;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.run.RunOpenBerlinScenario;
import org.matsim.synthetic.actitopp.RunActitopp;
import org.matsim.synthetic.download.DownloadCommuterStatistic;
import picocli.CommandLine;

import java.util.List;

/**
 * This scenario class is used for run a MATSim scenario in various stages of the calibration process.
 */
@CommandLine.Command(header = ":: Open Berlin Calibration ::", version = RunOpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare({
		CreateLandUseShp.class, CreateBerlinPopulation.class, CreateBrandenburgPopulation.class, MergePopulations.class,
		LookupRegioStaR.class, ExtractFacilityShp.class, DownSamplePopulation.class, DownloadCommuterStatistic.class,
		AssignCommuters.class, RunActitopp.class, CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class,
		CleanNetwork.class, CreateMATSimFacilities.class, InitLocationChoice.class, FilterRelevantAgents.class
})
public class RunOpenBerlinCalibration extends MATSimApplication {

	@CommandLine.Option(names = "--mode", description = "Calibration mode that should be run.")
	private CalibrationMode mode;

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

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		// TODO: typical durations not from data yet
		for (String act : List.of("home", "work", "education", "leisure", "shopping", "other")) {
			config.planCalcScore()
					.addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act).setTypicalDuration(6 * 3600));
		}

		if (sample.isSet()) {
			config.qsim().setFlowCapFactor(sample.getSize() / 100d);
			config.qsim().setStorageCapFactor(sample.getSize() / 100d);

			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		}


		if (mode == null)
			throw new IllegalArgumentException("Calibration mode [--mode} not set!");

		if (mode == CalibrationMode.locationChoice) {

			config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);
			config.facilities().setInputFile("./berlin-v6.0-facilities.xml.gz");

			config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY).setWeight(0.1));
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

		}


		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {

		if (mode == CalibrationMode.locationChoice) {
			FrozenTastes.configure(controler);
		}

	}

	public enum CalibrationMode {
		locationChoice,
		cadyts
	}

}
