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

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
* @author ikaddoura
*/

public class RunBerlinScenario {

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class);

	private final String configFileName;
	private final String overridingConfigFileName;
	private Config config;
	private Scenario scenario;
	private Controler controler;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;
	
	public static void main(String[] args) {
		String configFileName ;
		String overridingConfigFileName = null;
		if ( args.length==0 || args[0].equals("")) {
			configFileName = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2-10pct.config.xml";
			overridingConfigFileName = "overridingConfig.xml";
		} else {
			configFileName = args[0];
			if ( args.length>1 ) overridingConfigFileName = args[1];
		}
		log.info( "config file: " + configFileName );
		new RunBerlinScenario( configFileName, overridingConfigFileName ).run() ;
	}
	
	public RunBerlinScenario( String configFileName, String overridingConfigFileName) {
		this.configFileName = configFileName;
		this.overridingConfigFileName = overridingConfigFileName;
	}

	public Controler prepareControler( AbstractModule... overridingModules ) {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = new Controler( scenario );
		
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
					+ "Should only be used for testing or car-focused studies with fixed modal split.  ");
		}
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
			}
		} );
		
		for ( AbstractModule overridingModule : overridingModules ) {
			controler.addOverridingModule( overridingModule );
		}
		
		hasPreparedControler = true ;
		return controler;
	}
	
	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}
		
		// so that config settings in code, which come after the settings from the initial config file, can
		// be overridden without having to change the jar file.  Normally empty.
		if (this.overridingConfigFileName==null || this.overridingConfigFileName=="null" || this.overridingConfigFileName=="") {
			// do not load overriding config
		} else {
			ConfigUtils.loadConfig( config, this.overridingConfigFileName );	
		}
		// note that the path for this is different when run from GUI (path of original config) vs.
		// when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
		
		scenario = ScenarioUtils.loadScenario( config );

		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... customModules) {
		OutputDirectoryLogging.catchLogEntries();
		
		config = ConfigUtils.loadConfig( configFileName, customModules ) ; // I need this to set the context
		
		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
//		config.plansCalcRoute().setRoutingRandomness( 3. );
		// FIXME yyyyyy for next version.  ihab/kai, aug'18
		
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
		
		// vsp defaults
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
		
		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			final ActivityParams params = new ActivityParams( "home_" + ii + ".0" ) ;
			params.setTypicalDuration( ii );
			config.planCalcScore().addActivityParams( params );
		}
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			final ActivityParams params = new ActivityParams( "work_" + ii + ".0" ) ;
			params.setTypicalDuration( ii );
			params.setOpeningTime(6. * 3600.);
			params.setClosingTime(20. * 3600.);
			config.planCalcScore().addActivityParams( params );
		}
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			final ActivityParams params = new ActivityParams( "leisure_" + ii + ".0" ) ;
			params.setTypicalDuration( ii );
			params.setOpeningTime(9. * 3600.);
			params.setClosingTime(27. * 3600.);
			config.planCalcScore().addActivityParams( params );
		}
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			final ActivityParams params = new ActivityParams( "shopping_" + ii + ".0" ) ;
			params.setTypicalDuration( ii );
			params.setOpeningTime(8. * 3600.);
			params.setClosingTime(20. * 3600.);
			config.planCalcScore().addActivityParams( params );
		}
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			final ActivityParams params = new ActivityParams( "other_" + ii + ".0" ) ;
			params.setTypicalDuration( ii );
			config.planCalcScore().addActivityParams( params );
		}
		{
			final ActivityParams params = new ActivityParams( "freight" ) ;
			params.setTypicalDuration( 12.*3600. );
			config.planCalcScore().addActivityParams( params );
		}
		
		hasPreparedConfig = true ;
		return config ;
	}
	
	 public void run() {
		if ( !hasPreparedControler ) {
			prepareControler() ;
		}
		controler.run();
		log.info("Done.");
	}
	
	final ScoreStats getScoreStats() {
		return controler.getScoreStats() ;
	}
	
	final Population getPopulation() {
		return controler.getScenario().getPopulation();
	}

}

