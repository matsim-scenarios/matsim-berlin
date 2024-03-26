/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.legacy.run.ptdisturbances;

import static org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType.AStarLandmarks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitScheduleChangedEvent;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.withinday.utils.EditTrips;
import org.matsim.withinday.utils.ReplanningException;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
* @author smueller, ikaddoura
*/

public final class RunPtDisturbancesBerlin {

	private static final Logger log = LogManager.getLogger(RunPtDisturbancesBerlin.class );

	public static void main(String[] args) {

		for (String arg : args) {
			log.info( arg );
		}

		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}

		Config config = prepareConfig( args ) ;

		Scenario scenario = prepareScenario( config ) ;

//		NetworkChangeEvents are added so there are no U9 departures between 0730 and 0830. This ensures that no agent can use U9 in the disturbed period
		addNetworkChangeEvents( scenario );


		Controler controler = prepareControler( scenario ) ;

		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule(config,
				QSimComponentsConfigGroup.class);

		// the following requests that a component registered under the name "...NAME"
		// will be used:
		List<String> cmps = qsimComponentsConfig.getActiveComponents();
		cmps.add(DisturbanceAndReplanningEngine.NAME);
		qsimComponentsConfig.setActiveComponents(cmps);

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				// the following registers the component under the name "...NAME":
				this.addQSimComponentBinding(DisturbanceAndReplanningEngine.NAME)
						.to(DisturbanceAndReplanningEngine.class);
//				bind(TransitStopHandlerFactory.class).to(SimpleTransitStopHandlerFactory.class);
			}
		});

		controler.run() ;

	}

	private static void addNetworkChangeEvents(Scenario scenario) {

		{
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();

		Link oldFirstLink = scenario.getNetwork().getLinks().get(Id.createLinkId("pt_43431"));
		oldFirstLink.setFreespeed(50.);
		Node toNodeLink1 = oldFirstLink.getFromNode();
		Node fromNodeLink1 = networkFactory.createNode(Id.createNodeId("dummyNodeRathausSteglitz1"), CoordUtils.createCoord(toNodeLink1.getCoord().getX() + 1000, toNodeLink1.getCoord().getY()));
		Node fromNodeLink0 = networkFactory.createNode(Id.createNodeId("dummyNodeRathausSteglitz0"), CoordUtils.createCoord(toNodeLink1.getCoord().getX() + 1010, toNodeLink1.getCoord().getY()));
		Link link1 = networkFactory.createLink(Id.createLinkId("dummyLinkRathausSteglitz1"), fromNodeLink1, toNodeLink1);
		link1.setAllowedModes(oldFirstLink.getAllowedModes());
		link1.setFreespeed(999.);
		link1.setCapacity(oldFirstLink.getCapacity());

		Link link0 = networkFactory.createLink(Id.createLinkId("dummyLinkRathausSteglitz0"), fromNodeLink0, fromNodeLink1);
		link0.setAllowedModes(oldFirstLink.getAllowedModes());
		link0.setFreespeed(999.);
		link0.setCapacity(oldFirstLink.getCapacity());

		scenario.getNetwork().addNode(fromNodeLink1);
		scenario.getNetwork().addNode(fromNodeLink0);
		scenario.getNetwork().addLink(link1);
		scenario.getNetwork().addLink(link0);

		TransitLine disturbedLine = scenario.getTransitSchedule().getTransitLines().get(Id.create("U9---17526_400", TransitLine.class));

		for (TransitRoute transitRoute : disturbedLine.getRoutes().values()) {
			if (transitRoute.getRoute().getStartLinkId().equals(oldFirstLink.getId())) {

				List<Id<Link>> newRouteLinkIds = new ArrayList<>();
				List<Id<Link>> oldRouteLinkIds = new ArrayList<>();
				newRouteLinkIds.add(link0.getId());
				newRouteLinkIds.add(link1.getId());
				oldRouteLinkIds = transitRoute.getRoute().getLinkIds();
				newRouteLinkIds.add(transitRoute.getRoute().getStartLinkId());
				newRouteLinkIds.addAll(oldRouteLinkIds);
				newRouteLinkIds.add(transitRoute.getRoute().getEndLinkId());

				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, scenario.getNetwork());
				transitRoute.setRoute(networkRoute);

				transitRoute.setRoute(networkRoute);
			}
		}

		NetworkChangeEvent networkChangeEvent1 = new NetworkChangeEvent(7.5*3600);
//		Link link = scenario.getNetwork().getLinks().get(Id.createLinkId("pt_43431"));
		networkChangeEvent1.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, link1.getLength()/3600));
		networkChangeEvent1.addLink(link1);
		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent1);
