package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This tool creates a trims TransitRoutes, so as not to enter a user-specified ESRI shape file.
 * There are several modifier methods that can be used separately or in combination.
 *
 * TODO: Keep one TransitStop within the shapefile
 * TODO: Change Offsets
 *
 * @author jakobrehmann
 */

public class TransitRouteTrimmer {
    private static final Logger log = Logger.getLogger(TransitRouteTrimmer.class);

    // Parameters
    private static boolean removeEmptyLines = true;
    private static boolean allowOneStopWithinZone = true ;
    private int minimumRouteLength = 2;

    private Set<String> modes2Trim;
    private Vehicles vehicles;
    private TransitSchedule transitScheduleOld;
    private TransitSchedule transitScheduleNew;
    private Set<Id<TransitStopFacility>> stopsInZone;
    private TransitScheduleFactory tsf;
    
    //TODO: Vehicles new
    


    enum modMethod {
        DeleteRoutesEntirelyInsideZone,
        SkipStopsWithinZone,
        TrimEnds,
        ChooseLongerEnd,
        SplitOldRouteIntoMultiplePieces
    }


    public TransitRouteTrimmer(TransitSchedule transitSchedule,Vehicles vehicles,Set<String> modes2Trim, List<PreparedGeometry> geometries) {
        this.transitScheduleOld = transitSchedule;
        this.transitScheduleNew = (new TransitScheduleFactoryImpl()).createTransitSchedule(); // TODO: Is this okay?
        this.vehicles = vehicles;
        this.modes2Trim = modes2Trim;
        this.tsf = transitScheduleNew.getFactory(); //TODO: ???
        stopsInZone = new HashSet<>();
        for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                this.stopsInZone.add(stop.getId());
            }
        }


    }

    public TransitRouteTrimmer(TransitSchedule transitSchedule,Vehicles vehicles,Set<String> modes2Trim, Set<Id<TransitStopFacility>> stopsInZone) {
        this.transitScheduleOld = transitSchedule;
        this.transitScheduleNew = (new TransitScheduleFactoryImpl()).createTransitSchedule(); // TODO: Is this okay?
        this.tsf = transitScheduleNew.getFactory(); //TODO: ???
        this.vehicles = vehicles;
        this.modes2Trim = modes2Trim;
        this.stopsInZone = stopsInZone;

    }

    public static void main(String[] args) throws IOException, SchemaException {

        final String outputRouteShapeRoot = "C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\output\\routes";

        final String inScheduleFile = "D:\\runs\\gladbeck\\input\\optimizedSchedule.xml.gz";
        final String inVehiclesFile = "D:\\runs\\gladbeck\\input\\optimizedVehicles.xml.gz";
        final String inNetworkFile = "D:\\runs\\gladbeck\\input\\optimizedNetwork.xml.gz";
        final String zoneShpFile = "D:/runs/gladbeck/input/area_B_en_detail.shp";
        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output3/";

        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);
        config.vehicles().setVehiclesFile(inVehiclesFile);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

        Set<String> modes2Trim = new HashSet<>();
        modes2Trim.add("bus");


        TransitSchedule transitSchedule = scenario.getTransitSchedule();


        Set<Id<TransitLine>> linesToModify = transitSchedule.getTransitLines().keySet();

//        Set<Id<TransitLine>> linesToModify = transitSchedule.getTransitLines().values().stream()
//                .filter(v -> v.getId().toString().contains("SB"))
//                .map(v -> v.getId())
//                .collect(Collectors.toSet()
//                );


//        Set<Id<TransitLine>> linesToModify = transitSchedule.getTransitLines().values().stream()
//                .filter(v -> v.getRoutes().get(0).getTransportMode().equals("bus"))
//                .map(v -> v.getId())
//                .collect(Collectors.toSet()
//                );

        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new File(zoneShpFile).toURI().toURL());
