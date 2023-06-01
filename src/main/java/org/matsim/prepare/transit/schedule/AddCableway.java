package org.matsim.prepare.transit.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddCableway {

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("~/git/matsim-berlin/scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml");
        config.qsim().setFlowCapFactor(1.0d);
        config.qsim().setStorageCapFactor(1.0d);
        config.plans().setInputFile(null);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        VehicleType vehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("cableway", VehicleType.class));
        vehicleType.setLength(1.0d);
        vehicleType.setNetworkMode("cableway");
        vehicleType.getCapacity().setSeats(1);
        scenario.getTransitVehicles().addVehicleType(vehicleType);

        addCablewayRoute1(scenario);
        addCablewayRoute2(scenario);

        NetworkUtils.writeNetwork(scenario.getNetwork(), "berlin-v5.5-1pct.network_cableway.xml.gz");
        (new TransitScheduleWriter(scenario.getTransitSchedule())).writeFile("berlin-v5.5-1pct.transitSchedule_cableway.xml.gz");
        (new MatsimVehicleWriter(scenario.getTransitVehicles())).writeFile("berlin-v5.5-1pct.transitVehicles_cableway.xml.gz");

        // Test
        Controler controler = new Controler(scenario);
        controler.getConfig().controler().setLastIteration(1);
        controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore);
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();
    }

    private static void addCablewayRoute1(Scenario scenario) {
        addLinkAndStop(scenario, "pt_070101002924", "pt_070101002924", 1, 5d);
        addForthAndBackLinksAndStops(scenario, "pt_070101002924", "pt_070101002265", 1271.1368867781757d, 5.5d);
        addForthAndBackLinksAndStops(scenario, "pt_070101002265", "pt_000008010404", 1067.642657849755d, 5.5d);
        addForthAndBackLinksAndStops(scenario, "pt_000008010404", "pt_070101000790", 898.1498361340358d, 5.5d);
        addForthAndBackLinksAndStops(scenario, "pt_070101000790", "pt_070101003642", 1402.2202971472982d, 5.5d);

        TransitLine transitLine = scenario.getTransitSchedule().getFactory().createTransitLine(Id.create("Cableway1", TransitLine.class));
        NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(
                getLinkIdFromToNodes("pt_070101002924", "pt_070101002924"),
                List.of(getLinkIdFromToNodes("pt_070101002924", "pt_070101002265"),
                        getLinkIdFromToNodes("pt_070101002265", "pt_000008010404"),
                        getLinkIdFromToNodes("pt_000008010404", "pt_070101000790"),
                        getLinkIdFromToNodes("pt_070101000790", "pt_070101003642"),
                        getLinkIdFromToNodes("pt_070101003642", "pt_070101000790"),
                        getLinkIdFromToNodes("pt_070101000790", "pt_000008010404"),
                        getLinkIdFromToNodes("pt_000008010404", "pt_070101002265")),
                getLinkIdFromToNodes("pt_070101002265", "pt_070101002924"));

        List<TransitRouteStop> stops = new ArrayList<>();
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101002924", "pt_070101002924"),
                TransitStopFacility.class), 0.0d, 0.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101002924", "pt_070101002265"),
                TransitStopFacility.class), 254.0d, 254.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101002265", "pt_000008010404"),
                TransitStopFacility.class), 468.0d, 468.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_000008010404", "pt_070101000790"),
                TransitStopFacility.class), 648.0d, 648.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101000790", "pt_070101003642"),
                TransitStopFacility.class), 928.0d, 928.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101003642", "pt_070101000790"),
                TransitStopFacility.class), 1208.0d, 1208.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101000790", "pt_000008010404"),
                TransitStopFacility.class), 1388.0d, 1388.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_000008010404", "pt_070101002265"),
                TransitStopFacility.class), 1602.0d, 1602.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101002265", "pt_070101002924"),
                TransitStopFacility.class), 1856.0d, 1856.0d));

        stops.forEach(stop -> stop.setAwaitDepartureTime(true));

        TransitRoute transitRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(
                Id.create("Cableway1", TransitRoute.class),
                networkRoute,
                stops,
                "");

        for (int i = 0; i < 30*60*60; i++) {
            Departure departure = scenario.getTransitSchedule().getFactory().createDeparture(Id.create("cableway_1_" + i, Departure.class), i * 9);
            scenario.getTransitVehicles().addVehicle(
                    scenario.getTransitVehicles().getFactory().createVehicle(
                            Id.createVehicleId("cableway_1_" + i),
                            scenario.getTransitVehicles().getVehicleTypes().get(Id.create("cableway", VehicleType.class))));
            departure.setVehicleId(Id.createVehicleId("cableway_1_" + i));
            transitRoute.addDeparture(departure);
        }

        transitLine.addRoute(transitRoute);
        scenario.getTransitSchedule().addTransitLine(transitLine);
    }

    private static void addCablewayRoute2(Scenario scenario) {
        addLinkAndStop(scenario, "pt_070201093704", "pt_070201093704", 1, 5d);
        addForthAndBackLinksAndStops(scenario, "pt_070201093704", "pt_070101000447", 1001.4029869474583d, 5.5d);
        addForthAndBackLinksAndStops(scenario, "pt_070101000447", "pt_060054105611", 1246.9131669067094d, 5.5d);
        addForthAndBackLinksAndStops(scenario, "pt_060054105611", "pt_070201074102", 1362.526321305025d, 5.5d);
        addForthAndBackLinksAndStops(scenario, "pt_070201074102", "pt_070201013002", 1156.7549654803352d, 5.5d);

        TransitLine transitLine = scenario.getTransitSchedule().getFactory().createTransitLine(Id.create("Cableway2", TransitLine.class));
        NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(
                getLinkIdFromToNodes("pt_070201093704", "pt_070201093704"),
                List.of(getLinkIdFromToNodes("pt_070201093704", "pt_070101000447"),
                        getLinkIdFromToNodes("pt_070101000447", "pt_060054105611"),
                        getLinkIdFromToNodes("pt_060054105611", "pt_070201074102"),
                        getLinkIdFromToNodes("pt_070201074102", "pt_070201013002"),
                        getLinkIdFromToNodes("pt_070201013002", "pt_070201074102"),
                        getLinkIdFromToNodes("pt_070201074102", "pt_060054105611"),
                        getLinkIdFromToNodes("pt_060054105611", "pt_070101000447")),
                getLinkIdFromToNodes("pt_070101000447", "pt_070201093704"));

        List<TransitRouteStop> stops = new ArrayList<>();
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070201093704", "pt_070201093704"),
                TransitStopFacility.class), 0.0d, 0.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070201093704", "pt_070101000447"),
                TransitStopFacility.class), 254.0d, 254.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101000447", "pt_060054105611"),
                TransitStopFacility.class), 468.0d, 468.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_060054105611", "pt_070201074102"),
                TransitStopFacility.class), 648.0d, 648.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070201074102", "pt_070201013002"),
                TransitStopFacility.class), 928.0d, 928.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070201013002", "pt_070201074102"),
                TransitStopFacility.class), 1208.0d, 1208.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070201074102", "pt_060054105611"),
                TransitStopFacility.class), 1388.0d, 1388.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_060054105611", "pt_070101000447"),
                TransitStopFacility.class), 1602.0d, 1602.0d));
        stops.add(createTransitRouteStop(scenario, Id.create(getNameFromToNodes("pt_070101000447", "pt_070201093704"),
                TransitStopFacility.class), 1856.0d, 1856.0d));

        stops.forEach(stop -> stop.setAwaitDepartureTime(true));

        TransitRoute transitRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(
                Id.create("Cableway2", TransitRoute.class),
                networkRoute,
                stops,
                "");

        for (int i = 0; i * 9 < 30*60*60; i++) {
            Departure departure = scenario.getTransitSchedule().getFactory().createDeparture(Id.create("cableway_2_" + i, Departure.class), i * 9);
            scenario.getTransitVehicles().addVehicle(
                    scenario.getTransitVehicles().getFactory().createVehicle(
                            Id.createVehicleId("cableway_2_" + i),
                            scenario.getTransitVehicles().getVehicleTypes().get(Id.create("cableway", VehicleType.class))));
            departure.setVehicleId(Id.createVehicleId("cableway_2_" + i));
            transitRoute.addDeparture(departure);
        }

        transitLine.addRoute(transitRoute);
        scenario.getTransitSchedule().addTransitLine(transitLine);
    }

    private static TransitRouteStop createTransitRouteStop(Scenario scenario, Id<TransitStopFacility> stop, double arrivalOffset, double departureOffset) {
        return scenario.getTransitSchedule().getFactory().createTransitRouteStop(
                scenario.getTransitSchedule().getFacilities().get(stop),
                arrivalOffset,
                departureOffset);
    }

    private static void addForthAndBackLinksAndStops(Scenario scenario, String fromNodeId, String toNodeId, double length, double speed) {
        addLinkAndStop(scenario, fromNodeId, toNodeId, length, speed);
        addLinkAndStop(scenario, toNodeId, fromNodeId, length, speed);
    }

    private static void addLinkAndStop (Scenario scenario, String fromNodeId, String toNodeId, double length, double speed) {
        Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(getNameFromToNodes(fromNodeId, toNodeId)),
                scenario.getNetwork().getNodes().get(Id.createNodeId(fromNodeId)),
                scenario.getNetwork().getNodes().get(Id.createNodeId(toNodeId)));
        link.setLength(length);
        link.getFreespeed(speed);
        link.setCapacity(100000.0);
        link.setAllowedModes(Set.of("cableway"));
        scenario.getNetwork().addLink(link);

        TransitStopFacility transitStopFacility = scenario.getTransitSchedule().getFactory().createTransitStopFacility(
                Id.create(getNameFromToNodes(fromNodeId, toNodeId), TransitStopFacility.class),
                scenario.getNetwork().getNodes().get(Id.createNodeId(toNodeId)).getCoord(),
                false);
        transitStopFacility.setLinkId(getLinkIdFromToNodes(fromNodeId, toNodeId));
        scenario.getTransitSchedule().addStopFacility(transitStopFacility);
    }

    private static String getNameFromToNodes(String fromNodeId, String toNodeId) {
        return "cableway_" + fromNodeId + "-" + toNodeId;
    }

    private static Id<Link> getLinkIdFromToNodes(String fromNodeId, String toNodeId) {
        return Id.createLinkId(getNameFromToNodes(fromNodeId, toNodeId));
    }
}
