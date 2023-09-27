/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.drt.BerlinShpUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.*;

public class RunZonalCarKMAnalysis {

	private static final Logger log = Logger.getLogger(RunZonalCarKMAnalysis.class);

	private static final String BERLIN_SHP = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
	private static final String HUNDEKOPF_SHP = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/berlin-planungsraum-hundekopf/berlin-hundekopf-based-on-planungsraum.shp";
	private static final String BERLIN_LINKS = "scenarios/berlinLinks-v6.txt";
	private static final String HUNDEKOPF_LINKS = "scenarios/hundekopfLinks-v6.txt";


//			"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-10pct.output_events.xml.gz";
	private static final String EVENTS_FILE = "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-berlin/calibration-3rd/mode-choice-new-prices-v12_10pct/runs/004/004.output_events.xml.gz";

	private static String NETWORK = "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-berlin/calibration-3rd/mode-choice-new-prices-v12_10pct/runs/004/004.output_network.xml.gz";
	private static final CoordinateTransformation NETWORK_2_SHAPE_TRANSFORMATION = TransformationFactory.getCoordinateTransformation("EPSG:25832", TransformationFactory.DHDN_GK4);

//	"D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-"
	private static final String OUTPUT_PATH = "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-berlin/calibration-3rd/mode-choice-new-prices-v12_10pct/runs/004/004.";

