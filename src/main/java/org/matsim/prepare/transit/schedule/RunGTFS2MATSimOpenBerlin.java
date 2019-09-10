/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.time.LocalDate;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gtfs.RunGTFS2MATSim;
import org.matsim.contrib.gtfs.TransitSchedulePostProcessTools;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.VehicleWriterV1;

/**
 * @author  vsp-gleich
 * This is an example script that utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 * 
 * copy from GTFS2MATSim repository, adapted for Berlin file paths
 */

public class RunGTFS2MATSimOpenBerlin {

	
	public static void main(String[] args) {
	
		//this was tested for the latest VBB GTFS, available at 
		// http://www.vbb.de/de/article/fahrplan/webservices/datensaetze/1186967.html
		
		//input data, https paths don't work probably due to old GTFS library :(
		String gtfsZipFile = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/original-data/GTFS-VBB-20181214/GTFS-VBB-20181214.zip"; 
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25833");
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
		TransitSchedulePostProcessTools.copyLateDeparturesToStartOfDay(scenario.getTransitSchedule(), 22 * 3600, "copied", false);
		TransitSchedulePostProcessTools.copyEarlyDeparturesToFollowingNight(scenario.getTransitSchedule(), 6 * 3600, "copied");
		
		//if neccessary, parse in an existing network file here:
//		new MatsimNetworkReader(scenario.getNetwork()).readFile("network.xml");
		
		//Create a network around the schedule
		new CreatePseudoNetwork(scenario.getTransitSchedule(),scenario.getNetwork(),"pt_").createNetwork();
		
		//Create simple transit vehicles
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();
		
		//Write out network, vehicles and schedule
		new NetworkWriter(scenario.getNetwork()).write(networkFile);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleFile);
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(transitVehiclesFile);
		
	}
}
