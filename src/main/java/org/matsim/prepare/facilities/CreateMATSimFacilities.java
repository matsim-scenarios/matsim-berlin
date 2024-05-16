package org.matsim.prepare.facilities;

import com.google.common.math.Quantiles;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.TopologyException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.matsim.prepare.population.Attributes;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "facilities",
		description = "Creates MATSim facilities from shape-file and network"
)
public class CreateMATSimFacilities implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateMATSimFacilities.class);

	/**
	 * Filter link types that don't have a facility associated.
	 */
	public static final Set<String> IGNORED_LINK_TYPES = Set.of("motorway", "trunk",
			"motorway_link", "trunk_link", "secondary_link", "primary_link");

	@CommandLine.Option(names = "--network", required = true, description = "Path to car network")
	private Path network;

	@CommandLine.Option(names = "--output", required = true, description = "Path to output facility file")
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new CreateMATSimFacilities().execute(args);
	}

	/**
	 * Generate a new unique id within population.
	 */
	public static Id<ActivityFacility> generateId(ActivityFacilities facilities, SplittableRandom rnd) {

		Id<ActivityFacility> id;
		byte[] bytes = new byte[3];
		do {
			rnd.nextBytes(bytes);
			id = Id.create( "f" + HexFormat.of().formatHex(bytes), ActivityFacility.class);

		} while (facilities.getFacilities().containsKey(id));

		return id;
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shp file with facilities is required.");
			return 2;
		}

		Network completeNetwork = NetworkUtils.readNetwork(this.network.toString());
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(completeNetwork);
		Network carOnlyNetwork = NetworkUtils.createNetwork();
		filter.filter(carOnlyNetwork, Set.of(TransportMode.car));

		List<SimpleFeature> fts = shp.readFeatures();

		List<Holder> data = fts.parallelStream()
			.map(ft -> processFeature(ft, carOnlyNetwork))
			.filter(Objects::nonNull)
			.toList();

		// Compute statistics on the attraction values
		DescriptiveStatistics work = new DescriptiveStatistics();
		DescriptiveStatistics other = new DescriptiveStatistics();

		for (Holder d : data) {
			work.addValue(d.attractionWork);
			other.addValue(d.attractionOther);
		}

		// Upper bounds for attraction
		double workUpper = work.getPercentile(95);
		double otherUpper = other.getPercentile(95);

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		SplittableRandom rnd = new SplittableRandom();
		ActivityFacilitiesFactory f = facilities.getFactory();

		for (Holder h : data) {

			// Create mean coordinate
			OptionalDouble x = h.coords.stream().mapToDouble(Coord::getX).average();
			OptionalDouble y = h.coords.stream().mapToDouble(Coord::getY).average();

			if (x.isEmpty() || y.isEmpty()) {
				log.warn("Empty coordinate (Should never happen)");
				continue;
			}

			Id<ActivityFacility> id = generateId(facilities, rnd);

			ActivityFacility facility = f.createActivityFacility(id, CoordUtils.round(new Coord(x.getAsDouble(), y.getAsDouble())));
			for (String act : h.activities) {
				facility.addActivityOption(f.createActivityOption(act));
			}

			// Filter outliers from the attraction and normalize the attraction
			// This warrant for further investigate as the best way to normalize the attraction is not yet known
			facility.getAttributes().putAttribute(Attributes.ATTRACTION_WORK,
				Math.min(Math.max(h.attractionWork / 5, 1), workUpper)
			);
			facility.getAttributes().putAttribute(Attributes.ATTRACTION_OTHER,
				Math.min(Math.max(h.attractionOther / 5, 1), otherUpper)
			);

			facilities.addActivityFacility(facility);
		}

		log.info("Created {} facilities, writing to {}", facilities.getFacilities().size(), output);

		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(output.toString());

		return 0;
	}

	/**
	 * Sample points and choose link with the nearest points. Aggregate everything so there is at most one facility per link.
	 */
	private Holder processFeature(SimpleFeature ft, Network network) {

		Set<String> activities = activities(ft);
		if (activities.isEmpty())
			return null;

		// Pairs of coords and corresponding links
		List<Coord> coords = samplePoints((MultiPolygon) ft.getDefaultGeometry(), 23);
		List<Id<Link>> links = coords.stream().map(coord -> NetworkUtils.getNearestLinkExactly(network, coord).getId()).toList();

		Map<Id<Link>, Long> map = links.stream()
				.filter(l -> !IGNORED_LINK_TYPES.contains(NetworkUtils.getType(network.getLinks().get(l))))
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		// Everything could be filtered and map empty
		if (map.isEmpty())
			return null;

		Object2DoubleMap<String> features = new Object2DoubleOpenHashMap<>();
		for (int i = 0; i < ft.getAttributeCount(); i++) {
			if (ft.getAttribute(i) instanceof Number number) {
				features.put(ft.getFeatureType().getDescriptor(i).getLocalName(), number.doubleValue());
			}
		}

		double area = (double) ft.getAttribute("area");

		List<Map.Entry<Id<Link>, Long>> counts = map.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.toList();

		// The "main" link of the facility
		Id<Link> link = counts.get(counts.size() - 1).getKey();

		Holder holder = new Holder(link, activities, new ArrayList<>(),
			area * FacilityAttractionModelWork.INSTANCE.predict(features, null),
			area * FacilityAttractionModelOther.INSTANCE.predict(features, null)
		);

		// Search for the original drawn coordinate of the associated link
		for (int i = 0; i < links.size(); i++) {
			if (links.get(i).equals(link)) {
				holder.coords.add(coords.get(i));
				break;
			}
		}

		return holder;
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

			Coord coord = CoordUtils.round(new Coord(
					bbox.getMinX() + (bbox.getMaxX() - bbox.getMinX()) * rnd.nextDouble(),
					bbox.getMinY() + (bbox.getMaxY() - bbox.getMinY()) * rnd.nextDouble()
			));

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

		if (hasAttribute(ft, "work")) {
			act.add("work");
			act.add("work_business");
		}
		if (hasAttribute(ft, "shop")) {
			act.add("shop_other");
		}
		if (hasAttribute(ft, "shop_daily")) {
			act.add("shop_other");
			act.add("shop_daily");
		}
		if (hasAttribute(ft, "leisure"))
			act.add("leisure");
		if (hasAttribute(ft, "dining"))
			act.add("dining");
		if (hasAttribute(ft, "edu_higher"))
			act.add("edu_higher");
		if (hasAttribute(ft, "edu_prim")) {
			act.add("edu_primary");
			act.add("edu_secondary");
		}
		if (hasAttribute(ft, "edu_kiga"))
			act.add("edu_kiga");
		if (hasAttribute(ft, "edu_other"))
			act.add("edu_other");
		if (hasAttribute(ft, "p_business") || hasAttribute(ft, "medical") || hasAttribute(ft, "religious")) {
			act.add("personal_business");
		}
		if (hasAttribute(ft, "p_business"))
			act.add("work_business");

		return act;
	}

	private static boolean hasAttribute(SimpleFeature ft, String name) {
		return ft.getAttribute(name) != null &&
			(Boolean.TRUE.equals(ft.getAttribute(name)) ||
				(ft.getAttribute(name) instanceof Number number && number.intValue() > 0)
			);
	}

	/**
	 * Temporary data holder for facilities.
	 */
	private record Holder(Id<Link> linkId, Set<String> activities, List<Coord> coords,
						  double attractionWork, double attractionOther) {

	}

}
