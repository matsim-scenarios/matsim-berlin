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
import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

/**
* @author ikaddoura
*/

public class RunBerlinScenario {

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class);
	private final Controler controler;
	
	public static void main(String[] args) {
		String configFile ;
		if ( args.length==0 || args[0].equals("")) {
			configFile = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
//			configFile = "scenarios/berlin-v5.0-0.1pct-2018-06-18/input/berlin-5.0_config_full.xml";
		} else {
			configFile = args[0];
		}
		log.info("config file: " + configFile);
		new RunBerlinScenario( ConfigUtils.loadConfig( configFile ) ).run() ;
		// If modification of config is desired, should be done in run method, not here, to help
		// with regression testing. kai, jun'18
	}
	
	RunBerlinScenario( Config config ) {
		config.transit().setUsingTransitInMobsim( false );
		
		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
		// vsp defaults
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
		
		// ---
		
		// so that config settings in code, which come after the settings from the initial config file, can
		// be overridden without having to change the jar file.  Normally empty.
		ConfigUtils.loadConfig( config, "overridingConfig.xml" );
		
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config );
		
		// ---
		
		this.controler = new Controler( scenario );
		
		// use the sbb pt raptor router
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				install( new SwissRailRaptorModule() );
			}
		} );
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
			}
		} );
		
	}
	
	void run() {
		controler.run();
		log.info("Done.");
	}
	
	 final ScoreStats getScoreStats() {
		return controler.getScoreStats() ;
	}

}

