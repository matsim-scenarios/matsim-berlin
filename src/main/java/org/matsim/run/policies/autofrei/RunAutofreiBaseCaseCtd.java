package org.matsim.run.policies.autofrei;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.run.OpenBerlinScenario;

public class RunAutofreiBaseCaseCtd extends OpenBerlinScenario {
	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiBaseCaseCtd.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);

		//disabled because some checks have been introduced after this scenario was created and it now fails.
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		AutofreiUtils.cleanPopulation(scenario.getPopulation());
	}
}
