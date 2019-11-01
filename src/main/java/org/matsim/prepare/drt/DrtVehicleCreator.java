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

package org.matsim.prepare.drt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.drt.BerlinShpUtils;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;


/**
 * @author  gleich
 *
 */
public class DrtVehicleCreator {
	private static final Logger log = Logger.getLogger(DrtVehicleCreator.class);

	private final String vehiclesFilePrefix;
	private final CoordinateTransformation ct;

	private final Scenario scenario ;
	private final Random random = MatsimRandom.getRandom();
	private final String drtNetworkMode = "drt";
	private final BerlinShpUtils shpUtils;
	private final Network drtNetwork;

	public static void main(String[] args) {

		String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
		String drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
	    CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:31468"); 
		
		String vehiclesFilePrefix = "berlin-drt-v5.5.drt-";
	    int numberOfVehicles = 10000;
	    int seats = 4;
		
		DrtVehicleCreator tvc = new DrtVehicleCreator(networkFile, drtServiceAreaShapeFile, vehiclesFilePrefix, ct);
		
		tvc.run(numberOfVehicles, seats);
}

	public DrtVehicleCreator(String networkfile, String drtServiceAreaShapeFile, String vehiclesFilePrefix, CoordinateTransformation ct) {
		this.vehiclesFilePrefix = vehiclesFilePrefix;
		this.ct = ct;
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkfile);
		this.scenario = ScenarioUtils.loadScenario(config);
		
		shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
		RunDrtOpenBerlinScenario.addDRTmode(scenario, drtNetworkMode, drtServiceAreaShapeFile);
		
		Set<String> modes = new HashSet<>();
		modes.add(drtNetworkMode);
		
		drtNetwork = NetworkUtils.createNetwork();
		Set<String> filterTransportModes = new HashSet<>();
		filterTransportModes.add(drtNetworkMode);
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(drtNetwork, filterTransportModes);
		new NetworkWriter(drtNetwork).write("drtNetwork.xml.gz");
	}
	
	public final void run(int amount, int seats) {	
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

		for (int i = 0 ; i< amount; i++) {
			Link link = null;
			
			while (link == null) {
				Point p = shpUtils.getRandomPointInServiceArea(random);
				link = NetworkUtils.getNearestLinkExactly(drtNetwork, ct.transform( MGC.point2Coord(p)));
				if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord()) && shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					if (link.getAllowedModes().contains(drtNetworkMode)) {
						// ok
					} else {
						link = null;
					}
					// ok, the link is within the shape file
				} else {
					link = null;
				}
			}
			
			if (i%100 == 0) log.info("#"+i);

			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt" + i, DvrpVehicle.class))
					.startLinkId(link.getId())
					.capacity(seats)
					.serviceBeginTime(Math.round(1))
					.serviceEndTime(Math.round(30 * 3600))
					.build());


		}
		new FleetWriter(vehicles.stream()).write(vehiclesFilePrefix + amount + "vehicles-" + seats + "seats.xml.gz");
	}

}
