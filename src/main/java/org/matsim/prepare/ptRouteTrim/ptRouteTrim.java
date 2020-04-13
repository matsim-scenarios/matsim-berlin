package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitLineImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import playground.vsp.andreas.utils.pt.TransitLineRemover;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ptRouteTrim {
    private static final Logger log = Logger.getLogger(ptRouteTrim.class);

    public static void main(String[] args) throws MalformedURLException {
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedNetwork.xml.gz";
        final String outScheduleFile = "/output/trimmedSched.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedScheduleWoBusTouchingZone.xml.gz";
        final String zoneShpFile = "file:C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\input\\berlin_hundekopf.shp";// "file://../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";


        // Prepare Scenario
        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
        TransitSchedule inTransitSchedule = scenario.getTransitSchedule();

        // Get Stops within Area
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));

        Set<Id<TransitStopFacility>> stopsInArea = new HashSet<>(); //getStopIdsWithinArea(inTransitSchedule, geometries);
        for (TransitStopFacility stop : inTransitSchedule.getFacilities().values()){
            if(ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)){
                stopsInArea.add(stop.getId());
            }
        }


        // Step 1: Remove all lines that are completely within the Shp
//        TransitSchedule tS = (new TransitScheduleFactoryImpl()).createTransitSchedule();
//        Set<Id<TransitLine>> linesToRemove = inTransitSchedule.getTransitLines().values().stream().
////                filter(line -> line.getRoutes().values().stream().allMatch(route -> route.getTransportMode().equals("bus"))).
//                filter(line -> completelyInZone(line, geometries)).
//                map(line -> line.getId()).
//                collect(Collectors.toSet());
//
//        linesToRemove.stream().sorted().forEach(l -> log.info(l.toString()));
//
//        TransitSchedule outTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(inTransitSchedule, linesToRemove);
//
//        countLinesInOut(inTransitSchedule, stopsInArea);
//        countLinesInOut(outTransitSchedule, stopsInArea);

        // Step 2: Lines that have routes that touch the zone:
