package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
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
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import playground.vsp.andreas.utils.pt.TransitLineRemover;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) throws MalformedURLException {
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedNetwork.xml.gz";
        final String outScheduleFile = "/output/trimmedSched.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedScheduleWoBusTouchingZone.xml.gz";
        final String zoneShpFile = "file:C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\input\\berlin_hundekopf.shp";// "file://../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";


        // P R E P A R A T I O N
        //load shp file
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));

        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);

        MutableScenario scenario = (MutableScenario)ScenarioUtils.loadScenario(config);
        TransitSchedule inTransitSchedule = scenario.getTransitSchedule();
        TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
        TransitSchedule outTransitSchedule = builder.createTransitSchedule();


        // Map all Stops, and whether they are inside or outside of shp file
        Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone = new HashMap<>();
        for (TransitStopFacility stop : inTransitSchedule.getFacilities().values()){
            stop2LocationInZone.put(stop.getId(), ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries));
        }
//        for (Id<TransitStopFacility> stopID : stop2LocationInZone.keySet()){
//            System.out.println(stopID + " - " + stop2LocationInZone.get(stopID));
//        }

        int inCount = 0;
        int outCount = 0 ;
        int wrongCount = 0;
        int halfCount = 0 ;
        int totalCount = 0 ;
        ArrayList<Id<TransitRoute>> in = new ArrayList<>();
        ArrayList<Id<TransitRoute>> out = new ArrayList<>();
        ArrayList<Id<TransitRoute>> half = new ArrayList<>();


        for (TransitLine line : inTransitSchedule.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
//                System.out.println("\n\nROUTE                 " + route.getId());
                totalCount++ ;
                ArrayList<Boolean> inOutList = new ArrayList<>();
                for (TransitRouteStop stop : route.getStops()) {
                    Id<TransitStopFacility> id = stop.getStopFacility().getId();
                    inOutList.add(stop2LocationInZone.get(id));
//                    System.out.println(id + "  :   " + stop2LocationInZone.get(id));
                }
                if (inOutList.contains(true) && inOutList.contains(false)) {
                    halfCount++;
                    half.add(route.getId());
                } else if (inOutList.contains(true)) {
                    inCount++ ;
                    in.add(route.getId());
                } else if (inOutList.contains(false)) {
                    outCount++;
                    out.add(route.getId());
                } else {
                    wrongCount++;
                }
            }
        }

        System.out.printf("in: %d, out: %d, half: %d, wrong: %d", inCount, outCount,halfCount,wrongCount);
        System.out.printf("%nin: %d, out: %d, half: %d, wrong: %d", in.size(), out.size(),half.size(),wrongCount);

        TransitScheduleFactory tsf = scenario.getTransitSchedule().getFactory();
        TransitSchedule tS = (new TransitScheduleFactoryImpl()).createTransitSchedule();
        Iterator var3 = inTransitSchedule.getFacilities().values().iterator();

        while(var3.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility)var3.next();
            tS.addStopFacility(stop);
        }

        var3 = inTransitSchedule.getTransitLines().values().iterator();

        while(var3.hasNext()) {
            TransitLine line = (TransitLine) var3.next();
            if (!in.contains(line.getId())) {
                tS.addTransitLine(line);
            }
        }

        countLinesInOut(tS, stop2LocationInZone);


