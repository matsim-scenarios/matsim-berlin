package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinEBikeScenario;

/**
 * Run the additional eBike scenario for berlin.
 */
public final class OpenBerlinEBikeScenarioWrapper {

	private OpenBerlinEBikeScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinEBikeScenario.class, args);
	}
}
