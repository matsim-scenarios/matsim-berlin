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
		// * prepareConfig(...) (which is empty by default but is typically overridden, in this case in OpenBerlinScenario).  In our case, this sets the typical scoring params and the typical replanning strategies.
		// * next one can override the config from some yaml file provided as a commandline option
		// * next args is parsed and set
		// * then some standard CL options are detected and set
		// * then createScenario(config) is called (which can be overwritten but is not)
		// * then prepareScenario(scenario) is called (which can be overwritten but is not)
		// * then a standard controler is created from scenario
		// * then prepareControler is called which can be overwritten

	}

}
