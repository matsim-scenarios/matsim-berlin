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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunBerlinScenarioTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	// 10pct, testing the scores in iteration 0
	@Test
	public final void test1() {
		try {
			String configFilename = "scenarios/berlin-v5.0-10pct-2018-06-18/input/berlin-5.0_config.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( configFilename ) ;
			Config config =  berlin.prepareConfig() ;
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			berlin.run() ;
			
			Assert.assertEquals("Wrong avg. AVG score in iteration 0.", 115.26173800545439, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;			
			Assert.fail("Wasn't able to run the berlin scenario.");
		}
	}
	
	// 1pct (version 5.0)
	// testing the score in the 0th and 100th iteration
	// testing the modal split in the 100th iteration
	@Test
	public final void test2a() {
		try {
			String configFile = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( configFile ) ;
			final Config config = berlin.prepareConfig();;
			config.controler().setLastIteration(1);
			config.qsim().setNumberOfThreads(1); // to have it fully deterministic
			config.strategy().setFractionOfIterationsToDisableInnovation(1.);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			berlin.run() ;

			testScores(berlin.getScoreStats().getScoreHistory());
			testModalSplit(analyzeModeStats(berlin.getPopulation()));

		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;			
			Assert.fail("Wasn't able to run the berlin scenario.");
		}
	}

	// 1pct (version 5.1)
	// testing the score in the 0th and 100th iteration
	// testing the modal split in the 100th iteration
	@Test
	public final void test2b() {
		try {
			String configFile = "scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( configFile) ;
			Config config = berlin.prepareConfig() ;
			config.controler().setLastIteration(1);
			config.qsim().setEndTime(30 * 3600.); // TODO: adjust in the config file!
			config.qsim().setNumberOfThreads(1); // to have it fully deterministic
			config.strategy().setFractionOfIterationsToDisableInnovation(1.);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			berlin.run() ;

			testScores(berlin.getScoreStats().getScoreHistory());
			testModalSplit(analyzeModeStats(berlin.getPopulation()));
			
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;			
			Assert.fail("Wasn't able to run the berlin scenario.");
		}
	}
	
	private Map<String, Double> analyzeModeStats(Population population) {
		
		Map<String,Double> modeCnt = new TreeMap<>() ;

		StageActivityTypesImpl stageActivities = new StageActivityTypesImpl(Arrays.asList("pt interaction", "car interaction", "ride interaction", "bicycle interaction", "freight interaction"));
		MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
		
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan() ;

			List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivities) ;
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
	
	private void testScores(Map<ScoreItem, Map<Integer, Double>> scoreHistory) {
		Assert.assertEquals("Major change in the avg. AVG score in iteration 0.", 115.2173655596178, scoreHistory.get(ScoreItem.average).get(0), 1.0);
		Assert.assertEquals("Major change in the avg. AVG score in iteration 100.", 115.39338160261939, scoreHistory.get(ScoreItem.average).get(100), 1.0);
	}

	private void testModalSplit(Map<String, Double> modeCnt) {
		
		double sum = 0 ;
		for ( Double val : modeCnt.values() ) {
			sum += val ;
		}
		
		Assert.assertEquals("Major change in the car trip share (iteration 100).", 0.41707279676702186, modeCnt.get("car") / sum, 0.02);
		Assert.assertEquals("Major change in the pt trip share (iteration 100)", 0.1932777710849971, modeCnt.get("pt") / sum, 0.02);
		Assert.assertEquals("Major change in the bicycle trip share (iteration 100)", 0.1403804663286204, modeCnt.get("bicycle") / sum, 0.02);
		Assert.assertEquals("Major change in the walk trip share (iteration 100)", 0.15878500476404298, modeCnt.get("walk") / sum, 0.02);
		Assert.assertEquals("Change in the freight trip share (iteration 100)", 0.0014730201842096616, modeCnt.get("freight") / sum, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Change in the ride trip share (iteration 100)", 0.089010940871108, modeCnt.get("ride") / sum, MatsimTestUtils.EPSILON);
	}
}
