package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.*;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class TransitRouteSplitter {
    private static boolean removeEmptyLines = true;
    private static boolean allowOneStopWithinZone = true ;
    private static int minimumRouteLength = 1 ;
    private static String modifiedRouteSuffix = "_mod";
    private static final Logger log = Logger.getLogger(TransitRouteTrimmer.class);
    private static Scenario scenario ;

//    private Vehicles newVehicles;
//    private Vehicles vehicles;
    private int vehicleCnt = 0;

    public static void main(String[] args) throws IOException, SchemaException {


        final String inScheduleFile = "D:\\runs\\gladbeck\\input\\optimizedSchedule.xml.gz";
        final String inVehiclesFile = "D:\\runs\\gladbeck\\input\\optimizedVehicles.xml.gz";
        final String inNetworkFile = "D:\\runs\\gladbeck\\input\\optimizedNetwork.xml.gz";
        final String zoneShpFile = "D:/runs/gladbeck/input/area_B_en_detail.shp";
        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output2/";


//        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedSchedule.xml.gz";
//        final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedVehicles.xml.gz";
//        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedNetwork.xml.gz";
//        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/gladbeck_umland.shp";
//        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output2/";

//        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
//        final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz";
//        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
//        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
//        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output/";

        TransitRouteSplitter transitRouteSplitter = new TransitRouteSplitter();

        // Prepare Scenario
        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.transit().setVehiclesFile(inVehiclesFile);
        config.network().setInputFile(inNetworkFile);

        scenario =  ScenarioUtils.loadScenario(config);

        TransitSchedule inTransitSchedule = scenario.getTransitSchedule();
        new MatsimVehicleReader.VehicleReader(scenario.getVehicles()).readFile(inVehiclesFile);


        // Get Stops within Area
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new File(zoneShpFile).toURI().toURL());//(new URL(zoneShpFile));

        Set<Id<TransitStopFacility>> stopsInArea = new HashSet<>();
        for (TransitStopFacility stop : inTransitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                stopsInArea.add(stop.getId());
            }
        }


        Set<Id<TransitLine>> linesToModify = inTransitSchedule.getTransitLines().keySet(); // all lines will be examined

        for (TransitLine line : inTransitSchedule.getTransitLines().values()) {
            line.getRoutes().get(0).getTransportMode();
        }

        // Base Case
        String caseName = "base";
        TransitRouteTrimmer.countLinesInOut(inTransitSchedule, stopsInArea);
        TransitSchedule2Shape.createShpFile(inTransitSchedule, outputPath + caseName + "Routes.shp");
        writeTransitSchedule(outputPath + caseName + "Sched.xml.gz", scenario, inTransitSchedule);

        //Modify Routes: Split Routes into shorter sections (first stop not included)
//        caseName = "splitWithoutFirstStop";
//        minimumRouteLength = 1 ;
//        allowOneStopWithinZone = false ;
//        TransitSchedule outTransitSchedule = modifyTransitLines(inTransitSchedule, linesToModify, stopsInArea, scenario);
//        System.out.println("\n Modify Routes: " + caseName);
//        TransitRouteTrimmer.countLinesInOut(outTransitSchedule, stopsInArea);
//        TransitSchedule2Shape.createShpFile(inTransitSchedule, outputPath + caseName + "Routes.shp");
//        writeTransitSchedule(outputPath + caseName + "Sched.xml.gz", scenario, outTransitSchedule);

        //Modify Routes: Split Routes into shorter sections (first stop included)
        caseName = "smaller";
        minimumRouteLength=1;
        allowOneStopWithinZone = true ;
        TransitSchedule outTransitSchedule2 = transitRouteSplitter.modifyTransitLines(inTransitSchedule, linesToModify, stopsInArea, scenario);
        System.out.println("\n Modify Routes: " + caseName);
        TransitRouteTrimmer.countLinesInOut(outTransitSchedule2, stopsInArea);
        TransitSchedule2Shape.createShpFile(outTransitSchedule2, outputPath + caseName + "Routes.shp");
        writeTransitSchedule(outputPath + caseName + "Sched.xml.gz", scenario, outTransitSchedule2);

