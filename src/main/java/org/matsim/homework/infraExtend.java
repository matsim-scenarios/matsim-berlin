package org.matsim.homework;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class infraExtend {
	private static final String DEFAULT_CONFIG_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v7.0/input/berlin-v7.0.config.xml";
	private static final String OUTPUT_DIR = "output/infrastructure-scenario";

	public static void main(String[] args) {

		//Konfiguration
		Config config = ConfigUtils.loadConfig(DEFAULT_CONFIG_PATH);
		config.controller().setOutputDirectory(OUTPUT_DIR);
		config.controller().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Datenstrukturen anlegen
		TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory();
		List<TransitRouteStop> forwardRouteStops  = new ArrayList<>();
    	List<TransitRouteStop> backwardRouteStops = new ArrayList<>();
		List<Id<Link>> linkIdsF = new ArrayList<>();
  		List<Id<Link>> linkIdsB = new ArrayList<>();
		List<TransitStopFacility> extensionStopsForward = new ArrayList<>();
		List<TransitStopFacility> extensionStopsBackward = new ArrayList<>();

		//add Data for Extension: M10
		Set<Integer> prependM10 = Set.of(0, 4, 5, 6, 10, 12, 13, 15);
		Set<Integer> appendM10 = Set.of(1, 2, 3, 7, 8, 11, 18, 25, 28);
		Node[] stopM10 = new Node[]{
			scenario.getNetwork().getNodes().get(Id.createNodeId("pt_75928_tram")), //H Warschauer straße, end of M10
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new1"), new Coord(802648.44, 5828118.063)),  //H Falckenstraße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new2"), new Coord(801369.589, 5825548.911)),  //H Görlitzer park
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new3"), new Coord(801173.733, 5825164.820)),  //H Reichenberger Straße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new4"), new Coord(800952.363, 5824717.524)),    //H Framstraße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new5"), new Coord(800770.939, 5824360.132)),    //H Pannierstraße
			scenario.getNetwork().getFactory().createNode(Id.createNodeId("pt_M10_new6"), new Coord(800418.433, 5824504.268))}; //H Hermannplatz
		TransitLine m10 = scenario.getTransitSchedule().getTransitLines().get(Id.create("M10---20481", TransitLine.class));

		//Ausführen der Netzwerkerweiterung
		createNetwork(scenario, 13.02,150,"tram",stopM10,extensionStopsForward, extensionStopsBackward, factory, linkIdsF, linkIdsB);
		extendTransitRoutes(factory, forwardRouteStops, backwardRouteStops,extensionStopsForward,extensionStopsBackward,m10,prependM10,appendM10,linkIdsF,linkIdsB);
		Export(scenario);

		//Simulation starten
		//Controler controler = new Controler(scenario);
		//controler.run(); //Auskommentiert, da es nicht läuft mir der Config.
	}

private static void createNetwork(Scenario scenario,double freespeed, double capacity, String mode, Node[] stops, List<TransitStopFacility> extensionStopsForward, List<TransitStopFacility> extensionStopsBackward, TransitScheduleFactory factory,List<Id<Link>> linkIdsF, List<Id<Link>> linkIdsB) {

    // Nodes hinzufügen
    for (Node node : stops) if (!scenario.getNetwork().getNodes().containsKey(node.getId())) { {scenario.getNetwork().addNode(node);}}

    // Links in beide Richtungen erzeugen
    for (int i = 0; i < stops.length - 1; i++) {
      	Link fw = createDirection(scenario, stops[i], stops[i+1], "M10LinkStop" + (i + 1) + "comingFromStop" + i, freespeed, capacity, mode);
		Link bw = createDirection(scenario,stops[i + 1],stops[i],"M10LinkStop" + i + "comingFromStop" + (i + 1),freespeed,capacity,mode);
		createTransitStopFacility(scenario,factory,extensionStopsForward, fw, stops[i+1]);createTransitStopFacility(scenario,factory,extensionStopsBackward, bw, stops[i+1]);
		linkIdsF.add(fw.getId());linkIdsB.add(bw.getId());
    }
}
private static Link createDirection(Scenario scenario,Node from, Node to, String linkId,  double freespeed, double capacity, String mode) {
	//Funktion zum Erstellen der Links
    Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(linkId),from, to);
    link.setFreespeed(freespeed);
    link.setLength(NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord()));
    link.setCapacity(capacity);
    link.setAllowedModes(Set.of(mode));
    scenario.getNetwork().addLink(link);
    return link;
}
private static void createTransitStopFacility(Scenario scenario, TransitScheduleFactory factory, List<TransitStopFacility> extensionStops, Link link, Node node){
	//Funktion zum Erstellen der TransitStopfacilities
	TransitStopFacility stop = factory.createTransitStopFacility(Id.create(link.getId()+"_stop", TransitStopFacility.class), node.getCoord(), true);
		stop.setLinkId(link.getId());
		scenario.getTransitSchedule().addStopFacility(stop);
		extensionStops.add(stop);
}

