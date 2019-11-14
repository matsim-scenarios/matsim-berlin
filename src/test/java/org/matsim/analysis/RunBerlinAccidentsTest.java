package org.matsim.analysis;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author hacastrom
 *
 */

//Provisional Test class for personal use 
//Test the pre-processing done for the Berlin scenario, it may not be merged on the main branch

public class RunBerlinAccidentsTest {
	
String outputFile = "/../../shared-svn/studies/countries/de/accidents/data/output/"; 
String landOSMInputShapeFile = "/../../shared-svn/studies/countries/de/accidents/data/input/osmBerlin/gis.osm_landuse_a_free_1.shx";  
String placesOSMInputFile = "/../../shared-svn/studies/countries/de/accidents/data/input/osmBerlinBrandenburg/gis.osm_landuse_a_free_1_GK4.shx";
String tunnelLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
String planfreeLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5.planfree-linkIDs.csv";

	
	private static final Logger log = Logger.getLogger(RunBerlinAccidentsTest.class);
	
	
	@Test
	public final void columnTestTunnels() throws IOException {
		
		String[] testString1 = RunBerlinAccidents.readColumn(0, tunnelLinkCSVInputFile, ";");
		Assert.assertEquals("Elements were not read right it should be 9467 and is: " + testString1[2],"9467",testString1[2] );
	}
	
	@Test
	public final void columnTestPlanfree() throws IOException {
		
		String[] testString2 = RunBerlinAccidents.readColumn(0, planfreeLinkCSVInputFile, ";");
		Assert.assertEquals("Elements were not read right it should be 9467 and is: " + testString2[2],"100025",testString2[2] );
	}
	
	@Test
	public final void networkProcessing() throws IOException {
		final String[] args = {"scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"};
		Config config = RunBerlinScenario.prepareConfig(args);
		config.controler().setOutputDirectory(outputFile);

		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
		
		Scenario scenario = RunBerlinScenario.prepareScenario(config);
		AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
		accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(
				landOSMInputShapeFile,
				placesOSMInputFile,"EPSG:31468",
				RunBerlinAccidents.readColumn(0,tunnelLinkCSVInputFile,";"),
				RunBerlinAccidents.readColumn(0,planfreeLinkCSVInputFile, ";")
				);
		System.out.println(scenario.getNetwork().getLinks().get("33530"));
		 String bvwpType = scenario.getNetwork().getLinks().get("33530").getAttributes().getAttribute("type").toString();
		 System.out.println(bvwpType);
		 Assert.assertEquals("Elements were not read right it should be 0,3,1 and is: " + bvwpType,"0,3,1",bvwpType );
	}
	
	

}
