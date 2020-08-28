package org.matsim.prepare.ptRouteTrim;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TransitRouteTrimmerTest {
    private TransitRouteTrimmer transitRouteTrimmer;

    private Scenario scenario;


    @Before
    public void prepare() throws MalformedURLException {
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
        final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output4/";
        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);
        config.vehicles().setVehiclesFile(inVehiclesFile);

        scenario = ScenarioUtils.loadScenario(config);

        Set<String> modes2Trim = new HashSet<>();
        modes2Trim.add("bus");
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));
        System.out.println("\n Modify Routes: SplitRoute");
        transitRouteTrimmer = new TransitRouteTrimmer(scenario.getTransitSchedule(), scenario.getVehicles(), modes2Trim, geometries);

    }

    @Test
    public void testDeleteRoutesEntirelyInsideZoneAllIn() {

        Id<TransitLine> transitLineId = Id.create("265---17372_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("265---17372_700_0", TransitRoute.class);


        // old
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRouteOld.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRouteOld.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        assertEquals(outCnt1, 0);
        assertEquals(inCnt1, sizeOld);
//
//        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.DeleteRoutesEntirelyInsideZone);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        assertTrue("sched should include empty transit line",transitScheduleNew.getTransitLines().containsKey(transitLineId));
        assertEquals("transitLine should not longer contain any routes", transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(),0);

    }

    @Test
    public void testTrimEnds_HalfInHalfOut() {

        // old
        TransitRoute transitRoute1 = scenario.getTransitSchedule().getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15", TransitRoute.class));
        int sizeOld = transitRoute1.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRoute1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.TrimEnds);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();


        // new
        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15_mod1", TransitRoute.class));
        int sizeNew = transitRouteNew.getStops().size();
        int inCnt2 = 0;
        int outCnt2 = 0;
        for (TransitRouteStop stop : transitRouteNew.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt2++;
            } else {
                outCnt2++;
            }
        }

        Assert.assertTrue(sizeOld != sizeNew);
        assertEquals("there should only be one stop within the zone", 1, inCnt2);
        assertEquals("# of stops outside of zone should remain same", outCnt1, outCnt2);

    }

    @Test
    public void testTrimEnds_AllIn() {

        Id<TransitLine> transitLineId = Id.create("265---17372_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("265---17372_700_0", TransitRoute.class);


        // old
        TransitRoute transitRoute1 = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRoute1.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRoute1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        assertEquals(outCnt1, 0);
        assertEquals(inCnt1, sizeOld);
//
//        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.TrimEnds);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        assertTrue("sched should include empty transit line",transitScheduleNew.getTransitLines().containsKey(transitLineId));
        assertEquals("transitLine should not longer contain any routes", transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(),0);

    }


    @Test
    public void testTrimEnds_MiddleIn() {

        Id<TransitLine> transitLineId = Id.create("161---17326_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("161---17326_700_21", TransitRoute.class);
        Set<Id<TransitStopFacility>> stopsInZone = transitRouteTrimmer.getStopsInZone();


        // old
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsOld = transitRouteOld.getStops().size();
        int numLinksOld = transitRouteOld.getRoute().getLinkIds().size();

        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(0).getStopFacility().getId()));
        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(numStopsOld - 1).getStopFacility().getId()));



        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.TrimEnds);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        TransitRoute routeNew = transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().get(Id.create("161---17326_700_21_mod1", TransitRoute.class));
        int numStopsNew = routeNew.getStops().size();
        int numLinksNew = routeNew.getRoute().getLinkIds().size();

        Assert.assertTrue("line should still exist", transitScheduleNew.getTransitLines().containsKey(transitLineId));
        Assert.assertEquals("new route should contain same number of stops as old one", numStopsOld, numStopsNew);
        Assert.assertEquals("new route should contain same number of links as old one", numLinksOld, numLinksNew);
    }


    @Test
    public void testSkipStops_HalfInHalfOut() {

        // old
        TransitRoute transitRoute1 = scenario.getTransitSchedule().getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15", TransitRoute.class));
        int sizeOld = transitRoute1.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRoute1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SkipStopsWithinZone);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();


        // new
        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15_mod1", TransitRoute.class));
        int sizeNew = transitRouteNew.getStops().size();
        int inCnt2 = 0;
        int outCnt2 = 0;
        for (TransitRouteStop stop : transitRouteNew.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt2++;
            } else {
                outCnt2++;
            }
        }

        Assert.assertTrue(sizeOld != sizeNew);
        assertEquals("there should only be one stop within the zone", 1, inCnt2);
        assertEquals("# of stops outside of zone should remain same", outCnt1, outCnt2);

    }

    @Test
    public void testSkipStops_AllIn() {

        Id<TransitLine> transitLineId = Id.create("265---17372_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("265---17372_700_0", TransitRoute.class);


        // old
        TransitRoute transitRoute1 = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRoute1.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRoute1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        assertEquals(outCnt1, 0);
        assertEquals(inCnt1, sizeOld);

        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SkipStopsWithinZone);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        assertTrue("sched should include empty transit line",transitScheduleNew.getTransitLines().containsKey(transitLineId));
        assertEquals("transitLine should not longer contain any routes", transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(),0);

    }

    @Test
    public void testSkipStops_MiddleIn() {

        Id<TransitLine> transitLineId = Id.create("161---17326_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("161---17326_700_21", TransitRoute.class);
        Set<Id<TransitStopFacility>> stopsInZone = transitRouteTrimmer.getStopsInZone();


        // old
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsOld = transitRouteOld.getStops().size();
        int numLinksOld = transitRouteOld.getRoute().getLinkIds().size();

        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(0).getStopFacility().getId()));
        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(numStopsOld - 1).getStopFacility().getId()));



        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SkipStopsWithinZone);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        TransitRoute routeNew = transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().get(Id.create("161---17326_700_21_mod1", TransitRoute.class));
        int numStopsNew = routeNew.getStops().size();
        int numLinksNew = routeNew.getRoute().getLinkIds().size();


        int inCntNew = 0;
        for (TransitRouteStop stop : routeNew.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCntNew++;
            }
        }


        Assert.assertTrue("line should still exist", transitScheduleNew.getTransitLines().containsKey(transitLineId));
        Assert.assertNotEquals("new route should NOT contain same number of stops as old one", numStopsOld, numStopsNew);
        Assert.assertEquals("new route should contain same number of links as old one", numLinksOld, numLinksNew);
        Assert.assertEquals("new route should only have two stops within zone, one per zone entrance/exit", 2, inCntNew);

    }


    @Test
    public void testSplitRoutes_HalfInHalfOut() {

        // old
        Id<TransitLine> transitLineId = Id.create("184---17340_700", TransitLine.class);
        TransitRoute transitRoute1 = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(Id.create("184---17340_700_15", TransitRoute.class));
        int sizeOld = transitRoute1.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRoute1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SplitRoute);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();



        assertTrue(transitScheduleNew.getTransitLines().containsKey(transitLineId));
        TransitLine transitLine = transitScheduleNew.getTransitLines().get(transitLineId);
        assertTrue(transitLine.getRoutes().containsKey(Id.create("184---17340_700_15_mod1", TransitRoute.class)));
        // new
        TransitRoute transitRouteNew = transitScheduleNew.getTransitLines().get(Id.create("184---17340_700", TransitLine.class)).getRoutes().get(Id.create("184---17340_700_15_mod1", TransitRoute.class));
        int sizeNew = transitRouteNew.getStops().size();
        int inCnt2 = 0;
        int outCnt2 = 0;
        for (TransitRouteStop stop : transitRouteNew.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt2++;
            } else {
                outCnt2++;
            }
        }

        Assert.assertTrue(sizeOld != sizeNew);
        assertEquals("there should only be one stop within the zone", 1, inCnt2);
        assertEquals("# of stops outside of zone should remain same", outCnt1, outCnt2);

    }

    @Test
    public void testSplitRoutes_AllIn() {

        Id<TransitLine> transitLineId = Id.create("265---17372_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("265---17372_700_0", TransitRoute.class);


        // old
        TransitRoute transitRoute1 = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int sizeOld = transitRoute1.getStops().size();
        int inCnt1 = 0;
        int outCnt1 = 0;
        for (TransitRouteStop stop : transitRoute1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCnt1++;
            } else {
                outCnt1++;
            }
        }

        assertEquals(outCnt1, 0);
        assertEquals(inCnt1, sizeOld);

        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SplitRoute);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        assertTrue("sched should include empty transit line",transitScheduleNew.getTransitLines().containsKey(transitLineId));
        assertEquals("transitLine should not longer contain any routes", transitScheduleNew.getTransitLines().get(transitLineId).getRoutes().size(),0);

    }

    @Test
    public void testSplitRoutes_MiddleIn() {

        Id<TransitLine> transitLineId = Id.create("161---17326_700", TransitLine.class);
        Id<TransitRoute> transitRouteId = Id.create("161---17326_700_21", TransitRoute.class);
        Set<Id<TransitStopFacility>> stopsInZone = transitRouteTrimmer.getStopsInZone();


        // old
        TransitRoute transitRouteOld = scenario.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId);
        int numStopsOld = transitRouteOld.getStops().size();
        int numLinksOld = transitRouteOld.getRoute().getLinkIds().size();

        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(0).getStopFacility().getId()));
        assertFalse(stopsInZone.contains(transitRouteOld.getStops().get(numStopsOld - 1).getStopFacility().getId()));



        // Modification
        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();
        transitRouteTrimmer.removeEmptyLines = false;
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SplitRoute);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();

        assertTrue("line should still exist", transitScheduleNew.getTransitLines().containsKey(transitLineId));
        TransitLine transitLineNew = transitScheduleNew.getTransitLines().get(transitLineId);

        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_21_mod1", TransitRoute.class)));
        assertTrue(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_21_mod2", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_21_mod0", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_21_mod3", TransitRoute.class)));
        assertFalse(transitLineNew.getRoutes().containsKey(Id.create("161---17326_700_21", TransitRoute.class)));


        TransitRoute routeNew1 = transitLineNew.getRoutes().get(Id.create("161---17326_700_21_mod1", TransitRoute.class));
        TransitRoute routeNew2 = transitLineNew.getRoutes().get(Id.create("161---17326_700_21_mod2", TransitRoute.class));

        assertNotEquals(transitRouteOld.getStops().size(), routeNew1.getStops().size() + routeNew2.getStops().size());
        assertNotEquals(routeNew1.getStops().size(), routeNew2.getStops().size());

        int inCntNew1 = 0;
        for (TransitRouteStop stop : routeNew1.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCntNew1++;
            }
        }

        int inCntNew2 = 0;
        for (TransitRouteStop stop : routeNew2.getStops()) {
            if (transitRouteTrimmer.getStopsInZone().contains(stop.getStopFacility().getId())) {
                inCntNew2++;
            }
        }



//        Assert.assertNotEquals("new route should NOT contain same number of stops as old one", numStopsOld, numStopsNew);
//        Assert.assertEquals("new route should contain same number of links as old one", numLinksOld, numLinksNew);
        Assert.assertEquals("new route #1 should only have one stop within zone", 1, inCntNew1);
        Assert.assertEquals("new route #2 should only have one stop within zone", 1, inCntNew2);


        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output4/";
        Vehicles vehiclesNew = transitRouteTrimmer.getVehicles();
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "output-trimmed-schedule.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "output-vehicles.xml.gz");

    }





}
