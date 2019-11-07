package org.matsim.run.drt;

import java.util.Map;
import java.util.Random;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.DrtSpeedUpModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author ikaddoura
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunDrtOpenBerlinScenarioWithDrtSpeedUpTest {
		
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test1() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args , new DrtSpeedUpConfigGroup()) ;
			config.controler().setLastIteration(2);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setWritePlansInterval(4);
			config.controler().setWriteEventsInterval(4);
			config.planCalcScore().setWriteExperiencedPlans(true);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.global().setNumberOfThreads(1);
			config.plans().setInputFile("../../../../test/input/drt/drt-test-agents.xml");
			
			DrtSpeedUpModule.adjustConfig(config);

			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			downsample( scenario.getPopulation().getPersons(), 1.0 );
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			controler.addOverridingModule(new DrtSpeedUpModule());
			controler.run() ;			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Ignore
	@Test
	public final void test2() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args , new DrtSpeedUpConfigGroup()) ;
			config.controler().setLastIteration(30);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setWritePlansInterval(30);
			config.controler().setWriteEventsInterval(30);
			config.global().setNumberOfThreads(1);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			
			DrtSpeedUpModule.adjustConfig(config);

			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			downsample( scenario.getPopulation().getPersons(), 0.01 );
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;			
			controler.addOverridingModule(new DrtSpeedUpModule());
			controler.run() ;			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	private static void downsample( final Map<Id<Person>, ? extends Person> map, final double sample ) {
		final Random rnd = MatsimRandom.getLocalInstance();
		map.values().removeIf( person -> rnd.nextDouble() > sample ) ;
	}
}
