package org.matsim.analysis.linkAnalysis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LinkAnalysisEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {
    //Affected Vehicles.
    public static Set<Id> affectedVehicles = new HashSet<>();
    public static double totalDistanceTravelledByAffectedVehicles = 0.0;


    //AffectedResidents.
    public static Set<Id> affectedResidents = new HashSet<>();
    public static double totalTimeSpentByAffectedResidentsInTraffic = 0.0;
    public static Map<Id, Double> timeMap_affectedResidents = new HashMap<>();
    public static Set<Id> affectedResidentsUsingPT = new HashSet<>();

    //Residents.
    public static Set<Id> residents = new HashSet<>();
    public static double totalTimeSpentByResidentsInTraffic = 0.0;
    public static Map<Id, Double> timeMap = new HashMap<>();
    public static Set<Id> residentsUsingPT = new HashSet<>();

    //NoneResidentsAffectedAgents.
    public static Set<Id> noneResidentsAffectedAgents = new HashSet<>();
    public static double totalTimeSpentByNoneResidentsAffectedAgentsInTraffic = 0.0;
    public static Map<Id, Double> timeMap_noneResidentsAffectedAgents = new HashMap<>();
    public static Set<Id> noneResidentsAffectedAgentsUsingPT = new HashSet<>();

    //AffectedAgents.
    public static Set<Id> affectedAgents = new HashSet<>();
    public static double totalTimeSpentByAffectedAgentsInTraffic = 0.0;
    public static Map<Id, Double> timeMap_affectedAgents = new HashMap<>();
    public static Set<Id> affectedAgentsUsingPT = new HashSet<>();

    //OtherAgents.
    public static Set<Id> otherAgents = new HashSet<>();
    public static double totalTimeSpentByOtherAgentsInTraffic = 0.0;
    public static Map<Id, Double> timeMap_otherAgents = new HashMap<>();
    public static Set<Id> otherAgentsUsingPT = new HashSet<>();


    //Modified links
    public static List<Id> modifiedLinks = new ArrayList<Id>();
    public static Map<Id, Double> vehicleHasEnteredModifiedZone = new HashMap<Id, Double>();
    public static double totalTimeSpentInModifiedZone = 0.0;
    public static double totalDistanceTravelledInModifiedZone = 0.0;
    public static Map<Id, Double> distanceOfLinks = new HashMap<Id, Double>();
    public static Set<Id> vehiclesGoingThroughModifiedZone = new HashSet();


    /*//Additional (outside the modified zone).
    public static List<Id> HundekopfLinks = new ArrayList<Id>();
    public static double totalTimeSpentInErnstReuter = 0.0;
    public static double totalDistanceTravelledInErnstReuter = 0.0;
    public static Set<Id> vehiclesGoingThroughErnstReuter = new HashSet();
    public static Map<Id, Double> vehicleHasEnteredHundekopf = new HashMap<Id, Double>();*/

    //Other
    public static Set<Id> agentsUsingPt = new HashSet<>();

    public LinkAnalysisEventHandler() throws IOException, ParserConfigurationException, SAXException {
        //Create every link list.

        String networkInputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/berlin-v5.5-network.xml";
        // Get network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        Path filePath = Paths.get("/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/25percent/NOTin25percent.txt");
        Scanner scanner = new Scanner(filePath);
        List<Integer> NOTin25list = new ArrayList<>();
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                NOTin25list.add(scanner.nextInt());
            } else {
                scanner.next();
            }
        }

        for (int i = 1; i < 160; i++) {
            if (NOTin25list.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader = new ShapeFileReader();
                Collection<SimpleFeature> features = ShapeFileReader.readFileAndInitialize("/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/S000" + i + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries = new HashMap<>();
                for (SimpleFeature feature : features) {
                    zoneGeometries.put((String) feature.getAttribute("Name"),
                            (Geometry) feature.getDefaultGeometry());
                }
                Geometry areaGeometry = zoneGeometries.get("Superblock" + i);

                for (Link link : scenario.getNetwork().getLinks().values()) {

                    Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
                    if (areaGeometry.contains(linkCenterAsPoint)) {
                        modifiedLinks.add(link.getId());
                    }
                }
            }


            Scanner scanner5 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/A25/affected_Vehicles_A25.txt"));
            while (scanner5.hasNextLine()) {
                String id = scanner5.nextLine();
                affectedVehicles.add(Id.createVehicleId(id));
            }
            scanner5.close();

            Scanner scanner6 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/A25/affected_Residents_A25.txt"));
            while (scanner6.hasNextLine()) {
                String id = scanner6.nextLine();
                affectedResidents.add(Id.createPersonId(id));
            }
            scanner6.close();

            Scanner scanner7 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/A25/Person_Superblock_A25.txt"));
            while (scanner7.hasNextLine()) {
                String id = scanner7.nextLine();
                residents.add(Id.createPersonId(id));
            }
            scanner7.close();

            Scanner scanner8 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/A25/noneResidentsAffectedAgents_A25.txt"));
            while (scanner8.hasNextLine()) {
                String id = scanner8.nextLine();
                noneResidentsAffectedAgents.add(Id.createPersonId(id));
            }
            scanner8.close();

            Scanner scanner9 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/A25/affected_Agents_A25.txt"));
            while (scanner9.hasNextLine()) {
                String id = scanner9.nextLine();
                affectedAgents.add(Id.createPersonId(id));
            }
            scanner9.close();

            Scanner scanner10 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/A25/otherAgents_A25.txt"));
            while (scanner10.hasNextLine()) {
                String id = scanner10.nextLine();
                otherAgents.add(Id.createPersonId(id));
            }
            scanner10.close();


            //Now we are going to store all the link distances in the map:
            File network = new File("/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/berlin-v5.5-network.xml");
            Scanner sc = new Scanner(network);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dct = builder.parse(network);
            dct.getDocumentElement().normalize();
            NodeList nodeList = dct.getElementsByTagName("link");
            for (int j = 0; j < nodeList.getLength(); j++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;
                    Double linkDist = Double.parseDouble(e.getAttribute("length"));
                    distanceOfLinks.put(Id.createLinkId(e.getAttribute("id")), linkDist);
                }
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event){
        Id link = event.getLinkId();
        Id vehicle = event.getVehicleId();

        if(affectedVehicles.contains(vehicle)){
            totalDistanceTravelledByAffectedVehicles += distanceOfLinks.get(link);
        }

        if (modifiedLinks.contains(link)){
            //Add vehicle to the set of vehicles going through the modified zone.
            vehiclesGoingThroughModifiedZone.add(event.getVehicleId());
            //Add the link's distance to the total distance travelled counter.
            totalDistanceTravelledInModifiedZone += distanceOfLinks.get(event.getLinkId());
            //Vehicle enters for the first time in our zone, so we record the time.
            vehicleHasEnteredModifiedZone.putIfAbsent(vehicle, event.getTime());
        }else{
            if (vehicleHasEnteredModifiedZone.containsKey(vehicle)){
                //Vehicle is leaving the modified zone, we add the time spent in traffic to the counter.
                Double currentTime = event.getTime();
                totalTimeSpentInModifiedZone += (currentTime-vehicleHasEnteredModifiedZone.get(vehicle));
                //Vehicle's id removed from the map so that next time it enters zone the time is stored.
                vehicleHasEnteredModifiedZone.remove(vehicle);
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event){
        Id person = event.getPersonId();
        if(residents.contains(person)){
            timeMap.putIfAbsent(person, event.getTime());
        } else if(noneResidentsAffectedAgents.contains(person)) {
            timeMap_noneResidentsAffectedAgents.putIfAbsent(person, event.getTime());
        } else if(otherAgents.contains(person)){
            timeMap_otherAgents.putIfAbsent(person, event.getTime());
        }

        if(affectedResidents.contains(person)){
            timeMap_affectedResidents.putIfAbsent(person, event.getTime());
        }
        if(affectedAgents.contains(person)){
            timeMap_affectedAgents.putIfAbsent(person, event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event){
        Id person = event.getPersonId();
        if(timeMap.containsKey(person)){
            totalTimeSpentByResidentsInTraffic += (event.getTime()-timeMap.get(person));
            timeMap.remove(person);
        }else if(timeMap_noneResidentsAffectedAgents.containsKey(person)){
            totalTimeSpentByNoneResidentsAffectedAgentsInTraffic += (event.getTime()-timeMap_noneResidentsAffectedAgents.get(person));
            timeMap_noneResidentsAffectedAgents.remove(person);
        }else if(timeMap_otherAgents.containsKey(person)){
            totalTimeSpentByOtherAgentsInTraffic += (event.getTime()-timeMap_otherAgents.get(person));
            timeMap_otherAgents.remove(person);
        }

        if(timeMap_affectedResidents.containsKey(person)){
            totalTimeSpentByAffectedResidentsInTraffic += (event.getTime()-timeMap_affectedResidents.get(person));
            timeMap_affectedResidents.remove(person);
        }
        if(timeMap_affectedAgents.containsKey(person)){
            totalTimeSpentByAffectedAgentsInTraffic += (event.getTime()-timeMap_affectedAgents.get(person));
            timeMap_affectedAgents.remove(person);
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event){
        String activityType = event.getActType();
        Id person = event.getPersonId();
        if (activityType.equals("pt interaction")){
            if(residents.contains(person)){
                residentsUsingPT.add(person);
            }else if(noneResidentsAffectedAgents.contains(person)){
                noneResidentsAffectedAgentsUsingPT.add(person);
            }else if(otherAgents.contains(person)){
                otherAgentsUsingPT.add(person);
            }

            if(affectedResidents.contains(person)){
                affectedResidentsUsingPT.add(person);
            }
            if(affectedAgents.contains(person)){
                affectedAgentsUsingPT.add(person);
            }

            agentsUsingPt.add(person);
        }
    }

    public void printResults(){
        System.out.println("***************************************************************");
        System.out.println("MODIFIED ZONE");
        System.out.println("Total distance travelled in modified zones: " + totalDistanceTravelledInModifiedZone);
        System.out.println("Total time travelled in modified zones: " + totalTimeSpentInModifiedZone);
        System.out.println("Total number of vehicles going through modified zones: " + vehiclesGoingThroughModifiedZone.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("AFFECTED VEHICLES:");
        System.out.println("Number of affected vehicles: " + affectedVehicles.size());
        System.out.println("Total distance travelled by the affected vehicles: " + totalDistanceTravelledByAffectedVehicles);
        System.out.println("Average distance travelled by the affected vehicles: " + totalDistanceTravelledByAffectedVehicles/affectedVehicles.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("AffectedResidents:");
        System.out.println("Number of affected residents: " + affectedResidents.size());
        System.out.println("Total time spent in traffic by the affected residents: " + totalTimeSpentByAffectedResidentsInTraffic);
        System.out.println("Average time spent in traffic by the affected residens: " + totalTimeSpentByAffectedResidentsInTraffic / affectedResidents.size());
        System.out.println("Number of affected residents using pt: " + affectedResidentsUsingPT.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("Residents:");
        System.out.println("Number of residents: " + residents.size());
        System.out.println("Total time spent in traffic by the residents: " + totalTimeSpentByResidentsInTraffic);
        System.out.println("Average time spent in traffic by the residens: " + totalTimeSpentByResidentsInTraffic / residents.size());
        System.out.println("Number of residents using pt: " + residentsUsingPT.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("NoneResidentsAffectedAgents:");
        System.out.println("Number of none-resident affected agents: " + noneResidentsAffectedAgents.size());
        System.out.println("Total time spent in traffic by the none-resident affected agents: " + totalTimeSpentByNoneResidentsAffectedAgentsInTraffic);
        System.out.println("Average time spent in traffic by the none-resident affected agents: " + totalTimeSpentByNoneResidentsAffectedAgentsInTraffic / noneResidentsAffectedAgents.size());
        System.out.println("Number of none-resident affected agents using pt: " + noneResidentsAffectedAgentsUsingPT.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("AffectedAgents:");
        System.out.println("Number of affected agents: " + affectedAgents.size());
        System.out.println("Total time spent in traffic by the affected agents: " + totalTimeSpentByAffectedAgentsInTraffic);
        System.out.println("Average time spent in traffic by the affected agents: " + totalTimeSpentByAffectedAgentsInTraffic / affectedAgents.size());
        System.out.println("Number of affected agents using pt: " + affectedAgentsUsingPT.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("OtherAgents:");
        System.out.println("Number of other agents: " + otherAgents.size());
        System.out.println("Total time spent in traffic by the other agents: " + totalTimeSpentByOtherAgentsInTraffic);
        System.out.println("Average time spent in traffic by the other agents: " + totalTimeSpentByOtherAgentsInTraffic / otherAgents.size());
        System.out.println("Number of other agents using pt: " + otherAgentsUsingPT.size());
        System.out.println("---------------------------------------------------------------");

        System.out.println("OTHER:");
        System.out.println("Number of agents using pt: " + agentsUsingPt.size());
        System.out.println("***************************************************************");
    }

}
