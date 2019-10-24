package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.run.RunBerlinScenario;



public class RunBerlinAccidents {
	
	private static final Logger log = Logger.getLogger(RunBerlinAccidents.class);
	
	public static void main(String[] args) throws IOException { 
	
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}
		
	String outputFile = "/../shared-svn/studies/countries/de/accidents/data/output/"; 
	String landOSMInputShapeFile = "/../shared-svn/studies/countries/de/accidents/data/input/osmBerlin/gis.osm_landuse_a_free_1.shx";  
	String placesOSMInputFile = "/../shared-svn/studies/countries/de/accidents/data/input/osmBerlinBrandenburg/gis.osm_landuse_a_free_1_GK4.shx";
	String tunnelLinkCSVInputFile = "D:/GIT/shared-svn/studies/countries/de/accidents/data/input/CSV files/tunnellinks.csv";
	String planfreeLinkCSVInputFile = "/../shared-svn/studies/countries/de/accidents/data/input/CSV files/planfreelinks.csv";

	//The Ids of the between the two versions have change. BEcause of that the equivalent in the new network should be found 
	
	Config config = RunBerlinScenario.prepareConfig( args );
	Scenario scenario = RunBerlinScenario.prepareScenario(config);
	Controler controler = RunBerlinScenario.prepareControler(scenario);
	
	config.controler().setOutputDirectory(outputFile);
	config.controler().setLastIteration(0);

	
	AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
	accidentsSettings.setScaleFactor(100);
	AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
	accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(landOSMInputShapeFile, placesOSMInputFile, "EPSG:31468" ,readLinksFromCSV(tunnelLinkCSVInputFile),readLinksFromCSV(planfreeLinkCSVInputFile) );
	controler.run();	
	}

	private static String[] readLinksFromCSV(String CSV) {
	    String line = ";";
	    //Setting the links that are considered tunnels
	    String [] links = null;
	    try(BufferedReader br = new BufferedReader(new FileReader(CSV))) {
	    while ((line = br.readLine()) != null)
	    {
	     links  = line.split(",");   
	    }
	    } 
		catch (IOException e) {
			e.printStackTrace();
	  //Some error logging
	    }
	    return links;
	}	
	}

