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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * part 1 of tests for {@link org.matsim.run.RunBerlinScenario}
 *
 * @author ikaddoura
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunBerlinScenarioSmallTests {
	private static final Logger log = Logger.getLogger( RunBerlinScenarioSmallTests.class ) ;
	
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
			String configFilename = "scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml";
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
			String configFilename = "scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml";
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
