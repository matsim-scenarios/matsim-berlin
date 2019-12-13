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

package org.matsim.run.drt;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PtRoutingModeWrapper implements RoutingModule {
	
//	@Inject @Named(TransportMode.pt) RoutingModule ptRouter; // This does not work, is always null
	private final RoutingModule ptRouter;
	private final List<Tuple<String,String>> personAttributes2Values;
	
	@Inject
	PtRoutingModeWrapper (List<Tuple<String,String>> personAttributes2Values, @Named(TransportMode.pt) RoutingModule ptRouter) {
		this.personAttributes2Values = personAttributes2Values;
		this.ptRouter = ptRouter;
		System.err.println(ptRouter.toString());
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		for (Tuple<String,String> attribute2value: personAttributes2Values) {
			person.getAttributes().putAttribute(attribute2value.getFirst(), attribute2value.getSecond());
		}

		List<? extends PlanElement> route = ptRouter.calcRoute(fromFacility, toFacility, departureTime, person);
		
		for (Tuple<String,String> attribute2value: personAttributes2Values) {
			person.getAttributes().removeAttribute(attribute2value.getFirst());
		}
		
		return route;
	}

}
