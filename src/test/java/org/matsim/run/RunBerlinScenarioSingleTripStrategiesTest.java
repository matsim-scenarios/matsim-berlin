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
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunBerlinScenarioSingleTripStrategiesTest {
	private static final Logger log = Logger.getLogger( RunBerlinScenarioSingleTripStrategiesTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test1() {
		try {
			final String[] args = {"./test/input/berlin-v5.5-1pct.config_singleTripStrategies.xml"};
			
			Config config =  RunBerlinScenario.prepareConfig( args );
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1.0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("test-agents.xml");
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config );
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;
			
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
}
