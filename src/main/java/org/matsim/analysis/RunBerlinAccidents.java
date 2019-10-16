package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsConfigGroup.AccidentsComputationMethod;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
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
	String tunnelLinkCSVInputFile = "/../shared-svn/studies/countries/de/accidents/data/input/CSV files/tunnellinks.csv";
	String planFreeLinkCSVInputFile = "/../shared-svn/studies/countries/de/accidents/data/input/CSV files/planfreelinks.csv";

	
	Config config = RunBerlinScenario.prepareConfig( args );
	Scenario scenario = RunBerlinScenario.prepareScenario(config);
	Controler controler = RunBerlinScenario.prepareControler(scenario);
	
	config.controler().setOutputDirectory(outputFile);
	config.controler().setLastIteration(0);

	
	AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
	accidentsSettings.setScaleFactor(100);
	
	//doing some preprocessing
	
	//adding BVWP attributes as link Attributes
	for (Link link : scenario.getNetwork().getLinks().values()) {
	link.getAttributes().putAttribute(accidentsSettings.getAccidentsComputationMethodAttributeName(), AccidentsComputationMethod.BVWP.toString());
	
	//adding the Lanes as an attribute
	int numberOfLanesBVWP;
	int typeofIntersections = 0;
	int typeofRoad = 0;
	
	if (link.getNumberOfLanes() > 4){
		numberOfLanesBVWP = 4;
	} else {
		numberOfLanesBVWP = (int) link.getNumberOfLanes();
	}
	
	String aux = link.getId().toString();
    String line = ";";
    
    //Setting the links that are considered tunnels
    List<String[]> tunnelLinks  = new ArrayList<>();  
    try(BufferedReader br = new BufferedReader(new FileReader(tunnelLinkCSVInputFile))) {
    while ((line = br.readLine()) != null)
    {
        tunnelLinks.add(line.split(","));
    }
    } 
	catch (FileNotFoundException e) {
  //Some error logging
    }
    //comparing Ids from the ones in the list
    for (int x=0; x<tunnelLinks.size();x++ ) {   
    if (aux.equals(tunnelLinks.get(x))) {
    	typeofIntersections = 1;
    }
    
    //Defining with int the attribute for BVWP
    
  	link.getAttributes().putAttribute(accidentsSettings.getBvwpRoadTypeAttributeName(), typeofIntersections +","+ typeofRoad + "," + numberOfLanesBVWP);
    System.out.println("the link " +link.getId().toString() + "was categorized with the Type BVWP" + link.getAttributes().getAttribute(accidentsSettings.getBvwpRoadTypeAttributeName()));
    }
	}


	
//	accidentsSettings.setSampleSize(100);
//	accidentsSettings.setLanduseOSMInputShapeFile(landOSMInputShapeFile);
//	accidentsSettings.setPlacesOSMInputFile(placesOSMInputFile);
//	accidentsSettings.setTunnelLinkCSVInputFile(tunnelLinkCSVInputFile);
//	accidentsSettings.setPlanFreeLinkCSVInputFile(planFreeLinkCSVInputFile);
	controler.run();
	
	
	}
}
