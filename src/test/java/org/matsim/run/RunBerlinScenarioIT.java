/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import static org.matsim.run.RunBerlinScenarioTest.analyzeModeStats;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestUtils;

public class RunBerlinScenarioIT{
	private static final Logger log = Logger.getLogger( RunBerlinScenarioIT.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	// 10pct, testing the scores in iteration 0 and 1
	@Test
	public final void testEquil() {
		try {
			final String[] args = {"scenarios/equil/config.xml"};

			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;

			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

			controler.run() ;

			// TODO: enable once we move towards a release
//			Assert.assertEquals("Different scores in iteration 0.", 115.39990555612046, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
//			Assert.assertEquals("Different scores in iteration 1.", 113.59365288084939, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), 0.001);

			// The differences in the scores compared to the run in the public-svn are probably related to the pt raptor router
			// which seems to produce slightly different results (e.g. in case two routes are identical).
			// Thus the large epsilon. ihab, dec'18

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

	// 10pct, testing the scores in iteration 0 and 1
	@Test
	public final void test10pctUntilIteration1() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"};

			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;

			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

			controler.run() ;

			// TODO: enable once we move towards a release
//			Assert.assertEquals("Different scores in iteration 0.", 115.39990555612046, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
//			Assert.assertEquals("Different scores in iteration 1.", 113.59365288084939, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), 0.001);

			// The differences in the scores compared to the run in the public-svn are probably related to the pt raptor router
			// which seems to produce slightly different results (e.g. in case two routes are identical).
			// Thus the large epsilon. ihab, dec'18

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

	// 1pct, testing the score and modal split in iteration 0 and 40.
	@Test
	public final void test1pctManyIterations() {
		// (right now cannot run even up to 50 iterations because logfile becomes to long.  I can tell maven to send the output
		// to file, but then we don't see anything.  So we will have to play around with the number of iterations.  Thus the
		// imprecise name of the test.   kai, aug'18)

		final int iteration = 40;
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};

			Config config = RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(iteration);

			config.qsim().setNumberOfThreads( 1 );
			config.global().setNumberOfThreads( 1 );
			// small number of threads in hope to consume less memory.  kai, jul'18

			config.strategy().setFractionOfIterationsToDisableInnovation( 1.0 );

			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
			config.controler().setWritePlansUntilIteration( 0 );
			config.controler().setWritePlansInterval( 0 );
			
			final double sample = 0.1;
			config.qsim().setFlowCapFactor( config.qsim().getFlowCapFactor()*sample );
			config.qsim().setStorageCapFactor( config.qsim().getStorageCapFactor()*sample );
			BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
			berlinCfg.setPopulationDownsampleFactor(sample);
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;

			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;

			Gbl.assertNotNull( controler.getScoreStats() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ) );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(0) );

			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(iteration) );

			Map<String,Double> modeCnt = analyzeModeStats(scenario.getPopulation());

			double sum = 0 ;
			for ( Double val : modeCnt.values() ) {
				sum += val ;
			}

			Assert.assertEquals("Major change in the car trip share.", 0.22781865397100196, modeCnt.get("bicycle" ) / sum, 0.01 );
			Assert.assertEquals("Major change in the car trip share.", 0.29084613719974034, modeCnt.get("car" ) / sum, 0.01 );
			Assert.assertEquals("Major change in the car trip share.", 0.0012443194113828176, modeCnt.get("freight" ) / sum, 0.01 );
			Assert.assertEquals("Major change in the car trip share.", 0.22273317463752434, modeCnt.get("pt" ) / sum, 0.01 );
			Assert.assertEquals("Major change in the car trip share.", 0.08899588833585804, modeCnt.get("ride" ) / sum, 0.01 );
			Assert.assertEquals("Major change in the car trip share.", 0.16836182644449255, modeCnt.get("walk" ) / sum, 0.01 );

		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}

}
