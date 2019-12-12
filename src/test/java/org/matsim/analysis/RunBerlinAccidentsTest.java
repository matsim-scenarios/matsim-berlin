package org.matsim.analysis;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.filter.Filter;

/**
 * @author hacastrom
 *
 */

//Provisional Test class for personal use 
//Test the pre-processing done for the Berlin scenario, it may not be merged on the main branch

public class RunBerlinAccidentsTest {
	
String outputFile = "/../../shared-svn/studies/countries/de/accidents/data/output/"; 
String landOSMInputShapeFile = "D:/GIT/shared-svn/studies/countries/de/accidents/data/input/osmBerlinBrandenburg/gis.osm_landuse_a_free_1_GK4.shp";  
String placesOSMInputFile = null;
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
		final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
		
		Config config = RunBerlinScenario.prepareConfig(args);
		config.controler().setOutputDirectory(outputFile);
        AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
		
        Scenario scenario = RunBerlinScenario.prepareScenario(config);
		
		Id<Link> id = Id.createLinkId("100016");
		Link link = scenario.getNetwork().getLinks().get(id);
		Network network = NetworkUtils.createNetwork();
		network.addLink(link);
		NetworkUtils.writeNetwork(network, "");
		Config configtest = ConfigUtils.createConfig();
		
		AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
		accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(
				landOSMInputShapeFile,
				"EPSG:31468",
				RunBerlinAccidents.readColumn(0,tunnelLinkCSVInputFile,";"),
				RunBerlinAccidents.readColumn(0,planfreeLinkCSVInputFile, ";")
				);
		 String bvwpType = scenario.getNetwork().getLinks().get(id).getAttributes().getAttribute(AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME).toString();
		 System.out.println(bvwpType);
//		 Assert.assertEquals("Elements were not read right it should be 1,3,1 and is: " + bvwpType,"1,3,1",bvwpType );
	}
	
	

}
