package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.synthetic.RunOpenBerlinCalibration;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = RunOpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare()
public class RunOpenBerlinScenario extends MATSimApplication {

	private static final Logger log = LogManager.getLogger(RunOpenBerlinCalibration.class);

	public static final String VERSION = "6.0";
	public static final String CRS = "EPSG:25832";

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(100, 25, 10, 1);
	public RunOpenBerlinScenario() {
		super(String.format("input/v%s/berlin-v%s-base-calib.config.xml", VERSION, VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		sw.defaultParams().mapCenter = "13.39,52.51";
		sw.defaultParams().mapZoomLevel = 9.1;

		if (sample.isSet()) {
			double sampleSize = sample.getSize() / 100d;

			config.qsim().setFlowCapFactor(sampleSize);
			config.qsim().setStorageCapFactor(sampleSize);

			// Counts can be scaled with sample size
			config.counts().setCountsScaleFactor(sampleSize);
			sw.defaultParams().sampleSize = String.valueOf(sampleSize);

			config.controler().setRunId(sample.adjustName(config.controler().getRunId()));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		}

		Activities.addScoringParams(config, true);

		// Required for all calibration strategies
		for (String subpopulation : List.of("person", "businessTraffic", "businessTraffic_service")) {
			config.strategy().addStrategySettings(
				new StrategyConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);

			// TODO: other strategies

		}

		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new SimWrapperModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(AnalysisMainModeIdentifier.class).to(RoutingModeMainModeIdentifier.class);
			}
		});

	}
}
