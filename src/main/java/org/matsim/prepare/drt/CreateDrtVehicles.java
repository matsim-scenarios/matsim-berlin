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

import com.opencsv.CSVWriter;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@CommandLine.Command(name = "create-drt-vehicles", description = "Create DRT vehicles for a scenario")
public class CreateDrtVehicles implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateDrtVehicles.class);
	private final SplittableRandom random = new SplittableRandom(1234);
	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private String networkPath;
	@CommandLine.Option(names = "--output", description = "Output prefix, actual names will be appended", required = true)
	private String output;
	@CommandLine.Option(names = "--drt-allowed-mode", description = "Network mode where drt is allowed to operate", defaultValue = "car")
	private String drtAllowedMode = "car";
	@CommandLine.Option(names = "--vehicles", description = "List of number of vehicles to generate", defaultValue = "100", split = ",")
	private Set<Integer> numberOfVehicles;
	@CommandLine.Option(names = "--seats", description = "Number of seats per vehicle", defaultValue = "4")
	private int seats = 4;
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions(OpenBerlinScenario.CRS, OpenBerlinScenario.CRS);

	public static void main(String[] args) {
		new CreateDrtVehicles().execute(args);
	}

	private void writeVehStartPositionsCSV(Network network, List<DvrpVehicleSpecification> vehicles, String fileNameBase) {
		Map<Id<Link>, Long> linkId2NrVeh = vehicles.stream().
			map(DvrpVehicleSpecification::getStartLinkId).
			collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(fileNameBase + "_startPositions.csv")), ';', '"', '"', "\n");
			writer.writeNext(new String[]{"link", "x", "y", "drtVehicles"}, false);
			linkId2NrVeh.forEach((linkId, numberVeh) -> {
				Coord coord = network.getLinks().get(linkId).getCoord();
				double x = coord.getX();
				double y = coord.getY();
				writer.writeNext(new String[]{linkId.toString(), "" + x, "" + y, "" + numberVeh}, false);
			});

			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	public final void createVehiclesByRandomPointInShape(Network network, int amount) {
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

		Geometry g = shp.getGeometry();
		ShpOptions.Index index = shp.createIndex(ProjectionUtils.getCRS(network), "_");
		CoordinateTransformation ct = shp.createInverseTransformation(ProjectionUtils.getCRS(network));

		ProgressBar pb = new ProgressBar("Creating DRT vehicles", amount);

		for (int i = 0; i < amount; i++) {
			Link link = null;

			while (link == null) {
				Point p = getRandomPointInServiceArea(g);
				link = NetworkUtils.getNearestLinkExactly(network, ct.transform(MGC.point2Coord(p)));
				if (index.contains(link.getFromNode().getCoord()) && index.contains(link.getToNode().getCoord())) {
					if (link.getAllowedModes().contains(drtAllowedMode)) {
						// ok
					} else {
						link = null;
					}
					// ok, the link is within the shape file
				} else {
					link = null;
				}
			}

			pb.step();

			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt" + i, DvrpVehicle.class))
				.startLinkId(link.getId())
				.capacity(seats)
				.serviceBeginTime(1)
				.serviceEndTime(30 * 3600)
				.build());

		}
		String fileNameBase = output + "drt-by-rndLocations-%dvehicles-%dseats".formatted(amount, seats);
		new FleetWriter(vehicles.stream()).write(fileNameBase + ".xml.gz");

		writeVehStartPositionsCSV(network, vehicles, fileNameBase);

		pb.close();
	}

	private Point getRandomPointInServiceArea(Geometry g) {
		Point p;
		double x;
		double y;
		do {
			x = g.getEnvelopeInternal().getMinX() + random.nextDouble()
				* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + random.nextDouble()
				* (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		}
		while (!g.contains(p));
		return p;
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			throw new IllegalArgumentException("A shape file with service area is required.");
		}

		Network network = NetworkUtils.readNetwork(networkPath);

		for (int numberOfVehicles : numberOfVehicles) {
			createVehiclesByRandomPointInShape(network, numberOfVehicles);
		}

		return 0;

	}
}