//        Vehicles cleanedVehicles = TransitUtilsJR.removeUnusedVehicles(scenario.getVehicles(), outTransitSchedule2);

        new MatsimVehicleWriter(scenario.getVehicles()).writeFile(outputPath + "Vehicles.xml.gz");

    }

    private static void writeTransitSchedule(String outScheduleFile, Scenario scenario, TransitSchedule outTransitSchedule) {
        TransitSchedule outTransitScheduleCleaned = TransitScheduleCleaner.removeStopsNotUsed(outTransitSchedule);
        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(outTransitScheduleCleaned, scenario.getNetwork());
        log.warn(validationResult.getErrors());
        new TransitScheduleWriter(outTransitScheduleCleaned).writeFile(outScheduleFile);
    }


    private TransitSchedule modifyTransitLines(TransitSchedule transitSchedule, Set<Id<TransitLine>> linesToModify, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        TransitSchedule tS = (new TransitScheduleFactoryImpl()).createTransitSchedule();
        Iterator iter = transitSchedule.getFacilities().values().iterator();

        while (iter.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility) iter.next();
            tS.addStopFacility(stop);
        }

        iter = transitSchedule.getTransitLines().values().iterator();

        while (iter.hasNext()) {
            TransitLine line = (TransitLine) iter.next();
            if (!linesToModify.contains(line.getId())) {
                tS.addTransitLine(line);
                continue;
            }


            TransitLine lineNew = transitSchedule.getFactory().createTransitLine(line.getId());
            for (TransitRoute route : line.getRoutes().values()) {
                ArrayList<TransitRoute> routesNew = splitRoutes(route, stopsInArea, scenario);
                for (TransitRoute rt : routesNew) {
                    lineNew.addRoute(rt);
                }
            }

            if (lineNew.getRoutes().size() == 0 && removeEmptyLines) {
                log.info(lineNew.getId() + " does not contain routes. It will NOT be added to the schedule");
                continue;
            }

            tS.addTransitLine(lineNew);

        }

        log.info("Old schedule contained " + transitSchedule.getTransitLines().values().size() + " lines.");
        log.info("New schedule contains " + tS.getTransitLines().values().size() + " lines.");
        return tS;
    }

    private ArrayList<TransitRoute> splitRoutes(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        ArrayList<TransitRoute> resultRoutes = new ArrayList<>();

        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        // Make new stops and links lists
        List<TransitRouteStop> stopsNew = new ArrayList<>();

        int newRouteCnt = 0;
        for (int i = 0; i < stopsOld.size(); i++) {
            Id<TransitStopFacility> stopFacilityId = stopsOld.get(i).getStopFacility().getId();
            if (!stopsInArea.contains(stopFacilityId)) { // we are outside of zone --> we keep the stop

                // adds first stop that's within zone
                if (stopsNew.size() == 0 && i > 0 && allowOneStopWithinZone) {
                    stopsNew.add(stopsOld.get(i - 1));
                }

                stopsNew.add(stopsOld.get(i));
            } else if (stopsInArea.contains(stopFacilityId)) { // inside of zone --> discard

                // The following is only done, if stop i is the first stop to enter the zone.
                if (stopsNew.size() > 0) {
                    //adds first stop in zone
                    if (allowOneStopWithinZone) {
                        stopsNew.add(stopsOld.get(i));
                    }

                    // creates route and clears stopsNew and linksNew
                    if (stopsNew.size() >= minimumRouteLength) {
                        TransitRoute routeNew = createNewRoute(routeOld, scenario, stopsNew, newRouteCnt);
                        resultRoutes.add(routeNew);
                        newRouteCnt++;
                    }
                    stopsNew.clear();
                }
            }
        }

        if (stopsNew.size() >= minimumRouteLength && stopsNew.size() > 0) {
            TransitRoute routeNew = createNewRoute(routeOld, scenario, stopsNew, newRouteCnt);
            resultRoutes.add(routeNew);
        }

        return resultRoutes;
    }


    private TransitRoute createNewRoute(TransitRoute routeOld, Scenario scenario, List<TransitRouteStop> stopsNew, int modNumber) {
//        int routecnt = 0;
//        List<TransitRoute> newRoutes = new ArrayList<TransitRoute>();
        //copy the old Routes link sequence
        List<Id<Link>> oldLinkIds = new ArrayList<>();
        oldLinkIds.add(routeOld.getRoute().getStartLinkId());
        oldLinkIds.addAll(routeOld.getRoute().getLinkIds());
        oldLinkIds.add(routeOld.getRoute().getEndLinkId());
        ListIterator<Id<Link>> linkIdIterator = oldLinkIds.listIterator();
        TransitScheduleFactory tsf = scenario.getTransitSchedule().getFactory();

//        TransitRoute transitRoute;
        NetworkRoute networkRoute;
//        List<TransitRouteStop> stops;
        Id<Link> startLinkId, endLinkId;
        List<Id<Link>> linkIds;
        Id<Link> tempId;
        Double initialDepartureOffset = routeOld.getStops().get(0).getDepartureOffset().seconds();
        Double departureOffset;

        // calculate the departure offset for this fragment
        departureOffset = stopsNew.get(0).getDepartureOffset().seconds() - initialDepartureOffset;
        // create the links for the network route
        linkIds = new ArrayList<>();
        startLinkId = stopsNew.get(0).getStopFacility().getLinkId();
        endLinkId = stopsNew.get(stopsNew.size() - 1).getStopFacility().getLinkId();
        // add all links between start and end
        boolean start = false;
        while (linkIdIterator.hasNext()) {
            tempId = linkIdIterator.next();
            if (!start) {
                // start the sequence with the first link after the start link
                if (tempId.equals(startLinkId)) start = true;
                continue;
            }
            if (tempId.equals(endLinkId)) {
                // end of this route part, skip the end link
                break;
            }
            // it is neither the start nor the end link, thus we store it...
            linkIds.add(tempId);
        }

        // create the network Route
        networkRoute = RouteUtils.createLinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
        List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
        // copy and process the necessary stops. Thus, keep the, the facilities and awaitDeparture but shift the departureOffsets
        stops.addAll(copyStops(stopsNew, tsf, departureOffset));
        // create the new departures
        TransitRoute routeNew = tsf.createTransitRoute(Id.create(routeOld.getId().toString() + "_" + modNumber, TransitRoute.class),
                networkRoute, stops, routeOld.getTransportMode());
        // copy and shift the departures according to the calculated offset
//        for(Departure departure : copyDepartures(routeOld.getDepartures(), tsf, departureOffset)){
//            routeNew.addDeparture(departure);
//        }
        // now we have a new and complete transitRoute
//        return routeNew;
//        newRoutes.add(transitRoute);

//        networkRoute = RouteUtils.createLinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
//
////        NetworkRoute networkRouteNew = RouteUtils.createNetworkRoute(linksNew, scenario.getNetwork());
//        TransitScheduleFactory tsf = scenario.getTransitSchedule().getFactory();
        VehiclesFactory vf = scenario.getTransitVehicles().getFactory();
//
//        String routeIdOld = routeOld.getId().toString();
//        Id<TransitRoute> routeIdNew = Id.create( routeIdOld + modifiedRouteSuffix + modNumber, TransitRoute.class);
//        TransitRoute routeNew = tsf.createTransitRoute(routeIdNew, networkRoute, stopsNew, routeOld.getTransportMode());
        routeNew.setDescription(routeOld.getDescription());
//
        for (Departure departure : routeOld.getDepartures().values()) {
            String vehIdOld = departure.getVehicleId().toString();
//            scenario.getVehicles().removeVehicle(departure.getVehicleId());
            String depIdOld = departure.getId().toString();
            Departure departureNew = tsf.createDeparture(Id.create(depIdOld + modifiedRouteSuffix + modNumber, Departure.class), departure.getDepartureTime());
            departureNew.setVehicleId(Id.createVehicleId(vehIdOld + modifiedRouteSuffix + modNumber));
            routeNew.addDeparture(departureNew);

            Map<Id<Vehicle>, Vehicle> vehicles = scenario.getVehicles().getVehicles();
            Vehicle vehicle1 = vehicles.get(departure.getVehicleId());
            VehicleType vehicleType = vehicle1.getType();
            Vehicle vehicle = vf.createVehicle(Id.createVehicleId(vehIdOld + modifiedRouteSuffix + modNumber), vehicleType);
            if (!scenario.getVehicles().getVehicles().containsKey(vehicle.getId())) {
                scenario.getVehicles().addVehicle(vehicle);
            }
//
        }

//
        return routeNew;
    }



    /**
     * @param departures
     * @param factory
     * @param departureOffset
     * @return
     */
