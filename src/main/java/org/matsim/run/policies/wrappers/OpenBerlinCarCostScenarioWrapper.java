package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinCarCostScenario;

/**
 * Run the car cost scenario for berlin.
 */
public final class OpenBerlinCarCostScenarioWrapper {

	private OpenBerlinCarCostScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinCarCostScenario.class, args);
	}
}
