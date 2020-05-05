/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.analysis.wasteCollection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class RunAnalyse {
	static final Logger log = Logger.getLogger(RunAnalyse.class);

	private static final String dir_moLargeBinsDiesel100it = "../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Montag/a1_Mo_Big_Diesel_100it/";
	private static final String dir_moSmallBinsDiesel100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Montag/a2_Mo_Small_Diesel_100it/";
	private static final String dir_moLargeBinsElektro100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Montag/e1_Mo_Big_Electro_100it/";
	private static final String dir_moSmallBinsElektro100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Montag/e2_Mo_Small_Elektro_100it/";
	private static final String dir_weLargeBinsDiesel100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch/a1_Mo_Big_Diesel_100it/";
	private static final String dir_weSmallBinsDiesel100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch/a2_Mo_Small_Diesel_100it/";
	private static final String dir_weLargeBinsElektro100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch/e1_Mo_Big_Electro_100it/";
	private static final String dir_weSmallBinsElektro100it = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch/e2_Mo_Small_Elektro_100it/";

	private enum scenarioAuswahl {
		moLargeBinsDiesel100it, moLargeBinsElektro100it, moSmallBinsDiesel100it, moSmallBinsElektro100it,
		weLargeBinsDiesel100it, weLargeBinsElektro100it, weSmallBinsDiesel100it, weSmallBinsElektro100it,
	}

	public static void main(String[] args) throws IOException {

		log.info("Starting");

		scenarioAuswahl scenarioWahl = scenarioAuswahl.moLargeBinsDiesel100it;
		String inputDir;
		Map<Id<Person>, Double> personId2tourDistanceKm = new HashMap<>();
		Map<Id<Person>, Integer> personId2tourNumCollections = new HashMap<>();
		Map<Id<Person>, Double> personId2tourWasteCollectedTons = new HashMap<>();
		Map<Id<Person>, Integer> personId2tourDurations = new HashMap<>();

		switch (scenarioWahl) {
		case moLargeBinsDiesel100it:
			inputDir = dir_moLargeBinsDiesel100it;
			break;
		case moSmallBinsDiesel100it:
			inputDir = dir_moSmallBinsDiesel100it;
			break;
		case moLargeBinsElektro100it:
			inputDir = dir_moLargeBinsElektro100it;
			break;
		case moSmallBinsElektro100it:
			inputDir = dir_moSmallBinsElektro100it;
			break;
		case weLargeBinsDiesel100it:
			inputDir = dir_weLargeBinsDiesel100it;
			break;
		case weSmallBinsDiesel100it:
			inputDir = dir_weSmallBinsDiesel100it;
			break;
		case weLargeBinsElektro100it:
			inputDir = dir_weLargeBinsElektro100it;
			break;
		case weSmallBinsElektro100it:
			inputDir = dir_weSmallBinsElektro100it;
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + scenarioWahl);
		}

		log.info("Running analysis for " + scenarioWahl + " : " + inputDir);

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers)
				.readFile(new File(inputDir + "output_CarrierPlans.xml").getCanonicalPath());

		Network network = NetworkUtils.readNetwork(inputDir + "output_network.xml.gz");

		for (Carrier carrier : carriers.getCarriers().values()) {
			double distanceTourKM;
			int numCollections;
			double wasteCollectedTons;
			int tourNumber = 0;

			Collection<ScheduledTour> tours = carrier.getSelectedPlan().getScheduledTours();
			Map<Id<CarrierShipment>, CarrierShipment> shipments = carrier.getShipments();
			HashMap<String, Integer> shipmentSizes = new HashMap<String, Integer>();

			for (Entry<Id<CarrierShipment>, CarrierShipment> entry : shipments.entrySet()) {
				String shipmentId = entry.getKey().toString();
				int shipmentSize = entry.getValue().getSize();
				shipmentSizes.put(shipmentId, shipmentSize);
			}
			for (ScheduledTour scheduledTour : tours) {
				distanceTourKM = 0.0;
				numCollections = 0;
				wasteCollectedTons = 0.0;
				int startTime = 10000000;
				int endTime = 0;

				List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
				for (Tour.TourElement element : elements) {
					if (element instanceof Tour.Pickup) {
						numCollections++;
						Tour.Pickup pickupElement = (Tour.Pickup) element;
						String pickupShipmentId = pickupElement.getShipment().getId().toString();
						wasteCollectedTons = wasteCollectedTons + (shipmentSizes.get(pickupShipmentId) / 1000);
					}
					if (element instanceof Tour.Leg) {
						Tour.Leg legElement = (Tour.Leg) element;
						if (legElement.getRoute().getDistance() != 0)
							distanceTourKM = distanceTourKM
									+ RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0, network)
											/ 1000;
						if (startTime > legElement.getExpectedDepartureTime())
							startTime = (int) legElement.getExpectedDepartureTime();
						if (endTime < (legElement.getExpectedDepartureTime() + legElement.getExpectedTransportTime()))
							endTime = (int) (legElement.getExpectedDepartureTime()
									+ legElement.getExpectedTransportTime());
					}
				}

				Id<Person> personId = Id.create(
						carrier.getId().toString() + scheduledTour.getVehicle().getVehicleId().toString() + tourNumber,
						Person.class);
				personId2tourDistanceKm.put(personId, distanceTourKM);
				personId2tourNumCollections.put(personId, numCollections);
				personId2tourWasteCollectedTons.put(personId, wasteCollectedTons);
				personId2tourDurations.put(personId, endTime - startTime);

				tourNumber++;
			}
		}

		writeOutput(inputDir, personId2tourDistanceKm, personId2tourNumCollections, personId2tourWasteCollectedTons,
				personId2tourDurations);

		log.info("### Done.");

	}

	static void writeOutput(String directory, Map<Id<Person>, Double> personId2tourDistanceKm,
			Map<Id<Person>, Integer> personId2tourNumCollections,
			Map<Id<Person>, Double> personId2tourWasteCollectedTons, Map<Id<Person>, Integer> personId2tourDurations) {
		BufferedWriter writer;
		File file;
		file = new File(directory + "/03_TourStatistics.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file, true));
			String now = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
			writer.write("Tourenstatisitik erstellt am: " + now + "\n\n");

			// Headline
			writer.write(
					"TourID\t\t\t\t\t\t\t\t\t\tduration hh:mm:ss\t\tdistance (km)\t\t#ofDeliveries\t\tdelivered Volume (m3)\n\n");

			for (Id<Person> id : personId2tourDistanceKm.keySet()) {
				Double tourDistance = (double) Math.round(personId2tourDistanceKm.get(id) / 1000);
				Integer tourNumCollections = personId2tourNumCollections.get(id);
				Double tourWasteCollected = personId2tourWasteCollectedTons.get(id);
				int duration = personId2tourDurations.get(id);

				writer.write(id + "\t\t" + timeTransmission(duration) + "\t\t\t\t\t" + tourDistance + "\t\t\t\t"
						+ tourNumCollections + "\t\t\t\t\t" + tourWasteCollected);
				writer.newLine();

			}

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Output geschrieben");
	}

	public static String timeTransmission(int duration) {
		int stunden = (int) duration / 3600;
		int minuten = (int) (duration - stunden * 3600) / 60;
		int sekunden = duration - stunden * 3600 - minuten * 60;
		return stunden + ":" + minuten + ":" + sekunden;
	}
}
