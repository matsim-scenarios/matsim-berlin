package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

import static org.matsim.run.RunBerlinScenarioTest.analyzeModeStats;

public class RunBerlinScenarioIT{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	// 10pct, testing the scores in iteration 0 and 1
	@Test
	public final void test10pctUntilIteration1() {
		try {
			final String[] args = {"scenarios/berlin-v5.4-10pct/input/berlin-v5.4-10pct.config.xml"};

			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

			controler.run() ;

			// Changes in the MATSim core, in particular some fixes related to the teleportation speed of car access_walk and car egress_walk legs, the scores have changed
			// As far as I can see, the car mode has become slightly less attractive which probably requires a re-calibration of the Berlin scenario.
			// Once, we have the next version, the following score comparisons have to be re-activated and updated. ihab April'19

			//			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/.", 115.866073407524, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("The scores in iteration 1 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/.", 115.02251116746, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(1), 0.001);

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
			final String[] args = {"scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml"};

			Config config = RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(iteration);
			//			config.qsim().setEndTime(30 * 3600.);

			config.qsim().setNumberOfThreads( 1 );
			config.global().setNumberOfThreads( 1 );
			// small number of threads in hope to consume less memory.  kai, jul'18

			config.strategy().setFractionOfIterationsToDisableInnovation( 1.0 );

			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
			config.controler().setWritePlansUntilIteration( 0 );
			config.controler().setWritePlansInterval( 0 );

			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			//			final double sample = 0.1;
			//			downsample( scenario.getPopulation().getPersons(), sample ) ;
			//			config.qsim().setFlowCapFactor( config.qsim().getFlowCapFactor()*sample );
			//			config.qsim().setStorageCapFactor( config.qsim().getStorageCapFactor()*sample );

			Controler controler = new Controler( scenario ) ;
			
			controler.run() ;

			Gbl.assertNotNull( controler.getScoreStats() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ) );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(0 ) );

			// Changes in the MATSim core, in particular some fixes related to the teleportation speed of car access_walk and car egress_walk legs, the scores have changed
			// As far as I can see, the car mode has become slightly less attractive which probably requires a re-calibration of the Berlin scenario.
			// Once, we have the next version, the following score comparisons have to be re-activated and updated. ihab April'19

			//			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 115.072273500216, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 115.072273500216, berlin.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(iteration ) );

			// Changes in the MATSim core, in particular some fixes related to the teleportation speed of car access_walk and car egress_walk legs, the scores have changed
			// As far as I can see, the car mode has become slightly less attractive which probably requires a re-calibration of the Berlin scenario.
			// Once, we have the next version, the following score comparisons have to be re-activated and updated. ihab April'19

			//			Assert.assertEquals("Major change in the avg. AVG score in iteration " + iteration + " compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 114.125389832973, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(iteration), 0.001);
			//			Assert.assertEquals("Major change in the avg. AVG score in iteration " + iteration + " compared to https://https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 113.181180406316, berlin.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), 0.001);

			Map<String,Double> modeCnt = analyzeModeStats(scenario.getPopulation() );

			double sum = 0 ;
			for ( Double val : modeCnt.values() ) {
				sum += val ;
			}

			Assert.assertEquals("Major change in the car trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/.", 0.339940203527443, modeCnt.get("car" ) / sum, 0.01 );
			Assert.assertEquals("Major change in the pt trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/.", 0.218322955810955, modeCnt.get("pt") / sum, 0.01);
			Assert.assertEquals("Major change in the bicycle trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/.", 0.188655127958965, modeCnt.get("bicycle") / sum, 0.01);
			Assert.assertEquals("Major change in the walk trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/.", 0.160435581644128, modeCnt.get("walk") / sum, 0.01);
			Assert.assertEquals("Change in the freight trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/.", 0.00146473928189373, modeCnt.get("freight") / sum, MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in the ride trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/.", 0.0911813917766135, modeCnt.get("ride") / sum, MatsimTestUtils.EPSILON);

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

}
