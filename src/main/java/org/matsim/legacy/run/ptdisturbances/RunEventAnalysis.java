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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
* @author smueller
*/

public class RunEventAnalysis {

	private static final Logger log = LogManager.getLogger(RunEventAnalysis.class );

	public static void main(String[] args) throws IOException {


		Map<Id<Person>, List<Event>> personMapBase = handleEvents("/Volumes/smueller_ssd/Replanning0800neu/berlin-drt-v5.5-10pct.output_events.xml");

		writeTravelTimesToCSV(personMapBase);

	}

	private static Map<Id<Person>, List<Event>> handleEvents(String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		PtAnalysisEventHandler ptAnalysisEventHandler = new PtAnalysisEventHandler();
		events.addHandler(ptAnalysisEventHandler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		Map<Id<Person>, List<Event>> personMap = PtAnalysisEventHandler.getPersonMap();
		return personMap;
	}

	private static void writeTravelTimesToCSV(Map<Id<Person>, List<Event>> personMap) throws IOException {

		BufferedWriter bw;
		FileWriter fw = new FileWriter("Events.csv");
		bw = new BufferedWriter(fw);
		bw.write("PersonId;TripIndex;PersonId+TripId;LegStartTime;LegEndTime;TravelTime;NextActType;LineChanges;Line1;Line2;Line3;Line4;Line5;Line6;Line7;Line8");
		bw.newLine();
		for (List<Event> eventList : personMap.values()) {
			double legStartTime = 0;
			double legEndTime = 0;
			boolean isPtTrip = false;
			int tripId = 0;
			List<String> vehicles = new ArrayList<>();

			for (int ii = 0; ii < eventList.size(); ii++) {
				Event event = eventList.get(ii);

				if (event.getEventType().equals("actend")) {
					legStartTime = event.getTime();
					tripId++;
				}

				if (event.getEventType().equals("PersonEntersVehicle")) {
					String vehicle = ((PersonEntersVehicleEvent) event).getVehicleId().toString();
					vehicles.add(vehicle);
					if (vehicle.startsWith("pt")) {
						isPtTrip = true;
					}
				}

				if (event.getEventType().equals("actstart")) {

					legEndTime = event.getTime();

					if(isPtTrip && legStartTime > 6. * 3600 && legStartTime < 10. * 3600) {
//						if(isPtTrip) {

						bw.write(((ActivityStartEvent) event).getPersonId().toString());
						bw.write(";");
						bw.write(String.valueOf(tripId));
						bw.write(";");
						bw.write(((ActivityStartEvent) event).getPersonId().toString()+"+"+String.valueOf(tripId));
						bw.write(";");
						bw.write(String.valueOf(legStartTime));
						bw.write(";");
						bw.write(String.valueOf(legEndTime));
						bw.write(";");
						bw.write(String.valueOf(legEndTime-legStartTime));
						bw.write(";");
						bw.write(((ActivityStartEvent) event).getActType().toString());
						bw.write(";");
						bw.write(String.valueOf(vehicles.size()-1));
						bw.write(";");
						if (vehicles != null) {
							for (int jj = 0; jj < vehicles.size(); jj++) {
								String[] lines = vehicles.get(jj).split("---");
								bw.write(lines[0]);
								bw.write(";");
							}
						}

						bw.newLine();
					}

					legStartTime = 0;
					legEndTime= 0;
					vehicles.clear();
					isPtTrip = false;
				}
			}

			bw.flush();
		}

		bw.close();
		log.info("done writing events to csv");

	}


}

