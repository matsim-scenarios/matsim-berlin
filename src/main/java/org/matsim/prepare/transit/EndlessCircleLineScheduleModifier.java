package org.matsim.prepare.transit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

public class EndlessCircleLineScheduleModifier {
	private final static Logger log = LogManager.getLogger(EndlessCircleLineScheduleModifier.class);
	private TransitSchedule schedule;
	private TransitScheduleFactory factory;

	public static void main(String[] args) {
		EndlessCircleLineScheduleModifier runner = new EndlessCircleLineScheduleModifier();
		runner.run();
	}

	private void run() {
		String inputScheduleFile = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-transitSchedule.xml.gz";
		String transitLineS41Id = "S41---4715";
		String transitLineS42Id = "S42---5444";
		// check with every schedule update transit route ids (still full loop?), headways, first departure time
		String singleLoopingToCopyS41TransitRouteId = "S41---4715_0";// in berlin v6.3 schedule 200 departures, remaining S41 routes less than 8 deps each
		String multipleLoopingsS41TransitRouteId1 = "S41---4715_loop1";
		String multipleLoopingsS41TransitRouteId2 = "S41---4715_loop2";

		int vehIdOffset = 20; // TODO: hacky, get rid of this and cleanly add / remove transit vehicles

		String singleLoopingToCopyS42TransitRouteId = "S42---5444_0";// in berlin v6.3 schedule 180 departures, remaining S42 routes less than 15 deps each
		String multipleLoopingsS42TransitRouteId1 = "S42---5444_loop1";
		String multipleLoopingsS42TransitRouteId2 = "S42---5444_loop2";

		//departureTransitStopS41FacilityId = "469985" Beusselstr., link pt_46478 from pt_469985 to pt_469985 -> loop link, do not use. Unfortunately, other partial looping routes keep using it. Agents will either wait at this stop or the other and cannot switch freely.
		//loopStartTransitStopS41FacilityId = "469985.1" Beusselstr., link pt_46505 from pt_16724 to pt_469985 -> connecting link, use this

		double loopingTravelTime = 60 * 60.0;

		// TODO: necessary? private double bufferTimeToCompensateLongerFirstLink = 2 * 60.0;  // if starting from a long

		// first S41---4715_0 departure at 4:08:24 in Beusselstr., then 10 min headway until 5:08:24, then 5 min until 20:18:24, then 10 min headway until 23:58:24.
		// for simplification and reducing number of concurrent transit routes and associated issues oif agents prefering one tranist route over others of the same transit line
		// have one transit route reflecting a 5 min headway until 20:03:24, i.e. 4:08 dep starts last loop 19:08.
		int numberLoopingsTransitRoute1 = 19-4;
		double headwayTransitRoute1 = 5 * 60.0;
		double firstDepartureTimeS41TransitRoute1 = 4 * 3600.0 + 8 * 60.0 + 24;
		// then another transit route from 20:08 every 10 min until 23:58, i.e. 20:08 starts last loop at 23:08
		int numberLoopingsTransitRoute2 = 23-20;
		double headwayTransitRoute2 = 10 * 60.0;
		double firstDepartureTimeS41TransitRoute2 = 20 * 3600.0 + 8 * 60.0 + 24;

		// S42 is more complex. For simplificatrion re-use many S41 values for S42 despite differences
		// to be precise S42---5444_0 operates from 3:58:18 to,5:28:18 every 10 min, then every 5 min until 20:18:18, then every 10 min until 21:08:18. Then 5444_1 every 10 min from 21:18.18 until 22:08:18 (65 min loop time!) and 5444_5 from 22:23:18 every 10 min until 24:33:18
		double firstDepartureTimeS42TransitRoute1 = 3 * 3600.0 + 58 * 60.0 + 18;
		double firstDepartureTimeS42TransitRoute2 = 19 * 3600.0 + 58 * 60.0 + 18;

		String outputScheduleFile = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-transitSchedule_endlessCircleLine.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
		scheduleReader.readFile(inputScheduleFile);
		schedule = scenario.getTransitSchedule();
		factory = schedule.getFactory();

		// S41
		createLoopingTransitRoute(Id.create(transitLineS41Id, TransitLine.class),
			Id.create(singleLoopingToCopyS41TransitRouteId, TransitRoute.class),
			Id.create(multipleLoopingsS41TransitRouteId1, TransitRoute.class),
			loopingTravelTime, numberLoopingsTransitRoute1, headwayTransitRoute1,
			firstDepartureTimeS41TransitRoute1, 0);

		createLoopingTransitRoute(Id.create(transitLineS41Id, TransitLine.class),
			Id.create(singleLoopingToCopyS41TransitRouteId, TransitRoute.class),
			Id.create(multipleLoopingsS41TransitRouteId2, TransitRoute.class),
			loopingTravelTime, numberLoopingsTransitRoute2, headwayTransitRoute2,
			firstDepartureTimeS41TransitRoute2, vehIdOffset);

		// delete old non-looping transit route
		schedule.getTransitLines().get(Id.create(transitLineS41Id, TransitLine.class)).removeRoute(schedule.getTransitLines().get(Id.create(transitLineS41Id, TransitLine.class)).getRoutes().get(Id.create(singleLoopingToCopyS41TransitRouteId, TransitRoute.class)));

		// S42
		createLoopingTransitRoute(Id.create(transitLineS42Id, TransitLine.class),
			Id.create(singleLoopingToCopyS42TransitRouteId, TransitRoute.class),
			Id.create(multipleLoopingsS42TransitRouteId1, TransitRoute.class),
			loopingTravelTime, numberLoopingsTransitRoute1, headwayTransitRoute1,
			firstDepartureTimeS42TransitRoute1, 0);

		createLoopingTransitRoute(Id.create(transitLineS42Id, TransitLine.class),
			Id.create(singleLoopingToCopyS42TransitRouteId, TransitRoute.class),
			Id.create(multipleLoopingsS42TransitRouteId2, TransitRoute.class),
			loopingTravelTime, numberLoopingsTransitRoute2, headwayTransitRoute2,
			firstDepartureTimeS42TransitRoute2, vehIdOffset);

		// delete old non-looping transit route
		schedule.getTransitLines().get(Id.create(transitLineS42Id, TransitLine.class)).removeRoute(schedule.getTransitLines().get(Id.create(transitLineS42Id, TransitLine.class)).getRoutes().get(Id.create(singleLoopingToCopyS42TransitRouteId, TransitRoute.class)));


		TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(schedule);
		transitScheduleWriter.writeFile(outputScheduleFile);
	}

