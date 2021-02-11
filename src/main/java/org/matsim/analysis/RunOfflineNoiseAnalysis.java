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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author ikaddoura
 *
 */
public class RunOfflineNoiseAnalysis {
	private static final Logger log = Logger.getLogger(RunOfflineNoiseAnalysis.class);
	
	private final String runDirectory;
	private final String runId;
	private final String analysisOutputDirectory;
	
//	private final String tunnelLinkIdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
	private final String tunnelLinkIdFile = null;

//	private final String noiseBarriersFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-buildings/osm-buildings-dissolved.geojson";
	private final String noiseBarriersFile = null;

	public RunOfflineNoiseAnalysis(String runDirectory, String runId, String analysisOutputDirectory) {
		this.runDirectory = runDirectory;
		this.runId = runId;
		
		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;
	}

	public static void main(String[] args) {
		
		final String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
		final String runId = "berlin-v5.4-1pct";
		
		RunOfflineNoiseAnalysis analysis = new RunOfflineNoiseAnalysis(runDirectory, runId, "./scenario/");
		analysis.run();
	}

	void run() {
		double receiverPointGap = 100.;
		double timeBinSize = 3600.;
		
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.global().setCoordinateSystem("GK4");
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setRunId(runId);
						
		// adjust the default noise parameters
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
		noiseParameters.setReceiverPointGap(receiverPointGap);

		double xMin = 4573258.;
		double yMin = 5801225.;
		double xMax = 4620323.;
		double yMax = 5839639.;
		
		noiseParameters.setReceiverPointsGridMinX(xMin);
		noiseParameters.setReceiverPointsGridMinY(yMin);
		noiseParameters.setReceiverPointsGridMaxX(xMax);
		noiseParameters.setReceiverPointsGridMaxY(yMax);
		
		String[] consideredActivitiesForDamages = {"home*", "work*", "leisure*", "shopping*", "other*"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
		
		// ################################
		
		noiseParameters.setUseActualSpeedLevel(false);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(100.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setThrowNoiseEventsAffected(true);
		noiseParameters.setThrowNoiseEventsCaused(false);
		
		String[] hgvIdPrefixes = { "freight" };
		noiseParameters.setHgvIdPrefixesArray(hgvIdPrefixes);
		
		noiseParameters.setTunnelLinkIdFile(tunnelLinkIdFile);
		noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);

		noiseParameters.setConsiderNoiseBarriers(false);
		noiseParameters.setNoiseBarriersFilePath(noiseBarriersFile);
		noiseParameters.setNoiseBarriersSourceCRS("EPSG:31468");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, analysisOutputDirectory);
		noiseCalculation.run();	
		
		// some processing of the output data
		String outputFilePath = analysisOutputDirectory + "noise-analysis/";
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();
				
		final String[] labels = { "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();
		
		log.info("Done.");
	}
}
		

