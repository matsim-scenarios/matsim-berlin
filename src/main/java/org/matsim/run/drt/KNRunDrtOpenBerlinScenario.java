/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.run.drt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;

/**
* @author ikaddoura
*/

public class KNRunDrtOpenBerlinScenario {

	public static void main(String[] args) {
		Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
		config.transit().setUsingTransitInMobsim(false);
		ControlerUtils.checkConfigConsistencyAndWriteToLog( config, "before reading scenario" );
		
		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
		
		for( Person person : scenario.getPopulation().getPersons().values() ){
			person.getPlans().removeIf( (plan) -> plan!=person.getSelectedPlan() ) ;
		}
		
		Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
		controler.run() ;
	}

}

