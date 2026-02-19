package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinM2GMultimodalMassScenario;

/**
 * Run the M2G multimodal mass scenario for berlin.
 */
public final class OpenBerlinM2GMultimodalMassScenarioWrapper {

	private OpenBerlinM2GMultimodalMassScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinM2GMultimodalMassScenario.class, args);
	}
}
