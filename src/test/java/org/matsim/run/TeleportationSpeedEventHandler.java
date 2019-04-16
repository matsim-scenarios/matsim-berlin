/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;

/**
* @author ikaddoura
*/

public class TeleportationSpeedEventHandler implements TeleportationArrivalEventHandler, PersonDepartureEventHandler {

	private final Map<Id<Person>, Double> personId2previousDepartureTime = new HashMap<>();

	private final Map<Id<Person>, List<Double>> personId2teleportationDistances = new HashMap<>();
	private final Map<Id<Person>, List<Double>> personId2teleportationTT = new HashMap<>();
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2previousDepartureTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (personId2teleportationDistances.get(event.getPersonId()) == null) {
			List<Double> teleportationDistances = new ArrayList<>();
			teleportationDistances.add(event.getDistance());
			personId2teleportationDistances.put(event.getPersonId(), teleportationDistances);
		} else {
			personId2teleportationDistances.get(event.getPersonId()).add(event.getDistance());
		}
		
		if (personId2teleportationTT.get(event.getPersonId()) == null) {
			List<Double> teleportationTT = new ArrayList<>();
			teleportationTT.add(event.getTime() - personId2previousDepartureTime.get(event.getPersonId()));
			personId2teleportationTT.put(event.getPersonId(), teleportationTT);
		} else {
			personId2teleportationTT.get(event.getPersonId()).add(event.getTime() - personId2previousDepartureTime.get(event.getPersonId()));
		}
	}

	public Map<Id<Person>, List<Double>> getPersonId2teleportationDistances() {
		return personId2teleportationDistances;
	}
	
	public Map<Id<Person>, List<Double>> getPersonId2teleportationTT() {
		return personId2teleportationTT;
	}
	
}

