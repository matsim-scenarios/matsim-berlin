/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.run.deparking;


import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author mrieser, jfbischoff (SBB)
 */
public class DeParkingTracker implements ActivityStartEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(DeParkingTracker.class);
	private final Map<Id<Vehicle>, ParkingInfo> parkingPerVehicle = new HashMap<>();
	private final Map<Id<Person>, Id<Vehicle>> lastVehiclePerDriver = new HashMap<>();
	private final String trackedMode;
	private final Set<String> untrackedActivities;
	private final String purpose;

	@Inject
	EventsManager events;
	@Inject
	Network network;
	@Inject
	ParkingCostTracker parkingCostTracker;

	public DeParkingTracker(String mode, Set<String> untrackedActivities) {
		this.untrackedActivities = untrackedActivities;
		this.trackedMode = mode;
		this.purpose = mode + " parking cost";
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (event.getNetworkMode().equals(trackedMode)) {
			ParkingInfo pi = this.parkingPerVehicle.remove(event.getVehicleId());
			if (pi == null) {
				return;
			}
			Link link = network.getLinks().get(pi.parkingLinkId);
			double parkDuration = event.getTime() - pi.startParkingTime;
			double hourlyParkingCost = parkingCostTracker.cost(link.getId(), pi.startParkingTime);
			double parkingCost = hourlyParkingCost * (parkDuration / 3600.0);
			this.events.processEvent(new PersonMoneyEvent(event.getTime(), pi.driverId, -parkingCost, purpose, null, link.getId().toString()));
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (event.getNetworkMode().equals(trackedMode)) {
			ParkingInfo pi = new ParkingInfo(event.getLinkId(), event.getPersonId(), event.getTime());
			this.parkingPerVehicle.put(event.getVehicleId(), pi);
			this.lastVehiclePerDriver.put(event.getPersonId(), event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (this.untrackedActivities.stream().anyMatch(s -> event.getActType().contains(s))) {
			Id<Vehicle> vehicleId = this.lastVehiclePerDriver.get(event.getPersonId());
			if (vehicleId != null) {
				this.parkingPerVehicle.remove(vehicleId);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.parkingPerVehicle.clear();
		this.lastVehiclePerDriver.clear();
	}

	private record ParkingInfo(Id<Link> parkingLinkId, Id<Person> driverId, double startParkingTime) {
	}
}
