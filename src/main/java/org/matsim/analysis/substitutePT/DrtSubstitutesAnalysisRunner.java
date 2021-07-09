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
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.optDRT.MultiModeOptDrtConfigGroup;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.accessibility.RunBerlinScenarioWithAccessibilities;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.run.dynamicShutdown.DynamicShutdownConfigGroup;

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

//TODO restructure this such that it has the structure of a run class (prepareConfig+ prepareScenario + prepareControler) and it configures everything needed (dependent of whether drt is used in the underlying run)
class DrtSubstitutesAnalysisRunner {

	private static final Logger log = Logger.getLogger(DrtSubstitutesAnalysisRunner.class);
	private final boolean drtUsed;

	public DrtSubstitutesAnalysisRunner(boolean drtUsedInUnderlyingRun){
		this.drtUsed = drtUsedInUnderlyingRun;
	}

	public static void main(String[] args) {

//		DrtSubstitutesAnalysisRunner analysisRunner = new DrtSubstitutesAnalysisRunner(true);
//		Config config = analysisRunner.prepareConfigBasedOnOutputConfig("D:/VW/test/i501.output_config.xml", new DynamicShutdownConfigGroup(), new MultiModeOptDrtConfigGroup());

		DrtSubstitutesAnalysisRunner analysisRunner = new DrtSubstitutesAnalysisRunner(false);
		Config config = analysisRunner.prepareConfigBasedOnOutputConfig("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/output-berlin-v5.5-1pct/berlin-v5.5.3-1pct.output_config.xml");
		config.controler().setOutputDirectory("D:/VW/test-berlin-1pct");



		Scenario scenario = analysisRunner.prepareScenario(config);
		Controler controler = analysisRunner.prepareControler(scenario);

		controler.run();
	}

	//TODO allow more custom modules to add?
	//TODO could think of making AccessibilityCfg the 3rd parameter and copy everything into the config created/loaded here (so the user can configure the accessibility outside)
	/*package */
	 Config prepareConfigBasedOnOutputConfig(String pathToOutputConfig, ConfigGroup... customModules ){

		String[] args = new String[1];
		args[0] = pathToOutputConfig;
		Config config;
		ConfigGroup[] customModulesToAdd = new ConfigGroup[]{new AccessibilityConfigGroup()};
		 ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];
		 int counter = 0;
		 for (ConfigGroup customModule : customModules) {
			 customModulesAll[counter] = customModule;
			 counter++;
		 }
		 for (ConfigGroup customModule : customModulesToAdd) {
			 customModulesAll[counter] = customModule;
			 counter++;
		 }
		 if(drtUsed){
			 config = RunDrtOpenBerlinScenario.prepareConfig(args, customModulesAll);
		 } else {
			 config = RunBerlinScenario.prepareConfig(args, customModulesAll);
		 }

		//now do everything that RunBerlinScenarioWithAccessibilities.prepareConfig does
		{
			String opportunitiesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/drtSubstitutesPT/input/accessibility/amenities/2018-05-30/facilities_classified.xml";
			config.facilities().setInputFile(opportunitiesFile);

			AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
			acg.setTimeOfDay((8*60.+5.)*60.); //8.05 am
			acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
			acg.setShapeFileCellBasedAccessibility("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp");
			acg.setTileSize_m(1000);
			acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
			acg.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
			acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
			acg.setOutputCrs(config.global().getCoordinateSystem());
		}

		config.controler().setLastIteration(0);

		 //set output plans from underlying run as input plans for our analysis run
		 String runId = config.controler().getRunId() == null ? "" : config.controler().getRunId() + ".";
		 config.plans().setInputFile(runId + "output_plans.xml.gz");
//		config.plans().setInputFile("D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.plans.xml.gz"); //TODO

		config.controler().setOutputDirectory(pathToOutputConfig.substring(0, pathToOutputConfig.lastIndexOf("/") + 1) + "drtSubstitutesPTAnalysis");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles); //TODO change to failIfDirectoryExists

		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setDumpDataAtEnd(false);
		return config;
	}

	/*package */
 	Scenario prepareScenario(Config config){
		if(drtUsed){
			return RunDrtOpenBerlinScenario.prepareScenario(config);
		} else {
			return RunBerlinScenario.prepareScenario(config);
		}
	}

	Controler prepareControler(Scenario scenario){
		Controler controler = RunBerlinScenarioWithAccessibilities.prepareControler(scenario);

		//now bind our custom analysis modules
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
//				creates output table containing drt operator KPIs
				install(new PersonMoneyEventsAnalysisModule());
//				creates output on pt occupancy
				install(new PtStop2StopAnalysisModule());

			}
		});

		if(drtUsed) {
			// drt + dvrp module
			controler.addOverridingModule(new MultiModeDrtModule());
			controler.addOverridingModule(new DvrpModule());
			controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

			controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					// use a main mode identifier which knows how to handle intermodal trips generated by the used sbb pt raptor router
					// the SwissRailRaptor already binds its IntermodalAwareRouterModeIdentifier, however drt obviuosly replaces it
					// with its own implementation
					// So we need our own main mode indentifier which replaces both :-(
					bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
					bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.class);
				}
			});
			controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
			controler.addOverridingModule(new PtIntermodalRoutingModesModule());
		}

		return controler;
	}

}
