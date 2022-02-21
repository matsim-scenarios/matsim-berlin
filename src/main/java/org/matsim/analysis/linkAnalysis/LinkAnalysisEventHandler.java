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

/*
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

    //Mode bicycle
    public static Set<Id> modeBicycle = new HashSet<>();
    public static double totalTimeSpentInModeBicycleBerlin = 0.0;
    public static double totalDistanceTravelledInModeBicycleBerlin = 0.0;
    public static double totalTimeSpentInModeBicycleHundekopf = 0.0;
    public static double totalDistanceTravelledInModeBicycleHundekopf = 0.0;
    public static double totalTimeSpentInModeBicycleSuperblock = 0.0;
    public static double totalDistanceTravelledInModeBicycleSuperblock = 0.0;

    //Mode car
    public static Set<Id> modeCar = new HashSet<>();
    public static double totalTimeSpentInModeCarBerlin = 0.0;
    public static double totalDistanceTravelledInModeCarBerlin = 0.0;
    public static double totalTimeSpentInModeCarHundekopf = 0.0;
    public static double totalDistanceTravelledInModeCarHundekopf = 0.0;
    public static double totalTimeSpentInModeCarSuperblock = 0.0;
    public static double totalDistanceTravelledInModeCarSuperblock = 0.0;

    //Mode pt
    public static Set<Id> modePt = new HashSet<>();
    public static double totalTimeSpentInModePtBerlin = 0.0;
    public static double totalDistanceTravelledInModePtBerlin = 0.0;
    public static double totalTimeSpentInModePtHundekopf = 0.0;
    public static double totalDistanceTravelledInModePtHundekopf = 0.0;
    public static double totalTimeSpentInModePtSuperblock = 0.0;
    public static double totalDistanceTravelledInModePtSuperblock = 0.0;

    //Mode ride
    public static Set<Id> modeRide = new HashSet<>();
    public static double totalTimeSpentInModeRideBerlin = 0.0;
    public static double totalDistanceTravelledInModeRideBerlin = 0.0;
    public static double totalTimeSpentInModeRideHundekopf = 0.0;
    public static double totalDistanceTravelledInModeRideHundekopf = 0.0;
    public static double totalTimeSpentInModeRideSuperblock = 0.0;
    public static double totalDistanceTravelledInModeRideSuperblock = 0.0;

    //Mode freight
    public static Set<Id> modeFreight = new HashSet<>();
    public static double totalTimeSpentInModeFreightBerlin = 0.0;
    public static double totalDistanceTravelledInModeFreightBerlin = 0.0;
    public static double totalTimeSpentInModeFreightHundekopf = 0.0;
    public static double totalDistanceTravelledInModeFreightHundekopf = 0.0;
    public static double totalTimeSpentInModeFreightSuperblock = 0.0;
    public static double totalDistanceTravelledInModeFreightSuperblock = 0.0;

    //Mode walk
    public static Set<Id> modeWalk = new HashSet<>();
    public static double totalTimeSpentInModeWalkBerlin = 0.0;
    public static double totalDistanceTravelledInModeWalkBerlin = 0.0;
    public static double totalTimeSpentInModeWalkHundekopf = 0.0;
    public static double totalDistanceTravelledInModeWalkHundekopf = 0.0;
    public static double totalTimeSpentInModeWalkSuperblock = 0.0;
    public static double totalDistanceTravelledInModeWalkSuperblock = 0.0;

    //Berlin links
    public static List<Id> berlinLinks = new ArrayList<Id>();
    public static Map<Id, Double> vehicleHasEnteredBerlinZone = new HashMap<Id, Double>();
    public static double totalTimeSpentInBerlinZone = 0.0;
    public static double totalDistanceTravelledInBerlinZone = 0.0;
    public static Map<Id, Double> distanceOfLinks1 = new HashMap<Id, Double>();
    public static Set<Id> vehiclesGoingThroughBerlinZone = new HashSet();

    //Hundekopf links
    public static List<Id> hundekopfLinks = new ArrayList<Id>();
    public static Map<Id, Double> vehicleHasEnteredHundekopfZone = new HashMap<Id, Double>();
    public static double totalTimeSpentInHundekopfZone = 0.0;
    public static double totalDistanceTravelledInHundekopfZone = 0.0;
    public static Map<Id, Double> distanceOfLinks2 = new HashMap<Id, Double>();
    public static Set<Id> vehiclesGoingThroughHundekopfZone = new HashSet();

    //Superblock links
    public static List<Id> superblockLinks = new ArrayList<Id>();
    public static Map<Id, Double> vehicleHasEnteredSuperblockZone = new HashMap<Id, Double>();
    public static double totalTimeSpentInSuperblockZone = 0.0;
    public static double totalDistanceTravelledInSuperblockZone = 0.0;
    public static Map<Id, Double> distanceOfLinks3 = new HashMap<Id, Double>();
    public static Set<Id> vehiclesGoingThroughSuperblockZone = new HashSet();


    */
