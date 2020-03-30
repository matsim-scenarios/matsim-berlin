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

package org.matsim.run.dynamicShutdown;

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
public class ModeChoiceCoverageControlerListener implements StartupListener, IterationEndsListener,
        ShutdownListener {

	private static final String FILENAME_MODESTATS = "modeChoiceStats";

	final private Population population;

	final private Map<Integer, BufferedWriter> modeOutMap = new HashMap<>();
	final private String modeFileName;

	private final boolean createPNG;
	private final ControlerConfigGroup controlerConfigGroup;


	private Integer[] limits = new Integer[]{1, 5, 10};
	private static Map<Integer, Map<String, Map<Integer, Double>>> modeHistoryAll = new HashMap<>();
	private Map<Id<Person>, Map<Integer, Map<String, Integer>>> megaMap = new LinkedHashMap<>();
	// Map (      Person,    Map (Trip #,Map (Mode,  Count)))

	private int minIteration = 0;
	private MainModeIdentifier mainModeIdentifier;

	private final Set<String> modes;

	private final static Logger log = Logger.getLogger(org.matsim.analysis.ModeStatsControlerListener.class);




	@Inject
	ModeChoiceCoverageControlerListener(ControlerConfigGroup controlerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO,
										PlanCalcScoreConfigGroup scoreConfig, AnalysisMainModeIdentifier mainModeIdentifier) {

		this.controlerConfigGroup = controlerConfigGroup;
		this.population = population1;
		this.modeFileName = controlerIO.getOutputFilename(FILENAME_MODESTATS);
//		this.createPNG = controlerConfigGroup.isCreateGraphs();
		this.createPNG = true; //jr

		this.modes = new TreeSet<>();
		this.modes.addAll(scoreConfig.getAllModes());
		for (Integer limit : limits){
			BufferedWriter modeOut = IOUtils.getBufferedWriter(this.modeFileName  + limit + "x.txt"); //jr
			try {
				modeOut.write("Iteration");

				for (String mode : modes) {
					modeOut.write("\t" + mode);
				}
				modeOut.write("\n");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			this.modeOutMap.put(limit, modeOut);
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
		/*
		 *  megaMap: for each person-trip, how many times (iterations) was each mode used. The following code adds the
		 * 	mode information from the current iteration to the megaMap.
		 */

		updateMegaMapWithCurrentIteration();


		/*
		 * 		 Looks through megaMap at each person-trip. How many of those person trips have used the each mode more than the
		 * 		 predefined limits.
		 */
		int totalPersonTripCount = 0;
		Map<Integer, Map<String, Double>> modeCountCurrentIteration = new TreeMap<>();

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
			BufferedWriter modeOut = modeOutMap.get(limit);
			try {
				modeOut.write(event.getIteration()+"");
				log.info("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
				for (String mode : modes) {
					Double cnt = modeCnt.get(mode);
					double share = 0.;
					if (cnt != null) {
						share = cnt / sum;
					}
					log.info("-- mode choice coverage (" + limit + "x) of mode " + mode + " = " + share);
					modeOut.write("\t" + share);

					Map<Integer, Double> iterationShareMap = modeIterationShareMap.computeIfAbsent(mode, k -> new TreeMap<>());
					iterationShareMap.put(event.getIteration(), share);

				}
				modeOut.write("\n");
				modeOut.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Produces Graphs
		if (this.createPNG && event.getIteration() > this.minIteration) {
			produceGraphs();
		}
	}

	private void updateMegaMapWithCurrentIteration() {
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
	}

	private void produceGraphs() {
		for (Integer limit : limits) {
			XYLineChart chart = new XYLineChart("Mode Choice Coverage Statistics (Mode Used >= " + limit + "x per trip)", "iteration", "mode choice coverage");
			for (Entry<String, Map<Integer, Double>> entry : modeHistoryAll.get(limit).entrySet()) {
				String mode = entry.getKey();
				Map<Integer, Double> history = entry.getValue();
				chart.addSeries(mode, history);
			}
			chart.addMatsimLogo();
			chart.saveAsPng(this.modeFileName + limit+ "x" + ".png", 800, 600);
		}
	}


	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {

		for (BufferedWriter modeOut : modeOutMap.values()) {
			try {
				modeOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


	}



	public static Map<Integer, Map<String, Map<Integer, Double>>> getModeHistory() {
		return modeHistoryAll;
	}

}
