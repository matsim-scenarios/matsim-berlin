package org.matsim.run.policies.autofrei;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.run.OpenBerlinScenario;

public class RunAutofreiBaseCaseCtd extends OpenBerlinScenario {
	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiBaseCaseCtd.class, args);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		AutofreiUtils.cleanPopulation(scenario.getPopulation());
	}
}
