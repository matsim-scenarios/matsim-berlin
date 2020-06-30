package org.matsim.analysis;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBerlinScenario;

/**
 * @author Hugo
 */


public class RunBerlinAccidents {
	
	private static final Logger log = Logger.getLogger(RunBerlinAccidents.class);
	
	public static void main(String[] args) throws IOException { 
	
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
 			args = new String[] {"scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"}  ;
		}
		
	String outputFile = "scenarios/berlin-v5.5-10pct/output-berlin-v5.5-10pct-accidents/"; 
	String BVWPNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network-with-bvwp-accidents-attributes.xml.gz";
	String plans = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";

	Config config = RunBerlinScenario.prepareConfig(args);
	config.controler().setOutputDirectory(outputFile);
	config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
	config.controler().setLastIteration(500);
	config.plans().setInputFile(plans);
 
	AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
	accidentsSettings.setEnableAccidentsModule(true);
	accidentsSettings.setScaleFactor(10);
  config.network().setInputFile(BVWPNetwork);
  config.planCalcScore().getModes().get("car").setMonetaryDistanceRate(-0.0004);

	Scenario scenario = RunBerlinScenario.prepareScenario(config);

	Controler controler = RunBerlinScenario.prepareControler(scenario);
	controler.addOverridingModule(new AccidentsModule());

	controler.run();
	}
}

