/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.run.drt;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;


/**
 * @author Mzhuoxiao
 *
 */

public class DrtUsersEventHandler implements PersonDepartureEventHandler,PersonStuckEventHandler {
	
	private Map<String, Integer> modeToNumberOfLegs = new HashMap<>();
	private Map<Id<Person>,String> person2DrtUsers = new HashMap<>();
	private Map<String, Integer> modeToRejected = new HashMap<>();
	private Map<String, Integer> stuckCnt = new HashMap<>();
	
	private int cntOfDrtTrips;
	private int cntOfDrtUsers;
	private int cntOfDrtStuck;

	@Inject private Network network ;
	
	public DrtUsersEventHandler() {
		// deliberately no arguments in constructor; do not change. kai, jul'19
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		String drt = "drt";
		
		if (event.getLegMode().equals(drt)) {
			this.cntOfDrtTrips = this.cntOfDrtTrips + 1;
			
			if(!this.person2DrtUsers.containsKey(event.getPersonId())) {
				this.person2DrtUsers.put(event.getPersonId(), event.getLegMode());
				this.cntOfDrtUsers = this.cntOfDrtUsers + 1;
			}
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		String mode = event.getLegMode();
		if ( this.stuckCnt.containsKey(mode)) {
			int countSoFar = this.stuckCnt.get(mode);
			this.stuckCnt.put(mode, countSoFar+1);
		} else {
			this.stuckCnt.put(mode, 1);
		}
		if (this.stuckCnt.containsKey("drt")){
			this.cntOfDrtStuck = this.stuckCnt.get("drt");
		} else
			this.cntOfDrtStuck = 0;
		
	}

	int getCntOfDrtTrips() {
		return cntOfDrtTrips;
	}

	int getCntOfDrtUsers() {
		return cntOfDrtUsers;
	}

	int getCntOfDrtStuck() {
		return cntOfDrtStuck;
	}
	
}
