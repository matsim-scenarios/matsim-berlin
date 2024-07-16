package org.matsim.policies.gartenfeld;

import org.matsim.application.MATSimApplication;
import org.matsim.run.OpenBerlinScenario;

/**
 * Run class for the Gartenfeld scenario.
 */
public final class RunGartenfeldScenario {

	private RunGartenfeldScenario() {
	}

	public static void main(String[] args) {
		MATSimApplication.runWithDefaults(OpenBerlinScenario.class, args,
			"--config:plans.inputPlansFile", "../../input/gartenfeld/gartenfeld-population-10pct.xml.gz",
			"--config:network.inputNetworkFile", "../../input/gartenfeld/gartenfeld-network.xml.gz"
			);
	}

}
