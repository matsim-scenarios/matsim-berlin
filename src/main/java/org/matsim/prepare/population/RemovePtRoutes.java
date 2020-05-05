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

package org.matsim.prepare.population;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;

/**
 * 
 * Not ready for intermodal trips 
 * 
 * @author gleich
 *
 */
public class RemovePtRoutes {
	
	private final static Logger log = Logger.getLogger(RemovePtRoutes.class);

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (Person person: scenario.getPopulation().getPersons().values()) {
			for (Plan plan: person.getPlans()) {
				// new TransitActsRemover().run(plan); // converts pt trips to a single non_network_walk leg :-(
				
				// TripsToLegsAlgorithm affects not only pt trips, but all trips, so not useable yet here,
				// new TripsToLegsAlgorithm(new StageActivityTypesImpl(), new MainModeIdentifierImpl()).run(plan);
				
				// use adapted copy of TripsToLegsAlgorithm.run() to restrict to pt trips
				run(plan);
				
				// check if there are references to the old now replaced pseudo pt network
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getRoute() != null) {
							if (leg.getRoute().getStartLinkId().toString().startsWith("pt_") || leg.getRoute().getEndLinkId().toString().startsWith("pt_")) {
								log.error("agent " + person.getId()
										+ " still has route starting or ending at a link of the pt network after removing pt routes: "
										+ leg.getRoute().getStartLinkId().toString() + " / "
										+ leg.getRoute().getEndLinkId().toString());
								log.error("full plan: " + plan);
							}
							Gbl.assertIf(!leg.getRoute().getStartLinkId().toString().startsWith("pt_"));
							Gbl.assertIf(!leg.getRoute().getEndLinkId().toString().startsWith("pt_"));
							if (leg.getRoute() instanceof NetworkRoute) {
								NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
								for (Id<Link> linkId: networkRoute.getLinkIds()) {
									Gbl.assertIf(!linkId.toString().startsWith("pt_"));
								}
							}
						}
					} else if (pe instanceof Activity){
						Activity act =  (Activity) pe;
						if (act.getLinkId() != null) {
							Gbl.assertIf(!act.getLinkId().toString().startsWith("pt_"));
						}
					}
				}
			}
		}
		
		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation());
		popWriter.write("berlin-v5.5-10pct.plans_wo_PtRoutes.xml.gz");
	}

	private static void run(final Plan plan) {
		final List<PlanElement> planElements = plan.getPlanElements();
		final List<Trip> trips = TripStructureUtils.getTrips( plan );

		for ( Trip trip : trips ) {
			final List<PlanElement> fullTrip =
				planElements.subList(
						planElements.indexOf( trip.getOriginActivity() ) + 1,
						planElements.indexOf( trip.getDestinationActivity() ));
			final String mode = (new OpenBerlinIntermodalPtDrtRouterModeIdentifier()).identifyMainMode( fullTrip );
			if (mode.equals(TransportMode.pt)) {
				fullTrip.clear();
				fullTrip.add( PopulationUtils.createLeg(mode) );
				if ( fullTrip.size() != 1 ) throw new RuntimeException( fullTrip.toString() );
			}
		}
	}
}
