package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinAdditionalCarModeScenario;

/**
 * Run the additional car mode scenario for berlin.
 */
public final class OpenBerlinAdditionalCarModeScenarioWrapper {

	private OpenBerlinAdditionalCarModeScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinAdditionalCarModeScenario.class, args);
	}
}
