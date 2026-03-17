package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinScooterSharingScenario;

/**
 * Run the eScooter sharing scenario for berlin.
 */
public final class OpenBerlinScooterSharingScenarioWrapper {

	private OpenBerlinScooterSharingScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinScooterSharingScenario.class, args);
	}
}