//		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8.5*3600);
//		networkChangeEvent2.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 50. / 3.6));
//		networkChangeEvent2.addLink(link1);
//		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent2);
		}

		{
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();

		Link oldFirstLink = scenario.getNetwork().getLinks().get(Id.createLinkId("pt_43450"));
		oldFirstLink.setFreespeed(50.);
		Node toNodeLink1 = oldFirstLink.getFromNode();
		Node fromNodeLink1 = networkFactory.createNode(Id.createNodeId("dummyNodeOslo1"), CoordUtils.createCoord(toNodeLink1.getCoord().getX() + 1000, toNodeLink1.getCoord().getY()));
		Node fromNodeLink0 = networkFactory.createNode(Id.createNodeId("dummyNodeOslo0"), CoordUtils.createCoord(toNodeLink1.getCoord().getX() + 1010, toNodeLink1.getCoord().getY()));
		Link link1 = networkFactory.createLink(Id.createLinkId("dummyLinkOslo1"), fromNodeLink1, toNodeLink1);
		link1.setAllowedModes(oldFirstLink.getAllowedModes());
		link1.setFreespeed(999.);
		link1.setCapacity(oldFirstLink.getCapacity());

		Link link0 = networkFactory.createLink(Id.createLinkId("dummyLinkOslo0"), fromNodeLink0, fromNodeLink1);
		link0.setAllowedModes(oldFirstLink.getAllowedModes());
		link0.setFreespeed(999.);
		link0.setCapacity(oldFirstLink.getCapacity());

		scenario.getNetwork().addNode(fromNodeLink1);
		scenario.getNetwork().addNode(fromNodeLink0);
		scenario.getNetwork().addLink(link1);
		scenario.getNetwork().addLink(link0);

		TransitLine disturbedLine = scenario.getTransitSchedule().getTransitLines().get(Id.create("U9---17526_400", TransitLine.class));

		for (TransitRoute transitRoute : disturbedLine.getRoutes().values()) {
			if (transitRoute.getRoute().getStartLinkId().equals(oldFirstLink.getId())) {

				List<Id<Link>> newRouteLinkIds = new ArrayList<>();
				List<Id<Link>> oldRouteLinkIds = new ArrayList<>();
				newRouteLinkIds.add(link0.getId());
				newRouteLinkIds.add(link1.getId());
				oldRouteLinkIds = transitRoute.getRoute().getLinkIds();
				newRouteLinkIds.add(transitRoute.getRoute().getStartLinkId());
				newRouteLinkIds.addAll(oldRouteLinkIds);
				newRouteLinkIds.add(transitRoute.getRoute().getEndLinkId());

				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, scenario.getNetwork());
				transitRoute.setRoute(networkRoute);

				transitRoute.setRoute(networkRoute);
			}
		}

		NetworkChangeEvent networkChangeEvent1 = new NetworkChangeEvent(7.5*3600);
//		Link link = scenario.getNetwork().getLinks().get(Id.createLinkId("pt_43431"));
		networkChangeEvent1.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, link1.getLength()/3600));
		networkChangeEvent1.addLink(link1);
		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent1);
//		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8.5*3600);
//		networkChangeEvent2.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 50. / 3.6));
//		networkChangeEvent2.addLink(link1);
//		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent2);
		}



	}

	public static Controler prepareControler( Scenario scenario ) {
		// note that for something like signals, and presumably drt, one needs the controler object

		Gbl.assertNotNull(scenario);

		final Controler controler = new Controler( scenario );

		if (controler.getConfig().transit().isUsingTransitInMobsim()) {
			// use the sbb pt raptor router
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		} else {
			log.warn("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with a fixed modal split.  ");
		}

		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
			}
		} );

		return controler;
	}

	public static Scenario prepareScenario( Config config ) {
		Gbl.assertNotNull( config );

		// note that the path for this is different when run from GUI (path of original config) vs.
		// when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
		// yy Does this comment still apply?  kai, jul'19

		final Scenario scenario = ScenarioUtils.loadScenario( config );

		return scenario;
	}

	public static Config prepareConfig( String [] args, ConfigGroup... customModules ) {
		OutputDirectoryLogging.catchLogEntries();

		String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );

		final Config config = ConfigUtils.loadConfig( args[ 0 ], customModules ); // I need this to set the context

		config.controller().setRoutingAlgorithmType( AStarLandmarks );
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);

		config.transit().setBoardingAcceptance( TransitConfigGroup.BoardingAcceptance.checkStopOnly );

		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );

		config.routing().setRoutingRandomness( 3. );
		config.routing().removeModeRoutingParams(TransportMode.ride);
		config.routing().removeModeRoutingParams(TransportMode.pt);
		config.routing().removeModeRoutingParams(TransportMode.bike);
		config.routing().removeModeRoutingParams("undefined");

		config.network().setTimeVariantNetwork(true);

		// TransportMode.non_network_walk has no longer a default,
		// in the long run: copy from walk; for now: use the parameter set given in the config (for backward compatibility)