	public static void main(String[] args) {

		Network network = NetworkUtils.readNetwork(NETWORK);

		{
//			//only needed once in order to create local txt files and save
			List<PreparedGeometry> berlinGeoms = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(BERLIN_SHP));
			List<PreparedGeometry> hundekopfGeoms = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(HUNDEKOPF_SHP));
			log.info("start assigning links to zones");
			writeLinksInAreaToTxt(network, berlinGeoms, BERLIN_LINKS, NETWORK_2_SHAPE_TRANSFORMATION);
			writeLinksInAreaToTxt(network, hundekopfGeoms, HUNDEKOPF_LINKS, NETWORK_2_SHAPE_TRANSFORMATION);
		}

		Set<Id<Link>> berlinLinks = readLinksInAreaTxt(BERLIN_LINKS);
		Set<Id<Link>> hundekopfLinks = readLinksInAreaTxt(HUNDEKOPF_LINKS);

		Set<?>[] areas =  new Set<?>[2];
		areas[0] = berlinLinks;
		areas[1] = hundekopfLinks;

		ActivityInZoneHandler activitiesInZoneHandler = new ActivityInZoneHandler(HUNDEKOPF_SHP,
				Set.of(0, -100, -250, -500, -1000, -2000),
				network,
				NETWORK_2_SHAPE_TRANSFORMATION);

		PersonalZonalCarKMHandler personalZonalCarKMHandler = new PersonalZonalCarKMHandler(network,
				(Set<Id<Link>>[]) areas);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(activitiesInZoneHandler);
		manager.addHandler(personalZonalCarKMHandler);
		manager.initProcessing();
		EventsUtils.readEvents(manager, EVENTS_FILE);
		manager.finishProcessing();

		writeOutput(activitiesInZoneHandler, personalZonalCarKMHandler);

	}

	private static void writeOutput(ActivityInZoneHandler activitiesInZoneHandler, PersonalZonalCarKMHandler personalZonalCarKMHandler) {
		try {
			MutableDouble totalBerlinKM = new MutableDouble(0);
			MutableDouble totalHundekopfKM = new MutableDouble(0);

			personalZonalCarKMHandler.carDrivers2KMInZone.values().stream()
					.forEach(doubles -> {
						totalBerlinKM.add(doubles[0]);
						totalHundekopfKM.add(doubles[1]);
					});

			BufferedWriter writer =  new BufferedWriter(new FileWriter(OUTPUT_PATH + "zonalCarKM_total_persons.tsv"));
			writer.write("berlinCarKm;" + (totalBerlinKM));
			writer.newLine();
			writer.write("hundeKopfCarKm;" + (totalHundekopfKM));
			writer.close();

			CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(OUTPUT_PATH + "zonalCarKM_ofActivePersons_persons.tsv"), CSVFormat.DEFAULT
					.withHeader("buffer\tpersonsWithAtLeast1ActinZone\tcarKMofThosePersonsInBerlin\tpercentageOFTotalBerlinCarKM\tcarKMofThosePersonsInHundekopf\tpercentageOfTotalHundekopfCarKM")
					.withDelimiter('\t'));
			activitiesInZoneHandler.personsWithActivityInZoneWithBuffer
					.forEach( (buffer, persons) -> {
						double berlinKM = 0.;
						double hundekopfKM = 0;
						for (Id<Person> person : persons) {
							if(personalZonalCarKMHandler.carDrivers2KMInZone.get(person) != null){
								berlinKM += personalZonalCarKMHandler.carDrivers2KMInZone.get(person)[0];
								hundekopfKM += personalZonalCarKMHandler.carDrivers2KMInZone.get(person)[1];
							}
							//else the person did not use car
						}
						try {
							printer.printRecord(buffer, persons.size(), berlinKM, (berlinKM / totalBerlinKM.doubleValue()), hundekopfKM, (hundekopfKM / totalHundekopfKM.doubleValue()));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});

			printer.flush();
			printer.close();


		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void writeLinksInAreaToTxt(Network network, List<PreparedGeometry> geoms, String txtFile, CoordinateTransformation network2ShapeTransformation){
		log.info("will try to write to file " + txtFile);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile));
			writer.write("linkIds");
			network.getLinks().values().stream()
					.forEach(link -> {
						if(ShpGeometryUtils.isCoordInPreparedGeometries(network2ShapeTransformation.transform(link.getToNode().getCoord()), geoms)
								|| ShpGeometryUtils.isCoordInPreparedGeometries(network2ShapeTransformation.transform(link.getFromNode().getCoord()), geoms) ){
							try {
								writer.newLine();
								writer.write(link.getId().toString());
							} catch (IOException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	static Set<Id<Link>> readLinksInAreaTxt(String pathToFile){
		Set<Id<Link>> links = new HashSet<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(pathToFile));
			String header = reader.readLine();
			String line = reader.readLine();
			while(line != null){
				links.add(Id.createLinkId(line));
				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException("could not load file " + pathToFile + ".\n you should run writeLinksInAreaToTxt() first");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return links;
	}

}

/**
 * we listen to both ActivityStarts and Ends to be really sure. Because the first activities do not have a start event and some activities might not be ended.
 */
class ActivityInZoneHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

	final Map<Integer, Set<Id<Person>>> personsWithActivityInZoneWithBuffer = new HashMap<>();
	private final BerlinShpUtils berlinShpUtils;
	private final Network network;
	private final CoordinateTransformation network2ShapeTransformation;

	ActivityInZoneHandler(String pathToZoneFile, Set<Integer> buffers, Network network, CoordinateTransformation network2ShapeTransformation) {
		this.network = network;
		buffers.forEach(buffer -> personsWithActivityInZoneWithBuffer.put(buffer, new HashSet<>()));
		this.berlinShpUtils = new BerlinShpUtils(pathToZoneFile);
		this.network2ShapeTransformation = network2ShapeTransformation;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!TripStructureUtils.isStageActivityType(event.getActType())){
			handleActivityEvent(event);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!TripStructureUtils.isStageActivityType(event.getActType())){
			handleActivityEvent(event);
		}
	}

	private void handleActivityEvent(Event event) {
		Id<Person> personId = ((HasPersonId) event).getPersonId();
		Coord coord = ((BasicLocation) event).getCoord();
		Id<Link> linkId = ((HasLinkId) event).getLinkId();

//		if (!(personId.toString().contains("drt"))) {
		if(!personId.toString().contains("freight")) {
			for (Integer buffer : personsWithActivityInZoneWithBuffer.keySet()) {
				Set<Id<Person>> personsInBufferZone = personsWithActivityInZoneWithBuffer.get(buffer);
				if (!personsInBufferZone.contains(personId)) {
					coord = coord != null ? coord : network.getLinks().get(linkId).getToNode().getCoord();
					Coord transformed = network2ShapeTransformation.transform(coord);

					if (berlinShpUtils.isCoordInDrtServiceAreaWithBuffer(transformed, buffer)) {
						personsInBufferZone.add(personId);
					}
				}
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.personsWithActivityInZoneWithBuffer.replaceAll((integer, ids) -> new HashSet<>());
		ActivityStartEventHandler.super.reset(iteration);
	}
}

class PersonalZonalCarKMHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler {

	private final Network network;
	private Map<Id<Vehicle>, Id<Person>> cars2Drivers = new HashMap<>();
	Map<Id<Person>, double[]> carDrivers2KMInZone = new HashMap<>();
	private Set<Id<Link>> zonalLinkIds = new HashSet<>();

	private final Set<Id<Link>>[] subAreas;
	private List personsInCars = new ArrayList();

	public PersonalZonalCarKMHandler(Network network, Set<Id<Link>>[] subAreaLinkSets) {
		this.subAreas = subAreaLinkSets;
		this.network = network;
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> person = cars2Drivers.get(event.getVehicleId());
		if(person != null){
			for (int i = 0; i < subAreas.length; i++) {
				Set<Id<Link>> subArea = subAreas[i];
				if(subArea.contains(event.getLinkId())){
					carDrivers2KMInZone.get(person)[i] += network.getLinks().get(event.getLinkId()).getLength() / 1000;
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.cars2Drivers.remove(event.getVehicleId());
		this.personsInCars.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(personsInCars.contains(event.getPersonId())) this.cars2Drivers.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void reset(int iteration) {
		this.personsInCars.clear();
		this.cars2Drivers.clear();
		this.carDrivers2KMInZone.clear();
		LinkEnterEventHandler.super.reset(iteration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().equals(TransportMode.car)){
			if(!this.carDrivers2KMInZone.containsKey(event.getPersonId())){
				double[] zonalKMArray = new double[subAreas.length];
				Arrays.fill(zonalKMArray, 0.0);
				this.carDrivers2KMInZone.put(event.getPersonId(), zonalKMArray);
			}
			this.personsInCars.add(event.getPersonId());
		}
	}
}


