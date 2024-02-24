package org.matsim.run;


import org.matsim.application.MATSimApplication;

/**
 * Run the {@link OpenBerlinScenario} with default configuration.
 */
public final class RunOpenBerlinScenario {

	private RunOpenBerlinScenario() {
	}

	public static void main(String[] args) {
		MATSimApplication.runWithDefaults(OpenBerlinScenario.class, args);
	}

}
