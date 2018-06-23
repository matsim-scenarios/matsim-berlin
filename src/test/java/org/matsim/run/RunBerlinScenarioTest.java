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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunBerlinScenarioTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	// Run the Berlin scenario for 1 iteration without a population
	@Test
	public final void test0() {
		try {
			Config config;

			String configFile = "scenarios/berlin-v5.0-2018-06-18/input/berlin-5.0_config.xml";
			config = ConfigUtils.loadConfig(configFile);
			config.plans().setInputFile(null);
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			RunBerlinScenario.run(config);
			
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;			
			Assert.fail("Wasn't able to run the berlin scenario without a population.");
		}

	}
	
	@Ignore
	@Test
	public final void test1() {
		try {
			Config config;

			String configFile = "scenarios/berlin-v5.0-2018-06-18/input/berlin-5.0_config.xml";
			config = ConfigUtils.loadConfig(configFile);
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			RunBerlinScenario.run(config);
			
			//TODO: compare scores and maybe also modal split
			
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;			
			Assert.fail("Wasn't able to run the berlin scenario with the full population.");
		}

	}

}
