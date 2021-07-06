/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.analysis.substitutePT;

import org.apache.log4j.Logger;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.accessibility.RunBerlinScenarioWithAccessibilities;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

import java.io.File;

/**
 *
 * (example) script to obtain metrics from matsim-berlin that help understanding where pt is ineffiecient and drt might be a better fit.
 * runs MATSim for one iteration (necessary to obtain accessibility).<p>
 *
 * Metrics can be visualised with the aftersim UI and include: <ul>
 * <li> DRT operator costs		(will be not 0 or not available as there is no drt in the base case)
 * <li> DRT operator income		(will be not 0 or not available as there is no drt in the base case)
 * <li> PT operator costs
 * <li> PT occupancy
 * <li> PT accessibility
 * <li> Demand Potential (trip origins and destinations from the trips.csv)
 *
 */

//TODO restructure this such that it has the structure of a run class (prepareConfig+ prepareScenario + prepareControler) and it configures everything (independently of whether drt is used in the underlying run)
class DrtSubstitutesAnalysisRunner {

	private static final Logger log = Logger.getLogger(DrtSubstitutesAnalysisRunner.class);

	public static void main(String[] args) {

		//TODO should we really do this?
		log.warn("overwriting program arguments!!");
		args = new String[1];
		args[0] = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-10pct.output_config.xml";

		//compute accessibilities. will result in a shape file
		Config config = RunBerlinScenarioWithAccessibilities.prepareConfig(args);

		//use existing matsim-berlin run output and only run for one iteration in order to create new/additional output
		config.controler().setLastIteration(0); //actually also set in RunBerlinScenarioWithAccessibilities.prepareConfig
		config.plans().setInputFile("berlin-v5.5.3-10pct.output_plans.xml.gz");
		config.controler().setOutputDirectory("../../projects/drtSubstitutesPT");

		Scenario scenario = RunBerlinScenario.prepareScenario(config);

		Controler controler = RunBerlinScenarioWithAccessibilities.prepareControler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				//creates output table containing drt operator KPIs
				install(new PersonMoneyEventsAnalysisModule());
				//creates output on pt occupancy
				install(new PtStop2StopAnalysisModule());

				//TODO install pt fare computation
			}
		});

		controler.run();
	}

	//TODO allow more custom modules to add?
	static Config prepareConfigBasedOnOutputConfig(String pathToOutputConfig, boolean drtUsed){

		String[] args = new String[1];
		args[0] = pathToOutputConfig;
		Config config;
		if(drtUsed){
			config = RunDrtOpenBerlinScenario.prepareConfig(args);
		} else {
			ConfigGroup[] customModulesToAdd = new ConfigGroup[]{new AccessibilityConfigGroup()};
			config = RunBerlinScenario.prepareConfig(args, customModulesToAdd);
		}

		//now do everything that RunBerlinScenarioWithAccessibilities.prepareConfig does
		{
			File opportunitiesFile = new File("../../shared-svn/projects/accessibility-berlin/osm/berlin/amenities/2018-05-30/facilities_classified.xml");
			config.facilities().setInputFile(opportunitiesFile.getAbsolutePath());

			AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
			acg.setTimeOfDay((8*60.+5.)*60.);
			acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
			acg.setShapeFileCellBasedAccessibility("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp");
			acg.setTileSize_m(5000);
			acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
			acg.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
			acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
			acg.setOutputCrs(config.global().getCoordinateSystem());
		}


		config.controler().setLastIteration(0);
		config.plans().setInputFile(""); //TODO output plans file from the given config (think there is some utility method hidden somewhere for that)
		config.controler().setOutputDirectory("drtSubstitutesPTAnalysis");//TODO probably will have to be adjusted after this method is called. can only set this such that nothing from the original run will be overwritten

		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setDumpDataAtEnd(false); //TODO check! (idea is to minimize output as much as possible to our specific analysis. but we need trips.csv.gz)
		return config;
	}
}
