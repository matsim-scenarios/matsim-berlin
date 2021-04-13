/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import java.util.*;

/**
 * part 2 of tests for {@link org.matsim.run.RunBerlinScenario}
 *
 * @author ikaddoura
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunBerlinScenarioTest {
	private static final Logger log = Logger.getLogger( RunBerlinScenarioTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void dTest1person1iteration() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
			
			Config config =  RunBerlinScenario.prepareConfig( args );
			config.controler().setLastIteration(0);
			config.strategy().setFractionOfIterationsToDisableInnovation(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../test/input/test-agents.xml");
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config );
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;
			
			Assert.assertEquals("Change in score (ride + walk agent)", 127.8015913751874, scenario.getPopulation().getPersons().get(Id.createPersonId("100087501")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (bicycle agent)", 130.00394930541987, scenario.getPopulation().getPersons().get(Id.createPersonId("100200201")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (ride agent)", 134.2287546857898, scenario.getPopulation().getPersons().get(Id.createPersonId("10099501")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (pt agent)", 136.2304371580296, scenario.getPopulation().getPersons().get(Id.createPersonId("100024301")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			
			log.info( "Done with dTest1person1iteration"  );
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void eTest1pctUntilIteration1() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
			
			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
			berlinCfg.setPopulationDownsampleFactor(0.1);

			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;
			
			// TODO: Add asserts once we move towards a release...
			// Scores in iteration 0
//			Assert.assertEquals("Different avg. executed score in iteration 0 .", 114.526932327335, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
//			Assert.assertEquals("Different avg. avg. score in iteration 0 .", 114.526932327335, controler.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			
			// Scores in iteration 1
//			Assert.assertEquals("Different avg. executed score in iteration 1 .", 112.76204261716643, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), MatsimTestUtils.EPSILON);
//			Assert.assertEquals("Different avg. avg. score in iteration 1.", 113.75644633346553, controler.getScoreStats().getScoreHistory().get(ScoreItem.average).get(1), MatsimTestUtils.EPSILON);

			// The differences in the scores compared to the run in the public-svn are probably related to the pt raptor router
			// which seems to produce slightly different results (e.g. in case two routes are identical).
			// Thus the large epsilon. ihab, dec'18
			
			log.info( "Done with eTest1pctUntilIteration1"  );
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}

	@Test
	public final void testTaggingAndKeepingInitialPlans() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};

			Config config = RunBerlinScenario.prepareConfig(args);
			// should be enough iterations to exceed 2 new plans per agent (given maximum 3 plans memory per agent)
			// -> check that the initial plan is not deleted
			config.controler().setLastIteration(15);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.strategy().setMaxAgentPlanMemorySize(3);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.plans().setInputFile("../../../test/input/test-agents.xml");
			config.transit().setUsingTransitInMobsim(false); // not relevant for the test, speeds up

			BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
			berlinCfg.setPlanTypeOverwriting(BerlinExperimentalConfigGroup.PlanTypeOverwriting.TAG_INITIAL_SELECTED_PLAN_AND_MODIFIED_PLANS_DIFFERENTLY);

			Scenario scenario = RunBerlinScenario.prepareScenario(config);

			Map<Id<Person>, List<Tuple<String, Double>>> person2initialPlanModesAndDepartureTimes = new HashMap<>();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan selectedPlan = person.getSelectedPlan();
				List<Tuple<String, Double>> modesAndDepartureTimes = new ArrayList<>();
				for (PlanElement planElement : selectedPlan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						modesAndDepartureTimes.add(new Tuple(leg.getMode(), leg.getDepartureTime()));
					}
				}
				person2initialPlanModesAndDepartureTimes.put(person.getId(), modesAndDepartureTimes);
			}

			Controler controler = RunBerlinScenario.prepareControler(scenario);

			controler.run();

			for (Person person : scenario.getPopulation().getPersons().values()) {
				int countInitialPlans = 0;
				Plan initialPlan = null;
				for (Plan plan : person.getPlans()) {
					if (plan.getType().equals(PlanTypeOverwriter.initialPlanType)) {
						countInitialPlans++;
						initialPlan = plan;
					}
				}
				Assert.assertEquals("Found " + countInitialPlans + " plans of type " +
								PlanTypeOverwriter.initialPlanType + " for person " + person.getId().toString() +
								" but should be exactly one plan of that type.",
						1, countInitialPlans);

				/*
				 * Check the initial plan is really the initial plan we read in before iteration 0. This might give false alarms if
				 * legs are routed or otherwise added in PrepareForSim step or similar. We do not compare routes or
				 * activities, this might be added later.
				 */
				int legCounter = 0;
				List<Tuple<String, Double>> initialModesAndDepartureTimes = person2initialPlanModesAndDepartureTimes.get(person.getId());
				for (PlanElement planElement : initialPlan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						Assert.assertEquals("Initial plan read in in iteration 0 has different leg mode at " +
										legCounter + "th leg than plan marked initial at the last iteration for person " +
										person.getId().toString(),
								initialModesAndDepartureTimes.get(legCounter).getFirst(), leg.getMode());
						Assert.assertEquals("Initial plan read in in iteration 0 has different leg departure time at " +
										legCounter + "th leg than plan marked initial at the last iteration for person " +
										person.getId().toString(),
								initialModesAndDepartureTimes.get(legCounter).getSecond(), leg.getDepartureTime());
						legCounter++;
					}
				}
			}

			log.info("Done with testTaggingAndKeepingInitialPlans");
			log.info("");


		} catch (Exception ee) {
			ee.printStackTrace();
			throw new RuntimeException(ee);
		}
	}
	

	static Map<String, Double> analyzeModeStats( Population population ) {
		
		Map<String,Double> modeCnt = new TreeMap<>() ;

		MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
		
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan() ;

			List<Trip> trips = TripStructureUtils.getTrips(plan) ;
			for ( Trip trip : trips ) {
				String mode = mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ;
				
				Double cnt = modeCnt.get( mode );
				if ( cnt==null ) {
					cnt = 0. ;
				}
				modeCnt.put( mode, cnt + 1 ) ;
			}
		}

		Logger.getLogger(modeCnt.toString()) ;			
		return modeCnt;	
	}
}
