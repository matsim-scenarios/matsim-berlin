package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.run.RunBerlinScenario;

public class RunBerlinAccidents {
	
	private static final Logger log = Logger.getLogger(RunBerlinAccidents.class);
	
	public static void main(String[] args) { 
	
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}
		
	String outputFile = "/../../shared-svn/studies/countries/de/accidents/data/output/"; 
	String landOSMInputShapeFile = "/../../shared-svn/studies/countries/de/accidents/data/input/osmBerlin/gis.osm_landuse_a_free_1.shx";  
	String placesOSMInputFile = "/../../shared-svn/studies/countries/de/accidents/data/input/osmBerlinBrandenburg/gis.osm_landuse_a_free_1_GK4.shx";
//	String tunnelLinkCSVInputFile = "/../../shared-svn/studies/countries/de/accidents/data/input/tunnellinks.csv";
//	String planFreeLinkCSVInputFile = "/../../shared-svn/studies/countries/de/accidents/data/input/planfreelinks.csv";
	
	
	Config config = RunBerlinScenario.prepareConfig( args );
	Scenario scenario = RunBerlinScenario.prepareScenario(config);
	Controler controler = RunBerlinScenario.prepareControler(scenario);
	
	config.controler().setOutputDirectory(outputFile);

	AccidentsConfigGroup accidentsConfig = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
	accidentsConfig.setSampleSize(10);
	accidentsConfig.setLanduseOSMInputShapeFile(landOSMInputShapeFile);
	accidentsConfig.setPlacesOSMInputFile(placesOSMInputFile);
//	accidentsConfig.setTunnelLinkCSVInputFile(tunnelLinkCSVInputFile);
//	accidentsConfig.setPlanFreeLinkCSVInputFile(planFreeLinkCSVInputFile);
	controler.run();
	
	
	}
}
