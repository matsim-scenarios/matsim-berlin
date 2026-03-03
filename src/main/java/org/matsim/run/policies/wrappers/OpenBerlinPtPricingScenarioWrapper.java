package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinPtPricingScenario;

/**
 * Run the pt pricing scenario for berlin.
 */
public final class OpenBerlinPtPricingScenarioWrapper {

	private OpenBerlinPtPricingScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinPtPricingScenario.class, args);
	}
}
