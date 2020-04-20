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
import org.matsim.pt.transitSchedule.DepartureImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicle;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.io.IOException;
import java.net.URL;
import java.util.*;

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
    static double pctThresholdToKeepRouteEntirely = 0.0;
    static double pctThresholdToRemoveRouteEntirely = 1.0;

    enum modMethod {
        DeleteRoutesEntirelyInside,
        DeleteAllStopsWithin,
        TrimEnds,
        ChooseLongerEnd,
        SplitOldRouteIntoMultiplePieces
    }

    public static void main(String[] args) throws IOException, SchemaException {
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedNetwork.xml.gz";
        final String outScheduleFile = "C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\output\\output-transit-schedule.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedScheduleWoBusTouchingZone.xml.gz";
        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";

        final String outputRouteShapeRoot = "C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\output\\routes";

        // Prepare Scenario
        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
        TransitSchedule inTransitSchedule = scenario.getTransitSchedule();

        // Get Stops within Area
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));

        Set<Id<TransitStopFacility>> stopsInArea = new HashSet<>();
        for (TransitStopFacility stop : inTransitSchedule.getFacilities().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries)) {
                stopsInArea.add(stop.getId());
            }
        }


        Set<Id<TransitLine>> linesToModify = inTransitSchedule.getTransitLines().keySet(); // all lines will be examined

        // Modify Routes: Delete all routes entirely inside shp
//        TransitSchedule outTransitSchedule = modifyTransitLinesFromTransitSchedule(inTransitSchedule, linesToModify, stopsInArea, scenario, modMethod.DeleteRoutesEntirelyInside);

        System.out.println("\n Before Modification of routes");
        countLinesInOut(inTransitSchedule, stopsInArea);
        TransitSchedule2Shape.createShpFile(inTransitSchedule, outputRouteShapeRoot + "before.shp");

