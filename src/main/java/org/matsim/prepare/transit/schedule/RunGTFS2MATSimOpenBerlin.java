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
import java.util.List;

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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
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
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * @author  vsp-gleich
 * This is an script that utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 * 
 * It then adapts the link freespeeds of the pt pseudo network to reduce pt delays and early arrivals. This is not perfect.
 * Check manually after running, e.g. using the log output on maximum delays per TransitLine.
 * 
 * TODO: Theoretically we would have to increase the boarding/alighting time and reduce the capacity of the transit vehicle types
 * according to the sample size.
 */

public class RunGTFS2MATSimOpenBerlin {

	private static final Logger log = Logger.getLogger(RunGTFS2MATSimOpenBerlin.class);
	
	public static void main(String[] args) {
	
		//this was tested for the latest VBB GTFS, available at 
		// http://www.vbb.de/de/article/fahrplan/webservices/datensaetze/1186967.html
		
		//input data, https paths don't work probably due to old GTFS library :(
		String gtfsZipFile = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/original-data/GTFS-VBB-20181214/GTFS-VBB-20181214.zip"; 
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		// choose date not too far away (e.g. on 2019-12-12 S2 is almost completey missing for 2019-08-20 gtfs data set!), 
		// but not too close either (diversions and interruptions due to short term construction work included in GTFS)
		// -> hopefully no construction sites in GTFS for that date
		// -> Thursday is more "typical" than Friday
		// check date for construction work in BVG Navi booklet: 18-20 Dec'2018 seemed best over the period from Dec'2018 to Sep'2019
		LocalDate date = LocalDate.parse("2018-12-20"); 

		//output files
		String outputDirectory = "RunGTFS2MATSimOpenBerlin";
		String networkFile = outputDirectory + "/berlin-v5.5-network.xml.gz";
		String scheduleFile = outputDirectory + "/berlin-v5.5-transit-schedule.xml.gz";
		String transitVehiclesFile = outputDirectory + "/berlin-v5.5-transit-vehicles.xml.gz";
		
		// ensure output directory exists
	    File directory = new File(outputDirectory);
	    if (! directory.exists()){
	        directory.mkdirs();
	    }
		
		//Convert GTFS
		RunGTFS2MATSim.convertGtfs(gtfsZipFile, scheduleFile, date, ct, false);
		
		//Parse the schedule again
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		
		// copy late/early departures to have at complete schedule from ca. 0:00 to ca. 30:00 
		TransitSchedulePostProcessTools.copyLateDeparturesToStartOfDay(scenario.getTransitSchedule(), 24 * 3600, "copied", false);
		TransitSchedulePostProcessTools.copyEarlyDeparturesToFollowingNight(scenario.getTransitSchedule(), 6 * 3600, "copied");
		
		//if necessary, parse in an existing network file here:
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");
		
		//remove existing pt network (nodes and links)
		Network networkWoPt = getNetworkWOExistingPtLinksAndNodes(scenario.getNetwork(), "pt_");
		new NetworkWriter(networkWoPt).write(outputDirectory + "/network_filtered_woNewPt.xml.gz");
		
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
		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(transitVehiclesFile);
		
		// test for delays
		String testRunOutputDirectory = outputDirectory + "/runOneIteration";
		runOneIteration(scenario, testRunOutputDirectory);
		CheckPtDelays delayChecker = new CheckPtDelays(testRunOutputDirectory + "/output_events.xml.gz", scheduleFile);
		delayChecker.run();
		DelayRecord minDelay = delayChecker.getMinDelay();
		DelayRecord maxDelay = delayChecker.getMaxDelay();
		log.warn("min delay: " + minDelay);
		log.warn("average of negatively delayed over total number of arrivals and departures "
				+ delayChecker.getAverageOfNegativelyDelayedOverTotalNumberOfRecords() + " s");
		if (minDelay.getDelay() < -1) {
			log.warn(delayChecker.minDelayPerTransitLine());
		}
		log.warn("max delay: " + maxDelay);
		log.warn("average of positively delayed over positively delayed arrivals and departures "
				+ delayChecker.getAverageOfPositivelyDelayedOverPositivelyDelayed() + " s");
		log.warn("average of positively delayed over total number of arrivals and departures "
				+ delayChecker.getAverageOfPositivelyDelayedOverTotalNumberOfRecords() + " s");
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
		VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

		VehicleType reRbVehicleType = vehicleFactory.createVehicleType( Id.create( "RE_RB_veh_type", VehicleType.class ) );
		{
			VehicleCapacity capacity = reRbVehicleType.getCapacity();
			capacity.setSeats( 500 );
			capacity.setStandingRoom( 600 );
			VehicleUtils.setDoorOperationMode(reRbVehicleType, DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(reRbVehicleType, 1.0 / 10.0); // 1s per boarding agent, distributed on 10 doors
			VehicleUtils.setEgressTime(reRbVehicleType, 1.0 / 10.0); // 1s per alighting agent, distributed on 10 doors
			scenario.getTransitVehicles().addVehicleType( reRbVehicleType );
		}
		VehicleType sBahnVehicleType = vehicleFactory.createVehicleType( Id.create( "S-Bahn_veh_type", VehicleType.class ) );
		{
			VehicleCapacity capacity = sBahnVehicleType.getCapacity();
			capacity.setSeats( 400 );
			capacity.setStandingRoom( 800 );
			VehicleUtils.setDoorOperationMode(sBahnVehicleType, DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(sBahnVehicleType, 1.0 / 24.0); // 1s per boarding agent, distributed on 8*3 doors
			VehicleUtils.setEgressTime(sBahnVehicleType, 1.0 / 24.0); // 1s per alighting agent, distributed on 8*3 doors
			scenario.getTransitVehicles().addVehicleType( sBahnVehicleType );
		}
		VehicleType uBahnVehicleType = vehicleFactory.createVehicleType( Id.create( "U-Bahn_veh_type", VehicleType.class ) );
		{
			VehicleCapacity capacity = uBahnVehicleType.getCapacity() ;
			capacity.setSeats( 300 );
			capacity.setStandingRoom( 600 );
			VehicleUtils.setDoorOperationMode(uBahnVehicleType, DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(uBahnVehicleType, 1.0 / 18.0); // 1s per boarding agent, distributed on 6*3 doors
			VehicleUtils.setEgressTime(uBahnVehicleType, 1.0 / 18.0); // 1s per alighting agent, distributed on 6*3 doors
			scenario.getTransitVehicles().addVehicleType( uBahnVehicleType );
		}
		VehicleType tramVehicleType = vehicleFactory.createVehicleType( Id.create( "Tram_veh_type", VehicleType.class ) );
		{
			VehicleCapacity capacity = tramVehicleType.getCapacity() ;
			capacity.setSeats( 80 );
			capacity.setStandingRoom( 170 );
			VehicleUtils.setDoorOperationMode(tramVehicleType, DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(tramVehicleType, 1.0 / 5.0); // 1s per boarding agent, distributed on 5 doors
			VehicleUtils.setEgressTime(tramVehicleType, 1.0 / 5.0); // 1s per alighting agent, distributed on 5 doors
			scenario.getTransitVehicles().addVehicleType( tramVehicleType );
		}
		VehicleType busVehicleType = vehicleFactory.createVehicleType( Id.create( "Bus_veh_type", VehicleType.class ) );
		{
			VehicleCapacity capacity = busVehicleType.getCapacity() ;
			capacity.setSeats( 50 );
			capacity.setStandingRoom( 100 );
			VehicleUtils.setDoorOperationMode(busVehicleType, DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(busVehicleType, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
			VehicleUtils.setEgressTime(busVehicleType, 1.0 / 3.0); // 1s per alighting agent, distributed on 3 doors
			scenario.getTransitVehicles().addVehicleType( busVehicleType );
		}
		VehicleType ferryVehicleType = vehicleFactory.createVehicleType( Id.create( "Ferry_veh_type", VehicleType.class ) );
		{
			VehicleCapacity capacity = ferryVehicleType.getCapacity() ;
			capacity.setSeats( 100 );
			capacity.setStandingRoom( 100 );
			VehicleUtils.setDoorOperationMode(ferryVehicleType, DoorOperationMode.serial); // first finish boarding, then start alighting
			VehicleUtils.setAccessTime(ferryVehicleType, 1.0 / 1.0); // 1s per boarding agent, distributed on 1 door
			VehicleUtils.setEgressTime(ferryVehicleType, 1.0 / 1.0); // 1s per alighting agent, distributed on 1 door
			scenario.getTransitVehicles().addVehicleType( ferryVehicleType );
		}
		// set link speeds and create vehicles according to pt mode
		for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
			VehicleType lineVehicleType;
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
				stopFilter = "station_S/U/RE/RB";
				break;
			case 109: 
				// S-Bahn-Berlin is agency id 1
				lineVehicleType = sBahnVehicleType;
				stopFilter = "station_S/U/RE/RB";
				break;
			case 400: 
				lineVehicleType = uBahnVehicleType;
				stopFilter = "station_S/U/RE/RB";
				break;
			case 3: // bus, same as 700
			case 700: 
				// BVG is agency id 796
				lineVehicleType = busVehicleType;
				break;
			case 900: 
				lineVehicleType = tramVehicleType;
				break;
			case 1000: 
				lineVehicleType = ferryVehicleType;
				break;
			default:
				log.error("unknown transit mode! Line id was " + line.getId().toString() + 
						"; gtfs route type was " + (String) line.getAttributes().getAttribute("gtfs_route_type"));
				throw new RuntimeException("unknown transit mode");
			}
			
			for (TransitRoute route: line.getRoutes().values()) {
				int routeVehId = 0; // simple counter for vehicle id _per_ TransitRoute
				
				// increase speed if current freespeed is lower. 
				List<TransitRouteStop> routeStops = route.getStops();
				if (routeStops.size() < 2) {
					log.error("TransitRoute with less than 2 stops found: line " + line.getId().toString() + 
							", route " + route.getId().toString());
					throw new RuntimeException("");
				}
				
				double lastDepartureOffset = route.getStops().get(0).getDepartureOffset().seconds();
				// min. time spend at a stop, useful especially for stops whose arrival and departure offset is identical,
				// so we need to add time for passengers to board and alight
				double minStopTime = 30.0;
				
				for (int i = 1; i < routeStops.size(); i++) {
					// TODO cater for loop link at first stop? Seems to just work without.
					TransitRouteStop routeStop = routeStops.get(i);
					// if there is no departure offset set (or infinity), it is the last stop of the line, 
					// so we don't need to care about the stop duration
					double stopDuration = routeStop.getDepartureOffset().isDefined() ?
							routeStop.getDepartureOffset().seconds() - routeStop.getArrivalOffset().seconds() : minStopTime;
					// ensure arrival at next stop early enough to allow for 30s stop duration -> time for passengers to board / alight
					// if link freespeed had been set such that the pt veh arrives exactly on time, but departure tiome is identical 
					// with arrival time the pt vehicle would have been always delayed
					// Math.max to avoid negative values of travelTime
					double travelTime = Math.max(1, routeStop.getArrivalOffset().seconds() - lastDepartureOffset - 1.0 -
							(stopDuration >= minStopTime ? 0 : (minStopTime - stopDuration))) ;
					Link link = network.getLinks().get(routeStop.getStopFacility().getLinkId());
					increaseLinkFreespeedIfLower(link, link.getLength() / travelTime);
					lastDepartureOffset = routeStop.getDepartureOffset().seconds();
				}
				
				// create vehicles for Departures
				for (Departure departure: route.getDepartures().values()) {
					Vehicle veh = vehicleFactory.createVehicle(Id.create("pt_" + route.getId().toString() + "_" + Long.toString(routeVehId++), Vehicle.class), lineVehicleType);
					scenario.getTransitVehicles().addVehicle(veh);
					departure.setVehicleId(veh.getId());
				}
				
				// tag RE, RB, S- and U-Bahn stations for Drt stop filter attribute
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