//		ModeRoutingParams walkRoutingParams = config.routing().getOrCreateModeRoutingParams(TransportMode.walk);
//		ModeRoutingParams non_network_walk_routingParams = new ModeRoutingParams(TransportMode.non_network_walk);
//		non_network_walk_routingParams.setBeelineDistanceFactor(walkRoutingParams.getBeelineDistanceFactor());
//		non_network_walk_routingParams.setTeleportedModeSpeed(walkRoutingParams.getTeleportedModeSpeed());
//		config.routing().addModeRoutingParams(non_network_walk_routingParams);

		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );

		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );

		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			config.scoring().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
			config.scoring().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
			config.scoring().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
			config.scoring().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
			config.scoring().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
		}
		config.scoring().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );

		ConfigUtils.applyCommandline( config, typedArgs ) ;

		return config ;
	}

	private static class DisturbanceAndReplanningEngine implements MobsimEngine {
		public static final String NAME = "disturbanceAndReplanningEngine";

		@Inject
		private Scenario scenario;
		@Inject
		private EventsManager events;
		@Inject
		private Provider<TripRouter> tripRouterProvider;
		private InternalInterface internalInterface;

		@Override
		public void doSimStep(double now) {

			// replan after an affected bus has already departed -> pax on the bus are
			// replanned to get off earlier
			double replanTime = 7 * 3600 + 40 * 60;

			if ((int) now == replanTime - 1.) { // yyyyyy this needs to come one sec earlier. :-(
				// clear transit schedule from transit router provider:
				events.processEvent(new TransitScheduleChangedEvent(now));
			}

			if ((int) now == replanTime) {

				// modify transit schedule:

				final Id<TransitLine> disturbedLineId = Id.create("U9---17526_400", TransitLine.class);
				TransitLine disturbedLine = scenario.getTransitSchedule().getTransitLines().get(disturbedLineId);
				Gbl.assertNotNull(disturbedLine);

//				TransitRoute disturbedRoute = disturbedLine.getRoutes().get(Id.create("U9---17526_400_0", TransitRoute.class));
//				Gbl.assertNotNull(disturbedRoute);
//
//				log.warn("before removal: nDepartures=" + disturbedRoute.getDepartures().size());
//
//				List<Departure> toRemove = new ArrayList<>();
//				for (Departure departure : disturbedRoute.getDepartures().values()) {
//					if (departure.getDepartureTime() >= 7.5 * 3600. && departure.getDepartureTime() < 8.5 * 3600.) {
//						toRemove.add(departure);
//					}
//				}
//				for (Departure departure : toRemove) {
//					disturbedRoute.removeDeparture(departure);
//				}
//
//				log.warn("after removal: nDepartures=" + disturbedRoute.getDepartures().size());


//
				for (TransitRoute route : disturbedLine.getRoutes().values()) {
					log.warn("before removal: nDepartures= " + route.getDepartures().size() + "--- Route: " + route.getId());
				}

				List<Departure> toRemove = new ArrayList<>();
				for (TransitRoute route : disturbedLine.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						if (departure.getDepartureTime() >= 7.5 * 3600 && departure.getDepartureTime() < 8.5 * 3600.) {
							toRemove.add(departure);
						}
					}
				}

				for (Departure departure : toRemove) {
					for (TransitRoute route : disturbedLine.getRoutes().values()) {
						if (route.getDepartures().containsValue(departure)) {
							route.removeDeparture(departure);
						}
					}
				}

				for (TransitRoute route : disturbedLine.getRoutes().values()) {
					log.warn("after removal: nDepartures= " + route.getDepartures().size() + "--- Route: " + route.getId());
				}
//

				// ---

				replanPtPassengers(now, disturbedLineId, tripRouterProvider, scenario, internalInterface);

			}
		}

		@Override
		public void onPrepareSim() {
		}

		@Override
		public void afterSim() {
		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {
			this.internalInterface = internalInterface;
		}

	}

	static void replanPtPassengers(double now, final Id<TransitLine> disturbedLineId, Provider<TripRouter> tripRouterProvider, Scenario scenario, InternalInterface internalInterface) {

		final QSim qsim = internalInterface.getMobsim() ;

		// force new transit router:
		final TripRouter tripRouter = tripRouterProvider.get();
		EditTrips editTrips = new EditTrips( tripRouter, scenario, internalInterface, TimeInterpretation.create(scenario.getConfig()) );

		int currentTripsReplanned = 0;
		int futureTripsReplanned = 0;

		// find the affected agents and replan affected trips:

		for( MobsimAgent agent : (qsim).getAgents().values() ){
			if( agent instanceof TransitDriverAgentImpl ){
				/* This is a pt vehicle driver. TransitDriverAgentImpl does not support getModifiablePlan(...). So we should skip him.
				 * This probably means that the driver continues driving the pt vehicle according to the old schedule.
				 * However, this cannot be resolved by the editTrips.replanCurrentTrip() method anyway.
				 */
				continue;
			}

			Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );

			int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );

			TripStructureUtils.Trip currentTrip;

			try{
				currentTrip = editTrips.findCurrentTrip( agent );
			} catch( ReplanningException e ){
				// The agent might not be on a trip at the moment (but at a "real" activity).
				currentTrip = null;
			}

			Activity nextRealActivity = null; // would be nicer to use TripStructureUtils to find trips, but how can we get back to the original plan to modify it?

			for( int ii = currentPlanElementIndex ; ii < plan.getPlanElements().size() ; ii++ ){
				PlanElement pe = plan.getPlanElements().get( ii );
				// Replan each trip at maximum once, otherwise bad things might happen.
				// So we either have to keep track which Trip has already been re-planned
				// or move on manually to the next real activity after we re-planned.
				// Trips seem hard to identify, so try the latter approach.
				// Replanning the same trip twice could happen e.g. if first replanCurrentTrip is called and keeps or re-inserts
				// a leg with the disturbed line. So on a later plan element (higher ii) of the same trip replanCurrentTrip or
				// replanFutureTrip might be called. - gl, jul '19
				if (nextRealActivity != null) {
					// we are trying to move on to the next trip in order not to replan twice the same trip
					if( pe instanceof Activity && nextRealActivity.equals((Activity) pe)) {
							nextRealActivity = null;
						}
					// continue to next pe if we still are on the trip we just replanned.
					continue;
				} else if( pe instanceof Leg ){
					Leg leg = (Leg) pe;
					if( leg.getMode().equals( TransportMode.pt ) ){
						DefaultTransitPassengerRoute transitRoute = (DefaultTransitPassengerRoute) leg.getRoute();
						if( transitRoute.getLineId().equals( disturbedLineId ) ){
							TripStructureUtils.Trip affectedTrip = editTrips.findTripAtPlanElement( agent, pe );
							if( currentTrip != null && currentTrip.getTripElements().contains( pe ) ){
								// current trip is disturbed
								log.warn(agent.getId()+";current");
								editTrips.replanCurrentTrip( agent, now, TransportMode.pt );
								currentTripsReplanned++;
//								break;
							} else {
								// future trip is disturbed
								log.warn(agent.getId()+";future");
								editTrips.replanFutureTrip( affectedTrip, plan, TransportMode.pt );
								futureTripsReplanned++;
							}
							nextRealActivity = affectedTrip.getDestinationActivity();
						}
					}
				}
			}


			{
				// agents that abort their leg before boarding a vehicle need to be actively advanced:
				PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement( agent );
				if ( pe instanceof Activity ) {
					if ( StageActivityTypeIdentifier.isStageActivity( ((Activity) pe).getType() ) ){
						internalInterface.arrangeNextAgentState( agent );
						internalInterface.unregisterAdditionalAgentOnLink( agent.getId(), agent.getCurrentLinkId() ) ;
					}
				}
				// yyyyyy would be much better to hide this inside EditXxx. kai, jun'19
			}

		}

		log.warn("Replanned " + currentTripsReplanned + " current trips");
		log.warn("Replanned " + futureTripsReplanned + " future trips");
	}

}