//        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));


        System.out.println("\n Modify Routes: SplitOldRouteIntoMultiplePieces");
        TransitRouteTrimmer transitRouteTrimmer = new TransitRouteTrimmer(scenario.getTransitSchedule(), scenario.getVehicles(), modes2Trim, geometries);
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, modMethod.SplitOldRouteIntoMultiplePieces);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        Vehicles vehiclesNew = transitRouteTrimmer.getVehicles();
        TransitSchedule2Shape.createShpFile(transitScheduleNew, outputPath + "output-trimmed-routes.shp");
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "output-trimmed-schedule.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "output-vehicles.xml.gz");

        // Schedule Cleaner and Writer
//        TransitSchedule transitScheduleNewCleaned = TransitScheduleCleaner.removeStopsNotUsed(transitScheduleNew);
//        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNewCleaned, scenario.getNetwork());
//        log.warn(validationResult.getErrors());

    }


    public void modifyTransitLinesFromTransitSchedule(Set<Id<TransitLine>> linesToModify, modMethod modifyMethod) {
        Iterator var3 = transitScheduleOld.getFacilities().values().iterator();

        while (var3.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility) var3.next();
            transitScheduleNew.addStopFacility(stop);
        }

        var3 = transitScheduleOld.getTransitLines().values().iterator();

        while (var3.hasNext()) {
            TransitLine line = (TransitLine) var3.next();
            if (!linesToModify.contains(line.getId())) {
                transitScheduleNew.addTransitLine(line);
                continue;
            }


            TransitLine lineNew = transitScheduleOld.getFactory().createTransitLine(line.getId());
            for (TransitRoute route : line.getRoutes().values()) {
                TransitRoute routeNew = null;

                // Only handles specified routes.
                if (!this.modes2Trim.contains(route.getTransportMode())) {
                    lineNew.addRoute(route);
                    continue;
                }

                if (modifyMethod.equals(modMethod.DeleteRoutesEntirelyInsideZone)) {
                    if (pctOfStopsInZone(route, stopsInZone) == 1.0) {
                        continue;
                    }
                    routeNew = route;
//                } else if (modifyMethod.equals(modMethod.TrimEnds)) {
//                    routeNew = modifyRouteTrimEnds(route, stopsInZone, scenario);
//                } else if (modifyMethod.equals(modMethod.ChooseLongerEnd)) {
//                    routeNew = modifyRouteChooseLongerEnd(route, stopsInZone, scenario);
                } else if (modifyMethod.equals((modMethod.SkipStopsWithinZone))) {
                    routeNew = modifyRouteSkipStopsWithinZone(route);
                } else if (modifyMethod.equals(modMethod.SplitOldRouteIntoMultiplePieces)) {
                    ArrayList<TransitRoute> routesNew = modifyRouteCutIntoShorterRoutes(route);
                    for (TransitRoute rt : routesNew) {
                        lineNew.addRoute(rt);
                    }
                }

                if (routeNew != null) {
                    lineNew.addRoute(routeNew);
                }

            }

            if (lineNew.getRoutes().size() == 0 && removeEmptyLines) {
                log.info(lineNew.getId() + " does not contain routes. It will NOT be added to the schedule");
                continue;
            }

            transitScheduleNew.addTransitLine(lineNew);

        }

        log.info("Old schedule contained " + transitScheduleOld.getTransitLines().values().size() + " lines.");
        log.info("New schedule contains " + transitScheduleNew.getTransitLines().values().size() + " lines.");

        countLinesInOut(transitScheduleNew, stopsInZone);
    }


    public Vehicles getVehicles() {
        return vehicles;
    }

    public TransitSchedule getTransitScheduleNew() {
        return transitScheduleNew;
    }



    // This will skip stops within zone. If beginning or end of route is within zone, it will cut those ends off.
    private TransitRoute modifyRouteSkipStopsWithinZone(TransitRoute routeOld) {
        List<TransitRouteStop> stops2Keep = new ArrayList<>();
        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        for (int i = 0; i < stopsOld.size(); i++) {
            TransitRouteStop stop = stopsOld.get(i);
            if (!stopsInZone.contains(stop.getStopFacility().getId())) {
                stops2Keep.add(stop);
            } else {
                if (allowOneStopWithinZone) {
                    if (i > 0) {
                        Id<TransitStopFacility> prevStop = stopsOld.get(i - 1).getStopFacility().getId();
                        if (!stopsInZone.contains(prevStop)) {
                            stops2Keep.add(stop); // TODO ADD ATTRIBUTE
                            continue;
                        }
                    }

                    if (i < stopsOld.size() - 1) {
                        Id<TransitStopFacility> nextStop = stopsOld.get(i + 1).getStopFacility().getId();
                        if (!stopsInZone.contains(nextStop)) {
                            stops2Keep.add(stop);
                        }

                    }
                }
            }
        }


        if (stops2Keep.size() >= minimumRouteLength && stops2Keep.size() > 0) {
            return createNewRoute(routeOld, stops2Keep, 1);
        }

        return null; // What do we do here?

    }

