package org.matsim.analysis;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.prepare.accidents.WriteBVWPAccidentRoadTypesIntoLinkAttributes;
import org.matsim.run.RunBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author hacastrom
 *
 */

//Provisional Test class for personal use 
//Test the pre-processing done for the Berlin scenario, it may not be merged on the main branch

public class RunBerlinAccidentsTest {
	
@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
String landOSMInputShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/original-data/osmBerlin/gis.osm_landuse_a_free_1_GK4.shp";  
String tunnelLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
String planfreeLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5.planfree-linkIDs.csv";

	
	private static final Logger log = Logger.getLogger(RunBerlinAccidentsTest.class);
	
	
	@Test
	public final void columnTestTunnels() throws IOException {
		
		String[] testString1 = WriteBVWPAccidentRoadTypesIntoLinkAttributes.readColumn(0, tunnelLinkCSVInputFile, ";");
		Assert.assertEquals("Elements were not read right it should be 9467 and is: " + testString1[2],"9467",testString1[2] );
	}
	
	@Test
	public final void columnTestPlanfree() throws IOException {
		
		String[] testString2 = WriteBVWPAccidentRoadTypesIntoLinkAttributes.readColumn(0, planfreeLinkCSVInputFile, ";");
		Assert.assertEquals("Elements were not read right it should be 9467 and is: " + testString2[2],"100025",testString2[2] );
	}
	
	@Test
	public final void networkProcessing() throws IOException {
		final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
		
		Config config = RunBerlinScenario.prepareConfig(args);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.plans().setInputFile(null);
        ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
        
        Scenario scenario = RunBerlinScenario.prepareScenario(config);
		
        //Back up links of interest
		Id<Link> id1 = Id.createLinkId("100016");
		Link link = scenario.getNetwork().getLinks().get(id1);
		Id<Link> id2 = Id.createLinkId("124900");
		Link link2 = scenario.getNetwork().getLinks().get(id2);
		Id<Link> id3 = Id.createLinkId("149434");
		Link link3 = scenario.getNetwork().getLinks().get(id3);
		
		//erasing links
		Map<Id<Link>, ?> Links = scenario.getNetwork().getLinks();
		for (Id<Link> l : Links.keySet()) {
			scenario.getNetwork().removeLink(l);
		}
		
		//adding links in backup
		scenario.getNetwork().addLink(link);
		scenario.getNetwork().addLink(link2);
		scenario.getNetwork().addLink(link3);
		
		//doing pre-proccesing
		AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
		accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(
				landOSMInputShapeFile,
				"EPSG:31468",
				WriteBVWPAccidentRoadTypesIntoLinkAttributes.readColumn(0,tunnelLinkCSVInputFile,";"),
				WriteBVWPAccidentRoadTypesIntoLinkAttributes.readColumn(0,planfreeLinkCSVInputFile, ";")
				);
		
		//Getting link Attributes
		 String bvwpType1 = scenario.getNetwork().getLinks().get(id1).getAttributes().getAttribute(AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME).toString();
		 String bvwpType2 = scenario.getNetwork().getLinks().get(id2).getAttributes().getAttribute(AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME).toString();
		 String bvwpType3 = scenario.getNetwork().getLinks().get(id3).getAttributes().getAttribute(AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME).toString();

		 //Comparing Link attributes to manually calculated ones
		 Assert.assertEquals("Elements were not read right it should be 1,2,1 and is: " + bvwpType1,"1,2,1",bvwpType1 );
		 Assert.assertEquals("Elements were not read right it should be 1,0,1 and is: " + bvwpType2,"1,0,1",bvwpType2 );
		 Assert.assertEquals("Elements were not read right it should be 2,1,3 and is: " + bvwpType3,"2,1,3",bvwpType3 );
	
	}
}