//    private List<Departure> copyDepartures(Map<Id<Departure>, Departure> departures, TransitScheduleFactory factory, double departureOffset) {
//        List<Departure> newDepartures = new ArrayList<Departure>();
//        for(Departure dep: departures.values()){
//            // create a new vehicle of the same type
//            Vehicle v = newVehicles.getFactory().createVehicle(Id.create("cutOff_" + vehicleCnt, Vehicle.class), this.vehicles.getVehicles().get(dep.getVehicleId()).getType());
//            // copy the departures but shift the departures
//            Departure newDep = factory.createDeparture(Id.create(this.vehicleCnt,Departure.class), dep.getDepartureTime() + departureOffset);
//            // set the vehicle for the departure
//            newDep.setVehicleId(v.getId());
//            // store the new vehicle
//            this.newVehicles.getVehicles().put(v.getId(), v);
//            // and the new departure
//            newDepartures.add(newDep);
//            this.vehicleCnt++;
//        }
//        return newDepartures;
//    }


    /**
     * @param s
     * @param factory
     * @return
     */
    private Collection<? extends TransitRouteStop> copyStops(List<TransitRouteStop> s, TransitScheduleFactory factory, Double departureOffset) {
        List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
        TransitRouteStop newStop;
        for(TransitRouteStop oldStop: s){
            //shift the departures, but nothing else... set arrival and departureOffset identical as the arrival offset has no influence and is not compulsory...
            newStop = factory.createTransitRouteStop(oldStop.getStopFacility(), 0, 0); //TODO: REVERT
                    //oldStop.getDepartureOffset().seconds() - departureOffset,
                    //oldStop.getDepartureOffset().seconds() - departureOffset);
            newStop.setAwaitDepartureTime(oldStop.isAwaitDepartureTime());
            stops.add(newStop);
        }
        return stops;
    }
}