//    private TransitRoute modifyRouteTrimEnds(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInZone, Scenario scenario) {
//        TransitRoute routeNew = null;
//
//
//        List<TransitRouteStop> stops2Keep = new ArrayList<>();
//        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());
//
//
//
//        // Find which stops of route are within zone
//        ArrayList<Boolean> inOutList = new ArrayList<>();
//        for (TransitRouteStop stop : routeOld.getStops()) {
//            Id<TransitStopFacility> id = stop.getStopFacility().getId();
//            inOutList.add(stopsInZone.contains(id));
//        }
//
//        ArrayList<Boolean> keepDiscardList = new ArrayList<>();
//        for (int i = 0; i < inOutList.size(); i++) {
//            keepDiscardList.add(false);
//        }
//
//        // from beginning of trip
//        for (int i = 0; i < keepDiscardList.size(); i++) {
//            if (inOutList.get(i) == true) {
//                keepDiscardList.set(i, true);
////                if (allowOneStopWithinZone && i > 0) {
////                    keepDiscardList.set(i - 1, true);
////                }
//            } else {
//                break;
//            }
//        }
//
//        // from end of trip
//        for (int i = keepDiscardList.size() - 1; i >= 0; i--) {
//            if (inOutList.get(i) == true) {
//                keepDiscardList.set(i, true);
//                if (allowOneStopWithinZone && i < keepDiscardList.size() - 1) {
//                    keepDiscardList.set(i + 1, true);
//                }
//            } else {
//                break;
//            }
//        }
//
//
//        return createNewRoute(routeOld, scenario, keepDiscardList);
//    }