//        System.out.println("\n Modify Routes: Delete all routes entirely inside shp");
//        countLinesInOut(outTransitSchedule, stopsInArea);
//        TransitSchedule2Shape.createShpFile(outTransitSchedule, outputRouteShapeRoot + "afterDeleteInside.shp");
//
//        // Modify Routes: Trim Ends
//        outTransitSchedule = modifyTransitLinesFromTransitSchedule(outTransitSchedule, linesToModify, stopsInArea, scenario, modMethod.TrimEnds);
//
//        System.out.println("\n Modify Routes: Trim Ends");
//        countLinesInOut(outTransitSchedule, stopsInArea);
//        TransitSchedule2Shape.createShpFile(outTransitSchedule, outputRouteShapeRoot + "afterTrimEnds.shp");
//
//
//        // Modify Routes: ChooseLongerEnd
//        outTransitSchedule = modifyTransitLinesFromTransitSchedule(outTransitSchedule, linesToModify, stopsInArea, scenario, modMethod.ChooseLongerEnd);
//
//        System.out.println("\n Modify Routes: ChooseLongerEnd");
//        countLinesInOut(outTransitSchedule, stopsInArea);
//        TransitSchedule2Shape.createShpFile(outTransitSchedule, outputRouteShapeRoot + "afterChooseEnd.shp");


        //Modify Routes: Split Routes into shorter sections
        TransitSchedule outTransitSchedule = modifyTransitLinesFromTransitSchedule(inTransitSchedule, linesToModify, stopsInArea, scenario, modMethod.SplitOldRouteIntoMultiplePieces);

        System.out.println("\n Modify Routes: SplitOldRouteIntoMultiplePieces");
        countLinesInOut(outTransitSchedule, stopsInArea);
        TransitSchedule2Shape.createShpFile(outTransitSchedule, outputRouteShapeRoot + "SplitOldRouteIntoMultiplePieces.shp");

        // Schedule Cleaner and Writer
        TransitSchedule outTransitScheduleCleaned = TransitScheduleCleaner.removeStopsNotUsed(outTransitSchedule);
        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(outTransitScheduleCleaned, scenario.getNetwork());
        log.warn(validationResult.getErrors());
        new TransitScheduleWriter(outTransitScheduleCleaned).writeFile(outScheduleFile);


    }


    public static TransitSchedule modifyTransitLinesFromTransitSchedule(TransitSchedule transitSchedule, Set<Id<TransitLine>> linesToModify, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario, modMethod modifyMethod) {
//        log.info("modifying " + linesToModify + " lines from transit schedule...");
        TransitSchedule tS = (new TransitScheduleFactoryImpl()).createTransitSchedule();
        Iterator var3 = transitSchedule.getFacilities().values().iterator();

        while (var3.hasNext()) {
            TransitStopFacility stop = (TransitStopFacility) var3.next();
            tS.addStopFacility(stop);
        }

        var3 = transitSchedule.getTransitLines().values().iterator();

        while (var3.hasNext()) {
            TransitLine line = (TransitLine) var3.next();
            if (!linesToModify.contains(line.getId())) {
                tS.addTransitLine(line);
                continue;
            }


            TransitLine lineNew = transitSchedule.getFactory().createTransitLine(line.getId());
            for (TransitRoute route : line.getRoutes().values()) {
                TransitRoute routeNew = null;
                if (modifyMethod.equals(modMethod.DeleteRoutesEntirelyInside)) {
                    if (pctOfStopsInZone(route, stopsInArea) == 1.0) {
                        continue;
                    }
                    routeNew = route;
                } else if (modifyMethod.equals(modMethod.TrimEnds)) {
                    routeNew = modifyRouteTrimEnds(route, stopsInArea, scenario);
                } else if (modifyMethod.equals(modMethod.ChooseLongerEnd)) {
                    routeNew = modifyRouteChooseLongerEnd(route, stopsInArea, scenario);
                } else if (modifyMethod.equals((modMethod.DeleteAllStopsWithin))) {
                    routeNew = modifyRouteDeleteAllStopsWithin(route, stopsInArea, scenario);
                } else if (modifyMethod.equals(modMethod.SplitOldRouteIntoMultiplePieces)) {
                    ArrayList<TransitRoute> routesNew = modifyRouteCutIntoShorterRoutes(route, stopsInArea, scenario);
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

            tS.addTransitLine(lineNew);

        }

        log.info("Old schedule contained " + transitSchedule.getTransitLines().values().size() + " lines.");
        log.info("New schedule contains " + tS.getTransitLines().values().size() + " lines.");
        return tS;
    }

    private static TransitRoute modifyRouteDeleteAllStopsWithin(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        TransitRoute routeNew = null;

        // Find which stops of route are within zone
        ArrayList<Boolean> inOutList = new ArrayList<>();
        for (TransitRouteStop stop : routeOld.getStops()) {
            Id<TransitStopFacility> id = stop.getStopFacility().getId();
            inOutList.add(stopsInArea.contains(id));
        }
        // Collect all stops and links from original route
        return modifyRoute(routeOld, scenario, inOutList);
    }

    private static TransitRoute modifyRouteTrimEnds(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        TransitRoute routeNew = null;

        // Find which stops of route are within zone
        ArrayList<Boolean> inOutList = new ArrayList<>();
        for (TransitRouteStop stop : routeOld.getStops()) {
            Id<TransitStopFacility> id = stop.getStopFacility().getId();
            inOutList.add(stopsInArea.contains(id));
        }

        ArrayList<Boolean> keepDiscardList = new ArrayList<>();
        for (int i = 0; i < inOutList.size(); i++) {
            keepDiscardList.add(false);
        }

        // from beginning of trip
        for (int i = 0; i < keepDiscardList.size(); i++) {
            if (inOutList.get(i) == true) {
                keepDiscardList.set(i, true);
            } else {
                break;
            }
        }

        // from end of trip
        for (int i = keepDiscardList.size() - 1; i >= 0; i--) {
            if (inOutList.get(i) == true) {
                keepDiscardList.set(i, true);
            } else {
                break;
            }
        }
        return modifyRoute(routeOld, scenario, keepDiscardList);
    }

    private static TransitRoute modifyRouteChooseLongerEnd(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        // Find which stops of route are within zone
        ArrayList<Boolean> inOutList = new ArrayList<>();
        for (TransitRouteStop stop : routeOld.getStops()) {
            Id<TransitStopFacility> id = stop.getStopFacility().getId();
            inOutList.add(stopsInArea.contains(id));
        }

        int falseCountBeginning = 0;
        for (int i = 0; i < inOutList.size(); i++) {
            if (!inOutList.get(i)) {
                falseCountBeginning++;
            } else {
                break;
            }
        }

        int falseCountEnd = 0;
        for (int i = inOutList.size() - 1; i >= 0; i--) {
            if (!inOutList.get(i)) {
                falseCountEnd++;
            } else {
                break;
            }
        }

        ArrayList<Boolean> keepDiscardList = new ArrayList<>();
        for (int i = 0; i < inOutList.size(); i++) {
            keepDiscardList.add(true);
        }


        //TODO: Come up with better conditions, and a way to split one route into two
        if (falseCountBeginning >= falseCountEnd) {
            for (int i = 0; i < falseCountBeginning; i++) {
                keepDiscardList.set(i, false);
            }
        } else {
            for (int i = inOutList.size() - 1; i >= inOutList.size() - falseCountEnd; i--) {
                keepDiscardList.set(i, false);
            }
        }

        return modifyRoute(routeOld, scenario, keepDiscardList);
    }

    private static ArrayList<TransitRoute> modifyRouteCutIntoShorterRoutes(TransitRoute routeOld, Set<Id<TransitStopFacility>> stopsInArea, Scenario scenario) {
        // Find which stops of route are within zone
        List<Boolean> inOutList = new ArrayList<>();
        ArrayList<TransitRoute> resultRoutes = new ArrayList<>();
        for (TransitRouteStop stop : routeOld.getStops()) {
            Id<TransitStopFacility> id = stop.getStopFacility().getId();
            inOutList.add(stopsInArea.contains(id));
        }

        List<TransitRouteStop> stopsOld = new ArrayList<>(routeOld.getStops());

        List<Id<Link>> linksOld = new ArrayList<>();
        linksOld.add(routeOld.getRoute().getStartLinkId());
        linksOld.addAll(routeOld.getRoute().getLinkIds());
        linksOld.add(routeOld.getRoute().getEndLinkId());


        // Make new stops and links lists
        List<TransitRouteStop> stopsNew = new ArrayList<>();
        List<Id<Link>> linksNew = new ArrayList<>();

        int newRouteCnt = 0;
        for (int i = 0; i < inOutList.size(); i++) {
            if (inOutList.get(i) == false) {

                // adds first stop that's within zone
                if (stopsNew.size() == 0 && i > 0 && allowOneStopWithinZone) {
                    stopsNew.add(stopsOld.get(i-1));
                    linksNew.add(linksOld.get(i-1));
                }

                stopsNew.add(stopsOld.get(i));
                linksNew.add(linksOld.get(i));
            } else if (inOutList.get(i) == true) {

                // The following is only done, if stop i is the first stop to enter the zone.
                if (stopsNew.size() > 0) {
                    //adds first stop in zone
                    if (allowOneStopWithinZone) {
                        stopsNew.add(stopsOld.get(i));
                        linksNew.add(linksOld.get(i));
                    }

                    // creates route and clears stopsNew and linksNew
                    TransitRoute routeNew = modifyRoute(routeOld, scenario, stopsNew, linksNew, newRouteCnt);
                    resultRoutes.add(routeNew);
                    newRouteCnt++;

                    stopsNew.clear();
                    linksNew.clear();
                }
            }
        }

        if (stopsNew.size() > 0) {
            TransitRoute routeNew = modifyRoute(routeOld, scenario, stopsNew, linksNew, newRouteCnt);
            resultRoutes.add(routeNew);
            newRouteCnt++;
        }

        return resultRoutes;
    }


    private static TransitRoute modifyRoute(TransitRoute routeOld, Scenario scenario, ArrayList<Boolean> inOutList) {
        TransitRoute routeNew;
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

    private static TransitRoute modifyRoute(TransitRoute routeOld, Scenario scenario, List<TransitRouteStop> stopsNew, List<Id<Link>> linksNew, int modNumber) {

        TransitRoute routeNew;

//        if (stopsNew.size() == 0) {
//            return null;
//        }

        // make route
        NetworkRoute networkRouteNew = RouteUtils.createNetworkRoute(linksNew, scenario.getNetwork());
        TransitScheduleFactory tsf = scenario.getTransitSchedule().getFactory();
        String routeIdOld = routeOld.getId().toString();
        Id<TransitRoute> routeIdNew = Id.create( routeIdOld + "_mod" + modNumber, TransitRoute.class);
        routeNew = tsf.createTransitRoute(routeIdNew, networkRouteNew, stopsNew, routeOld.getTransportMode());
        routeNew.setDescription(routeOld.getDescription());

        for (Departure departure : routeOld.getDepartures().values()) {
            String vehIdOld = departure.getVehicleId().toString();
            String depIdOld = departure.getId().toString();
            Departure departureNew = tsf.createDeparture(Id.create(depIdOld + "_mod" + modNumber, Departure.class), departure.getDepartureTime());
            departureNew.setVehicleId(Id.createVehicleId(vehIdOld + "_mod" + modNumber));
            routeNew.addDeparture(departureNew);
        }
        return routeNew;
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

    private static void countLinesInOut(TransitSchedule tS, Set<Id<TransitStopFacility>> stopsInArea) {
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
                    inOutList.add(stopsInArea.contains(id));
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
