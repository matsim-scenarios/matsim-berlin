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


package org.matsim.run.ptdisturbances;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
	
	private static final Logger log = Logger.getLogger(RunEventAnalysis.class );
	private static final String eventsFile = "scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/berlin-v5.5-1pct.output_events.xml";

	public static void main(String[] args) throws IOException {
		
		
		EventsManager events = EventsUtils.createEventsManager();
		PtAnalysisEventHandler ptAnalysisEventHandler = new PtAnalysisEventHandler();
		events.addHandler(ptAnalysisEventHandler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);

		Map<Id<Person>, List<Event>> personMap = PtAnalysisEventHandler.getPersonMap();
		
		writeTravelTimesToCSV(personMap);
		
	}

	private static void writeTravelTimesToCSV(Map<Id<Person>, List<Event>> personMap) throws IOException {
		
		BufferedWriter bw;
		FileWriter fw = new FileWriter("Events.csv");
		bw = new BufferedWriter(fw);
		bw.write("PersonId;LegStartTime;LegEndTime;TravelTime;NextActType;Line1/Vehicle;Line2;Line3;Line4;Line5");
		bw.newLine();
		for (List<Event> eventList : personMap.values()) {
			double legStartTime = 0;
			double legEndTime = 0;
			boolean isPtTrip = false;
			List<String> vehicles = new ArrayList<>();
			
			for (int ii = 0; ii < eventList.size(); ii++) {
				Event event = eventList.get(ii);
				
				if (event.getEventType().equals("actend")) {
					legStartTime = event.getTime();
				}
				
				if (event.getEventType().equals("PersonEntersVehicle")) {
					String vehicle = ((PersonEntersVehicleEvent) event).getVehicleId().toString();
					vehicles.add(vehicle);
					if (vehicle.startsWith("pt")) {
						isPtTrip = true;
					}
				}

				if (event.getEventType().equals("actstart")) {
					if(isPtTrip) {
						legEndTime = event.getTime();
					
						bw.write(((ActivityStartEvent) event).getPersonId().toString());
						bw.write(";");
						bw.write(String.valueOf(legStartTime));
						bw.write(";");
						bw.write(String.valueOf(legEndTime));
						bw.write(";");
						bw.write(String.valueOf(legEndTime-legStartTime));
						bw.write(";");
						bw.write(((ActivityStartEvent) event).getActType().toString());
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

