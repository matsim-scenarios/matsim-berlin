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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author jfbischoff (SBB)
 */
public class DeParkingModule extends AbstractModule {
	private final static Set<String> PARKING_MODES = Set.of(TransportMode.car, TransportMode.truck, "freight");

	@Override
	public void install() {
		Collection<String> mainModes = switch (getConfig().controller().getMobsim()) {
			case "qsim" -> getConfig().qsim().getMainModes();
			default -> throw new RuntimeException("ParkingCosts are currently supported for Qsim and Hermes");
		};

		for (String mode : PARKING_MODES) {
			if (mainModes.contains(mode)) {
				addEventHandlerBinding().toInstance(new DeParkingTracker(mode, Set.of()));
			} else {
				throw new RuntimeException("Mode " + mode + " not found in main modes: " + mainModes);
			}
		}

		// Add parking analyzer and its event handlers
		bind(ParkingAnalyzer.class).asEagerSingleton();
		bind(ParkingAnalyzer.ParkingInitializerEventsHandler.class).toInstance(new ParkingAnalyzer.ParkingInitializerEventsHandler(PARKING_MODES));
		addControlerListenerBinding().to(ParkingAnalyzer.class);
		addEventHandlerBinding().to(ParkingAnalyzer.ParkingInitializerEventsHandler.class);

		bind(ParkingAnalyzer.ParkingEventHandler.class).toProvider(new ParkingAnalyzer.ParkingEventHandler.Factory(PARKING_MODES)).asEagerSingleton();
		addEventHandlerBinding().to(ParkingAnalyzer.ParkingEventHandler.class);

		// Bind cost and approach
		bind(ParkingCostHistory.class).toProvider(new ParkingCostHistory.Factory(Map.of(), 2 * 3600)).asEagerSingleton();

		bind(DeParkingApproach.class).to(InverseLinearDeParkingApproach.class);
	}
}
