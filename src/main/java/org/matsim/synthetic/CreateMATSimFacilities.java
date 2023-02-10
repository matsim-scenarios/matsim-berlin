package org.matsim.synthetic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.TopologyException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "facilities",
		description = "Creates MATSim facilities from shape-file and network"
)
public class CreateMATSimFacilities implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateMATSimFacilities.class);

	@CommandLine.Option(names = "--network", required = true, description = "Path to car network")
	private Path network;

	@CommandLine.Option(names = "--output", required = true, description = "Path to output facility file")
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new CreateMATSimFacilities().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shp file with facilities is required.");
			return 2;
		}

		Network network = NetworkUtils.readNetwork(this.network.toString());
		List<SimpleFeature> fts = shp.readFeatures();

		Map<Id<Link>, Holder> data = new ConcurrentHashMap<>();

		fts.parallelStream().forEach(ft -> processFeature(ft, network, data));

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		ActivityFacilitiesFactory f = facilities.getFactory();

		for (Map.Entry<Id<Link>, Holder> e : data.entrySet()) {

			Holder h = e.getValue();

			Id<ActivityFacility> id = Id.create(String.join("_", h.ids), ActivityFacility.class);

			// Create mean coordinate
			OptionalDouble x = h.coords.stream().mapToDouble(Coord::getX).average();
			OptionalDouble y = h.coords.stream().mapToDouble(Coord::getY).average();

			if (x.isEmpty() || y.isEmpty()) {
				log.warn("Empty coordinate (Should never happen)");
				continue;
			}

			ActivityFacility facility = f.createActivityFacility(id, new Coord(x.getAsDouble(), y.getAsDouble()));
			for (String act : h.activities) {
				facility.addActivityOption(f.createActivityOption(act));
			}

			facilities.addActivityFacility(facility);
		}

		log.info("Created {} facilities, writing to {}", facilities.getFacilities().size(), output);

		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(output.toString());

		return 0;
	}

	/**
	 * Sample points and choose link with the most nearest points. Aggregate everything so there is at most one facility per link.
	 */
	private void processFeature(SimpleFeature ft, Network network, Map<Id<Link>, Holder> data) {

		String[] id = ft.getID().split("\\.");

		List<Coord> coords = samplePoints((MultiPolygon) ft.getDefaultGeometry(), 23);
		List<Id<Link>> links = coords.stream().map(coord -> NetworkUtils.getNearestLinkExactly(network, coord).getId()).toList();

		Map<Id<Link>, Long> map = links.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		List<Map.Entry<Id<Link>, Long>> counts = map.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.toList();

		Id<Link> link = counts.get(counts.size() - 1).getKey();

		Holder holder = data.computeIfAbsent(link, k -> new Holder(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), Collections.synchronizedList(new ArrayList<>())));

		holder.ids.add(id[id.length - 1]);
		holder.activities.addAll(activities(ft));
		for (int i = 0; i < links.size(); i++) {
			if (links.get(i).equals(link))
				holder.coords.add(coords.get(i));
		}
	}

	/**
	 * Sample coordinates within polygon.
	 */
	private List<Coord> samplePoints(MultiPolygon geometry, int n) {

		SplittableRandom rnd = new SplittableRandom();

		List<Coord> result = new ArrayList<>();
		Envelope bbox = geometry.getEnvelopeInternal();
		int max = n * 10;
		for (int i = 0; i < max && result.size() < n; i++) {

			Coord coord = new Coord(
					bbox.getMinX() + (bbox.getMaxX() - bbox.getMinX()) * rnd.nextDouble(),
					bbox.getMinY() + (bbox.getMaxY() - bbox.getMinY()) * rnd.nextDouble()
			);

			try {
				if (geometry.contains(MGC.coord2Point(coord))) {
					result.add(coord);
				}
			} catch (TopologyException e) {
				if (geometry.getBoundary().contains(MGC.coord2Point(coord))) {
					result.add(coord);
				}
			}

		}

		if (result.isEmpty())
			result.add(MGC.point2Coord(geometry.getCentroid()));

		return result;
	}

	private Set<String> activities(SimpleFeature ft) {
		Set<String> act = new HashSet<>();

		if (Boolean.TRUE == ft.getAttribute("work"))
			act.add("work");
		if (Boolean.TRUE == ft.getAttribute("shop") || Boolean.TRUE == ft.getAttribute("shop_food"))
			act.add("shopping");
		if (Boolean.TRUE == ft.getAttribute("leisure"))
			act.add("leisure");
		if (Boolean.TRUE == ft.getAttribute("education"))
			act.add("edu");
		if (Boolean.TRUE == ft.getAttribute("p_business") || Boolean.TRUE == ft.getAttribute("medical") || Boolean.TRUE == ft.getAttribute("religious"))
			act.add("other");

		return act;
	}

	/**
	 * Temporary data holder for facilities.
	 */
	private record Holder(Set<String> ids, Set<String> activities, List<Coord> coords) {

	}

}
