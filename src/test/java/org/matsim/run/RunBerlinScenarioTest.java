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
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunBerlinScenarioTest {
	private static final Logger log = Logger.getLogger( RunBerlinScenarioTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void aTestTest() {
		// a dummy test to satisfy the matrix build by travis.
		log.info( "Done with aTestTest"  );
		log.info("") ;
		Assert.assertTrue( true );
	}
	
	@Test
	public final void bTestConfig1() {
		try {
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:controler.runId", "test-run-ID",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			Config config =  RunBerlinScenario.prepareConfig( args );
			Assert.assertEquals("Wrong parameter from command line", "test-run-ID", config.controler().getRunId());

			log.info( "Done with bTestConfig1"  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void cTestConfig2() {
		try {
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:controler.runId", "test-run-ID",
					"--config:controler.outputDirectory", utils.getOutputDirectory(),
					"--config:planCalcScore.scoringParameters[subpopulation=null].modeParams[mode=car].constant", "-0.12345"};
			
			Config config =  RunBerlinScenario.prepareConfig( args );
			Assert.assertEquals("Wrong parameter from command line", -0.12345, config.planCalcScore().getModes().get("car").getConstant(), MatsimTestUtils.EPSILON);

			log.info( "Done with cTestConfig2"  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void c2TestEquil() {
		// this should work (but does not since RunBerlinScenario enforces vsp-abort). kai, sep'19
		
		try {
			String configFilename = "scenarios/equil/config.xml" ;
			final String[] args = {configFilename,
					"--config:controler.outputDirectory", utils.getOutputDirectory(),
			};
			RunBerlinScenario.main( args );
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void c2TestEquilB() {
		// this is overriding the vsp-abort but is still not working.  kai, sep'19

		try {
			String configFilename = "scenarios/equil/config.xml" ;
			final String[] args = {configFilename,
					"--config:controler.outputDirectory", utils.getOutputDirectory(),
					"--config:vspExperimental.vspDefaultsCheckingLevel", "warn"
			};
			RunBerlinScenario.main( args );
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void dTest1person1iteration() {
		try {
			final String[] args = {"scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml"};
			
			Config config =  RunBerlinScenario.prepareConfig( args );
			config.controler().setLastIteration(0);
			config.strategy().setFractionOfIterationsToDisableInnovation(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../test/input/test-agents.xml");
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config );
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;
			
			Assert.assertEquals("Change in score (ride + walk agent)", 128.2797261151769, scenario.getPopulation().getPersons().get(Id.createPersonId("100087501")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (bicycle agent)", 129.80394930541985, scenario.getPopulation().getPersons().get(Id.createPersonId("100200201")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (ride agent)", 131.71443152316658, scenario.getPopulation().getPersons().get(Id.createPersonId("10099501")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (pt agent)", 134.91804284998503, scenario.getPopulation().getPersons().get(Id.createPersonId("100024301")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			
			log.info( "Done with dTest1person1iteration"  );
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void eTest1pctUntilIteration1() {
		try {
			final String[] args = {"scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml"};
			
			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;
			
			// Scores in iteration 0
			Assert.assertEquals("Different avg. executed score in iteration 0 .", 114.75008227161196, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Different avg. avg. score in iteration 0 .", 114.75008227161196, controler.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			
			// Scores in iteration 1
//			Assert.assertEquals("Different avg. executed score in iteration 1 .", 112.76204261716643, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), MatsimTestUtils.EPSILON);
//			Assert.assertEquals("Different avg. avg. score in iteration 1.", 113.75644633346553, controler.getScoreStats().getScoreHistory().get(ScoreItem.average).get(1), MatsimTestUtils.EPSILON);

			// The differences in the scores compared to the run in the public-svn are probably related to the pt raptor router
			// which seems to produce slightly different results (e.g. in case two routes are identical).
			// Thus the large epsilon. ihab, dec'18
			
			log.info( "Done with eTest1pctUntilIteration1"  );
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	

	static Map<String, Double> analyzeModeStats( Population population ) {
		
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
}
