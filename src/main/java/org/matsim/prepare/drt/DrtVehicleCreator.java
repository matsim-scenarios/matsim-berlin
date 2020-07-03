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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.run.drt.BerlinShpUtils;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;


/**
 * @author  gleich
 *
 */
public class DrtVehicleCreator {
	private static final Logger log = Logger.getLogger(DrtVehicleCreator.class);

	private final CoordinateTransformation ct;

	private final Scenario scenario ;
	private final Random random = MatsimRandom.getRandom();
	private final String drtNetworkMode = "drt";
	private final BerlinShpUtils shpUtils;
	private final Network drtNetwork;
	private List<Pair<Id<Link>, Double>> links2weights = new ArrayList();

	public static void main(String[] args) {

		String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
		String populationFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";
		String facilitiesFile = "";
		String drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
	    CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:31468");

//		String vehiclesFilePrefix = "berlin-drt-v5.5.spandau_b-drt-by-actLocations-sqrt-";
		String vehiclesFilePrefix = "berlin-drt-v5.5.drt-by-rndLocations-";

		Set<Integer> numbersOfVehicles = new HashSet<>();
		numbersOfVehicles.add(20);
		numbersOfVehicles.add(30);
		numbersOfVehicles.add(50);
		numbersOfVehicles.add(80);
		numbersOfVehicles.add(100);
		numbersOfVehicles.add(120);
		numbersOfVehicles.add(150);
		numbersOfVehicles.add(200);
		numbersOfVehicles.add(250);
		numbersOfVehicles.add(300);
		numbersOfVehicles.add(400);
		numbersOfVehicles.add(500);
		numbersOfVehicles.add(600);
		numbersOfVehicles.add(700);
		numbersOfVehicles.add(800);
		numbersOfVehicles.add(900);
		numbersOfVehicles.add(1000);
		numbersOfVehicles.add(1200);
		numbersOfVehicles.add(1500);
		numbersOfVehicles.add(2000);
		numbersOfVehicles.add(2500);
		numbersOfVehicles.add(3000);
		numbersOfVehicles.add(4000);
		numbersOfVehicles.add(5000);
		numbersOfVehicles.add(10000);
		int seats = 4;
		
		DrtVehicleCreator tvc = new DrtVehicleCreator(networkFile, drtServiceAreaShapeFile, ct);
//		tvc.setLinkWeightsByActivities(populationFile, facilitiesFile);
//		tvc.setWeightsToSquareRoot();
		for (int numberOfVehicles: numbersOfVehicles) {
//			tvc.createVehiclesByWeightedDraw(numberOfVehicles, seats, vehiclesFilePrefix);
			tvc.createVehiclesByRandomPointInShape(numberOfVehicles, seats, vehiclesFilePrefix);
		}
}

	public DrtVehicleCreator(String networkfile, String drtServiceAreaShapeFile, CoordinateTransformation ct) {
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

	public final void createVehiclesByWeightedDraw(int amount, int seats, String vehiclesFilePrefix) {
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		EnumeratedDistribution<Id<Link>> weightedLinkDraw = new EnumeratedDistribution<>(links2weights);

		for (int i = 0 ; i< amount; i++) {
			Id<Link> linkId = weightedLinkDraw.sample();
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt" + i, DvrpVehicle.class))
					.startLinkId(linkId)
					.capacity(seats)
					.serviceBeginTime(Math.round(1))
					.serviceEndTime(Math.round(30 * 3600))
					.build());
		}
		String fileNameBase = vehiclesFilePrefix + amount + "vehicles-" + seats + "seats";
		new FleetWriter(vehicles.stream()).write(fileNameBase + ".xml.gz");

		writeVehStartPositionsCSV(vehicles, fileNameBase);
	}

	private void writeVehStartPositionsCSV(List<DvrpVehicleSpecification> vehicles, String fileNameBase) {
		Map<Id<Link>, Long> linkId2NrVeh = vehicles.stream().
				map(veh -> veh.getStartLinkId()).
				collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(fileNameBase + "_startPositions.csv")), ';', '"', '"', "\n");
			writer.writeNext(new String[]{"link", "x", "y", "drtVehicles"}, false);
			linkId2NrVeh.forEach( (linkId, numberVeh) -> {
				Coord coord = scenario.getNetwork().getLinks().get(linkId).getCoord();
				double x = coord.getX();
				double y = coord.getY();
				writer.writeNext(new String[]{linkId.toString(), "" + x, "" + y, "" + numberVeh}, false);
			});

			writer.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public final void setLinkWeightsByActivities(String populationFile, String facilitiesFile) {
		links2weights.clear(); // initial reset if already set before
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(populationFile); //TODO: coord transformations
		if (facilitiesFile != null && !facilitiesFile.equals("")) {
			MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
			facilitiesReader.readFile(facilitiesFile); //TODO: coord transformations
		}

		Map<Id<Link>, Long> link2Occurences = scenario.getPopulation().getPersons().values().stream().
				map(person -> person.getSelectedPlan()).
				map(plan -> plan.getPlanElements()).
				flatMap(planElements -> planElements.stream()).
				filter(planElement -> planElement instanceof Activity).
				map(planElement -> (Activity) planElement).
				filter(activity -> activity.getType().equals(TripStructureUtils.createStageActivityType(TransportMode.pt)) || !StageActivityTypeIdentifier.isStageActivity(activity.getType())).
				filter(activity -> shpUtils.isCoordInDrtServiceAreaWithBuffer(PopulationUtils.decideOnCoordForActivity(activity, scenario), 2000.0)).
				map(activity -> getLinkIdOnDrtNetwork(activity)).
				collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		for (Map.Entry<Id<Link>, Long> entry : link2Occurences.entrySet()) {
			// check link is in shape file
			Link link = scenario.getNetwork().getLinks().get(entry.getKey());

			if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord()) && shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
				links2weights.add(new Pair<>(entry.getKey(), entry.getValue().doubleValue()));
			} // else forget that link because it's not usable for drt
		}
	}

	private Id<Link> getLinkIdOnDrtNetwork(Activity activity) {
		Id<Link> linkId = PopulationUtils.decideOnLinkIdForActivity(activity, scenario);
		if (!drtNetwork.getLinks().containsKey(linkId)) {
			linkId = NetworkUtils.getNearestLink(drtNetwork, PopulationUtils.decideOnCoordForActivity(activity, scenario)).getId();
		}
		return linkId;
	}

	/**
	 * Overwrites weights with the square root of the previous weight. Shifts some vehicles to otherwise empty areas.
	 */
	public final void setWeightsToSquareRoot() {
		links2weights = links2weights.stream().map(pair -> new Pair<>(pair.getFirst(), Math.sqrt(pair.getSecond()))).collect(Collectors.toList());
	}

	public final void createVehiclesByRandomPointInShape(int amount, int seats, String vehiclesFilePrefix) {
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
		String fileNameBase = vehiclesFilePrefix + amount + "vehicles-" + seats + "seats";
		new FleetWriter(vehicles.stream()).write(fileNameBase + ".xml.gz");

		writeVehStartPositionsCSV(vehicles, fileNameBase);
	}

}
