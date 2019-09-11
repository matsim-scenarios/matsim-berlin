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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitActsRemover;

/**
 * 
 * Not ready for intermodal trips 
 * 
 * @author gleich
 *
 */
public class RemovePtRoutes {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("GK4");
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");
		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/input/berlin-v5.4-1pct.plans.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (Person person: scenario.getPopulation().getPersons().values()) {
			for (Plan plan: person.getPlans()) {
				new TransitActsRemover().run(plan);			
				
				// TODO: Better TripsToLegsAlgorithm? Affects not only pt trips, but all trips, so not useable yet here,
				// new TripsToLegsAlgorithm(new StageActivityTypesImpl(), new MainModeIdentifierImpl()).run(plan);
				
				// check if there are references to the old now replaced pseudo pt network
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getRoute() != null) {
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
		popWriter.write("berlin-v5.4-1pct.plans_wo_PtRoutes.xml.gz");
	}

}
