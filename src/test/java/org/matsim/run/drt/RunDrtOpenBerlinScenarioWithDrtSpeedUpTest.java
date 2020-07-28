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

package org.matsim.run.drt;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.MultiModeDrtSpeedUpModule;
import org.matsim.run.BerlinExperimentalConfigGroup;
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
			config.planCalcScore().setWriteExperiencedPlans(false);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			config.plans().setInputFile("../../../../test/input/drt/drt-test-agents.xml");
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
			}
			
			BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
			berlinCfg.setPopulationDownsampleFactor(1.0);

			MultiModeDrtSpeedUpModule.addTeleportedDrtMode(config);

			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			controler.addOverridingModule(new MultiModeDrtSpeedUpModule());
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
			config.qsim().setNumberOfThreads(1);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
			}
			
			BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
			berlinCfg.setPopulationDownsampleFactor(0.01);

			MultiModeDrtSpeedUpModule.addTeleportedDrtMode(config);

			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;			
			controler.addOverridingModule(new MultiModeDrtSpeedUpModule());
			controler.run() ;			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
}
