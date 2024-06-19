package org.matsim.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.counts.NetworkIndex;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.run.OpenBerlinScenario;
import org.geotools.api.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * Data can be obtained at Data portal from Berlin.
 * <a href="https://fbinter.stadt-berlin.de/fb/?loginkey=alphaDataStart&alphaDataId=s_vmengen2019@senstadt">GeoPortal Berlin</a>
 */
@CommandLine.Command(name = "counts-from-geoportal", description = "Creates MATSim counts from Berlin FIS Broker count data")
@CommandSpec(
	requireNetwork = true,
	produces = {"dtv_berlin.csv", "dtv_links_capacity.csv"}
)
public class CreateCountsFromGeoPortalBerlin implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateCountsFromGeoPortalBerlin.class);

	/**
	 * Assumed peak traffic during one hour, relative to DTV.
	 */
	private static final double PEAK_PERCENTAGE = 0.09;

	private static final List<String> ROAD_TYPES = List.of("motorway", "trunk", "primary", "secondary", "tertiary", "residential");

	/**
	 * Stores all mappings.
	 */
	private final Map<Id<Link>, Mapping> mappings = new HashMap<>();
	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(CreateCountsFromGeoPortalBerlin.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(CreateCountsFromGeoPortalBerlin.class);
	@CommandLine.Option(names = "--network-geometries", description = "path to *linkGeometries.csv")
	private Path networkGeometries;
	@CommandLine.Mixin
	private CountsOptions counts = new CountsOptions();
	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new CreateCountsFromGeoPortalBerlin().execute(args);
	}

	private static MathTransform getCoordinateTransformation(String inputCRS, String targetCRS) {
		try {
			return CRS.findMathTransform(CRS.decode(inputCRS, true), CRS.decode(targetCRS, true));
		} catch (FactoryException e) {
			throw new RuntimeException("Please check the coordinate systems!", e);
		}
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			log.error("FIS-Broker shape file path [--shp] is required!");
			return 2;
		}

		log.info("Read shape file.");
		List<SimpleFeature> features = shp.readFeatures();
		List<Predicate<Link>> roadTypeFilter = createRoadTypeFilter();

		log.info("Read network and apply filters");
		Network filteredNetwork;
		{
			Network network = input.getNetwork();
			NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
			filter.addLinkFilter(link -> link.getAllowedModes().contains(TransportMode.car));
			filter.addLinkFilter(link -> roadTypeFilter.stream().anyMatch(predicate -> predicate.test(link)));

			filteredNetwork = filter.applyFilters();
		}

		log.info("Build Index.");
		Map<Id<Link>, Geometry> geometries = networkGeometries != null
			? NetworkIndex.readGeometriesFromSumo(networkGeometries.toString(), IdentityTransform.create(2))
			: new HashMap<>();
		NetworkIndex<MultiLineString> index = new NetworkIndex<>(filteredNetwork, geometries, 20, toMatch -> toMatch);

		// Compare two line strings
		index.setDistanceCalculator(NetworkIndex::minHausdorffDistance);

		MathTransform transformation = getCoordinateTransformation(shp.getShapeCrs(), OpenBerlinScenario.CRS);

		log.info("Processing features.");
		int counter = 0;
		for (SimpleFeature feature : features) {

			counter++;
			String id = (String) feature.getAttribute("link_id");
			if (counts.getIgnored().contains(id))
				continue;

			if (!(feature.getDefaultGeometry() instanceof MultiLineString ls)) {
				throw new RuntimeException("Geometry #" + counter + " is no LineString. Please check your shape file!.");
			}

			MultiLineString transformed = (MultiLineString) JTS.transform(ls, transformation);

			String direction = (String) feature.getAttribute("vricht");

			switch (direction) {
				case "R" -> handleMatch(feature, index.query(transformed, this::filterDirection), null);
				case "G" -> handleMatch(feature, null, index.query(transformed, this::filterOppositeDirection));
				case "B" ->
					handleMatch(feature, index.query(transformed, this::filterDirection), index.query(transformed, this::filterOppositeDirection));
				default -> throw new IllegalStateException("Unknown direction " + direction);
			}
		}

		Path out = output.getPath();
		log.info("Write results to {}", out);

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(out), CSVFormat.DEFAULT)) {

			csv.printRecord("station_id", "station_name", "to_link", "from_link", "vol_car", "vol_freight");

			// There are double entries
			Set<Mapping> ms = new HashSet<>(mappings.values());

			for (Mapping m : ms) {
				String to = m.toDirection != null ? m.toDirection.toString() : "";
				String from = m.fromDirection != null ? m.fromDirection.toString() : "";
				csv.printRecord(m.stationId, m.stationName, to, from, m.avgCar, m.avgHGV);
			}
		}

		InverseIndex invIndex = new InverseIndex(features, transformation);

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.getPath("dtv_links_capacity.csv")), CSVFormat.DEFAULT)) {
			createLinksCapacity(csv, filteredNetwork, invIndex, mappings);
		}

		return 0;
	}

	private void handleMatch(SimpleFeature feature, Link toDirection, Link fromDirection) {

		String name = (String) feature.getAttribute("str_name");
		// throws exceptions if attribute isn't casted to type 'long'
		long carDTVLong = (long) feature.getAttribute("dtvw_kfz");
		int carDTV = (int) carDTVLong;
		long freightDTVLong = (long) feature.getAttribute("dtvw_lkw");
		int freightDTV = (int) freightDTVLong;

		Mapping m = new Mapping((String) feature.getAttribute("link_id"), name,
			toDirection != null ? toDirection.getId() : null,
			fromDirection != null ? fromDirection.getId() : null,
			carDTV, freightDTV);

		// A link can not be mapped twice
		if ((m.toDirection != null && mappings.containsKey(m.toDirection)) ||
			(m.fromDirection != null && mappings.containsKey(m.fromDirection))) {
			log.warn("Entry with links that are already mapped {}", m);
			return;
		}

		if (m.toDirection != null)
			mappings.put(m.toDirection, m);

		if (m.fromDirection != null)
			mappings.put(m.fromDirection, m);

	}

	/**
	 * Create comparison of expected capacity and estimated peak for mapped links.
	 */
	private void createLinksCapacity(CSVPrinter csv, Network network, InverseIndex invIndex, Map<Id<Link>, Mapping> mappings) throws IOException {

		csv.printRecord("link_id", "station_id", "matched_name", "road_type", "peak_volume", "link_capacity", "is_valid");

		for (Link link : network.getLinks().values()) {

			String stationId = null;
			String stationName = null;
			double volume = 0;
			if (mappings.containsKey(link.getId())) {
				Mapping m = mappings.get(link.getId());
				stationId = m.stationId;
				stationName = m.stationName;
				volume = (m.avgCar + m.avgHGV) * PEAK_PERCENTAGE;
				// Links in both direction are assumed to be slightly asymmetric
				if (m.fromDirection != null && m.toDirection != null)
					volume /= 1.8;
			} else {
				SimpleFeature ft = invIndex.query(link);
				if (ft != null) {
					// actually nothing is mapped
					log.info("Inverse mapped {}", ft);

					stationId = (String) ft.getAttribute("link_id");
					stationName = (String) ft.getAttribute("str_name");
					volume = (long) ft.getAttribute("dtvw_kfz") + (long) ft.getAttribute("dtvw_lkw");
					volume *= PEAK_PERCENTAGE;

					String direction = (String) ft.getAttribute("vricht");
					if (direction.equals("B"))
						volume /= 1.8;
				}
			}

			if (stationId != null) {
				csv.printRecord(link.getId(), stationId, stationName, NetworkUtils.getHighwayType(link),
					volume, link.getCapacity(), link.getCapacity() >= volume);
			}
		}
	}

	private List<Predicate<Link>> createRoadTypeFilter() {

		List<Predicate<Link>> filter = new ArrayList<>();

		for (String type : CreateCountsFromGeoPortalBerlin.ROAD_TYPES) {
			Predicate<Link> p = link -> {
				String attr = NetworkUtils.getHighwayType(link);
				return attr.toLowerCase().contains(type);
			};

			filter.add(p);
		}
		return filter;
	}

	private boolean filterDirection(NetworkIndex.LinkGeometry link, MultiLineString other) {
		Coordinate[] coordinates = other.getCoordinates();
		Coordinate from = coordinates[0];
		Coordinate to = coordinates[coordinates.length - 1];

		GeometryFactory f = JTSFactoryFinder.getGeometryFactory();

		double angle = NetworkIndex.angle((LineString) link.geometry(), f.createLineString(new Coordinate[]{from, to}));

		return Math.abs(angle) < (Math.PI / 2) * 0.9;
	}

	private boolean filterOppositeDirection(NetworkIndex.LinkGeometry link, MultiLineString other) {
		Coordinate[] coordinates = other.getCoordinates();
		Coordinate from = coordinates[0];
		Coordinate to = coordinates[coordinates.length - 1];

		GeometryFactory f = JTSFactoryFinder.getGeometryFactory();

		// Same as above, but to and from are reversed
		double angle = NetworkIndex.angle((LineString) link.geometry(), f.createLineString(new Coordinate[]{to, from}));

		return Math.abs(angle) < (Math.PI / 2) * 0.9;
	}

	private record Mapping(String stationId, String stationName, Id<Link> toDirection, Id<Link> fromDirection, int avgCar, int avgHGV) {
	}

	/**
	 * Maps networks links to geometry. Which is the otherway round than the usual network index.
	 */
	private static class InverseIndex {

		private static final double THRESHOLD = 15;

		private final STRtree index;

		InverseIndex(List<SimpleFeature> features, MathTransform ct) throws TransformException {
			index = new STRtree();

			for (SimpleFeature ft : features) {
				Geometry geometry = JTS.transform((Geometry) ft.getDefaultGeometry(), ct);
				index.insert(geometry.getBoundary().getEnvelopeInternal(), ft);
			}

			index.build();
		}

		public SimpleFeature query(Link link) {

			GeometryFactory f = JTSFactoryFinder.getGeometryFactory();

			Coordinate[] coord = {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())};
			LineString ls = f.createLineString(coord);

			List<SimpleFeature> result = index.query(ls.buffer(THRESHOLD).getBoundary().getEnvelopeInternal());
			Comparator<SimpleFeature> cmp = Comparator.comparingDouble(value -> NetworkIndex.minHausdorffDistance(ls, (Geometry) value.getDefaultGeometry()));
			Optional<SimpleFeature> first = result.stream()
				.sorted(cmp.reversed())
				.filter(feature -> NetworkIndex.minHausdorffDistance(ls, (Geometry) feature.getDefaultGeometry()) < THRESHOLD)
				.findFirst();

			return first.orElse(null);
		}

	}

}
