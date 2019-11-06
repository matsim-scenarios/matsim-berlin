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

/**
 * @author Hugo
 */

//to run the program there is a problem with the input files from the config file, I think is very likely that in the current state of the branch 5.5 it is solved 


public class RunBerlinAccidents {
	
	private static final Logger log = Logger.getLogger(RunBerlinAccidents.class);
	
	public static void main(String[] args) throws IOException { 
	
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}
		
	String outputFile = "/../shared-svn/studies/countries/de/accidents/data/output/"; 
	String landOSMInputShapeFile = "/../shared-svn/studies/countries/de/accidents/data/input/osmBerlin/gis.osm_landuse_a_free_1.shx";  
	String placesOSMInputFile = "/../shared-svn/studies/countries/de/accidents/data/input/osmBerlinBrandenburg/gis.osm_landuse_a_free_1_GK4.shx";
	String tunnelLinkCSVInputFile = "D:/SVN-public/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
	//the CSV has also a column for the percentage of the links wich is tunnel, The new String is produced by a new class
	String planfreeLinkCSVInputFile = "/../shared-svn/studies/countries/de/accidents/data/input/CSV files/planfreelinks.csv";
	//The Ids of links the between the two versions have change. Because of that the equivalent in the new network  for the plan free links
	System.out.println(readLinksFromCSV(tunnelLinkCSVInputFile));

	Config config = RunBerlinScenario.prepareConfig(args);
	Scenario scenario = RunBerlinScenario.prepareScenario(config);
	Controler controler = RunBerlinScenario.prepareControler(scenario);
	
	config.controler().setOutputDirectory(outputFile);
	config.controler().setLastIteration(0);

	AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
	accidentsSettings.setScaleFactor(100);
	AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
	accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(landOSMInputShapeFile, placesOSMInputFile,"EPSG:31468", readColumn(1,tunnelLinkCSVInputFile,";"),readLinksFromCSV(planfreeLinkCSVInputFile));
	controler.run();	
	}

	private static String[] readLinksFromCSV(String CSV) {
	    String line = "";
	    //Setting the links that are considered tunnels
	    String [] lines = null;
	    String [] links = null;
	    int a = 0;
	    try(BufferedReader br = new BufferedReader(new FileReader(CSV))) {
	    while ((line = br.readLine()) != null)
	    {	    
	     lines  = line.split(";");
	     links [a] = lines [0];
	     a++;
	    }
	    } 
		catch (IOException e) {
			e.printStackTrace();
	  //Some error logging
	    }
	    return links;
	}	
	
	

	public static String[] readColumn(int numCol ,String CSV, String separator) {
				
		
		//java.util.ArrayList<String> lb = new java.util.ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		String line;

        try (BufferedReader br = new BufferedReader(new FileReader(CSV))) {

            // read line by line
            while ((line = br.readLine()) != null) 
            {
            	String value = "ERROR";
            	String list[] = line.split(separator);
            	if(numCol<list.length) {
            		value = list[numCol];
            	}
            	
                sb.append(value).append(separator);
            }

        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
        
        return sb.toString().split(separator);
	}
	}