public static void extendTransitRoutes (TransitScheduleFactory factory, List<TransitRouteStop> forwardRouteStops, List<TransitRouteStop> backwardRouteStops, List<TransitStopFacility> extensionStopsForward, List<TransitStopFacility> extensionStopsBackward, TransitLine line, Set<Integer> prependIds, Set<Integer> appendIds,List<Id<Link>> linkIdsF,List<Id<Link>> linkIdsB){
	//Funktion um die neuen Stops in den bestehenden TransitSchedule einzufügen
	double offsetF = 0;
		for (TransitStopFacility facility : extensionStopsForward) {
			forwardRouteStops.add(factory.createTransitRouteStop(facility, OptionalTime.defined(offsetF), OptionalTime.defined(offsetF)));
			offsetF += 120; // 2 Minuten wegzeit zwischen halten als überschlagener Wert.
		}

	double offsetB = 120;
		for (TransitStopFacility facility : extensionStopsBackward) {
			backwardRouteStops.add(factory.createTransitRouteStop(facility, OptionalTime.defined(offsetB), OptionalTime.defined(offsetB)));
			offsetB += 120; // 2 Minuten wegzeit zwischen halten als überschlagener Wert.
		}

		// diese Schleife iteriert über die Anzahl der Routen, die die zu erweiternede Linie hat
		for (int i = 0; i < line.getRoutes().size(); i++) {
			Id<TransitRoute> routeId = Id.create(line.getId() +"_"+ i, TransitRoute.class);
			TransitRoute existRoute = line.getRoutes().get(routeId);
			List<TransitRouteStop> stops = new ArrayList<>(); //Transit stops List
			if (existRoute == null || !prependIds.contains(i) && !appendIds.contains(i) )continue;

			NetworkRoute newRoute = existRoute.getRoute(); //declare new networkroute

			if (appendIds.contains(i)) {
				TransitRouteStop lastStop = existRoute.getStops().getLast();
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
				newRoute = RouteUtils.createLinkNetworkRouteImpl(oldNetworkRoute.getStartLinkId(), newLinkIds, newLinkIds.getLast());
			}
			else if (prependIds.contains(i)) {
				List<TransitRouteStop> shiftedExistingStops = new ArrayList<>();
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
			line.removeRoute(existRoute);
			line.addRoute(newTransitRoute);

		}
}

private static void Export(Scenario scenario){
	File outputDir = new File("output");
		if (!outputDir.exists()) {outputDir.mkdirs();}
		NetworkUtils.writeNetwork(scenario.getNetwork(),"output/network.xml.gz");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("output/transitSchedule.xml.gz");
		Links2ESRIShape exporter =new Links2ESRIShape(scenario.getNetwork(),"output/m10_network.shp",	scenario.getConfig().global().getCoordinateSystem());
		exporter.write();
		//System.out.println("Finished Export");
}
}