//        Set<Id<TransitLine>> linesToModify = outTransitSchedule.getTransitLines().values().stream().
////                filter(line -> line.getRoutes().values().stream().allMatch(route -> route.getTransportMode().equals("bus"))).
//                filter(line -> touchesZone(line, geometries)).
//                map(line -> line.getId()).
//                collect(Collectors.toSet());
        Set<Id<TransitLine>> linesToModify = inTransitSchedule.getTransitLines().keySet(); // all lines will be examined
        TransitSchedule outTransitSchedule = modifyTransitLinesFromTransitSchedule(inTransitSchedule, linesToModify, stopsInArea, scenario);

        System.out.println("\n Before Modification of routes");
        countLinesInOut(inTransitSchedule, stopsInArea);
        System.out.println("\n After modification of routes");
        countLinesInOut(outTransitSchedule, stopsInArea);

        for (TransitLine line : outTransitSchedule.getTransitLines().values()) {
            if (line.getRoutes().size() == 0) {
                System.out.println(line.getId());
            }
            for (TransitRoute route : line.getRoutes().values()) {
                if (route.getStops().size() == 0) {
                    System.out.println(route.getId());
                    ;
                }
            }
        }
        //TODO: Are there lines with no routes now?
    }

    private static double pctOfStopsInZone(TransitRoute route, Set<Id<TransitStopFacility>> stopsInArea) {
        double inAreaCount = 0.;
        for (TransitRouteStop stop : route.getStops()) {
            if (stopsInArea.contains(stop.getStopFacility().getId())) {
                inAreaCount++;
            }
        }
        return inAreaCount / route.getStops().size();
    }



    public static TransitSchedule modifyTransitLinesFromTransitSchedule(TransitSchedule transitSchedule, Set<Id<TransitLine>> linesToModify,  Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        log.info("modifying " + linesToModify + " lines from transit schedule...");
        TransitSchedule tS = (new TransitScheduleFactoryImpl()).createTransitSchedule();
        Iterator var3 = transitSchedule.getFacilities().values().iterator();

        while(var3.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility)var3.next();
            tS.addStopFacility(stop);
        }

        var3 = transitSchedule.getTransitLines().values().iterator();

        while(var3.hasNext()) {
            TransitLine line = (TransitLine)var3.next();
            if (!linesToModify.contains(line.getId())) {
                tS.addTransitLine(line);
            } else{
                Id<TransitLine> transitLineId = line.getId();
                TransitLine lineNew = transitSchedule.getFactory().createTransitLine(transitLineId);
                for (TransitRoute route : line.getRoutes().values()) {
                    TransitRoute routeNew = modifyRoute(route,stopsInArea, scenario);
                    if (routeNew!=null) {
                        lineNew.addRoute(routeNew);
                    }
                }
                tS.addTransitLine(lineNew);


            }

        }

        log.info("Old schedule contained " + transitSchedule.getTransitLines().values().size() + " lines.");
        log.info("New schedule contains " + tS.getTransitLines().values().size() + " lines.");
        return tS;
    }

    private static TransitRoute modifyRoute(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        TransitRoute routeNew = null ;


        // Find which stops of route are within zone
        ArrayList<Boolean> inOutList = new ArrayList<>();
        for (TransitRouteStop stop : routeOld.getStops()) {
            Id<TransitStopFacility> id = stop.getStopFacility().getId();
            inOutList.add(stopsInArea.contains(id));
        }



        // Collect all stops and links from original route
        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        List<Id<Link>> linksOld = new ArrayList<>();
        linksOld.add(routeOld.getRoute().getStartLinkId());
        linksOld.addAll(routeOld.getRoute().getLinkIds());
        linksOld.add(routeOld.getRoute().getEndLinkId());


        // Make new stops and links lists
        List<TransitRouteStop> stopsNew = new ArrayList<>();
        List<Id<Link>> linksNew = new ArrayList<>();

        for (int i = 0; i < inOutList.size(); i++) {
            if (!inOutList.get(i)) {
                stopsNew.add(stopsOld.get(i));
                linksNew.add(linksOld.get(i));
            }
        }

        if (stopsNew.size() == 0) {
            return null;
        }

        // make route
        NetworkRoute networkRouteNew = RouteUtils.createNetworkRoute(linksNew, scenario.getNetwork());
        String modeNew = routeOld.getTransportMode();
        TransitScheduleFactory tsf = scenario.getTransitSchedule().getFactory();
        routeNew = tsf.createTransitRoute(routeOld.getId(), networkRouteNew, stopsNew, modeNew);
        routeNew.setTransportMode(routeOld.getTransportMode());
        routeNew.setDescription(routeOld.getDescription());

        //TODO: Change Offsets
        for (Departure departure : routeOld.getDepartures().values()) {
            routeNew.addDeparture(departure);
        }
        return routeNew;
    }

    private static void countLinesInOut(TransitSchedule tS,  Set<Id<TransitStopFacility>> stopsInArea){
        int inCount = 0;
        int outCount = 0 ;
        int wrongCount = 0;
        int halfCount = 0 ;
        int totalCount = 0 ;

        for (TransitLine line : tS.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                totalCount++ ;
                ArrayList<Boolean> inOutList = new ArrayList<>();
                for (TransitRouteStop stop : route.getStops()) {
                    Id<TransitStopFacility> id = stop.getStopFacility().getId();
                    inOutList.add(stopsInArea.contains(id));
                }
                if (inOutList.contains(true) && inOutList.contains(false)) {
                    halfCount++;
                } else if (inOutList.contains(true)) {
                    inCount++ ;
                } else if (inOutList.contains(false)) {
                    outCount++;
                } else {
                    wrongCount++;
                }
            }
        }

        System.out.printf("%nin: %d, out: %d, half: %d, wrong: %d", inCount, outCount,halfCount,wrongCount);

    }




    // Deprecated:
    private static boolean completelyInZone(TransitLine line, List<PreparedGeometry> zones) {
        Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone = new HashMap<>();

        line.getRoutes().values().forEach(route -> checkAndWriteLocationPerStop(stop2LocationInZone, route, zones));
        return stop2LocationInZone.values().stream().allMatch(b -> b == true);
    }

    private static boolean touchesZone (TransitLine line, List<PreparedGeometry> zones) {
        Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone = new HashMap<>();

        line.getRoutes().values().forEach(route -> checkAndWriteLocationPerStop(stop2LocationInZone, route, zones));
        return stop2LocationInZone.values().stream().anyMatch(b -> b == true);
    }

    private static void checkAndWriteLocationPerStop(Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone, TransitRoute route, List<PreparedGeometry> zones) {
        route.getStops().forEach(stop -> stop2LocationInZone.put(stop.getStopFacility().getId(), ShpGeometryUtils.isCoordInPreparedGeometries(stop.getStopFacility().getCoord(), zones)));
    }

}
