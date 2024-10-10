package org.matsim.run.olympiastadion_study;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.run.OpenBerlinScenario.CRS;

public class RunWithTestingPlans {
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.0/input/berlin-v6.0-network-with-pt.xml.gz");
		config.plans().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Berlin/olympiastadion-study/testing-plans/dummy-plans.xml.gz");
		config.transit().setTransitScheduleFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.0/input/berlin-v6.0-transitSchedule.xml.gz");
		config.transit().setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.0/input/berlin-v6.0-transitVehicles.xml.gz");
		config.transit().setUseTransit(true);

		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);

		ScoringConfigGroup.ActivityParams dummyAct = new ScoringConfigGroup.ActivityParams("dummy");
		dummyAct.setTypicalDuration(3600);
		config.scoring().addActivityParams(dummyAct);

		config.global().setCoordinateSystem(CRS);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
				.setWeight(0.7)
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
				.setWeight(0.1)
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
				.setWeight(0.1)
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode)
				.setWeight(0.1)
		);

		config.controller().setLastIteration(1);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setOutputDirectory("/Users/luchengqi/Documents/MATSimScenarios/Berlin/olympiastadion-study/output/dummy-test");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
	}
}
