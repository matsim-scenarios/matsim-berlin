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

package org.matsim.run.drt.ptRoutingModes;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.run.drt.ptRoutingModes.PtIntermodalRoutingModesConfigGroup.PersonAttribute2ValuePair;
import org.matsim.run.drt.ptRoutingModes.PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet;

import com.google.common.collect.ImmutableList;

/**
 * 
 * @author vsp-gleich
 *
 */
class PtRoutingModeWrapper implements RoutingModule {
	
	private final RoutingModule ptRouter;
	private final PtIntermodalRoutingModeParameterSet routingModeParams;
	private final ImmutableList<PersonAttribute2ValuePair> personAttribute2ValuePairs;
	
	PtRoutingModeWrapper (PtIntermodalRoutingModeParameterSet routingModeParams, RoutingModule ptRouter) {
		this.routingModeParams = routingModeParams;
		this.personAttribute2ValuePairs = ImmutableList.copyOf(routingModeParams.getPersonAttribute2ValuePairs());
		this.ptRouter = ptRouter;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		for (PersonAttribute2ValuePair personAttribute2ValuePair: personAttribute2ValuePairs) {
			person.getAttributes().putAttribute(personAttribute2ValuePair.getPersonFilterAttribute(), 
					personAttribute2ValuePair.getPersonFilterValue());
		}

		List<? extends PlanElement> route = ptRouter.calcRoute(fromFacility, toFacility, departureTime, person);
		
		for (PersonAttribute2ValuePair personAttribute2ValuePair: personAttribute2ValuePairs) {
			person.getAttributes().removeAttribute(personAttribute2ValuePair.getPersonFilterAttribute());
		}
		
		return route;
	}

}
