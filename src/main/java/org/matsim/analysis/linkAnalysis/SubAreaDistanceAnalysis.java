package org.matsim.analysis.linkAnalysis;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.*;

public class SubAreaDistanceAnalysis {
    final static String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";

    private static final Logger log = Logger.getLogger(SubAreaDistanceAnalysis.class);

    public static void main(String[] args) {

        String root = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/";
        //String root = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/";

        String scenario1 = "Scenario";

        Set<String> runIDs = new HashSet<>();
        runIDs.add("A25");
        runIDs.add("A50");
		runIDs.add("A75");
		runIDs.add("A100");
        runIDs.add("C25");
        runIDs.add("C50");
        runIDs.add("C75");
        runIDs.add("C100");

        runIDs.forEach(runID -> processRun(root, scenario1, runID));
    }

    private static void processRun(String root, String scenario1, String runID) {
        String eventsFile = root + scenario1 + runID + ".output_events.xml.gz";
        String outputFileName = "/Users/moritzkreuschner/Desktop/Master Thesis/03_Eventfiles/" + runID + ".subAreaANOtherKm.txt";

//		eventsFile =  "D:/pave_runs/output-p-baseDRT100/p-baseDRT100.output_events.xml.gz";
//		outputFileName = "D:/pave_runs/output-p-baseDRT100/p-baseDRT100.subAreaCarKm.txt";

        log.info("load links-txt-files");
        Set<Id<Link>> berlinLinks = LinksInArea.loadLinksInBerlin();
        Set<Id<Link>> hundekopfLinks = LinksInArea.loadLinksInHundekopf();
        Set<Id<Link>> superblockLinks = LinksInArea.loadLinksInSuperblock();

        Set<?>[] areas =  new Set<?>[3];
        areas[0] = berlinLinks;
        areas[1] = hundekopfLinks;
        areas[2] = superblockLinks;

        SubAreaCarDistanceHandler handler = new SubAreaCarDistanceHandler(NetworkUtils.readNetwork(networkFile), (Set<Id<Link>>[]) areas);
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(handler);


        log.info("start events reading");
        eventsManager.initProcessing();
        EventsUtils.readEvents(eventsManager, eventsFile);


        eventsManager.finishProcessing();

        log.info("dump output to " + outputFileName);
        try {
            BufferedWriter writer =  new BufferedWriter(new FileWriter(outputFileName));
            writer.write("berlinPtKm;" + (handler.ptKm[0] / 1000));
            writer.newLine();
            writer.write("berlinBicycleKm;" + (handler.bicycleKm[0] / 1000));
            writer.newLine();
            writer.write("berlinWalkKm;" + (handler.walkKm[0] / 1000));
            writer.newLine();
            writer.write("berlinRideKm;" + (handler.rideKm[0] / 1000));
            writer.newLine();
            writer.write("berlinFreightKm;" + (handler.freightKm[0] / 1000));
            writer.newLine();
            writer.write("hundeKopfPtKm;" + (handler.ptKm[1] / 1000));
            writer.newLine();
            writer.write("hundeKopfBicycleKm;" + (handler.bicycleKm[1] / 1000));
            writer.newLine();
            writer.write("hundeKopfWalkKm;" + (handler.walkKm[1] / 1000));
            writer.newLine();
            writer.write("hundeKopfRideKm;" + (handler.rideKm[1] / 1000));
            writer.newLine();
            writer.write("hundeKopfFreightKm;" + (handler.freightKm[1] / 1000));
            writer.newLine();
            writer.write("superblockCarKm;" + (handler.carKm[2] / 1000));
            writer.newLine();
            writer.write("superblockPtKm;" + (handler.ptKm[2] / 1000));
            writer.newLine();
            writer.write("superblockBicycleKm;" + (handler.bicycleKm[2] / 1000));
            writer.newLine();
            writer.write("superblockWalkKm;" + (handler.walkKm[2] / 1000));
            writer.newLine();
            writer.write("superblockRideKm;" + (handler.rideKm[2] / 1000));
            writer.newLine();
            writer.write("superblockFreightKm;" + (handler.freightKm[2] / 1000));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Set<Id<Link>> loadLinksFile(String fileName){
        Set<Id<Link>> links = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String header = reader.readLine();
            String line = reader.readLine();
            while(line != null){
                links.add(Id.createLinkId(line));
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not load file " + fileName + ".\n you should run writeLinksInAreasFiles() first");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return links;
    }

}


class SubAreaCarDistanceHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler {

    private final Set<Id<Vehicle>> cars = new HashSet<>();
    private final Set<Id<Person>> carDrivers = new HashSet<>();
    private final Set<Id<Vehicle>> pts = new HashSet<>();
    private final Set<Id<Person>> ptDrivers = new HashSet<>();
    private final Set<Id<Vehicle>> bicycles = new HashSet<>();
    private final Set<Id<Person>> bicyclesDrivers = new HashSet<>();
    private final Set<Id<Vehicle>> walks = new HashSet<>();
    private final Set<Id<Person>> walkDrivers = new HashSet<>();
    private final Set<Id<Vehicle>> rides = new HashSet<>();
    private final Set<Id<Person>> rideDrivers = new HashSet<>();
    private final Set<Id<Vehicle>> freights = new HashSet<>();
    private final Set<Id<Person>> freightsDrivers = new HashSet<>();

    Set<Id<Link>>[] subAreas;
    double[] carKm;
    double[] ptKm;
    double[] bicycleKm;
    double[] walkKm;
    double[] rideKm;
    double[] freightKm;

    private final Network network;

    public SubAreaCarDistanceHandler(Network network, Set<Id<Link>>[] subAreaLinkSets) {
        this.subAreas = subAreaLinkSets;
        this.carKm = new double[subAreas.length];
        this.ptKm = new double[subAreas.length];
        this.bicycleKm = new double[subAreas.length];
        this.walkKm = new double[subAreas.length];
        this.rideKm = new double[subAreas.length];
        this.freightKm = new double[subAreas.length];
        Arrays.fill(carKm, 0.0);
        Arrays.fill(ptKm, 0.0);
        Arrays.fill(bicycleKm, 0.0);
        Arrays.fill(walkKm, 0.0);
        Arrays.fill(rideKm, 0.0);
        Arrays.fill(freightKm, 0.0);
        this.network = network;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if(pts.contains(event.getVehicleId())){
            for (int i = 0; i < subAreas.length; i++) {
                Set<Id<Link>> subArea = subAreas[i];
                if(subArea.contains(event.getLinkId()))
                    ptKm[i] += network.getLinks().get(event.getLinkId()).getLength();
            }
        } else if (bicycles.contains((event.getVehicleId()))){
            for (int i = 0; i < subAreas.length; i++) {
                Set<Id<Link>> subArea = subAreas[i];
                if (subArea.contains(event.getLinkId()))
                    bicycleKm[i] += network.getLinks().get(event.getLinkId()).getLength();
            }
        } else if (walks.contains((event.getVehicleId()))){
            for (int i = 0; i < subAreas.length; i++) {
                Set<Id<Link>> subArea = subAreas[i];
                if (subArea.contains(event.getLinkId()))
                    walkKm[i] += network.getLinks().get(event.getLinkId()).getLength();
            }
        } else if (rides.contains((event.getVehicleId()))){
            for (int i = 0; i < subAreas.length; i++) {
                Set<Id<Link>> subArea = subAreas[i];
                if (subArea.contains(event.getLinkId()))
                    rideKm[i] += network.getLinks().get(event.getLinkId()).getLength();
            }
        } else if (freights.contains((event.getVehicleId()))) {
            for (int i = 0; i < subAreas.length; i++) {
                Set<Id<Link>> subArea = subAreas[i];
                if (subArea.contains(event.getLinkId()))
                    freightKm[i] += network.getLinks().get(event.getLinkId()).getLength();
            }
        } else if (cars.contains((event.getVehicleId()))) {
            for (int i = 0; i < subAreas.length; i++) {
                Set<Id<Link>> subArea = subAreas[i];
                if (subArea.contains(event.getLinkId()))
                    carKm[i] += network.getLinks().get(event.getLinkId()).getLength();
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if(event.getLegMode().equals(TransportMode.pt)){
            this.ptDrivers.add(event.getPersonId());
        } else if (event.getLegMode().equals("bicycle")){
            this.bicyclesDrivers.add(event.getPersonId());
        } else if (event.getLegMode().equals(TransportMode.walk)){
            this.walkDrivers.add(event.getPersonId());
        } else if (event.getLegMode().equals(TransportMode.ride)){
            this.rideDrivers.add(event.getPersonId());
        } else if (event.getLegMode().equals("freight")) {
            this.freightsDrivers.add(event.getPersonId());
        } else if (event.getLegMode().equals(TransportMode.car)) {
            this.carDrivers.add(event.getPersonId());
        }
    }

    @Override
    public void reset(int iteration) {
        this.cars.clear();
        this.carDrivers.clear();
        Arrays.fill(carKm, 0.);
        this.pts.clear();
        this.ptDrivers.clear();
        Arrays.fill(ptKm, 0.);
        this.bicycles.clear();
        this.bicyclesDrivers.clear();
        Arrays.fill(bicycleKm, 0.);
        this.walks.clear();
        this.walkDrivers.clear();
        Arrays.fill(walkKm, 0.);
        this.rides.clear();
        this.rideDrivers.clear();
        Arrays.fill(rideKm, 0.);
        this.freights.clear();
        this.freightsDrivers.clear();
        Arrays.fill(freightKm, 0.);
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        this.cars.remove(event.getVehicleId());
        this.carDrivers.remove(event.getPersonId());
        this.pts.remove(event.getVehicleId());
        this.ptDrivers.remove(event.getPersonId());
        this.walks.remove(event.getVehicleId());
        this.walkDrivers.remove(event.getPersonId());
        this.bicycles.remove(event.getVehicleId());
        this.bicyclesDrivers.remove(event.getPersonId());
        this.rides.remove(event.getVehicleId());
        this.rideDrivers.remove(event.getPersonId());
        this.freights.remove(event.getVehicleId());
        this.freightsDrivers.remove(event.getPersonId());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if(carDrivers.contains(event.getPersonId())) this.cars.add(event.getVehicleId());
        if(ptDrivers.contains(event.getPersonId())) this.pts.add(event.getVehicleId());
        if(bicyclesDrivers.contains(event.getPersonId())) this.bicycles.add(event.getVehicleId());
        if(walkDrivers.contains(event.getPersonId())) this.walks.add(event.getVehicleId());
        if(rideDrivers.contains(event.getPersonId())) this.rides.add(event.getVehicleId());
        if(freightsDrivers.contains(event.getPersonId())) this.freights.add(event.getVehicleId());
    }

}