package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinSiemensScenario;

/**
 * Run the beta money scenario for berlin.
 */
public final class OpenBerlinSiemensScenarioWrapper {

	private OpenBerlinSiemensScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinSiemensScenario.class, args);
	}
}
