/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.run.deparking;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jfbischoff (SBB)
 */
public class DeParkingModule extends AbstractModule {
//	private final static Set<String> PARKING_MODES = Set.of(TransportMode.car, TransportMode.truck, "freight", RunAutofreiPolicy.NEW_MODE_SMALL_SCALE_COMMERCIAL);


	public DeParkingModule() {
	}

	public DeParkingModule(int writeInterval) {
		this.writeInterval = writeInterval;
	}

	private int writeInterval = 50;

	@Override
	public void install() {
//		for (String mode : PARKING_MODES) {
//			if (mainModes.contains(mode)) {
//				addEventHandlerBinding().toInstance(new DeParkingTracker(mode, Set.of()));
//			} else {
//				throw new RuntimeException("Mode " + mode + " not found in main modes: " + mainModes);
//			}
//		}

		// Add DeParkingTracker only for car, but track all parking modes
		addEventHandlerBinding().toInstance(new DeParkingTracker(TransportMode.car, Set.of()));

		Set<String> mainModes = new HashSet<>(this.getConfig().qsim().getMainModes());

		// Add parking analyzer and its event handlers
		ParkingAnalyzer analyzer = new ParkingAnalyzer(writeInterval);
		bind(ParkingAnalyzer.class).toInstance(analyzer);
		bind(ParkingInitializerEventsHandler.class).toInstance(new ParkingInitializerEventsHandler(mainModes));
		addControllerListenerBinding().to(ParkingAnalyzer.class);
		addEventHandlerBinding().to(ParkingInitializerEventsHandler.class);

		bind(ParkingEventHandler.class).toProvider(new ParkingEventHandler.Factory(mainModes)).asEagerSingleton();
		addEventHandlerBinding().to(ParkingEventHandler.class);

		// Bind cost and approach
		bind(ParkingCostTracker.class).toProvider(new ParkingCostTracker.Factory(Map.of(), 2 * 3600, writeInterval)).asEagerSingleton();
		addControllerListenerBinding().to(ParkingCostTracker.class);
		bind(DeParkingApproach.class).to(InverseLinearDeParkingApproach.class);
	}
}
