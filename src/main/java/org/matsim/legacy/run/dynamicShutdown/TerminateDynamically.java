
/* *********************************************************************** *
 * project: org.matsim.*
 * TerminateAtFixedIterationNumber.java
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

 package org.matsim.legacy.run.dynamicShutdown;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.TerminationCriterion;

import javax.inject.Inject;

/**
 * Shuts down MATSim, when one of two criteria are satisfied: a) the lastIteration specified in the config file is reached or b) dynamic lastIteration, which can be
 * retrieved from the DynamicShutdownControlerListener is reached (if dynamic shutdown is even initiated).
 *
 * @author jakobrehmann
 */

public class TerminateDynamically implements TerminationCriterion {

	private final int lastIteration;
	private final DynamicShutdownControlerListenerImpl convergenceDynamicShutdown;

	@Inject
	TerminateDynamically(ControllerConfigGroup controlerConfigGroup, DynamicShutdownControlerListenerImpl convergenceDynamicShutdown) {
		this.lastIteration = controlerConfigGroup.getLastIteration();
		this.convergenceDynamicShutdown = convergenceDynamicShutdown;
	}


	//FIXME
	@Override
	public boolean mayTerminateAfterIteration(int iteration) {
		if (convergenceDynamicShutdown.isDynamicShutdownInitiated()) {
			int lastIterationDynamic = convergenceDynamicShutdown.getDynamicShutdownIteration();
			return (iteration >= lastIteration || iteration >= lastIterationDynamic);
		}
		return iteration >= lastIteration ;
	}

	@Override
	public boolean doTerminate(int i) {
		if (convergenceDynamicShutdown.isDynamicShutdownInitiated()) {
			int lastIterationDynamic = convergenceDynamicShutdown.getDynamicShutdownIteration();
			return (i >= lastIteration || i >= lastIterationDynamic);
		}
		return i >= lastIteration ;
	}
}
