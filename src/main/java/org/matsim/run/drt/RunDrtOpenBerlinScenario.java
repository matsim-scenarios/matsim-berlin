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

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.IntermodalAwareRouterModeIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.run.RunBerlinScenario;


/**
 * This class starts a simulation run with DRT.
 * 
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- Initial plans are not modified.
 * 
 * @author ikaddoura
 */

public final class RunDrtOpenBerlinScenario {

	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenario.class);

	private static final String drtNetworkMode = TransportMode.drt;

	// TODO: also add the following to the avoev config group?
	private static final String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";


	public static void main(String[] args) throws CommandLine.ConfigurationException {
		Config config = prepareConfig( args ) ;
		Scenario scenario = prepareScenario( config ) ;
		Controler controler = prepareControler( scenario ) ;
		controler.run() ;
	}
	
	public static Controler prepareControler( Scenario scenario ) {

		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
		
		// drt + dvrp module
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents( DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				// use a main mode identifier which knows how to handle intermodal trips generated by the used sbb pt raptor router
				// the SwissRailRaptor already binds its IntermodalAwareRouterModeIdentifier, however drt obviuosly replaces it
				// with its own implementation
				// So we need our own main mode indentifier which replaces both :-(
				bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
			}
		});

		// Add drt-specific fare module
		controler.addOverridingModule(new DrtFareModule());
		// yyyy there is fareSModule (with S) in config. ?!?!  kai, jul'19

		return controler;
	}
	
	public static Scenario prepareScenario( Config config ) {

		Scenario scenario = RunBerlinScenario.prepareScenario( config );

		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		addDRTmode(scenario, drtNetworkMode);

		return scenario;
	}
	
	public static Config prepareConfig( String [] args ) {

		Config config = null ;

		if ( args.length != 0 ){
			config = RunBerlinScenario.prepareConfig( args ) ;
			AvoevConfigGroup avoevConfigGroup = ConfigUtils.addOrGetModule( config, AvoevConfigGroup.class ) ;
			
			// With the CommandLine.Builder removed from Avoev, we cannot pass the argument DrtServiceAreaShapeFileName any longer
			// However we cannot pass it as a normal command line argument (e.g. in args[1]) either, because RunBerlinScenario still uses CommandLine.Builder via ConfigUtils.applyCommandline.
			// ConfigUtils.applyCommandline prohibits positional arguments such as having DrtServiceAreaShapeFileName as unnamed args[1]
			// We cannot use the config file to pass that argument, because AvoevConfigGroup does not support that yet.
			// Hard code the DrtServiceAreaShapeFileName until some decision is made. gl jul'19
			avoevConfigGroup.setDrtServiceAreaShapeFileName(
					  "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp" );
			
		} else {
			config = RunBerlinScenario.prepareConfig( new String [] {"scenarios/berlin-v5.4-1pct/input/drt/berlin-drtA-v5.4-1pct-Berlkoenig.config.xml"} ) ;

			AvoevConfigGroup avoevConfigGroup = ConfigUtils.addOrGetModule( config, AvoevConfigGroup.class ) ;
			avoevConfigGroup.setDrtServiceAreaShapeFileName(
				  "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp" );
		}
		
		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class ) ;
		ConfigUtils.addOrGetModule( config, DrtConfigGroup.class ) ;
		ConfigUtils.addOrGetModule( config, DrtFaresConfigGroup.class ) ;
		
		ConfigUtils.addOrGetModule( config, VspExperimentalConfigGroup.class ).setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn); ;

		// add drt mode	
    	List<String> modes = new ArrayList<String>(Arrays.asList(config.subtourModeChoice().getModes()));
    	modes.add(TransportMode.drt);
    	config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    	
    	// required by drt module
    	config.qsim().setNumberOfThreads(1);
    	config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
		
		// add drt stage activity (per default only added in case of stop-based drt operation mode)
    	PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.drt + " interaction");
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(params));
		config.planCalcScore().addActivityParams(params);
		
		// add drt scoring parameters
		PlanCalcScoreConfigGroup.ModeParams drtModeParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.drt);
		drtModeParams.setConstant(0.);
		drtModeParams.setMarginalUtilityOfDistance(0.);
		drtModeParams.setMarginalUtilityOfTraveling(0.);
		drtModeParams.setMonetaryDistanceRate(0.);
		config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtModeParams));
    	    	
    	// set drt parameters
    	DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
    	drtCfg.getVehiclesFile();
    	drtCfg.setVehiclesFile(drtVehiclesFile);
    	drtCfg.setMaxTravelTimeAlpha(1.7);
    	drtCfg.setMaxTravelTimeBeta(120.0);
    	drtCfg.setStopDuration(60.);
    	drtCfg.setMaxWaitTime(300.);
    	drtCfg.setChangeStartLinkToLastLinkInSchedule(true);
    	drtCfg.setIdleVehiclesReturnToDepots(false);
    	drtCfg.setRequestRejection(false);
    	drtCfg.setPrintDetailedWarnings(false);
    	    	
    	// set drt fare
    	for (DrtFareConfigGroup drtFareCfg : DrtFaresConfigGroup.get(config).getDrtFareConfigGroups()) {
    		drtFareCfg.setBasefare(0.);
        	drtFareCfg.setDailySubscriptionFee(0.);
        	drtFareCfg.setDistanceFare_m(0.0015);
        	drtFareCfg.setMinFarePerTrip(4.0);
        	drtFareCfg.setTimeFare_h(0.);
    	}
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		// intermodal routing pt+drt(/walk)
		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

			configRaptor.setUseIntermodalAccessEgress(true);
			configRaptor.setIntermodalAccessEgressModeSelection(IntermodalAccessEgressModeSelection.RandomSelectOneModePerRoutingRequestAndDirection);
			{
				// walk
				IntermodalAccessEgressParameterSet paramSetWalk = new IntermodalAccessEgressParameterSet();
				// paramSetXxx.setMode( TransportMode.walk ); // this does not work because sbb raptor treats it in a special way
				paramSetWalk.setMode(TransportMode.non_network_walk);
				paramSetWalk.setRadius(100000);
				paramSetWalk.setInitialSearchRadius(3000);
				paramSetWalk.setSearchExtensionRadius(1000);
				configRaptor.addIntermodalAccessEgress(paramSetWalk);
				// (in principle, walk as alternative to drt will not work, since drt is always
				// faster. Need to give the ASC to the router! However, with
				// the reduced drt network we should be able to see differentiation.)
			}
			{
				// drt
				IntermodalAccessEgressParameterSet paramSetDrt = new IntermodalAccessEgressParameterSet();
				paramSetDrt.setMode(TransportMode.drt);
				paramSetDrt.setRadius(10000); // Berlkoenig service area is significantly smaller than a 10km diameter, access trips over 10km don't make sense
				paramSetDrt.setInitialSearchRadius(3000);
				paramSetDrt.setSearchExtensionRadius(1000);
				configRaptor.addIntermodalAccessEgress(paramSetDrt);
			}
		}
		// set up walk2 so we don't need walk in raptor:
		double margUtlTravPt = config.planCalcScore().getModes().get( TransportMode.pt ).getMarginalUtilityOfTraveling();
		config.plansCalcRoute().addModeRoutingParams( new ModeRoutingParams(  ).setMode( "walk2" ).setTeleportedModeSpeed(config.plansCalcRoute().getModeRoutingParams().get(TransportMode.walk).getTeleportedModeSpeed()) );
		config.planCalcScore().addModeParams( new ModeParams("walk2").setMarginalUtilityOfTraveling( margUtlTravPt ) ); // like transit_walk ?!
		
		// why is this necessary? PtAlongALine2Test works without it.gl 2019-07-11
		config.planCalcScore().addModeParams( new ModeParams(TransportMode.non_network_walk).setMarginalUtilityOfTraveling( margUtlTravPt ) );

		return config ;
	}
	
	private static void addDRTmode(Scenario scenario, String drtNetworkMode) {
		
		log.info("Adjusting network...");
		
		AvoevConfigGroup avoevConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), AvoevConfigGroup.class ) ;
		BerlinShpUtils shpUtils = new BerlinShpUtils( avoevConfigGroup.getDrtServiceAreaShapeFileName() );

		int counter = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				log.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(TransportMode.car)) {
				if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord())
						|| shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
					
					allowedModes.add(drtNetworkMode);

					link.setAllowedModes(allowedModes);
				}

			} else if (link.getAllowedModes().contains(TransportMode.pt)) {
				// skip pt links
			} else {
				throw new RuntimeException("Aborting...");
			}
		}
		
		// clean drt network
		Set<String> filterTransportModes = new HashSet<>();
		filterTransportModes.add(drtNetworkMode);
		Network subnetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(subnetwork, filterTransportModes);
		new NetworkCleaner().run(subnetwork);
		
		counter = 0;
		// remove drt from all links not included in the cleaned drt network
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				log.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(drtNetworkMode) && !subnetwork.getLinks().containsKey(link.getId())) {
				if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord())
						|| shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
					
					allowedModes.remove(drtNetworkMode);

					link.setAllowedModes(allowedModes);
				}
			}
		}
		
		// check
		Network subnetwork2 = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(subnetwork2, filterTransportModes);
		int nNodesBeforeCleaning = subnetwork2.getNodes().size();
		new NetworkCleaner().run(subnetwork2);
		
		if (subnetwork2.getNodes().size() != nNodesBeforeCleaning) {
			log.error("Cleaning drt network did not work properly.");
			throw new RuntimeException("Cleaning drt network did not work properly.");
		}
	}

}

