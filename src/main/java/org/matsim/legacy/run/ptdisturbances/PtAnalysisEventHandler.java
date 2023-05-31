/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
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


package org.matsim.legacy.run.ptdisturbances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypeIdentifier;

/**
* @author smueller
*/

public class PtAnalysisEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler {

	private static Map<Id<Person>, List<Event>> personMap = new HashMap<>();

	@Override
	public void handleEvent(ActivityEndEvent event) {

		if (StageActivityTypeIdentifier.isStageActivity(event.getActType()) == false) {
			if (personMap.containsKey(event.getPersonId())) {
				List<Event> eventList = personMap.get(event.getPersonId());
				eventList.add(event);
			}

			else {
				List<Event> eventList = new ArrayList<>();
				eventList.add(event);
				personMap.putIfAbsent(event.getPersonId(), eventList );
			}
		}


	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		if (StageActivityTypeIdentifier.isStageActivity(event.getActType()) == false) {
			List<Event> eventList = personMap.get(event.getPersonId());
			eventList.add(event);
		}


	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		if (event.getPersonId().toString().startsWith("pt_pt") == false) {
			List<Event> eventList = personMap.get(event.getPersonId());
			eventList.add(event);
		}


	}

	public static Map<Id<Person>, List<Event>> getPersonMap(){
		return personMap;
	}

}

