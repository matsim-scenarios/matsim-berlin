/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.drt.intermodalTripFareCompensator;

import org.matsim.core.controler.AbstractModule;

public class IntermodalTripFareCompensatorsModule extends AbstractModule {
	@Override
	public void install() {
		IntermodalTripFareCompensatorsConfigGroup intermodalFaresConfigGroup = IntermodalTripFareCompensatorsConfigGroup
				.get(getConfig());
		intermodalFaresConfigGroup.getIntermodalTripFareCompensatorConfigGroups().forEach(intermodalFareConfigGroup -> {
			switch (intermodalFareConfigGroup.getCompensationCondition()) {
			case PtModeUsedInSameTrip:
				IntermodalTripFareCompensatorPerTrip compensatorPerTrip = new IntermodalTripFareCompensatorPerTrip(intermodalFareConfigGroup);
				addEventHandlerBinding().toInstance(compensatorPerTrip);
				break;
			case PtModeUsedAnywhereInTheDay:
				IntermodalTripFareCompensatorPerDay compensatorPerDay = new IntermodalTripFareCompensatorPerDay(intermodalFareConfigGroup);
				addEventHandlerBinding().toInstance(compensatorPerDay);
				addControlerListenerBinding().toInstance(compensatorPerDay);
				break;
			default:
				throw new RuntimeException(
						"unknown CompensationCondition: " + intermodalFareConfigGroup.getCompensationCondition());
			}
		});
	}
}
