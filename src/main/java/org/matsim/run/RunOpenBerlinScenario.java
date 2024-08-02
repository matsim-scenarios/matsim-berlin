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

		// (I think that this will in the end just do MATSimApplication.run( OpenBerlinScenario.class, args ).  kai, aug'24)

		// (That "run" will instantiate an instance of OpenBerlinScenario (*), then do some args consistency checking, then call the piccoli execute method.  kai, aug'24)

		// (The piccoli execute method will essentially call the "call" method of MATSimApplication. kai, aug'24)

		// (I think that in this execution path, this.config in that call method will be null.  (The ctor of MATSimApplication was called via reflection at (*); I think that it was called without a config argument.)
		// This then does:
		// * getCustomModules() (which is empty by default but can be overriden)
		// * ConfigUtils.loadConfig(...) _without_ passing on the args
		// * prepareConfig(...) (which is empty by default but is typically overridden, in this case in OpenBerlinScenario)

	}

}
