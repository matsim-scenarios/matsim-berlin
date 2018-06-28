package org.matsim.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

class KNRunBerlinScenario {
	public static void main(String[] args) {
		String configFile ;
		if ( args.length==0 || args[0].equals("")) {
			configFile = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
//			configFile = "scenarios/berlin-v5.0-0.1pct-2018-06-18/input/berlin-5.0_config_full.xml";
		} else {
			configFile = args[0];
		}
		final Config config = ConfigUtils.loadConfig( configFile );
		
		config.transit().setUsingTransitInMobsim( false );
		
		final RunBerlinScenario berlin = new RunBerlinScenario( config );;
		berlin.run();
	}
	
}
