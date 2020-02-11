/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
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

package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.XYLineChart;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;


public class JRDynamicShutdownControlerListener implements StartupListener, IterationEndsListener,
        ShutdownListener {

	private static final String FILENAME_MODESTATS = "modeChoiceStats";

	final private Population population;
	private ScoreStats scoreStats ;
	private final ControlerConfigGroup controlerConfigGroup;
	private MainModeIdentifier mainModeIdentifier;
	private final static Logger log = Logger.getLogger(org.matsim.analysis.ModeStatsControlerListener.class);


//	final private BufferedWriter modeOut;
//	final private String modeFileName;
//



//	private Integer[] limits = new Integer[]{1, 5, 10};
	private Map<Integer, Map<String, Map<Integer, Double>>> modeHistoryAll = new HashMap<>();


	@Inject
    JRDynamicShutdownControlerListener(ControlerConfigGroup controlerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO,
                                       PlanCalcScoreConfigGroup scoreConfig, AnalysisMainModeIdentifier mainModeIdentifier, ScoreStats scoreStats1) {

		this.controlerConfigGroup = controlerConfigGroup;
		this.population = population1;
		this.scoreStats = scoreStats1 ;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
//		this.minIteration = controlerConfigGroup.getFirstIteration();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		checkDynamicShutdownConditions(event);
	}

	private void checkDynamicShutdownConditions(final IterationEndsEvent event) {

		Map<Integer, Double> scoreHistory = scoreStats.getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average);
		double currentScore = scoreHistory.get(event.getIteration());
		double previousScore = scoreHistory.get(event.getIteration() - 1);
		double pctChange = Math.abs(currentScore - previousScore) / previousScore;

		if (pctChange < 0.1) {
			int shutdownIter = event.getIteration() + 1;
			controlerConfigGroup.setLastIteration(shutdownIter);
			System.err.println("Dynamic Shutdown initiated to following Iteration, "+ shutdownIter);
		}
	}




	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
//		try {
//			this.modeOut.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}
}
