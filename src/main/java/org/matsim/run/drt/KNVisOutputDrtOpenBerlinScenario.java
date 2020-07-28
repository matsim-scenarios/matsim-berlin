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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.drtSpeedUp.MultiModeDrtSpeedUpModule;
import org.matsim.run.BerlinExperimentalConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.ArrayList;
import java.util.List;

/**
* @author knagel
*/

public class KNVisOutputDrtOpenBerlinScenario {
	private static final Logger log = Logger.getLogger( KNVisOutputDrtOpenBerlinScenario.class);

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

		MultiModeDrtSpeedUpModule.addTeleportedDrtMode(config);
		
		for ( final PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
			if ( params.getActivityType().endsWith( "interaction" ) ) {
				params.setScoringThisActivityAtAll( false ) ;
			}
		}

		config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves );
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );

		config.transit().setUsingTransitInMobsim( true );

		// ---
		
//		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory( config ) ;
		ScenarioUtils.loadScenario( scenario );

//		for ( final Person person : scenario.getPopulation().getPersons().values() ) {
//			person.getPlans().removeIf( (plan) -> !plan.equals( person.getSelectedPlan() ) ) ;
//		}
//		PopulationUtils.writePopulation( scenario.getPopulation(), "popWOnlySelectedPlans.xml.gz" );
		
		List<Person> toRemove = new ArrayList<>() ;
		for ( final Person person : scenario.getPopulation().getPersons().values() ) {
			boolean containsDrt = false ;
			boolean containsPT = false ;
			for ( final Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ) {
				if ( leg.getMode().contains( "drt" ) ) {
					containsDrt = true ;
				}
				if ( leg.getMode().contains( "pt" ) ) {
					containsPT = true ;
				}
			}
			if ( ! ( containsDrt  /*&& containsPT*/ ) ) {
				toRemove.add( person );
			}
		}
		log.warn( "population size before=" + scenario.getPopulation().getPersons().size() );
		scenario.getPopulation().getPersons().values().removeAll( toRemove ) ;
		log.warn( "population size after=" + scenario.getPopulation().getPersons().size() );
		
		PopulationUtils.writePopulation( scenario.getPopulation(), "popWOnlyDrtPtPlans.xml.gz" );
		
		// ---
		
		Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
		
		controler.addOverridingModule( new OTFVisLiveModule() ) ;
//		controler.addOverridingModule(new DrtSpeedUpModule());
		
		// ---
		
		controler.run() ;
	}

}

