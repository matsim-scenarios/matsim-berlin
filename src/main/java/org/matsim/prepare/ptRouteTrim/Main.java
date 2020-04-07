package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
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

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TransitSchedule inTransitSchedule = scenario.getTransitSchedule();



        // Map all Stops, and whether they are inside or outside of shp file
        Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone = new HashMap<>();
        for (TransitStopFacility stop : inTransitSchedule.getFacilities().values()){
            stop2LocationInZone.put(stop.getId(), ShpGeometryUtils.isCoordInPreparedGeometries(stop.getCoord(), geometries));
        }

        for (Id<TransitStopFacility> stopID : stop2LocationInZone.keySet()){
            System.out.println(stopID + " - " + stop2LocationInZone.get(stopID));
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
