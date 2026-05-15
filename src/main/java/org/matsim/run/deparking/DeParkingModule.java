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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jfbischoff (SBB)
 */
public class DeParkingModule extends AbstractModule {
	public DeParkingModule() {
	}

	@Override
	public void install() {
		// Add DeParkingTracker only for car, but track all parking modes
		DeParkingTracker tracker = new DeParkingTracker(TransportMode.car, Set.of());
		addEventHandlerBinding().toInstance(tracker);
		addMobsimListenerBinding().toInstance(tracker);

		Set<String> mainModes = new HashSet<>(this.getConfig().qsim().getMainModes());

		// Add parking analyzer and its event handlers
		ParkingAnalyzer analyzer = new ParkingAnalyzer();
		bind(ParkingAnalyzer.class).toInstance(analyzer);
		bind(ParkingInitializerEventsHandler.class).toInstance(new ParkingInitializerEventsHandler(mainModes));
		addControllerListenerBinding().to(ParkingAnalyzer.class);
		addEventHandlerBinding().to(ParkingInitializerEventsHandler.class);

		bind(ParkingEventHandler.class).toProvider(new ParkingEventHandler.Factory(mainModes)).asEagerSingleton();
		addEventHandlerBinding().to(ParkingEventHandler.class);

		// Bind cost and approach
		bind(ParkingCostTracker.class).toProvider(new ParkingCostTracker.Factory(Map.of(), 2 * 3600)).asEagerSingleton();
		addControllerListenerBinding().to(ParkingCostTracker.class);

		DeparkingConfigGroup.DeParkingApproachType deParkingApproachType = ConfigUtils.addOrGetModule(this.getConfig(), DeparkingConfigGroup.class).getDeParkingApproachType();
		switch (deParkingApproachType) {
			case INVERSE_LINEAR -> bind(DeParkingApproach.class).to(InverseLinearDeParkingApproach.class);
			case PDI -> bind(DeParkingApproach.class).to(PdiDeparkingApproach.class);
		}
	}
}
