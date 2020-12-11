/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import com.google.inject.Inject;

/**
 * 
 * @author vsp-gleich
 *
 */
public class IntermodalTripFareCompensatorPerDay implements PersonDepartureEventHandler, AfterMobsimListener {
	@Inject
	private EventsManager events;
	@Inject
	private QSimConfigGroup qSimConfigGroup;
	private Set<Id<Person>> personsOnPtTrip = new HashSet<>();
	private Map<Id<Person>, Integer> persons2DrtTrips = new HashMap<>();
	private double compensationMoneyPerDay;
	private double compensationMoneyPerTrip;
	private double compensationScorePerDay;
	private double compensationScorePerTrip;
	private Set<String> drtModes;
	private Set<String> ptModes;
	private double compensationTime = Double.NaN;

	IntermodalTripFareCompensatorPerDay(IntermodalTripFareCompensatorConfigGroup intermodalFareConfigGroup) {
		this.compensationMoneyPerDay = intermodalFareConfigGroup.getCompensationMoneyPerDay();
		this.compensationMoneyPerTrip = intermodalFareConfigGroup.getCompensationMoneyPerTrip();
		this.compensationScorePerDay = intermodalFareConfigGroup.getCompensationScorePerDay();
		this.compensationScorePerTrip = intermodalFareConfigGroup.getCompensationScorePerTrip();
		this.drtModes = intermodalFareConfigGroup.getDrtModes();
		this.ptModes = intermodalFareConfigGroup.getPtModes();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptModes.contains(event.getLegMode())) {
			personsOnPtTrip.add(event.getPersonId());
		}
		if (drtModes.contains(event.getLegMode())) {
			if (!persons2DrtTrips.containsKey(event.getPersonId())) {
				persons2DrtTrips.put(event.getPersonId(), 1);
			} else {
				persons2DrtTrips.put(event.getPersonId(), persons2DrtTrips.get(event.getPersonId()) + 1);
			}
		}
	}

	private void compensateMoney(double time, Id<Person> agentId, double amount) {
		events.processEvent(new PersonMoneyEvent(time, agentId, amount, "intermodalTripFareCompensation", ""));
	}

	private void compensateScore(double time, Id<Person> agentId, double amount) {
		events.processEvent(new PersonScoreEvent(time, agentId, amount, "intermodalTripFareCompensation"));
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		double time = getOrCalcCompensationTime();

		for (Entry<Id<Person>, Integer> person2DrtTrips : persons2DrtTrips.entrySet()) {
			if (personsOnPtTrip.contains(person2DrtTrips.getKey())) {
				compensateMoney(time, person2DrtTrips.getKey(), compensationMoneyPerDay + compensationMoneyPerTrip * person2DrtTrips.getValue());
				compensateScore(time, person2DrtTrips.getKey(), compensationScorePerDay + compensationScorePerTrip * person2DrtTrips.getValue());
			}
		}
	}
	
    @Override
    public void reset(int iteration) {
		// reset for stuck agents after mobsim
		personsOnPtTrip.clear();
		persons2DrtTrips.clear();
    }

	private double getOrCalcCompensationTime() {
		if (Double.isNaN(this.compensationTime)) {
			this.compensationTime = (Double.isFinite(qSimConfigGroup.getEndTime().seconds()) && qSimConfigGroup.getEndTime().seconds() > 0)
					? qSimConfigGroup.getEndTime().seconds()
					: Double.MAX_VALUE;
		}

		return this.compensationTime;
	}

}
