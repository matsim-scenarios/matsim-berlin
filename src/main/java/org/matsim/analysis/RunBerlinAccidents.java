package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsConfigGroup.AccidentsComputationMethod;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


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
	
	//doing some preprocessing
	
	//adding BVWP attributes as link Attributes
	int nofLinks = 0;
    //Setting the links that are considered tunnels
    List<String[]> tunnelLinks  = readLinksFromCSV(tunnelLinkCSVInputFile);  
    List<String[]> planfreeLinks  = readLinksFromCSV(planfreeLinkCSVInputFile);  
	for (Link link : scenario.getNetwork().getLinks().values()) {
	link.getAttributes().putAttribute(accidentsSettings.getAccidentsComputationMethodAttributeName(), AccidentsComputationMethod.BVWP.toString());
	
	//adding the Lanes as an attribute
	int numberOfLanesBVWP = defineNumberofLanes(link);
	int typeofIntersections = 0;
	int typeofRoad = 0;	   
    //comparing Ids from the ones in the list
	typeofIntersections = defineAtributteFromList(tunnelLinks,link, 1, typeofIntersections);
	typeofIntersections = defineAtributteFromList(planfreeLinks,link, 2, typeofIntersections);
	
  	link.getAttributes().putAttribute(accidentsSettings.getBvwpRoadTypeAttributeName(), typeofIntersections +","+ typeofRoad + "," + numberOfLanesBVWP);
    System.out.println("the link " +link.getId().toString() + "was categorized with the Type BVWP" + link.getAttributes().getAttribute(accidentsSettings.getBvwpRoadTypeAttributeName()));
	}
	System.out.println("the number of analyzed links was" + nofLinks);
//	running
	
	controler.run();	
	}
	
//	private String getOSMLandUseFeatureBBId(Link link, Map<String, SimpleFeature> landUseFeaturesBB, Scenario scenario, String osmInputFile) {
//		
//		Point ha = null;
//		
//		if (landUseFeaturesBB == null || landUseFeaturesBB.isEmpty()) return null;
//		
//		AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
//		CoordinateTransformation ctScenarioCRS2osmCRS = TransformationFactory.getCoordinateTransformation(scenario.getConfig().global().getCoordinateSystem() , osmInputFile);
//		
//		Coord linkCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getCoord()); // this Method gives the middle point of the link back
//		Point pMiddle = MGC.xy2Point(linkCoordinateTransformedToOSMCRS.getX(), linkCoordinateTransformedToOSMCRS.getY());
//		
//		Coord linkStartCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getFromNode().getCoord());
//		Point pStart = MGC.xy2Point(linkStartCoordinateTransformedToOSMCRS.getX(), linkStartCoordinateTransformedToOSMCRS.getY());
//		
//		Coord linkEndCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getToNode().getCoord());
//		Point pEnd = MGC.xy2Point(linkEndCoordinateTransformedToOSMCRS.getX(), linkEndCoordinateTransformedToOSMCRS.getY());
//		
//		String osmLandUseFeatureBBId = null;
//		
//		for (SimpleFeature feature : landUseFeaturesBB.values()) {
//			if (((Geometry) feature.getDefaultGeometry()).contains(pMiddle)) {
//				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//			}
//		}
		
//		for (SimpleFeature feature : landUseFeaturesBB.values()) {
//			if (((Geometry) feature.getDefaultGeometry()).contains(pStart)) {
//				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//			}
//		}
//		
//		for (SimpleFeature feature : landUseFeaturesBB.values()) {
//			if (((Geometry) feature.getDefaultGeometry()).contains(pEnd)) {
//				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//			}
//		}
		
		// look around the link
		
//		GeometryFactory geoFac = new GeometryFactory();
//		CoordinateTransformation cTosmCRSToGK4 = TransformationFactory.getCoordinateTransformation(osmInputFile , "EPSG:31468");
//		
//		double distance = 10.0;					
////		log.info("Link ID: " + link.getId());
//		
//		while (osmLandUseFeatureBBId == null && distance <= 500) {
//			Coord coordGK4 = cTosmCRSToGK4.transform(MGC.coordinate2Coord(pMiddle.getCoordinate()));
//			Point pGK4 = geoFac.createPoint(MGC.coord2Coordinate(coordGK4));
//			
//			Point pRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY()));
//			Point pRight = transformPointFromGK4ToOSMCRS(pRightGK4);
//			
//			Point pDownGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() - distance));
//			Point pDown = transformPointFromGK4ToOSMCRS(pDownGK4);
//			
//			Point pLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY()));
//			Point pLeft = transformPointFromGK4ToOSMCRS(pLeftGK4);
//			
//			Point pUpGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() + distance));
//			Point pUp = transformPointFromGK4ToOSMCRS(pUpGK4);
//			
//			Point pUpRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() + distance));
//			Point pUpRight = transformPointFromGK4ToOSMCRS(pUpRightGK4);
//			
//			Point pDownRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() - distance));
//			Point pDownRight = transformPointFromGK4ToOSMCRS(pDownRightGK4);
//			
//			Point pDownLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() - distance));
//			Point pDownLeft = transformPointFromGK4ToOSMCRS(pDownLeftGK4);
//			
//			Point pUpLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() + distance));
//			Point pUpLeft = transformPointFromGK4ToOSMCRS(pUpLeftGK4);
//										
//			for (SimpleFeature feature : landUseFeaturesBB.values()) {
//				
//				if (((Geometry) feature.getDefaultGeometry()).contains(pRight)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDown)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pLeft)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUp)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUpRight)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDownRight)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDownLeft)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUpLeft)) {
//					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
//					return osmLandUseFeatureBBId;
//				}
//			}
//			
//			distance += 10.0;
//		}
//		
//		log.warn("No area type found. Returning null...");
//		return null;
//	}
	
//	private Point transformPointFromGK4ToOSMCRS(Point pointGK4, String toSystem) {
////		AccidentsConfigGroup accidentSettings =  (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
//		CoordinateTransformation ctGK4toOSMCRS = TransformationFactory.getCoordinateTransformation("EPSG:31468", toSystem);
//		Coord coordGK4 = MGC.coordinate2Coord(pointGK4.getCoordinate());
//		Point pointOSMCRS = new GeometryFactory().createPoint(MGC.coord2Coordinate(ctGK4toOSMCRS.transform(coordGK4)));
//		return pointOSMCRS;
//	}

	private static List<String[]> readLinksFromCSV(String CSV) {
	    String line = ";";
	    //Setting the links that are considered tunnels
	    List<String[]> links  = new ArrayList<>();  
	    try(BufferedReader br = new BufferedReader(new FileReader(CSV))) {
	    while ((line = br.readLine()) != null)
	    {
	        links.add(line.split(","));
	    }
	    } 
		catch (IOException e) {
			e.printStackTrace();
	  //Some error logging
	    }
		return (links);
	}
	
	private static int defineNumberofLanes(Link link) {
		int numberOfLanes = 0;
		if (link.getNumberOfLanes() > 4){
			numberOfLanes = 4;
		} else {
			numberOfLanes = (int) link.getNumberOfLanes();
		}
		return numberOfLanes;
	}
	
	private static int defineAtributteFromList(List<String[]> list,Link link, int value, int attribute) {
	        if (list.contains(link.getId())) {
	        	attribute = value;
	        }
	        return (attribute);
	}
	}

