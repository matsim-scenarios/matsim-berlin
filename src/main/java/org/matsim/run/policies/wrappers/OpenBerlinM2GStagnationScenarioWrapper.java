package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinM2GStagnationScenario;

/**
 * Run the M2G stagnation scenario for berlin.
 */
public final class OpenBerlinM2GStagnationScenarioWrapper {

	private OpenBerlinM2GStagnationScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinM2GStagnationScenario.class, args);
	}
}
