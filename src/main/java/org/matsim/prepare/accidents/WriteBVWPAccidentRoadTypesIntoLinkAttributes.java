package org.matsim.prepare.accidents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.run.RunBerlinScenario;


public class WriteBVWPAccidentRoadTypesIntoLinkAttributes {
	
	private static final Logger log = Logger.getLogger(WriteBVWPAccidentRoadTypesIntoLinkAttributes.class);
	
	public static void main(String[] args) throws IOException { 
		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
 			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}
		
	String outputFile = "./scenarios/input/berlin-v5.5-network-with-bvwp-accidents-attributes.xml.gz"; 
	String landOSMInputShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/original-data/osmBerlin/gis.osm_landuse_a_free_1_GK4.shp";  
	
	String tunnelLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
	//the CSV has also a column for the percentage of the links wich is tunnel, The new String is produced by a new class
	String planfreeLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5.planfree-linkIDs.csv";
	//The Ids of links the between the two versions have change. Because of that the equivalent in the new network  for the plan free links
	
	Config config = RunBerlinScenario.prepareConfig(args);
	AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
	
	Scenario scenario = RunBerlinScenario.prepareScenario(config);
	
	AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
	NetworkUtils.writeNetwork(accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(
			landOSMInputShapeFile,
			"EPSG:31468",
			readColumn(0,tunnelLinkCSVInputFile,";"),
			readColumn(0,planfreeLinkCSVInputFile, ";")
			),
			outputFile);

	}
	
	public static String[] readColumn(int numCol, String csvFile, String separator) throws IOException {
		
		StringBuilder sb = new StringBuilder();
		String line;

		URL url = new URL(csvFile);
		BufferedReader br = new BufferedReader(
		        new InputStreamReader(url.openStream()));
		
		// read line by line
        try {
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
			e.printStackTrace();
		}
        
        return sb.toString().split(separator);
	}

}
