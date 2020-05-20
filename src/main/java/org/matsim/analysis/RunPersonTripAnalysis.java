/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class RunPersonTripAnalysis {
	private static final Logger log = Logger.getLogger(RunPersonTripAnalysis.class);
			
	public static void main(String[] args) throws IOException {
			
		String runDirectory = null;
		String runId = null;
		String runDirectoryToCompareWith = null;
		String runIdToCompareWith = null;
		String visualizationScriptInputDirectory = null;
		String scenarioCRS = null;	
		String shapeFileZones = null;
		String zonesCRS = null;
		String zoneId = null;
		String homeActivityPrefix = null;
		int scalingFactor;
		String modesString = null;
		String analysisOutputDirectory = null;
		
		if (args.length > 0) {
			if (!args[0].equals("null")) runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
			
			if (!args[1].equals("null")) runId = args[1];
			log.info("Run Id: " + runDirectory);
			
			if (!args[2].equals("null")) runDirectoryToCompareWith = args[2];
			log.info("Run directory to compare with: " + runDirectoryToCompareWith);
			
			if (!args[3].equals("null")) runIdToCompareWith = args[3];
			log.info("Run Id to compare with: " + runDirectory);
			
			if (!args[4].equals("null")) scenarioCRS = args[4];	
			log.info("Scenario CRS: " + scenarioCRS);
			
			if (!args[5].equals("null")) shapeFileZones = args[5];
			log.info("Shape file zones: " + shapeFileZones);

			if (!args[6].equals("null")) zonesCRS = args[6];
			log.info("Zones CRS: " + zonesCRS);
			
			if (!args[7].equals("null")) zoneId = args[7];
			log.info("Zones Id: " + zonesCRS);
			
			if (!args[8].equals("null")) homeActivityPrefix = args[8];
			log.info("Home activity prefix: " + homeActivityPrefix);

			scalingFactor = Integer.valueOf(args[9]);
			log.info("Scaling factor: " + scalingFactor);
		
			if (!args[10].equals("null")) visualizationScriptInputDirectory = args[10];
			log.info("Visualization script input directory: " + visualizationScriptInputDirectory);
			
			if (!args[11].equals("null")) modesString = args[11];
			log.info("modes: " + modesString);

		} else {
			
//			runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
//			runId = "berlin-v5.4-1pct";
//			runDirectoryToCompareWith = null;
//			runIdToCompareWith = null;
//
//			visualizationScriptInputDirectory = null;
//
//			scenarioCRS = TransformationFactory.DHDN_GK4;
//
//			shapeFileZones = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-bezirke/bezirke_berlin.shp";
//			zonesCRS = TransformationFactory.DHDN_GK4;
//			zoneId = "SCHLUESSEL";
//
//			homeActivityPrefix = "home";
//			scalingFactor = 100;
//
//			modesString = "car,pt,bicycle,walk,ride";
//
//			analysisOutputDirectory = "./scenarios/berlin-v5.5-1pct/";

			runDirectory = "/Users/dominik/Workspace/runs-svn/open_berlin_scenario/v5.5-bicycle/bc-23/output";

			runId = "berlin-v5.5-1pct-22b";
			runDirectoryToCompareWith = null;
			runIdToCompareWith = null;

			visualizationScriptInputDirectory = null;

			scenarioCRS = TransformationFactory.DHDN_GK4;

			shapeFileZones = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-bezirke/bezirke_berlin.shp";
			zonesCRS = TransformationFactory.DHDN_GK4;
			zoneId = "SCHLUESSEL";

			homeActivityPrefix = "home";
			scalingFactor = 100;

			modesString = "car,pt,bicycle,walk,ride";

			analysisOutputDirectory = "/Users/dominik/Workspace/runs-svn/open_berlin_scenario/v5.5-bicycle/bc-23/output/analysis";
		}
		
		Scenario scenario1 = loadScenario(runDirectory, runId, null);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, null);
		
		List<AgentFilter> agentFilters = new ArrayList<>();

		AgentAnalysisFilter agentFilter1 = new AgentAnalysisFilter("A");
		agentFilter1.setSubpopulation("person");
		agentFilter1.setPersonAttribute("berlin");
		agentFilter1.setPersonAttributeName("home-activity-zone");
		agentFilter1.preProcess(scenario1);
		agentFilters.add(agentFilter1);
		
		AgentAnalysisFilter agentFilter2 = new AgentAnalysisFilter("B");
		agentFilter2.preProcess(scenario1);
		agentFilters.add(agentFilter2);
		
		AgentAnalysisFilter agentFilter3 = new AgentAnalysisFilter("C");
		agentFilter3.setSubpopulation("person");
		agentFilter3.setPersonAttribute("brandenburg");
		agentFilter3.setPersonAttributeName("home-activity-zone");
		agentFilter3.preProcess(scenario1);
		agentFilters.add(agentFilter3);
		
		final List<TripFilter> tripFilters = new ArrayList<>();

		TripAnalysisFilter tripFilter1a = new TripAnalysisFilter("A");
		tripFilter1a.preProcess(scenario1);
		tripFilters.add(tripFilter1a);
		
		TripAnalysisFilter tripFilter1b = new TripAnalysisFilter("B");
		tripFilter1b.setZoneInformation(shapeFileZones, zonesCRS);
		tripFilter1b.preProcess(scenario1);
		tripFilters.add(tripFilter1b);
				
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		MatsimAnalysis analysis = new MatsimAnalysis();
		
		analysis.setScenario1(scenario1);	
		analysis.setScenario0(scenario0);
		
		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		
		analysis.setScenarioCRS(scenarioCRS);
		analysis.setZoneInformation(shapeFileZones, zonesCRS, zoneId);
		
		analysis.setModes(modes);
		analysis.setVisualizationScriptInputDirectory(visualizationScriptInputDirectory);
		analysis.setHomeActivityPrefix(homeActivityPrefix);
		analysis.setScalingFactor(scalingFactor);
		
		analysis.setAnalysisOutputDirectory(analysisOutputDirectory);
		
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFileToReplaceOutputFile) {
		log.info("Loading scenario...");
		
		if (runDirectory == null || runDirectory.equals("") || runDirectory.equals("null")) {
			return null;	
		}
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String networkFile;
		String populationFile;
		String configFile;
		
		configFile = runDirectory + runId + ".output_config.xml";	
		networkFile = runId + ".output_network.xml.gz";
		populationFile = runId + ".output_plans.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile);

		if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");

		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

