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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.drtSpeedUp.DrtSpeedUpModule;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.vsp.andreas.mzilske.pt.queuesim.GreedyUmlaufBuilderImpl;

import java.util.List;

/**
* @author knagel
*/

public class KNVisInfection{
	private static final Logger log = Logger.getLogger( KNVisInfection.class );
	private static final String MODIFIED_TRANSIT_ENGINE_NAME = "modifiedTransitEngine";

	public static void main(String[] args) {

//		final String base = "/Users/kainagel/mnt/mathe/ils3/leich/open-berlin/output/";
//		final String runID="berlin-drt-v5.5-1pct_drt-132";

//		final String base="/Users/kainagel/mnt/mathe/ils3/kaddoura/avoev-intermodal-routing/output/output-";
//		final String runID="i89e";

		final String base="/Users/kainagel/mnt/mathe/ils3/leich/open-berlin-intermodal-remove-buses/output/output-";
		final String runID="B115b";


//		Config config = RunDrtOpenBerlinScenario.prepareConfig( new String[] {"berlin-drt-v5.5-1pct_drt-114/berlin-drt-v5.5-1pct_drt-114.output_config.xml"} ) ;
//		Config config = RunDrtOpenBerlinScenario.prepareConfig( new String[] {"/Users/kainagel/mnt/mathe/ils3/leich/open-berlin/output/berlin-drt-v5" +
//															    ".5-1pct_drt-132/berlin-drt-v5.5-1pct_drt-132.output_config_reduced.xml"} ) ;
		// yyyyyy todo do the above in a more flexible way!
		Config config = RunDrtOpenBerlinScenario.prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation.acceptUnknownParamsBerlinConfig,
				new String[] {base + runID + "/" + runID + ".output_config_reduced.xml"} ) ;
		
		config.network().setInputFile( config.controler().getRunId() + ".output_network.xml.gz" );

		config.plans().setInputFile( config.controler().getRunId() + ".output_plans.xml.gz" );
//		config.plans().setInputFile( "/Users/kainagel/git/berlin-matsim/popSel.xml.gz" );

		config.transit().setTransitScheduleFile( config.controler().getRunId() + ".output_transitSchedule.xml.gz" );

		config.global().setNumberOfThreads( 6 );
		
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );

		final OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		otfVisConfigGroup.setDrawTransitFacilityIds( false );
		otfVisConfigGroup.setDrawTransitFacilities( false );
		otfVisConfigGroup.setLinkWidth( 10.f );
		otfVisConfigGroup.setColoringScheme( OTFVisConfigGroup.ColoringScheme.infection );
		
		DrtSpeedUpModule.adjustConfig(config);
		
		for ( final PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
			if ( params.getActivityType().endsWith( "interaction" ) ) {
				params.setScoringThisActivityAtAll( false ) ;
			}
		}

		config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves );
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );

		config.transit().setUsingTransitInMobsim( true );

//		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
//		List<String> components = qsimComponentsConfig.getActiveComponents();
//		components.remove( TransitEngineModule.TRANSIT_ENGINE_NAME );
//		components.add( MODIFIED_TRANSIT_ENGINE_NAME );
//		qsimComponentsConfig.setActiveComponents( components );

		// ---
		
//		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
		// yyyyyy why not use the default berlin scenario generation?  Is this a typo, or was there a reason?

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory( config ) ;
		ScenarioUtils.loadScenario( scenario );

//		for ( final Person person : scenario.getPopulation().getPersons().values() ) {
//			person.getPlans().removeIf( (plan) -> !plan.equals( person.getSelectedPlan() ) ) ;
//		}
//		PopulationUtils.writePopulation( scenario.getPopulation(), "popWOnlySelectedPlans.xml.gz" );
		
//		List<Person> toRemove = new ArrayList<>() ;
//		for ( final Person person : scenario.getPopulation().getPersons().values() ) {
//			boolean containsDrt = false ;
//			boolean containsPT = false ;
//			for ( final Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ) {
//				if ( leg.getMode().contains( "drt" ) ) {
//					containsDrt = true ;
//				}
//				if ( leg.getMode().contains( "pt" ) ) {
//					containsPT = true ;
//				}
//			}
//			if ( ! ( containsDrt  /*&& containsPT*/ ) ) {
//				toRemove.add( person );
//			}
//		}
//		log.warn( "population size before=" + scenario.getPopulation().getPersons().size() );
//		scenario.getPopulation().getPersons().values().removeAll( toRemove ) ;
//		log.warn( "population size after=" + scenario.getPopulation().getPersons().size() );
//
//		PopulationUtils.writePopulation( scenario.getPopulation(), "popWOnlyDrtPtPlans.xml.gz" );
		
		// ---
		
		Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addEventHandlerBinding().to( InfectionEventHandler.class );
			}
		} );

//		controler.addOverridingQSimModule( new AbstractQSimModule(){
//			@Override protected void configureQSim(){
//				bind( TransitQSimEngine.class ).asEagerSingleton();
//
//				this.addQSimComponentBinding( MODIFIED_TRANSIT_ENGINE_NAME ).to(  TransitQSimEngine.class );
//
//				if ( this.getConfig().transit().isUseTransit() && this.getConfig().transit().isUsingTransitInMobsim() ) {
//					bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
//				} else {
//					// Explicit bindings are required, so although it may not be used, we need provide something.
//					bind( TransitStopHandlerFactory.class ).to( SimpleTransitStopHandlerFactory.class );
//				}
//
//				bind( UmlaufBuilder.class ).to( GreedyUmlaufBuilderImpl.class );
//
//			}
//		} );
		
		controler.addOverridingModule( new OTFVisLiveModule() ) ;
//		controler.addOverridingModule(new DrtSpeedUpModule());
		
		// ---
		
		controler.run() ;
	}

}