//    private TransitRoute modifyRouteChooseLongerEnd(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInZone, Scenario scenario) {
//        // Find which stops of route are within zone
//        ArrayList<Boolean> inOutList = new ArrayList<>();
//        for (TransitRouteStop stop : routeOld.getStops()) {
//            Id<TransitStopFacility> id = stop.getStopFacility().getId();
//            inOutList.add(stopsInZone.contains(id));
//        }
//
//        int falseCountBeginning = 0;
//        for (int i = 0; i < inOutList.size(); i++) {
//            if (!inOutList.get(i)) {
//                falseCountBeginning++;
//            } else {
//                break;
//            }
//        }
//
//        int falseCountEnd = 0;
//        for (int i = inOutList.size() - 1; i >= 0; i--) {
//            if (!inOutList.get(i)) {
//                falseCountEnd++;
//            } else {
//                break;
//            }
//        }
//
//        ArrayList<Boolean> keepDiscardList = new ArrayList<>();
//        for (int i = 0; i < inOutList.size(); i++) {
//            keepDiscardList.add(true);
//        }
//
//        if (falseCountBeginning >= falseCountEnd) {
//            for (int i = 0; i < falseCountBeginning; i++) {
//                keepDiscardList.set(i, false);
//            }
//        } else {
//            for (int i = inOutList.size() - 1; i >= inOutList.size() - falseCountEnd; i--) {
//                keepDiscardList.set(i, false);
//            }
//        }
//
//        return createNewRoute(routeOld, scenario, keepDiscardList);
//    }

    private ArrayList<TransitRoute> modifyRouteCutIntoShorterRoutes(TransitRoute routeOld) {
        ArrayList<TransitRoute> resultRoutes = new ArrayList<>();
        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        List<TransitRouteStop> stops2Keep = new ArrayList<>();

        int newRouteCnt = 0;
        for (int i = 0; i < stopsOld.size(); i++) {
            Id<TransitStopFacility> stopFacilityId = stopsOld.get(i).getStopFacility().getId();
            if (!stopsInZone.contains(stopFacilityId)) { // we are outside of zone --> we keep the stop

                // adds first stop that's within zone
                if (stops2Keep.size() == 0 && i > 0 && allowOneStopWithinZone) {
                    //TODO: ADD ATTRIBUTE TO STOP
                    stops2Keep.add(stopsOld.get(i - 1));
                }

                stops2Keep.add(stopsOld.get(i));
            } else if (stopsInZone.contains(stopFacilityId)) { // inside of zone --> discard

                // The following is only done, if stop i is the first stop to enter the zone.
                if (stops2Keep.size() > 0) {
                    //adds first stop in zone
                    if (allowOneStopWithinZone) {
                        stops2Keep.add(stopsOld.get(i));
                    }

                    // creates route and clears stopsNew and linksNew
                    if (stops2Keep.size() >= minimumRouteLength) {
                        TransitRoute routeNew = createNewRoute(routeOld, stops2Keep, newRouteCnt);
                        resultRoutes.add(routeNew);
                        newRouteCnt++;
                    }
                    stops2Keep.clear();
                }
            }
        }

        if (stops2Keep.size() >= minimumRouteLength && stops2Keep.size() > 0) {
            TransitRoute routeNew = createNewRoute(routeOld, stops2Keep, newRouteCnt);
            resultRoutes.add(routeNew);
        }

        return resultRoutes;
    }


    private TransitRoute createNewRoute(TransitRoute routeOld, List<TransitRouteStop> stopsInNewRoute, int modNumber) {

        TransitRoute routeNew;

        List<Id<Link>> linksOld = new ArrayList<>();
        linksOld.add(routeOld.getRoute().getStartLinkId());
        linksOld.addAll(routeOld.getRoute().getLinkIds());
        linksOld.add(routeOld.getRoute().getEndLinkId());

        Id<Link> startLinkNew = stopsInNewRoute.get(0).getStopFacility().getLinkId();
        Id<Link> endLinkNew = stopsInNewRoute.get(stopsInNewRoute.size() - 1).getStopFacility().getLinkId();
        ArrayList<Id<Link>> midLinksNew = new ArrayList<>();

        boolean start = false;
        for (Id<Link> linkId : linksOld) {
            if (!start) {
                if (linkId.equals(startLinkNew)){
                    start = true;
                }
                continue;
            }

            if (linkId.equals(endLinkNew)) {
                break;
            }
            midLinksNew.add(linkId);

        }

        // Modify arrivalOffset and departureOffset for each stop
        double initialDepartureOffset = routeOld.getStops().get(0).getDepartureOffset().seconds();
        double departureOffset = stopsInNewRoute.get(0).getDepartureOffset().seconds() - initialDepartureOffset;
//        TransitRouteStop transitRouteStop = routeOld.getStops().get(0);
//        double initialArrivalOffset = transitRouteStop.getArrivalOffset().seconds();
//        double arrivalOffset = stopsInNewRoute.get(0).getArrivalOffset().seconds() - initialArrivalOffset;

        List<TransitRouteStop> stopsNew = new ArrayList<>(copyStops(stopsInNewRoute, departureOffset));


        // make route
//        NetworkRoute networkRouteNew = RouteUtils.createNetworkRoute(midLinksNew, scenario.getNetwork());
        NetworkRoute networkRouteNew = RouteUtils.createLinkNetworkRouteImpl(startLinkNew, midLinksNew, endLinkNew);
        String routeIdOld = routeOld.getId().toString();
        Id<TransitRoute> routeIdNew = Id.create( routeIdOld + "_mod" + modNumber, TransitRoute.class);
        routeNew = tsf.createTransitRoute(routeIdNew, networkRouteNew, stopsNew, routeOld.getTransportMode());
        routeNew.setDescription(routeOld.getDescription());


        VehiclesFactory vf = this.vehicles.getFactory();


        for (Departure departure : routeOld.getDepartures().values()) {
            Id<Vehicle> vehIdOld = departure.getVehicleId();
            Id<Vehicle> vehIdNew = Id.createVehicleId(vehIdOld.toString() + "_mod" + modNumber);
            VehicleType vehType = this.vehicles.getVehicles().get(vehIdOld).getType();
//            this.vehicles.removeVehicle(departure.getVehicleId()); //TODO VEH MUST BE REMOVED LATER ON IF UNUSED
            Vehicle vehicle = vf.createVehicle(vehIdNew, vehType);
            this.vehicles.addVehicle(vehicle);

            String depIdOld = departure.getId().toString();
            Departure departureNew = tsf.createDeparture(Id.create(depIdOld + "_mod" + modNumber, Departure.class), departure.getDepartureTime() + departureOffset);
            departureNew.setVehicleId(vehIdNew);
            routeNew.addDeparture(departureNew);
        }
        return routeNew;
    }


    // TODO: how to deal with arrival and departure offsets? I don't know the conventions...
    private Collection<? extends TransitRouteStop> copyStops(List<TransitRouteStop> s, Double departureOffset) {
        List<TransitRouteStop> stops = new ArrayList<>();
        TransitRouteStop newStop;
        for(TransitRouteStop oldStop: s){

            if (oldStop.getDepartureOffset().isDefined() && oldStop.getArrivalOffset().isDefined()) {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
                        oldStop.getDepartureOffset().seconds() - departureOffset,
                        oldStop.getArrivalOffset().seconds() - departureOffset);
            } else if (oldStop.getDepartureOffset().isDefined()) {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
                        oldStop.getDepartureOffset().seconds() - departureOffset,
                        oldStop.getDepartureOffset().seconds() - departureOffset);
            } else if (oldStop.getArrivalOffset().isDefined()) {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(),
                        oldStop.getArrivalOffset().seconds() - departureOffset,
                        oldStop.getArrivalOffset().seconds() - departureOffset);
            } else {
                newStop = tsf.createTransitRouteStop(oldStop.getStopFacility(), 0, 0);
            }

            newStop.setAwaitDepartureTime(oldStop.isAwaitDepartureTime());
            stops.add(newStop);

        }
        return stops;
    }


    private double pctOfStopsInZone(TransitRoute route, Set<Id<TransitStopFacility>> stopsInZone) {
        double inAreaCount = 0.;
        for (TransitRouteStop stop : route.getStops()) {
            if (stopsInZone.contains(stop.getStopFacility().getId())) {
                inAreaCount++;
            }
        }
        return inAreaCount / route.getStops().size();
    }

    static void countLinesInOut(TransitSchedule tS, Set<Id<TransitStopFacility>> stopsInZone) {
        int inCount = 0;
        int outCount = 0;
        int wrongCount = 0;
        int halfCount = 0;
        int totalCount = 0;

        for (TransitLine line : tS.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                totalCount++;
                ArrayList<Boolean> inOutList = new ArrayList<>();
                for (TransitRouteStop stop : route.getStops()) {
                    Id<TransitStopFacility> id = stop.getStopFacility().getId();
                    inOutList.add(stopsInZone.contains(id));
                }
                if (inOutList.contains(true) && inOutList.contains(false)) {
                    halfCount++;
                } else if (inOutList.contains(true)) {
                    inCount++;
                } else if (inOutList.contains(false)) {
                    outCount++;
                } else {
                    wrongCount++;
                }
            }
        }

        System.out.printf("in: %d, out: %d, half: %d, wrong: %d, total: %d %n", inCount, outCount, halfCount, wrongCount, totalCount);

    }
}
