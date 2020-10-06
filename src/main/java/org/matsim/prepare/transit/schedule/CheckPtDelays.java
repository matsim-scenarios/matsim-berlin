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

package org.matsim.prepare.transit.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author vsp-gleich
 *
 */
public class CheckPtDelays {
	private static final Logger log = Logger.getLogger(CheckPtDelays.class);
	private final String eventsFile;
	private final String scheduleFile;
	private Scenario scenario;
	private List<DelayRecord> allDelays = new ArrayList<>();
	private Map<Id<Vehicle>, DelayRecord> veh2minDelay = new HashMap<>();
	private Map<Id<Vehicle>, DelayRecord> veh2maxDelay = new HashMap<>();
	private Map<Id<TransitLine>, DelayRecord> line2minDelay = new TreeMap<>();
	private Map<Id<TransitLine>, DelayRecord> line2maxDelay = new TreeMap<>();
	private Map<Id<Vehicle>, Id<TransitRoute>> vehId2RouteId = new HashMap<>();
	private Map<Id<TransitRoute>, Id<TransitLine>> transitRouteId2TransitLineId = new HashMap<>();
	
	CheckPtDelays (String eventsFile, String scheduleFile) {
		this.eventsFile = eventsFile;
		this.scheduleFile = scheduleFile;
	}

	public static void main(String[] args) {
		String eventsFile = "scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/ITERS/it.0/berlin-v5.5-1pct.0.events.xml.gz";
		String scheduleFile = "transitSchedule.xml.gz";
		
		CheckPtDelays runner = new CheckPtDelays(eventsFile, scheduleFile);
		runner.run();
		
		log.info(runner.minDelayPerTransitLine());
		log.info(runner.maxDelayPerTransitLine());
	}
	
