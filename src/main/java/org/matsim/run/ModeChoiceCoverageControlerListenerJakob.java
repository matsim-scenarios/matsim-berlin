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
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Calculates at the end of each iteration mode statistics, based on the main mode identifier of a trip chain.
 * For multi-modal trips, this is only as accurate as your main mode identifier.
 * The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author mrieser
 */
public class ModeChoiceCoverageControlerListenerJakob implements StartupListener, IterationEndsListener,
        ShutdownListener {

	private static final String FILENAME_MODESTATS = "modeChoiceStats";

	final private Population population;

	final private BufferedWriter modeOut;
	final private String modeFileName;

	private final boolean createPNG;
	private final ControlerConfigGroup controlerConfigGroup;


	private Integer[] limits = new Integer[]{1, 5, 10};
	private Map<Integer, Map<String, Map<Integer, Double>>> modeHistoryAll = new HashMap<>();
	//      Map<Limit  , Map<mode  , Map<iter   , count >>>
//	private Map<String, Map<Integer, Double>> modeHistoriesOne = new HashMap<>();
//	private Map<String, Map<Integer, Double>> modeHistoriesFive = new HashMap<>();
//	private Map<String, Map<Integer, Double>> modeHistoriesTen = new HashMap<>();
	private int minIteration = 0;
	private MainModeIdentifier mainModeIdentifier;
//	private Map<String, Double> modeCnt = new TreeMap<>() ;

	private final Set<String> modes;

	private final static Logger log = Logger.getLogger(org.matsim.analysis.ModeStatsControlerListener.class);


	//jr
	private Map<Id<Person>, Map<Integer, Map<String, Integer>>> megaMap = new LinkedHashMap<>();
	// Map (      Person,    Map (Trip #,Map (Mode,  Count)))

	private Map<Integer, Map<String, Double>> modeCountCurrentIteration = new TreeMap<>();
	// Map     <limit  , Map<mode  , count >>
//	private Map<String, Double> modeUsedOneTime = new TreeMap<>();
//	private Map<String, Double> modeUsedFiveTimes = new TreeMap<>();
//	private Map<String, Double> modeUsedTenTimes = new TreeMap<>();


	// testfewagentsonly

	@Inject
	ModeChoiceCoverageControlerListenerJakob(ControlerConfigGroup controlerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO,
											 PlanCalcScoreConfigGroup scoreConfig, AnalysisMainModeIdentifier mainModeIdentifier) {

		this.controlerConfigGroup = controlerConfigGroup;
		this.population = population1;
		this.modeFileName = controlerIO.getOutputFilename(FILENAME_MODESTATS);
//		this.createPNG = controlerConfigGroup.isCreateGraphs();
		this.createPNG = true; //jr
		this.modeOut = IOUtils.getBufferedWriter(this.modeFileName + ".txt"); //jr
		try {
			this.modeOut.write("Iteration");
			this.modes = new TreeSet<>();
			this.modes.addAll(scoreConfig.getAllModes());
			for (String mode : modes) {
				this.modeOut.write("\t" + mode);
			}
			this.modeOut.write("\n");
			;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.minIteration = controlerConfigGroup.getFirstIteration();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		collectModeShareInfo(event);
	}

	private void collectModeShareInfo(final IterationEndsEvent event) {

		// Counts mode instances for current iteration
		for (Person person : this.population.getPersons().values()) {

			Map<Integer, Map<String, Integer>> mapForPerson = megaMap.computeIfAbsent(person.getId(), v -> new LinkedHashMap<>());

			Plan plan = person.getSelectedPlan();
			Integer tripNumber = 0;
			for (Trip trip : TripStructureUtils.getTrips(plan)) {

				tripNumber++;
				String mode = this.mainModeIdentifier.identifyMainMode(trip.getTripElements());

				Map<String, Integer> mapForPersonTrip = mapForPerson.computeIfAbsent(tripNumber, v -> new HashMap<>());

				Integer modeCount = mapForPersonTrip.computeIfAbsent(mode, v -> 0);
				mapForPersonTrip.put(mode, modeCount + 1);

				mapForPerson.put(tripNumber, mapForPersonTrip); // close map for person trip
			}
			megaMap.put(person.getId(), mapForPerson); // reinsert map for person
		}


		// Counts for how many person-trips, the mode count exceeds the predefined limits.
		int totalPersonTripCount = 0;
		modeCountCurrentIteration.clear();
		for (Map<Integer, Map<String, Integer>> mapForPerson : megaMap.values()) {
			for (Map<String, Integer> mapForPersonTrip : mapForPerson.values()) {
				totalPersonTripCount++;
				for (String mode : mapForPersonTrip.keySet()) {
					Integer realCount = mapForPersonTrip.get(mode);
					for (Integer limit : limits) {
						Map<String, Double> modeCountMap = modeCountCurrentIteration.computeIfAbsent(limit, k -> new TreeMap<>());
						Double modeCount = modeCountMap.computeIfAbsent(mode, k -> 0.);
						if (realCount >= limit) {
							modeCount++;
						}
						modeCountMap.put(mode, modeCount);
						modeCountCurrentIteration.put(limit, modeCountMap);
					}
				}
			}
		}


		double sum = totalPersonTripCount;

		for (Integer limit : limits) {
			Map<String, Double> modeCnt = modeCountCurrentIteration.get(limit);
			Map<String, Map<Integer, Double>> modeIterationShareMap = modeHistoryAll.computeIfAbsent(limit, k -> new HashMap<>());

			try {
				this.modeOut.write(String.valueOf(event.getIteration()) + " (" + limit + "x)");
				log.info("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
				for (String mode : modes) {
					Double cnt = modeCnt.get(mode);
					double share = 0.;
					if (cnt != null) {
						share = cnt / sum;
					}
					log.info("-- mode choice coverage (" + limit + "x) of mode " + mode + " = " + share);
					this.modeOut.write("\t" + share);

					Map<Integer, Double> IterationShareMap = modeIterationShareMap.computeIfAbsent(mode, k -> new TreeMap<>());
					IterationShareMap.put(event.getIteration(), share);

				}
				this.modeOut.write("\n");
				this.modeOut.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Produces Graphs
		if (this.createPNG && event.getIteration() > this.minIteration) {
			for (Integer limit : limits) {
				XYLineChart chart = new XYLineChart("Mode Choice Coverage Statistics (Mode Used >= " + limit + "x)", "iteration", "mode choice coverage");
				for (Entry<String, Map<Integer, Double>> entry : modeHistoryAll.get(limit).entrySet()) {
					String mode = entry.getKey();
					Map<Integer, Double> history = entry.getValue();
					chart.addSeries(mode, history);
				}
				chart.addMatsimLogo();
				chart.saveAsPng(this.modeFileName + limit+ "x" + ".png", 800, 600);
			}
		}
	}

//		// One Time
//		{
//
//			modeCnt = modeUsedOneTime;
//			Map<String, Map<Integer, Double>> modeHistoriesGeneric = modeHistoriesOne;
//			try {
//				this.modeOut.write(String.valueOf(event.getIteration()) + " (1x)");
//				log.info("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
//				for (String mode : modes) {
//					Double cnt = this.modeCnt.get(mode);
//					double share = 0.;
//					if (cnt != null) {
//						share = cnt / sum;
//					}
//					log.info("-- mode share of mode " + mode + " = " + share);
//					this.modeOut.write("\t" + share);
//
//					Map<Integer, Double> modeHistory = modeHistoriesGeneric.computeIfAbsent(mode, k -> new TreeMap<>());
//					modeHistory.put(event.getIteration(), share);
//
//				}
//				this.modeOut.write("\n");
//				this.modeOut.flush();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//
//
//			// yyyy the following does not work!!
//			// Why? The charts seem to be useful (JB, April 2017)
//			if (this.createPNG && event.getIteration() > this.minIteration) {
//				// create chart when data of more than one iteration is available.
//				XYLineChart chart = new XYLineChart("Mode Choice Coverage Statistics (Mode Used >= 1x)", "iteration", "mode choice coverage");
//				for (Entry<String, Map<Integer, Double>> entry : modeHistoriesGeneric.entrySet()) {
//					String mode = entry.getKey();
//					Map<Integer, Double> history = entry.getValue();
////				log.warn( "about to add the following series:" ) ;
////				for ( Entry<Integer, Double> item : history.entrySet() ) {
////					log.warn( item.getKey() + " -- " + item.getValue() );
////				}
//					chart.addSeries(mode, history);
//				}
//				chart.addMatsimLogo();
//				chart.saveAsPng(this.modeFileName + "One" + ".png", 800, 600);
//			}
//			modeHistoriesOne = modeHistoriesGeneric;
//			modeCnt.clear();
//		}
//
//        // Five Times
//		{
//			modeCnt = modeUsedFiveTimes;
//			Map<String, Map<Integer, Double>> modeHistoriesGeneric = modeHistoriesFive ;
//			try {
//				this.modeOut.write(String.valueOf (event.getIteration())+   " (5x)");
//				log.info("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
//				for (String mode : modes) {
//					Double cnt = this.modeCnt.get(mode);
//					double share = 0.;
//					if (cnt != null) {
//						share = cnt / sum;
//					}
//					log.info("-- mode share of mode " + mode + " = " + share);
//					this.modeOut.write("\t" + share);
//
//					Map<Integer, Double> modeHistory = modeHistoriesGeneric.get(mode);
//					if (modeHistory == null) {
//						modeHistory = new TreeMap<>();
//						modeHistoriesGeneric.put(mode, modeHistory);
//					}
//					modeHistory.put(event.getIteration(), share);
//
//				}
//				this.modeOut.write("\n");
//				this.modeOut.flush();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//
//
//			// yyyy the following does not work!!
//			// Why? The charts seem to be useful (JB, April 2017)
//			if (this.createPNG && event.getIteration() > this.minIteration) {
//				// create chart when data of more than one iteration is available.
//				XYLineChart chart = new XYLineChart("Mode Choice Coverage Statistics (Mode Used >= 5x)", "iteration", "mode choice coverage");
//				for (Entry<String, Map<Integer, Double>> entry : modeHistoriesGeneric.entrySet()) {
//					String mode = entry.getKey();
//					Map<Integer, Double> history = entry.getValue();
////				log.warn( "about to add the following series:" ) ;
////				for ( Entry<Integer, Double> item : history.entrySet() ) {
////					log.warn( item.getKey() + " -- " + item.getValue() );
////				}
//					chart.addSeries(mode, history);
//				}
//				chart.addMatsimLogo();
//				chart.saveAsPng(this.modeFileName + "Five" + ".png", 800, 600);
//			}
//			modeHistoriesFive = modeHistoriesGeneric ;
//			modeCnt.clear();
//		}
//
//
//		// Ten Times
//		{
//			modeCnt = modeUsedTenTimes;
//			Map<String, Map<Integer, Double>> modeHistoriesGeneric = modeHistoriesTen ;
//			try {
//				this.modeOut.write(String.valueOf (event.getIteration()) + " (10x)");
//				log.info("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
//				for (String mode : modes) {
//					Double cnt = this.modeCnt.get(mode);
//					double share = 0.;
//					if (cnt != null) {
//						share = cnt / sum;
//					}
//					log.info("-- mode share of mode " + mode + " = " + share);
//					this.modeOut.write("\t" + share);
//
//					Map<Integer, Double> modeHistory = modeHistoriesGeneric.get(mode);
//					if (modeHistory == null) {
//						modeHistory = new TreeMap<>();
//						modeHistoriesGeneric.put(mode, modeHistory);
//					}
//					modeHistory.put(event.getIteration(), share);
//
//				}
//				this.modeOut.write("\n");
//				this.modeOut.flush();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//
//
//			// yyyy the following does not work!!
//			// Why? The charts seem to be useful (JB, April 2017)
//			if (this.createPNG && event.getIteration() > this.minIteration) {
//				// create chart when data of more than one iteration is available.
//				XYLineChart chart = new XYLineChart("Mode Choice Coverage Statistics (Mode Used >= 10x)", "iteration", "mode choice coverage");
//				for (Entry<String, Map<Integer, Double>> entry : modeHistoriesGeneric.entrySet()) {
//					String mode = entry.getKey();
//					Map<Integer, Double> history = entry.getValue();
////				log.warn( "about to add the following series:" ) ;
////				for ( Entry<Integer, Double> item : history.entrySet() ) {
////					log.warn( item.getKey() + " -- " + item.getValue() );
////				}
//					chart.addSeries(mode, history);
//				}
//				chart.addMatsimLogo();
//				chart.saveAsPng(this.modeFileName + "Ten" + ".png", 800, 600);
//			}
//			modeHistoriesTen = modeHistoriesGeneric ;
//			modeCnt.clear();
//		}
//	}



	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.modeOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
