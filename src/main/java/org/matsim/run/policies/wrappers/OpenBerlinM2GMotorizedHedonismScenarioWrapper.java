package org.matsim.run.policies.wrappers;

import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinM2GMotorizedHedonismScenario;

/**
 * Run the M2G motorized hedonism scenario for berlin.
 */
public final class OpenBerlinM2GMotorizedHedonismScenarioWrapper {

	private OpenBerlinM2GMotorizedHedonismScenarioWrapper() {}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinM2GMotorizedHedonismScenario.class, args);
	}
}
