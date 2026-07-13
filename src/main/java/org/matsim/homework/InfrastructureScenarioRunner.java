package org.matsim.homework;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.vsp.qgis.QGisFileWriter;
import org.matsim.contrib.analysis.vsp.qgis.QGisWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class InfrastructureScenarioRunner {

	private static final String DEFAULT_CONFIG_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v7.0/input/berlin-v7.0.config.xml";
	private static final String OUTPUT_DIR = "output/infrastructure-scenario";

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig(DEFAULT_CONFIG_PATH);
		config.controller().setOutputDirectory(OUTPUT_DIR);

		config.controller().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		addTramExtension(scenario);
		Controler controler = new Controler(scenario);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		//controler.run(); //Auskommentiert, da es nicht läuft mir der Config.
	}

	private static void addTramExtension(Scenario scenario) {

		double TramFreespeed = 13.02; //CHECK FOR VALUES
		String Trammodes = "tram";
		double TramCapa = 150;
		TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory(); //get factory
		Set<Integer> prepend = Set.of(0, 4, 5, 6, 10, 12, 13, 15); //Routes that start at Warschauer Straße
		Set<Integer> append = Set.of(1, 2, 3, 7, 8, 11, 18, 25, 28); //Routes that end at Warschauer Straße
		List<TransitStopFacility> extensionStopsForward = new ArrayList<>();
		List<TransitStopFacility> extensionStopsBackward = new ArrayList<>(); // Liste, die die Extensionstops speichert
		List<Id<Link>> linkIdsF = new ArrayList<>();
		List<Id<Link>> linkIdsB = new ArrayList<>();

		// create new Nodes and Links for the Extension
		Node[] stopTram = new Node[]{
			scenario.getNetwork().getNodes().get(Id.createNodeId("pt_75928_tram")), //H Warschauer straße, end of M10
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new1"), new Coord(802648.44, 5828118.063)),  //H Falckenstraße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new2"), new Coord(801369.589, 5825548.911)),  //H Görlitzer park
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new3"), new Coord(801173.733, 5825164.820)),  //H Reichenberger Straße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new4"), new Coord(800952.363, 5824717.524)),    //H Framstraße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new5"), new Coord(800770.939, 5824360.132)),    //H Pannierstraße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new6"), new Coord(800418.433, 5824504.268))}; /*H Hermannplatz*/
		for (Node node : stopTram) {
				scenario.getNetwork().addNode(node);
		}
		String[] stopTramNames = new String[]{
			"Warschauer_Strasse", "Falckenstrasse", "Goerlitzer_Park", "Reichenberger_Strasse", "Framstrasse", "Pannierstrasse", "Hermannplatz"};

		//Create Links for Tram
		/*Link Loop = scenario.getNetwork().getFactory().createLink(Id.createLinkId("M10_LoopHermannplatz"), stopTram[6], stopTram[6]); //LoopLink at end of Line.
		Loop.setFreespeed(TramFreespeed);
		Loop.setAllowedModes(Set.of(Trammodes));
		Loop.setCapacity(TramCapa);
		Loop.setLength(NetworkUtils.getEuclideanDistance(stopTram[6].getCoord(), stopTram[6].getCoord()));
		scenario.getNetwork().addLink(Loop);*/

		//Transitline of Tram M10 is in Line 1169283, refid:id="M10---20481" -> 29 Routes.
		TransitLine m10 = scenario.getTransitSchedule().getTransitLines().get(Id.create("M10---20481", TransitLine.class));

		// for loop iterates over all Nodes to create pt_links in between Stops in  both directions
		for (int i = 0; i < stopTram.length - 1; i++) {

			Link direction1 = scenario.getNetwork().getFactory().createLink(Id.createLinkId("M10LinkStop" + (i + 1) + "comingFromStop" + i), stopTram[i], stopTram[i + 1]); //forward direction
			Link direction2 = scenario.getNetwork().getFactory().createLink(Id.createLinkId("M10LinkStop" + i + "comingFromStop" + (i + 1)), stopTram[i + 1], stopTram[i]); //backward direction

			direction1.setFreespeed(TramFreespeed);
			direction2.setFreespeed(TramFreespeed);
			direction1.setLength(NetworkUtils.getEuclideanDistance(stopTram[i].getCoord(), stopTram[i + 1].getCoord()));
			direction2.setLength(NetworkUtils.getEuclideanDistance(stopTram[i + 1].getCoord(), stopTram[i].getCoord()));
			direction1.setCapacity(TramCapa);
			direction2.setCapacity(TramCapa);
			direction1.setAllowedModes(Set.of(Trammodes));
			direction2.setAllowedModes(Set.of(Trammodes));

			scenario.getNetwork().addLink(direction1);
			scenario.getNetwork().addLink(direction2);

			//add: transitstopfacilities
			TransitStopFacility Stopdir1 = scenario.getTransitSchedule().getFactory().createTransitStopFacility(Id.create("M10Stop" + (i + 1) + "comingFrom" + i, TransitStopFacility.class), stopTram[i + 1].getCoord(), true);
			TransitStopFacility Stopdir2 = scenario.getTransitSchedule().getFactory().createTransitStopFacility(Id.create("M10Stop" + (i) + "comingFrom" + (i + 1), TransitStopFacility.class), stopTram[i + 1].getCoord(), true);

			Stopdir1.setLinkId(direction1.getId());
			Stopdir1.setName(stopTramNames[i + 1]);
			scenario.getTransitSchedule().addStopFacility(Stopdir1);
			linkIdsF.add(direction1.getId());
			extensionStopsForward.add(Stopdir1);

			Stopdir2.setLinkId(direction2.getId());
			Stopdir2.setName(stopTramNames[i]);
			scenario.getTransitSchedule().addStopFacility(Stopdir2);
			linkIdsB.add(direction2.getId());
			extensionStopsBackward.add(Stopdir2); // add stops to list

		}
		System.out.println("Forwardstops: " + extensionStopsForward);
		System.out.println("Backwardstops: " + extensionStopsBackward);
		//make RouteStops
		double offsetF = 0;
		List<TransitRouteStop> forwardRouteStops = new ArrayList<>();
		for (TransitStopFacility facility : extensionStopsForward) {
			forwardRouteStops.add(factory.createTransitRouteStop(facility, OptionalTime.defined(offsetF), OptionalTime.defined(offsetF)));
			offsetF += 120; // 2 Minuten wegzeit zwischen halten.
		}

		double offsetB = 0;
		List<TransitRouteStop> backwardRouteStops = new ArrayList<>();
		for (TransitStopFacility facility : extensionStopsBackward) {
			backwardRouteStops.add(factory.createTransitRouteStop(facility, OptionalTime.defined(offsetB), OptionalTime.defined(offsetB)));
			offsetB += 120; // 2 Minuten wegzeit zwischen halten.
		}
		System.out.println("Offset Backward" + offsetB);



		for (int i = 0; i < 30; i++) {
			Id<TransitRoute> routeId = Id.create("M10---20481_" + i, TransitRoute.class);
			TransitRoute existRoute = m10.getRoutes().get(routeId);
			List<TransitRouteStop> stops = new ArrayList<>(); //Transit stops List
			if (existRoute == null)continue;
			NetworkRoute newRoute = existRoute.getRoute(); //declare new networkroute
			if (!prepend.contains(i) && !append.contains(i))continue;

			if (append.contains(i)) {
				TransitRouteStop lastStop = existRoute.getStops().get(existRoute.getStops().size() - 1);
				double offset = lastStop.getDepartureOffset().seconds();
				stops.addAll(existRoute.getStops());
				for (TransitRouteStop Stop : backwardRouteStops) {
					TransitRouteStop newStop = factory.createTransitRouteStop(Stop.getStopFacility(), OptionalTime.defined(Stop.getArrivalOffset().seconds() + offset), OptionalTime.defined(Stop.getDepartureOffset().seconds() + offset));
					stops.add(newStop);
				}
				NetworkRoute oldNetworkRoute = existRoute.getRoute();
				List<Id<Link>> newLinkIds = new ArrayList<>();
				newLinkIds.addAll(oldNetworkRoute.getLinkIds());
				newLinkIds.addAll(linkIdsB);
				newRoute = RouteUtils.createLinkNetworkRouteImpl(oldNetworkRoute.getStartLinkId(), newLinkIds, newLinkIds.getLast()); //hierfür Liste mit allen Links -> Dann Networkroute erstellen?
			}
			else if (prepend.contains(i)) {
				List<TransitRouteStop> shiftedExistingStops = new ArrayList<>();
				double extensionTime = forwardRouteStops.get(forwardRouteStops.size() - 1).getDepartureOffset().seconds();
				stops.addAll(forwardRouteStops);
				//shift Offset from existing Stops
				for (TransitRouteStop oldStop : existRoute.getStops()) {

					TransitRouteStop newStop = factory.createTransitRouteStop(oldStop.getStopFacility(), OptionalTime.defined(oldStop.getArrivalOffset().seconds() + offsetF), OptionalTime.defined(oldStop.getDepartureOffset().seconds() + offsetF));
					shiftedExistingStops.add(newStop);
				}
				stops.addAll(shiftedExistingStops);
				NetworkRoute oldNetworkRoute = existRoute.getRoute();
				List<Id<Link>> newLinkIds = new ArrayList<>();
				newLinkIds.addAll(linkIdsF);
				newLinkIds.addAll(oldNetworkRoute.getLinkIds());
				newRoute = RouteUtils.createLinkNetworkRouteImpl(newLinkIds.getFirst(), newLinkIds, oldNetworkRoute.getEndLinkId());
			}
			//make new Transit Route: ersetzen der alten Route
			TransitRoute newTransitRoute = factory.createTransitRoute(existRoute.getId(), newRoute, stops, existRoute.getTransportMode());
			existRoute.getDepartures().values().forEach(newTransitRoute::addDeparture);
			m10.removeRoute(existRoute);
			m10.addRoute(newTransitRoute);

		}

		File outputDir = new File("output");
		if (!outputDir.exists()) {outputDir.mkdirs();}

		NetworkUtils.writeNetwork(
			scenario.getNetwork(),
			"output/network.xml.gz"
		);

		new TransitScheduleWriter(
			scenario.getTransitSchedule()
		).writeFile(
			"output/transitSchedule.xml.gz"
		);
		Links2ESRIShape exporter =
			new Links2ESRIShape(
				scenario.getNetwork(),
				"output/m10_network.shp",
				scenario.getConfig().global().getCoordinateSystem()
			);

		exporter.write();

		System.out.println("Finished");
	}

}