//"X10---17529_700"
//        for (TransitLine line : inTransitSchedule.getTransitLines().values()) {
//            for (TransitRoute routeOld : line.getRoutes().values()) {
//                TransitRoute routeNew = routeOld;
////                if (half.contains(routeOld.getId())) {
//                if (routeOld.getId().toString().equals("X10---17529_700_0")) {
//
//                    ArrayList<Boolean> inOutList = new ArrayList<>();
//                    for (TransitRouteStop stop : routeOld.getStops()) {
//                        Id<TransitStopFacility> id = stop.getStopFacility().getId();
//                        inOutList.add(stop2LocationInZone.get(id));
//                    } // duplicate code
//                    System.out.println(routeOld.getId());
//                    System.out.println(inOutList);
//                    List<TransitRouteStop> stopsNew = new ArrayList<>();
//                    for (TransitRouteStop stop : routeOld.getStops()) {
//                        stopsNew.add(stop);
//                    }
//
//                    List<Id<Link>> linksNew = new ArrayList<>();
//                    linksNew.add(routeOld.getRoute().getStartLinkId());
//                    for (Id<Link> linkId : routeOld.getRoute().getLinkIds()) {
//                        linksNew.add(linkId);
//                    }
//                    linksNew.add(routeOld.getRoute().getEndLinkId());
////                    linksNew.add(0,routeOld.getRoute().getStartLinkId());
//                    System.out.println("inout list size: " + inOutList.size());
//                    System.out.println("stops size: " + stopsNew.size());
//                    System.out.println("link size: " + linksNew.size());
////
//                    for (int i = inOutList.size()-1; i >=0 ; i--) {
//                        if (inOutList.get(i)) {
//                            inOutList.remove(i);
//                            linksNew.remove(i);
//                            stopsNew.remove(i);
//                        }
//                    }
//                    System.out.println(inOutList);
//                    System.out.println("inout list size: " + inOutList.size());
//                    System.out.println("stops size: " + stopsNew.size());
//                    System.out.println("link size: " + linksNew.size());
//                    NetworkRoute networkRouteNew = RouteUtils.createNetworkRoute(linksNew, scenario.getNetwork());
//                    String modeNew = routeOld.getTransportMode();
//                    routeNew = tsf.createTransitRoute(routeOld.getId(), networkRouteNew, stopsNew, modeNew);
//
//
//                    }
//                line.removeRoute(routeOld);
//                line.addRoute(routeNew);
//                }

            }





//
//        // ENTIRELY WITHIN ZONE -- remove
//        Set<Id<TransitLine>> linesToRemove = inTransitSchedule.getTransitLines().values().stream().
//                filter(line -> line.getRoutes().values().stream().allMatch(route -> route.getTransportMode().equals("bus"))).
//                filter(line -> completelyInZone(line, geometries)).
//                map(line -> line.getId()).
//                collect(Collectors.toSet());
//
//        linesToRemove.stream().sorted().forEach(l -> log.info(l.toString()));
//
//        TransitSchedule outTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(inTransitSchedule, linesToRemove);

        // ENTIRELY OUTSIDE OF ZONE

        // HALF IN HALF OUT

        // BEGINNING AND END OUT, MIDDLE IN

        // BEGINNING AND END IN, MIDDLE OUT




        // CLEAN-UP

//        TransitSchedule outTransitScheduleCleaned = TransitScheduleCleaner.removeStopsNotUsed(outTransitSchedule);
//
//        ValidationResult validationResult = TransitScheduleValidator.validateAll(outTransitScheduleCleaned, scenario.getNetwork());
//        log.warn(validationResult.getErrors());
//
//        new TransitScheduleWriter(outTransitScheduleCleaned).writeFile(outScheduleFile);
//    }

    private static void countLinesInOut(TransitSchedule tS, Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone){
        int inCount = 0;
        int outCount = 0 ;
        int wrongCount = 0;
        int halfCount = 0 ;
        int totalCount = 0 ;
        ArrayList<Id<TransitRoute>> in = new ArrayList<>();
        ArrayList<Id<TransitRoute>> out = new ArrayList<>();
        ArrayList<Id<TransitRoute>> half = new ArrayList<>();

        for (TransitLine line : tS.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
//                System.out.println("\n\nROUTE                 " + route.getId());
                totalCount++ ;
                ArrayList<Boolean> inOutList = new ArrayList<>();
                for (TransitRouteStop stop : route.getStops()) {
                    Id<TransitStopFacility> id = stop.getStopFacility().getId();
                    inOutList.add(stop2LocationInZone.get(id));
//                    System.out.println(id + "  :   " + stop2LocationInZone.get(id));
                }
                if (inOutList.contains(true) && inOutList.contains(false)) {
                    halfCount++;
                    half.add(route.getId());
                } else if (inOutList.contains(true)) {
                    inCount++ ;
                    in.add(route.getId());
                } else if (inOutList.contains(false)) {
                    outCount++;
                    out.add(route.getId());
                } else {
                    wrongCount++;
                }
            }
        }

        System.out.printf("in: %d, out: %d, half: %d, wrong: %d", inCount, outCount,halfCount,wrongCount);
        System.out.printf("%nin: %d, out: %d, half: %d, wrong: %d", in.size(), out.size(),half.size(),wrongCount);

    }

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