	private void createLoopingTransitRoute(Id<TransitLine> transitLineId, Id<TransitRoute> singleLoopingToCopyTransitRouteId,
										   Id<TransitRoute> loopingTransitRouteId,
										   double loopingTravelTime, int numberLoopings,
										   double headway, double firstDepartureTime, int vehIdOffset) {

		TransitLine lineToModify = schedule.getTransitLines().get(transitLineId);
		TransitRoute routeToCopy = lineToModify.getRoutes().get(singleLoopingToCopyTransitRouteId);

		List<Id<Link>> loopingNetworkRouteLinks = new ArrayList<>();
		List<TransitRouteStop> transitRouteStops = new ArrayList<>();
		// add first stop manually
		TransitRouteStop firstRouteStop = factory.createTransitRouteStop(
			routeToCopy.getStops().getLast().getStopFacility(),
			routeToCopy.getStops().getFirst().getArrivalOffset(),
			routeToCopy.getStops().getFirst().getDepartureOffset());
		firstRouteStop.setAwaitDepartureTime(true);
		transitRouteStops.add(firstRouteStop);

		loopingNetworkRouteLinks.add(firstRouteStop.getStopFacility().getLinkId());

		for (int loopingsDone = 0; loopingsDone < numberLoopings; loopingsDone++) {
			loopingNetworkRouteLinks.addAll(routeToCopy.getRoute().getLinkIds());
			loopingNetworkRouteLinks.add(routeToCopy.getRoute().getEndLinkId());

			// skip first and last stop and add merged stop instead to avoid stopping twice at loopStartTransitStopId
			for (TransitRouteStop stop : routeToCopy.getStops().subList(1, routeToCopy.getStops().size() - 1)) {
				TransitRouteStop transitRouteStop = factory.createTransitRouteStop(stop.getStopFacility(),
					stop.getArrivalOffset().seconds() + loopingsDone * loopingTravelTime,
					stop.getDepartureOffset().seconds() + loopingsDone * loopingTravelTime);
				transitRouteStop.setAwaitDepartureTime(true);
				transitRouteStops.add(transitRouteStop);
			}
			// add last stop of this looping which is first stop of next looping
			TransitRouteStop lastRouteStop = factory.createTransitRouteStop(
				routeToCopy.getStops().getLast().getStopFacility(),
                    routeToCopy.getStops().getLast().getArrivalOffset().seconds() + loopingsDone * loopingTravelTime,
				routeToCopy.getStops().getFirst().getDepartureOffset().seconds() + (loopingsDone + 1) * loopingTravelTime);
			lastRouteStop.setAwaitDepartureTime(true);
			transitRouteStops.add(lastRouteStop);
		}
		// at least for S41 and S42 last link in network route ends at same node as first link -> continuous
		NetworkRoute networkRoute = RouteUtils.createNetworkRoute(loopingNetworkRouteLinks);
		TransitRoute loopingRoute = factory.createTransitRoute(loopingTransitRouteId, networkRoute, transitRouteStops, "multiple loopings in one route");
		loopingRoute.setTransportMode(routeToCopy.getTransportMode());

		int departureIdCounter = 0;
		for (double departureTime = firstDepartureTime; departureTime < firstDepartureTime + loopingTravelTime; departureTime = departureTime + headway) {
			Id<Departure> departureId = Id.create(loopingTransitRouteId + "_" + departureIdCounter, Departure.class);
			Departure departure = factory.createDeparture(departureId, departureTime);
			// TODO: This makes assumptions on existing transit vehicles that are true for S41, S42. It would be cleaner be to create new vehicles and delete unused old ones and modify transit vehicles file.
			departure.setVehicleId(Id.createVehicleId("pt_" + singleLoopingToCopyTransitRouteId + "_" + (departureIdCounter + vehIdOffset)));
			loopingRoute.addDeparture(departure);
			departureIdCounter++;
		}

		lineToModify.addRoute(loopingRoute);
	}
}
