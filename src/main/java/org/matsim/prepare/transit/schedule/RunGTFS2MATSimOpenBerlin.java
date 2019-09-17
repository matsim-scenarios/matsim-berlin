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

/**
 * 
 */
package org.matsim.prepare.transit.schedule;

import java.io.File;
import java.time.LocalDate;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.gtfs.RunGTFS2MATSim;
import org.matsim.contrib.gtfs.TransitSchedulePostProcessTools;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.filter.NetworkNodeFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.prepare.transit.schedule.CheckPtDelays.DelayRecord;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * @author  vsp-gleich
 * This is an script that utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 * 
 * It then adapts the link freespeeds of the pt pseudo network to reduce pt delays and early arrivals. This is not perfect.
 * Check manually after running, e.g. using the log output on maximum delays per TransitLine.
 * 
 */

public class RunGTFS2MATSimOpenBerlin {

	private static final Logger log = Logger.getLogger(RunGTFS2MATSimOpenBerlin.class);
	
	public static void main(String[] args) {
	
		//this was tested for the latest VBB GTFS, available at 
		// http://www.vbb.de/de/article/fahrplan/webservices/datensaetze/1186967.html
		
		//input data, https paths don't work probably due to old GTFS library :(
		String gtfsZipFile = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/original-data/GTFS-VBB-20181214/GTFS-VBB-20181214.zip"; 
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		// choose date not too far away (e.g. on 2019-12-12 S2 is almost completey missing for 2019-08-20 gtfs data set!), 
		// but not too close either (diversions and interruptions due to short term construction work included in GTFS)
		// -> hopefully no construction sites in GTFS for that date
		// -> Thursday is more "typical" than Friday
		// check date for construction work in BVG Navi booklet: 18-20 Dec'2018 seemed best over the period from Dec'2018 to Sep'2019
		LocalDate date = LocalDate.parse("2018-12-20"); 

		//output files 
		String scheduleFile = "transitSchedule.xml.gz";
		String networkFile = "network.xml.gz";
		String transitVehiclesFile ="transitVehicles.xml.gz";
		
		//Convert GTFS
		RunGTFS2MATSim.convertGtfs(gtfsZipFile, scheduleFile, date, ct, false);
		
		//Parse the schedule again
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		
		// copy late/early departures to have at complete schedule from ca. 0:00 to ca. 30:00 
		TransitSchedulePostProcessTools.copyLateDeparturesToStartOfDay(scenario.getTransitSchedule(), 24 * 3600, "copied", false);
		TransitSchedulePostProcessTools.copyEarlyDeparturesToFollowingNight(scenario.getTransitSchedule(), 6 * 3600, "copied");
		
		//if necessary, parse in an existing network file here:
		new MatsimNetworkReader(scenario.getNetwork()).readFile("/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");
		
		//remove existing pt network (nodes and links)
		Network networkWoPt = getNetworkWOExistingPtLinksAndNodes(scenario.getNetwork(), "pt_");
		new NetworkWriter(networkWoPt).write(networkFile + "_network_filtered_woNewPt.xml.gz");
		
		//Create a network around the schedule and transit vehicles
		scenario = getScenarioWithPseudoPtNetworkAndTransitVehicles(networkWoPt, scenario.getTransitSchedule(), "pt_");
		
		//Check schedule and network
		ValidationResult checkResult = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), networkWoPt);
		if (checkResult.isValid()) {
			log.info("TransitSchedule and Network valid according to TransitScheduleValidator");
			log.warn("TransitScheduleValidator warnings:\n" + checkResult.getWarnings());
		} else {
			log.error(checkResult.getErrors());
			throw new RuntimeException("TransitSchedule and/or Network invalid");
		}
		
		//Write out network, vehicles and schedule
		new NetworkWriter(networkWoPt).write(networkFile);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleFile);
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(transitVehiclesFile);
		
		// test for delays
		String outputDirectory = "RunGTFS2MATSimOpenBerlin/runOneIteration";
		runOneIteration(scenario, outputDirectory);
		CheckPtDelays delayChecker = new CheckPtDelays(outputDirectory + "/output_events.xml.gz", scheduleFile);
		delayChecker.run();
		DelayRecord minDelay = delayChecker.getMinDelay();
		DelayRecord maxDelay = delayChecker.getMaxDelay();
		log.warn(minDelay);
		if (minDelay.getDelay() < -1) {
			log.warn(delayChecker.minDelayPerTransitLine());
		}
		log.warn(maxDelay);
		if (maxDelay.getDelay() > 1) {
			log.warn(delayChecker.maxDelayPerTransitLine());
		}
		
		// delays up to 60s are probably ok, because most input gtfs schedule data has an accuracy of only one minute
	}

	private static Network getNetworkWOExistingPtLinksAndNodes(Network network, String ptNetworkIdentifier) {
		NetworkFilterManager nfmPT = new NetworkFilterManager(network);
		nfmPT.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getId().toString().contains(ptNetworkIdentifier)) return false;
				else return true;
			}
		});
		nfmPT.addNodeFilter(new NetworkNodeFilter() {

			@Override
			public boolean judgeNode(Node n) {
				if (n.getId().toString().contains(ptNetworkIdentifier)) return false;
				else return true;
			}
		});
		Network networkWoPt = nfmPT.applyFilters();
		return networkWoPt;
	}
	
	private static Scenario getScenarioWithPseudoPtNetworkAndTransitVehicles(Network network, TransitSchedule schedule, 
			String ptNetworkIdentifier) {
		ScenarioUtils.ScenarioBuilder builder = new ScenarioUtils.ScenarioBuilder(ConfigUtils.createConfig());
		builder.setNetwork(network);
		builder.setTransitSchedule(schedule);
		Scenario scenario = builder.build();
		
		// add pseudo network for pt
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "pt_", 0.1, 100000.0).createNetwork();
		
		// create TransitVehicle types
		// see https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2014/14-24/ for veh capacities
		// the values set here are at the upper end of the typical capacity range, so on lines with high capacity vehicles the
		// capacity of the matsim vehicle equals roughly the real vehicles capacity and on other lines the Matsim vehicle 
		// capacity is higher than the real used vehicle's capacity (gtfs provides no information on which vehicle type is used,
		// and this would be beyond scope here). - gleich sep'19
		VehiclesFactory vb = scenario.getVehicles().getFactory();
		VehicleType reRbVehicleType = vb.createVehicleType(Id.create("RE_RB_veh_type", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(500));
		capacity.setStandingRoom(Integer.valueOf(600));
		reRbVehicleType.setCapacity(capacity);
		reRbVehicleType.setDoorOperationMode(DoorOperationMode.parallel);
		scenario.getTransitVehicles().addVehicleType(reRbVehicleType);
		
		VehicleType sBahnVehicleType = vb.createVehicleType(Id.create("S-Bahn_veh_type", VehicleType.class));
		capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(400));
		capacity.setStandingRoom(Integer.valueOf(800));
		sBahnVehicleType.setCapacity(capacity);
		reRbVehicleType.setDoorOperationMode(DoorOperationMode.parallel);
		scenario.getTransitVehicles().addVehicleType(sBahnVehicleType);
		
		VehicleType uBahnVehicleType = vb.createVehicleType(Id.create("U-Bahn_veh_type", VehicleType.class));
		capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(300));
		capacity.setStandingRoom(Integer.valueOf(600));
		uBahnVehicleType.setCapacity(capacity);
		reRbVehicleType.setDoorOperationMode(DoorOperationMode.parallel);
		scenario.getTransitVehicles().addVehicleType(uBahnVehicleType);
		
		VehicleType tramVehicleType = vb.createVehicleType(Id.create("Tram_veh_type", VehicleType.class));
		capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(80));
		capacity.setStandingRoom(Integer.valueOf(170));
		tramVehicleType.setCapacity(capacity);
		reRbVehicleType.setDoorOperationMode(DoorOperationMode.parallel);
		scenario.getTransitVehicles().addVehicleType(tramVehicleType);
		
		VehicleType busVehicleType = vb.createVehicleType(Id.create("Bus_veh_type", VehicleType.class));
		capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(50));
		capacity.setStandingRoom(Integer.valueOf(100));
		busVehicleType.setCapacity(capacity);
		reRbVehicleType.setDoorOperationMode(DoorOperationMode.parallel);
		scenario.getTransitVehicles().addVehicleType(busVehicleType);
		
		VehicleType ferryVehicleType = vb.createVehicleType(Id.create("Ferry_veh_type", VehicleType.class));
		capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(100));
		capacity.setStandingRoom(Integer.valueOf(100));
		ferryVehicleType.setCapacity(capacity);
		reRbVehicleType.setDoorOperationMode(DoorOperationMode.parallel);
		scenario.getTransitVehicles().addVehicleType(ferryVehicleType);
		
		// set link speeds and create vehicles according to pt mode
		for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
			VehicleType lineVehicleType;
			// the idea is to differentiate between short links (low average speed due to acceleration and braking)
			// and long links longer than 1km (almost speed limit) 
			double shortLinkFreespeed;
			double longLinkFreespeed;
			double shortLongLinkThreshold;
			String stopFilter = ""; 
			
			// identify veh type / mode using gtfs route type (3-digit code, also found at the end of the line id (gtfs: route_id))
			int gtfsTransitType;
			try {
				gtfsTransitType = Integer.parseInt( (String) line.getAttributes().getAttribute("gtfs_route_type"));
			} catch (NumberFormatException e) {
				log.error("unknown transit mode! Line id was " + line.getId().toString() + 
						"; gtfs route type was " + (String) line.getAttributes().getAttribute("gtfs_route_type"));
				throw new RuntimeException("unknown transit mode");
			}
			
			int agencyId;
			try {
				agencyId = Integer.parseInt( (String) line.getAttributes().getAttribute("gtfs_agency_id"));
			} catch (NumberFormatException e) {
				log.error("invalid transit agency! Line id was " + line.getId().toString() + 
						"; gtfs agency was " + (String) line.getAttributes().getAttribute("gtfs_agency_id"));
				throw new RuntimeException("invalid transit agency");
			}
			
			switch (gtfsTransitType) {
			// the vbb gtfs file generally uses the new gtfs route types, but some lines use the old enum in the range 0 to 7
			// see https://sites.google.com/site/gtfschanges/proposals/route-type 
			// and https://developers.google.com/transit/gtfs/reference/#routestxt
			// In GTFS-VBB-20181214.zip some RE lines are wrongly attributed as type 700 (bus)!
			
			// freespeed are set to make sure that no transit service is delayed 
			// and arrivals are as punctual (not too early) as possible
			case 100: 
				lineVehicleType = reRbVehicleType;
				// free speed:  RE2 Brand->Koenigs Wusterhausen ca. 140km/h -> 5 min delay at 100km/h!
				// however: RE7 Ostkreuz -> Schoenefeld ca. 5 min early arrival at 100km/h 
				// vmax is typically <= 160km/h
				shortLinkFreespeed = 80.0 / 3.6;
				longLinkFreespeed = 150.0 / 3.6;
				shortLongLinkThreshold = 3000.0;
				stopFilter = "station_S/U/RE/RB";
				break;
			case 109: 
				lineVehicleType = sBahnVehicleType;
				// vmax is typically <= 100km/h in Berlin, S-Bahn in other cities is rather similar to RB/RE in Berlin
				// (vmax 120/140/160km/h)
				// TODO check S Bahn Berlin has still the same agency id?
				int sbahnBerlinAcencyId = 1;
				shortLinkFreespeed = agencyId == sbahnBerlinAcencyId ? 50 / 3.6 : 60 / 3.6;
				longLinkFreespeed = agencyId == sbahnBerlinAcencyId ? 80 / 3.6 : 150 / 3.6;
				shortLongLinkThreshold = agencyId == sbahnBerlinAcencyId ? 1500.0 : 3000.0;
				stopFilter = "station_S/U/RE/RB";
				break;
			case 400: 
				lineVehicleType = uBahnVehicleType;
				// vmax is typically <= 70km/h
				shortLinkFreespeed = 40.0 / 3.6;
				longLinkFreespeed = 60.0 / 3.6;
				shortLongLinkThreshold = 1000;
				stopFilter = "station_S/U/RE/RB";
				break;
			case 3: // bus, same as 700
			case 700: 
				lineVehicleType = busVehicleType;
				// rural bus lines shall have higher speed
				// differentiate between rural and urban buses using the agency which runs them, i.e.
				// urban 30km/h: BVG (id=796 in GTFS-VBB-20181214.zip)
				// rural 50km/h: all other agencies
				// vmax is typically <= 80km/h
				// TODO check BVG has still the same agency id?
				try {
					agencyId = Integer.parseInt( (String) line.getAttributes().getAttribute("gtfs_agency_id"));
				} catch (NumberFormatException e) {
					log.error("invalid transit agency! Line id was " + line.getId().toString() + 
							"; gtfs agency was " + (String) line.getAttributes().getAttribute("gtfs_agency_id"));
					throw new RuntimeException("invalid transit agency");
				}
				int bvgAcencyId = 796;
				shortLinkFreespeed = agencyId == bvgAcencyId ? 40 / 3.6 : 60 / 3.6;
				longLinkFreespeed = agencyId == bvgAcencyId ? 60 / 3.6 : 90 / 3.6;
				shortLongLinkThreshold = agencyId == bvgAcencyId ? 600.0 : 1500.0;
				break;
			case 900: 
				lineVehicleType = tramVehicleType;
				// vmax is typically <= 70km/h
				shortLinkFreespeed = 30.0 / 3.6;
				longLinkFreespeed = 60.0 / 3.6;
				shortLongLinkThreshold = 600;
				break;
			case 1000: 
				lineVehicleType = ferryVehicleType;
				shortLinkFreespeed = 25.0 / 3.6;
				longLinkFreespeed = 25.0 / 3.6;
				shortLongLinkThreshold = 1000;
				break;
			default:
				log.error("unknown transit mode! Line id was " + line.getId().toString() + 
						"; gtfs route type was " + (String) line.getAttributes().getAttribute("gtfs_route_type"));
				throw new RuntimeException("unknown transit mode");
			}
			
			for (TransitRoute route: line.getRoutes().values()) {
				int routeVehId = 0; // simple counter for vehicle id _per_ TransitRoute
				
				// increase speed if current freespeed is lower. 
				// Should different transit modes use the same link, the higher freespeed will 
				NetworkRoute networkRoute = route.getRoute();
				
				if (network.getLinks().get(networkRoute.getStartLinkId()).getLength() < shortLongLinkThreshold) {
					increaseLinkFreespeedIfLower(network.getLinks().get(networkRoute.getStartLinkId()), shortLinkFreespeed);
				} else {
					increaseLinkFreespeedIfLower(network.getLinks().get(networkRoute.getStartLinkId()), longLinkFreespeed);
				}
				if (network.getLinks().get(networkRoute.getEndLinkId()).getLength() < shortLongLinkThreshold) {
					increaseLinkFreespeedIfLower(network.getLinks().get(networkRoute.getEndLinkId()), shortLinkFreespeed);
				} else {
					increaseLinkFreespeedIfLower(network.getLinks().get(networkRoute.getEndLinkId()), longLinkFreespeed);
				}
				
				for (Id<Link> linkId: networkRoute.getLinkIds()) {
					if (network.getLinks().get(linkId).getLength() < shortLongLinkThreshold) {
						increaseLinkFreespeedIfLower(network.getLinks().get(linkId), shortLinkFreespeed);
					} else {
						increaseLinkFreespeedIfLower(network.getLinks().get(linkId), longLinkFreespeed);
					}
				}
				
				// create vehicles for Departures
				for (Departure departure: route.getDepartures().values()) {
					Vehicle veh = vb.createVehicle(Id.create("pt_" + route.getId().toString() + "_" + Long.toString(routeVehId++), Vehicle.class), lineVehicleType);
					scenario.getTransitVehicles().addVehicle(veh);
					departure.setVehicleId(veh.getId());
				}
				
				// tag RE, RB, S- and U-Bahn stations for Drt satop filter attribute
				if (!stopFilter.isEmpty()) {
					for (TransitRouteStop routeStop: route.getStops()) {
						routeStop.getStopFacility().getAttributes().putAttribute("stopFilter", stopFilter);
					}
				}
			}
		}
		
		return scenario;
	}
	
	private static void increaseLinkFreespeedIfLower(Link link, double newFreespeed) {
		if (link.getFreespeed() < newFreespeed) {
			link.setFreespeed(newFreespeed); 
		}
	}
	
	private static void runOneIteration(Scenario scenario, String outputDirectory) {
		new File(outputDirectory).mkdirs();
		scenario.getConfig().controler().setOutputDirectory(outputDirectory);
		scenario.getConfig().controler().setLastIteration(0);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		scenario.getConfig().transit().setUseTransit(true);
		
		Controler controler = new Controler( scenario );
		
		// use the sbb pt raptor router which takes less time to build a transit router network
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				install( new SwissRailRaptorModule() );
			}
		} );
		
		controler.run();
	}
}