/*//*
/Additional (outside superblock in Hundekopf).
    public static List<Id> HundekopfLinks = new ArrayList<Id>();
    public static double totalTimeSpentOnPrimaryRd = 0.0;
    public static double totalDistanceTravelledOnPrimaryRd = 0.0;
    public static Set<Id> vehiclesGoingThroughPrimaryRd = new HashSet();
    public static Map<Id, Double> vehicleHasEnteredPrimaryRd = new HashMap<Id, Double>();*//*


    //Other
    public static Set<Id> agentsUsingPt = new HashSet<>();

    public LinkAnalysisEventHandler() throws IOException, ParserConfigurationException, SAXException {
        //Create every link list.

            //links in Berlin
            Scanner scanner1 = new Scanner(new File("/Users/moritzkreuschner/Desktop/linksInsideBerlin.txt"));
            while (scanner1.hasNextLine()) {
                String id = scanner1.nextLine();
                berlinLinks.add(Id.createLinkId(id));
            }
            scanner1.close();

            //links in Hundekopf
            Scanner scanner2 = new Scanner(new File("/Users/moritzkreuschner/Desktop/linksInsideHundekopf.txt"));
            while (scanner2.hasNextLine()) {
                String id = scanner2.nextLine();
                hundekopfLinks.add(Id.createLinkId(id));
            }
            scanner2.close();


            //links in Superblocks
            Scanner scanner3 = new Scanner(new File("/Users/moritzkreuschner/Desktop/linksInsideSuperblock_total.txt"));
            while (scanner3.hasNextLine()) {
                String id = scanner3.nextLine();
                superblockLinks.add(Id.createLinkId(id));
            }
            scanner3.close();


            Scanner scanner5 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/C25/affected_Vehicles_C25.txt"));
            while (scanner5.hasNextLine()) {
                String id = scanner5.nextLine();
                affectedVehicles.add(Id.createVehicleId(id));
            }
            scanner5.close();

            Scanner scanner6 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/C25/affected_Residents_C25.txt"));
            while (scanner6.hasNextLine()) {
                String id = scanner6.nextLine();
                affectedResidents.add(Id.createPersonId(id));
            }
            scanner6.close();

            Scanner scanner7 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/C25/Residents_C25.txt"));
            while (scanner7.hasNextLine()) {
                String id = scanner7.nextLine();
                residents.add(Id.createPersonId(id));
            }
            scanner7.close();

            Scanner scanner8 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/C25/noneResidentsAffectedAgents_C25.txt"));
            while (scanner8.hasNextLine()) {
                String id = scanner8.nextLine();
                noneResidentsAffectedAgents.add(Id.createPersonId(id));
            }
            scanner8.close();

            Scanner scanner9 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/C25/affected_Agents_C25.txt"));
            while (scanner9.hasNextLine()) {
                String id = scanner9.nextLine();
                affectedAgents.add(Id.createPersonId(id));
            }
            scanner9.close();

            Scanner scanner10 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/05_Analysis/C25/otherAgents_C25.txt"));
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
                Node node = nodeList.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;
                    Double linkDist = Double.parseDouble(e.getAttribute("length"));
                    distanceOfLinks3.put(Id.createLinkId(e.getAttribute("id")), linkDist);
                }
            }
    }


    @Override
    public void handleEvent(LinkEnterEvent event){
        Id link = event.getLinkId();
        Id vehicle = event.getVehicleId();

        if (berlinLinks.contains(link)){

        }

        if(affectedVehicles.contains(vehicle)){
            totalDistanceTravelledByAffectedVehicles += distanceOfLinks3.get(link);
        }

        if (superblockLinks.contains(link)){
            //Add vehicle to the set of vehicles going through the modified zone.
            vehiclesGoingThroughSuperblockZone.add(event.getVehicleId());
            //Add the link's distance to the total distance travelled counter.
            totalDistanceTravelledInSuperblockZone += distanceOfLinks3.get(event.getLinkId());
            //Vehicle enters for the first time in our zone, so we record the time.
            vehicleHasEnteredSuperblockZone.putIfAbsent(vehicle, event.getTime());
        }else{
            if (vehicleHasEnteredSuperblockZone.containsKey(vehicle)){
                //Vehicle is leaving the modified zone, we add the time spent in traffic to the counter.
                Double currentTime = event.getTime();
                totalTimeSpentInSuperblockZone += (currentTime-vehicleHasEnteredSuperblockZone.get(vehicle));
                //Vehicle's id removed from the map so that next time it enters zone the time is stored.
                vehicleHasEnteredSuperblockZone.remove(vehicle);
            }
        }

        if (berlinLinks.contains(link)){
            //Add vehicle to the set of vehicles going through the modified zone.
            vehiclesGoingThroughBerlinZone.add(event.getVehicleId());
            //Add the link's distance to the total distance travelled counter.
            totalDistanceTravelledInBerlinZone += distanceOfLinks1.get(event.getLinkId());
            //Vehicle enters for the first time in our zone, so we record the time.
            vehicleHasEnteredBerlinZone.putIfAbsent(vehicle, event.getTime());
        }else{
            if (vehicleHasEnteredBerlinZone.containsKey(vehicle)){
                //Vehicle is leaving the modified zone, we add the time spent in traffic to the counter.
                Double currentTime = event.getTime();
                totalTimeSpentInBerlinZone += (currentTime-vehicleHasEnteredBerlinZone.get(vehicle));
                //Vehicle's id removed from the map so that next time it enters zone the time is stored.
                vehicleHasEnteredBerlinZone.remove(vehicle);
            }
        }

        if (hundekopfLinks.contains(link)){
            //Add vehicle to the set of vehicles going through the modified zone.
            vehiclesGoingThroughHundekopfZone.add(event.getVehicleId());
            //Add the link's distance to the total distance travelled counter.
            totalDistanceTravelledInHundekopfZone += distanceOfLinks2.get(event.getLinkId());
            //Vehicle enters for the first time in our zone, so we record the time.
            vehicleHasEnteredHundekopfZone.putIfAbsent(vehicle, event.getTime());
        }else{
            if (vehicleHasEnteredHundekopfZone.containsKey(vehicle)){
                //Vehicle is leaving the modified zone, we add the time spent in traffic to the counter.
                Double currentTime = event.getTime();
                totalTimeSpentInHundekopfZone += (currentTime-vehicleHasEnteredHundekopfZone.get(vehicle));
                //Vehicle's id removed from the map so that next time it enters zone the time is stored.
                vehicleHasEnteredHundekopfZone.remove(vehicle);
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event){
        Id person = event.getPersonId();
        Id link = event.getLinkId();


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
        Id link = event.getLinkId();
        if(berlinLinks.contains(link)){
            totalTimeSpentInModeWalkBerlin += (event.getTime()-timeMap.get(person));
            timeMap.remove(person);

        }


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
        System.out.println("AFFECTED VEHICLES:");
        System.out.println("Number of affected vehicles: " + affectedVehicles.size());
        System.out.println("Total distance travelled by the affected vehicles: " + totalDistanceTravelledByAffectedVehicles);
        System.out.println("Average distance travelled by the affected vehicles: " + totalDistanceTravelledByAffectedVehicles/affectedVehicles.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("Superblock ZONE");
        System.out.println("Total distance travelled in modified zones: " + totalDistanceTravelledInSuperblockZone);
        System.out.println("Total time travelled in modified zones: " + totalTimeSpentInSuperblockZone);
        System.out.println("Total number of vehicles going through modified zones: " + vehiclesGoingThroughSuperblockZone.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("Hundekopf ZONE");
        System.out.println("Total distance travelled in modified zones: " + totalDistanceTravelledInHundekopfZone);
        System.out.println("Total time travelled in modified zones: " + totalTimeSpentInHundekopfZone);
        System.out.println("Total number of vehicles going through modified zones: " + vehiclesGoingThroughHundekopfZone.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("BERLIN ZONE");
        System.out.println("Total distance travelled in modified zones: " + totalDistanceTravelledInBerlinZone);
        System.out.println("Total time travelled in modified zones: " + totalTimeSpentInBerlinZone);
        System.out.println("Total number of vehicles going through modified zones: " + vehiclesGoingThroughBerlinZone.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("AffectedResidents:");
        System.out.println("Number of affected residents: " + affectedResidents.size());
        System.out.println("Total time spent in traffic by the affected residents: " + totalTimeSpentByAffectedResidentsInTraffic);
        System.out.println("Average time spent in traffic by the affected residens: " + totalTimeSpentByAffectedResidentsInTraffic / affectedResidents.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("Residents:");
        System.out.println("Number of residents: " + residents.size());
        System.out.println("Total time spent in traffic by the residents: " + totalTimeSpentByResidentsInTraffic);
        System.out.println("Average time spent in traffic by the residens: " + totalTimeSpentByResidentsInTraffic / residents.size());
        System.out.println("---------------------------------------------------------------");
        System.out.println("Mode biclycle:");
        System.out.println("Total time spent mode bicycle in Berlin: " + totalTimeSpentInModeBicycleBerlin);
        System.out.println("Total time spent mode bicycle in Hundekopf: " + totalTimeSpentInModeBicycleHundekopf);
        System.out.println("Total time spent mode bicycle in Superblock: " + totalTimeSpentInModeBicycleSuperblock);
        System.out.println("Total distance mode bicycle in Berlin: " + totalDistanceTravelledInModeBicycleBerlin);
        System.out.println("Total distance mode bicycle in Hundekopf: " + totalDistanceTravelledInModeBicycleHundekopf);
        System.out.println("Total distance mode bicycle in Superblock: " + totalDistanceTravelledInModeBicycleSuperblock);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Mode car:");
        System.out.println("Total time spent mode car in Berlin: " + totalTimeSpentInModeCarBerlin);
        System.out.println("Total time spent mode car in Hundekopf: " + totalTimeSpentInModeCarHundekopf);
        System.out.println("Total time spent mode car in Superblock: " + totalTimeSpentInModeCarSuperblock);
        System.out.println("Total distance mode car in Berlin: " + totalDistanceTravelledInModeCarBerlin);
        System.out.println("Total distance mode car in Hundekopf: " + totalDistanceTravelledInModeCarHundekopf);
        System.out.println("Total distance mode car in Superblock: " + totalDistanceTravelledInModeCarSuperblock);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Mode ride:");
        System.out.println("Total time spent mode ride in Berlin: " + totalTimeSpentInModeRideBerlin);
        System.out.println("Total time spent mode ride in Hundekopf: " + totalTimeSpentInModeRideHundekopf);
        System.out.println("Total time spent mode ride in Superblock: " + totalTimeSpentInModeRideSuperblock);
        System.out.println("Total distance mode ride in Berlin: " + totalDistanceTravelledInModeRideBerlin);
        System.out.println("Total distance mode ride in Hundekopf: " + totalDistanceTravelledInModeRideHundekopf);
        System.out.println("Total distance mode ride in Superblock: " + totalDistanceTravelledInModeRideSuperblock);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Mode pt:");
        System.out.println("Total time spent mode pt in Berlin: " + totalTimeSpentInModePtBerlin);
        System.out.println("Total time spent mode pt in Hundekopf: " + totalTimeSpentInModePtHundekopf);
        System.out.println("Total time spent mode pt in Superblock: " + totalTimeSpentInModePtSuperblock);
        System.out.println("Total distance mode pt in Berlin: " + totalDistanceTravelledInModePtBerlin);
        System.out.println("Total distance mode pt in Hundekopf: " + totalDistanceTravelledInModePtHundekopf);
        System.out.println("Total distance mode pt in Superblock: " + totalDistanceTravelledInModePtSuperblock);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Mode freight:");
        System.out.println("Total time spent mode freight in Berlin: " + totalTimeSpentInModeFreightBerlin);
        System.out.println("Total time spent mode freight in Hundekopf: " + totalTimeSpentInModeFreightHundekopf);
        System.out.println("Total time spent mode freight in Superblock: " + totalTimeSpentInModeFreightSuperblock);
        System.out.println("Total distance mode freight in Berlin: " + totalDistanceTravelledInModeFreightBerlin);
        System.out.println("Total distance mode freight in Hundekopf: " + totalDistanceTravelledInModeFreightHundekopf);
        System.out.println("Total distance mode freight in Superblock: " + totalDistanceTravelledInModeFreightSuperblock);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Mode walk:");
        System.out.println("Total time spent mode walk in Berlin: " + totalTimeSpentInModeWalkBerlin);
        System.out.println("Total time spent mode walk in Hundekopf: " + totalTimeSpentInModeWalkHundekopf);
        System.out.println("Total time spent mode walk in Superblock: " + totalTimeSpentInModeWalkSuperblock);
        System.out.println("Total distance mode walk in Berlin: " + totalDistanceTravelledInModeWalkBerlin);
        System.out.println("Total distance mode walk in Hundekopf: " + totalDistanceTravelledInModeWalkHundekopf);
        System.out.println("Total distance mode walk in Superblock: " + totalDistanceTravelledInModeWalkSuperblock);
        System.out.println("---------------------------------------------------------------");

    }

}
*/
