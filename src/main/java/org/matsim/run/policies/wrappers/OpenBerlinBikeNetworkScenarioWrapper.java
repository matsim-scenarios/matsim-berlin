package org.matsim.run.policies.wrappers;


import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinBikeNetworkScenario;

/**
 * Run the beta money scenario for berlin.
 */
public final class OpenBerlinBikeNetworkScenarioWrapper {

	private OpenBerlinBikeNetworkScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinBikeNetworkScenario.class, args);
	}
}
