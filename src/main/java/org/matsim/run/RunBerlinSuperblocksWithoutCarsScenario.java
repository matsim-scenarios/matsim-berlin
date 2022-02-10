/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;

import static org.matsim.run.RunBerlinScenario.*;

public class RunBerlinSuperblocksWithoutCarsScenario {

	private static final org.apache.log4j.Logger log = Logger.getLogger(RunBerlinSuperblocksWithoutCarsScenario.class );

	public static void main(String[] args) {

		for (String arg : args) {
			log.info( arg );
		}

		if ( args.length==0 ) {
			throw new IllegalArgumentException("you need to specify your own config that contains the modified network!")  ;
		}

		Config config = prepareConfig( args ) ;
		Scenario scenario = prepareScenario( config ) ;

		//delete all car routes
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Leg leg : TripStructureUtils.getLegs(plan)) {
					if (leg.getMode().equals(TransportMode.car)) leg.setRoute(null);
				}
			}
		}

		Controler controler = prepareControler( scenario ) ;
		controler.run();
	}

}
