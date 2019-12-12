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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.StageActivityTypeIdentifier;

import com.google.inject.Inject;

/**
 * 
 * This class assumes there can be at most 1 drt leg before a pt leg in a trip and at most 1 drt leg after. Further drt
 * legs will not be compensated! This is for performance reasons (set faster than Map)
 * 
 * @author vsp-gleich
 *
 */
public class PtDrtIntermodalTripDrtFareCompensator implements PersonDepartureEventHandler, ActivityStartEventHandler, AfterMobsimListener {
	@Inject private EventsManager events;
	Set<Id<Person>> personsOnPtTrip = new HashSet<>();
	Map<Id<Person>, String> person2DrtModeOnTrip = new HashMap<>();
	Map<String, Double> drtMode2Compensation;
	Set<String> ptModes;
	
	PtDrtIntermodalTripDrtFareCompensator (Map<String, Double> drtMode2Compensation, Set<String> ptModes) {
		this.drtMode2Compensation = drtMode2Compensation;
		this.ptModes = ptModes;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptModes.contains(event.getLegMode())) {
			personsOnPtTrip.add(event.getPersonId());
			
			String drtMode = person2DrtModeOnTrip.get(event.getPersonId());
 			if (drtMode != null) {
				// drt before pt case: compensate here when pt leg follows
				compensate(event.getTime(), event.getPersonId(), drtMode2Compensation.get(drtMode));
			}
		}
		if (drtMode2Compensation.containsKey(event.getLegMode())) {
			if (personsOnPtTrip.contains(event.getPersonId())) {
				// drt after pt case: compensate immediately
				compensate(event.getTime(), event.getPersonId(), drtMode2Compensation.get(event.getLegMode()));
			} else {
				// drt before pt case: compensate later _if_ pt leg follows
				person2DrtModeOnTrip.put(event.getPersonId(), event.getLegMode());
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!StageActivityTypeIdentifier.isStageActivity(event.getEventType())) {
			// reset after trip has finished
			personsOnPtTrip.remove(event.getPersonId());
			person2DrtModeOnTrip.remove(event.getPersonId());
		}
		
	}
	
	private void compensate(double time, Id<Person> agentId, double amount) {
		events.processEvent(new PersonMoneyEvent(time, agentId, amount));
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// reset for stuck agents after mobsim
		personsOnPtTrip.clear();
		person2DrtModeOnTrip.clear();
	}
	
}
