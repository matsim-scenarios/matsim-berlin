/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

public class SubtourModeChoiceRepairReRoute implements Provider<PlanStrategy> {

	@Inject private Provider<TripRouter> tripRouterProvider;
	@Inject private ActivityFacilities facilities;
	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new SubtourModeChoiceModule(tripRouterProvider, globalConfigGroup, subtourModeChoiceConfigGroup));
		strategy.addStrategyModule(new RepairReRouteModule(facilities, tripRouterProvider, globalConfigGroup));
		return strategy;
	}

}
