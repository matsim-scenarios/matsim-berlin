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

package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
* @author ikaddoura
*/

public class RunBerlinScenario {

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class);
	
	public static void main(String[] args) {
		String configFile ;
		if ( args.length==0 || args[0]=="" ) {
			configFile = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
		} else {
			configFile = args[0];
		}
		log.info("config file: " + configFile);
		run(ConfigUtils.loadConfig(configFile));
		// If modification of config is desired, should be done in run method, not here, to help
		// with regression testing. kai, jun'18
	}

	static void run(Config config) {
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---
		
		Controler controler = new Controler(scenario);
		
		// use the sbb pt raptor router
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
	    });
		
		// vsp defaults
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
			
		controler.run();
	
		log.info("Done.");
	}

}