	void run() {
		// read TransitSchedule in order to sum up results by TransitLine
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleReader tsReader = new TransitScheduleReader(scenario);
		tsReader.readFile(scheduleFile);
		
		for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
			
			for (TransitRoute route: line.getRoutes().values()) {
				transitRouteId2TransitLineId.put(route.getId(), line.getId());
				
				for (Departure dep: route.getDepartures().values()) {
					vehId2RouteId.put(dep.getVehicleId(), route.getId());
				}
			}
		}
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		VehicleDelayEventHandler delayHandler = this.new VehicleDelayEventHandler();
		eventsManager.addHandler(delayHandler);
		eventsManager.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
		eventsManager.finishProcessing();
		aggregatePerTransitLine();
	}
	
	// it seems that VehicleArrivesAtFacility and VehicleDepartsAtFacility evnt types are only used for transit vehicles,
	// because both have TransitStopFacility
	private class VehicleDelayEventHandler implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			Id<Vehicle> vehId = event.getVehicleId();
			Id<TransitRoute> routeId = vehId2RouteId.get(vehId);
			Id<TransitLine> lineId = transitRouteId2TransitLineId.get(routeId);
			DelayRecord delay = new DelayRecord(lineId, routeId, vehId, event.getFacilityId(), event.getDelay());
			
			if (Double.isFinite(delay.delay)) {
				allDelays.add(delay);
				if (!veh2minDelay.containsKey(event.getVehicleId())) veh2minDelay.put(event.getVehicleId(), delay);
				if (!veh2maxDelay.containsKey(event.getVehicleId())) veh2maxDelay.put(event.getVehicleId(), delay);
				if (delay.delay > 0) {
					if (delay.delay > veh2maxDelay.get(event.getVehicleId()).delay){
						veh2maxDelay.put(event.getVehicleId(), delay);
					}
				} else {
					if (delay.delay < veh2minDelay.get(event.getVehicleId()).delay){
						veh2minDelay.put(event.getVehicleId(), delay);
					}
				}
			}
		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
			Id<Vehicle> vehId = event.getVehicleId();
			Id<TransitRoute> routeId = vehId2RouteId.get(vehId);
			Id<TransitLine> lineId = transitRouteId2TransitLineId.get(routeId);
			DelayRecord delay = new DelayRecord(lineId, routeId, vehId, event.getFacilityId(), event.getDelay());
			
			if (Double.isFinite(delay.delay)) {
				allDelays.add(delay);
				if (!veh2maxDelay.containsKey(event.getVehicleId())) veh2maxDelay.put(event.getVehicleId(), delay);
				if (!veh2minDelay.containsKey(event.getVehicleId())) veh2minDelay.put(event.getVehicleId(), delay);
				if (delay.delay > 0) {
					if (delay.delay > veh2maxDelay.get(event.getVehicleId()).delay){
						veh2maxDelay.put(event.getVehicleId(), delay);
					}
				} else {
					if (delay.delay < veh2minDelay.get(event.getVehicleId()).delay){
						veh2minDelay.put(event.getVehicleId(), delay);
					}
				}
			}
		}
	}
	
	/**
	 * This assumes that each TransitVehicle is used on exactly one TransitLine, never on multiple lines
	 */
	private void aggregatePerTransitLine() {
		for (Entry<Id<Vehicle>, DelayRecord> entry: veh2maxDelay.entrySet()) {
			Id<TransitLine> lineId = entry.getValue().lineId;
			if (!line2maxDelay.containsKey(lineId)) line2maxDelay.put(lineId, entry.getValue());
			if (entry.getValue().delay > line2maxDelay.get(lineId).delay){
				line2maxDelay.put(lineId, entry.getValue());
			}
		}
		for (Entry<Id<Vehicle>, DelayRecord> entry: veh2minDelay.entrySet()) {
			Id<TransitLine> lineId = entry.getValue().lineId;
			if (!line2minDelay.containsKey(lineId)) line2minDelay.put(lineId, entry.getValue());
			if (entry.getValue().delay < line2minDelay.get(lineId).delay){
				line2minDelay.put(lineId, entry.getValue());
			}
		}
	}
	
	DelayRecord getMaxDelay() {
		return Collections.max(line2maxDelay.values(), new DelayRecordDelayComparator());
	}
	
	DelayRecord getMinDelay() {
		return Collections.min(line2minDelay.values(), new DelayRecordDelayComparator());
	}
	
	double getAverageOfPositivelyDelayedOverPositivelyDelayed() {
		double sum = 0;
		int counter = 0;
		for (DelayRecord delay: allDelays) {
			if (delay.delay > 0) {
				counter++;
				sum = sum + delay.delay;
			}
		}
		if (counter < 1) {
			return Double.NaN;
		}
		return sum / counter;
	}
	
	double getAverageOfPositivelyDelayedOverTotalNumberOfRecords() {
		double sum = 0;
		for (DelayRecord delay: allDelays) {
			if (delay.delay > 0) {
				sum = sum + delay.delay;
			}
		}
		if (allDelays.size() < 1) {
			return Double.NaN;
		}
		return sum / allDelays.size();
	}
	
	double getAverageOfNegativelyDelayedOverTotalNumberOfRecords() {
		double sum = 0;
		for (DelayRecord delay: allDelays) {
			if (delay.delay < 0) {
				sum = sum + delay.delay;
			}
		}
		if (allDelays.size() < 1) {
			return Double.NaN;
		}
		return sum / allDelays.size();
	}
	
	String minDelayPerTransitLine() {
		DelayRecord min = getMinDelay();
		
		StringBuilder str = new StringBuilder();
		str.append("#Min delay: " + min.toString());
		str.append("\nlineId;routeId;vehId;stopId;delaySec");
		
		for (DelayRecord delay: line2minDelay.values()) {
			str.append("\n");
			str.append(delay.toValueString());
		}
		
		return str.toString();
	}
	
	String maxDelayPerTransitLine() {
		DelayRecord max = getMaxDelay();
		
		StringBuilder str = new StringBuilder();
		str.append("#Max delay: " + max.toString());
		str.append("\nlineId;routeId;vehId;stopId;delaySec");
		
		for (DelayRecord delay: line2maxDelay.values()) {
			str.append("\n");
			str.append(delay.toValueString());
		}
		
		return str.toString();
	}
	
	class DelayRecord {
		private final Id<TransitLine> lineId;
		private final Id<TransitRoute> routeId;
		private final Id<Vehicle> vehId;
		private final Id<TransitStopFacility> stopId;
		private final double delay;
		
		DelayRecord(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<Vehicle> vehId, 
				Id<TransitStopFacility> stopId, double delay) {
			this.lineId = lineId;
			this.routeId = routeId;
			this.vehId = vehId;
			this.stopId = stopId;
			this.delay = delay;
		}
		
		@Override
		public String toString() {
			return "lineId " + lineId + "; routeId " + routeId + "; vehId " + vehId + "; stopId " + stopId + "; delay " + delay;
		}
		
		String toValueString() {
			return lineId + ";" + routeId + ";" + vehId + ";" + stopId + ";" + delay;
		}
		
		double getDelay() {
			return delay;
		}
	}
	
	private class DelayRecordDelayComparator implements Comparator<DelayRecord> {

		@Override
		public int compare(DelayRecord o1, DelayRecord o2) {
			return Double.compare(o1.delay, o2.delay);
		}
		
	}

}
