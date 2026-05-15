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
import org.apache.commons.math3.util.Precision;
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author mrieser, jfbischoff (SBB)
 */
public class DeParkingTracker implements ActivityStartEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, MobsimAfterSimStepListener {

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
	@Inject
	Config config;

	public DeParkingTracker(String mode, Set<String> untrackedActivities) {
		this.untrackedActivities = untrackedActivities;
		this.trackedMode = mode;
		this.purpose = mode + " parking cost";
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (!event.getNetworkMode().equals(trackedMode)) {
			return;
		}

		// this is correct if the subtour mode choice has coordDistance > 0. The link will be taken where the vehicle was parked, not where this event happens.
		ParkingInfo pi = this.parkingPerVehicle.remove(event.getVehicleId());

		if (pi == null) {
			DeparkingConfigGroup deparkingConfigGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);

			// this is the first time a vehicle leave was registered
			if (deparkingConfigGroup.getFirstAndLastParkingCharging() == DeparkingConfigGroup.FirstAndLastParkingCharging.CHARGE_BOTH) {
				// charge the parking cost at the current link
				pi = new ParkingInfo(event.getLinkId(), event.getPersonId(), 0, false);
			} else {
				// otherwise don't charge anything
				return;
			}
		}

		chargeParking(event.getTime(), pi);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (!event.getNetworkMode().equals(trackedMode)) {
			return;
		}

		ParkingInfo pi = new ParkingInfo(event.getLinkId(), event.getPersonId(), event.getTime(), false);
		this.parkingPerVehicle.put(event.getVehicleId(), pi);
		this.lastVehiclePerDriver.put(event.getPersonId(), event.getVehicleId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (this.untrackedActivities.stream().anyMatch(s -> event.getActType().contains(s))) {
			Id<Vehicle> vehicleId = this.lastVehiclePerDriver.get(event.getPersonId());
			if (vehicleId != null) {
				ParkingInfo parkingInfo = this.parkingPerVehicle.get(vehicleId);
				parkingInfo.ignore = true;
				this.parkingPerVehicle.put(vehicleId, parkingInfo);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.parkingPerVehicle.clear();
		this.lastVehiclePerDriver.clear();
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		DeparkingConfigGroup deparkingConfigGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		if (!(deparkingConfigGroup.getFirstAndLastParkingCharging() == DeparkingConfigGroup.FirstAndLastParkingCharging.CHARGE_BOTH)) {
			return;
		}

		if (Precision.equals(config.qsim().getEndTime().seconds(), e.getSimulationTime(), 0.001)) {
			for (ParkingInfo info : this.parkingPerVehicle.values()) {
				chargeParking(e.getSimulationTime(), info);
			}
		}
	}

	private void chargeParking(double now, ParkingInfo pi) {
		if (pi.ignore) {
			return;
		}

		Link link = network.getLinks().get(pi.parkingLinkId);
		double parkDuration = now - pi.startParkingTime;

		if (parkDuration == 0) {
			// this in particular the case if a parking start time happens in the last step
			return;
		}

		double hourlyParkingCost = parkingCostTracker.cost(link.getId(), pi.startParkingTime);
		double parkingCost = hourlyParkingCost * (parkDuration / 3600.0);

		// emit fewer events
		if (parkingCost > 0) {
			this.events.processEvent(new PersonMoneyEvent(now, pi.driverId, -parkingCost, purpose, null, link.getId().toString()));
		}
	}

	private static class ParkingInfo {
		Id<Link> parkingLinkId;
		Id<Person> driverId;
		double startParkingTime;
		boolean ignore;

		public ParkingInfo(Id<Link> parkingLinkId, Id<Person> driverId, double startParkingTime, boolean ignore) {
			this.parkingLinkId = parkingLinkId;
			this.driverId = driverId;
			this.startParkingTime = startParkingTime;
			this.ignore = ignore;
		}
	}
}
