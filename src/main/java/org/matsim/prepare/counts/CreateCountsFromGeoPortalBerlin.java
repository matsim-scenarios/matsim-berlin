package org.matsim.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOption;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.counts.NetworkIndex;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.run.RunOpenBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Data can be obtained at Data portal from Berlin:
 * <a href="https://fbinter.stadt-berlin.de/fb/?loginkey=alphaDataStart&alphaDataId=s_vmengen2019@senstadt">GeoPortal Berlin</a>
 */
@CommandLine.Command(name = "counts-from-geoportal", description = "Creates MATSim counts from Berlin FIS Broker count data")
@CommandSpec(
		requireNetwork = true,
		produces = {"dtv_berlin.csv", "mapping_overview.csv", "stations_per_road_type.csv"}
)
public class CreateCountsFromGeoPortalBerlin implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateCountsFromGeoPortalBerlin.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(CreateCountsFromGeoPortalBerlin.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(CreateCountsFromGeoPortalBerlin.class);

	@CommandLine.Option(names = "--network-geometries", description = "path to *linkGeometries.csv", required = true)
	private Path networkGeometries;

	@CommandLine.Option(names = "--road-type", description = "road type patterns to filter the network")
	private List<String> roadTypes = List.of("motorway", "trunk", "primary");

	@CommandLine.Mixin
	private CountsOption counts = new CountsOption();
	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	private static final double peakPercentage = 0.09;

	/**
	 * Stores all mappings.
	 */
	private Map<Id<Link>, Mapping> mappings = new HashMap<>();

	public static void main(String[] args) {
		new CreateCountsFromGeoPortalBerlin().execute(args);
	}

	private static MathTransform getCoordinateTransformation(String inputCRS, String targetCRS) {
		try {
			return CRS.findMathTransform(
					CRS.decode(inputCRS, true),
					CRS.decode(targetCRS, true)
			);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException("Please check the coordinate systems!");
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
		List<Predicate<Link>> roadTypeFilter = createRoadTypeFilter(roadTypes);

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
		NetworkIndex<MultiLineString> index = new NetworkIndex<>(filteredNetwork,
				NetworkIndex.readGeometriesFromSumo(networkGeometries.toString(), IdentityTransform.create(2)),
				20, toMatch -> toMatch);

		// Compare two line strings
		index.setDistanceCalculator(NetworkIndex::minHausdorffDistance);

		MathTransform transformation = getCoordinateTransformation(shp.getShapeCrs(), RunOpenBerlinScenario.CRS);

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
				case "B" -> handleMatch(feature, index.query(transformed, this::filterDirection), index.query(transformed, this::filterOppositeDirection));
				default -> throw new IllegalStateException("Unknown direction " + direction);
			}
		}

		Path out = output.getPath();
		log.info("Write results to {}", out);

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(out), CSVFormat.DEFAULT)) {

			csv.printRecord("station", "to_link", "from_link", "vol_car", "vol_freight");

			// There are double entries
			Set<Mapping> ms = new HashSet<>(mappings.values());

			for (Mapping m : ms) {
				csv.printRecord(m.station, m.toDirection, m.fromDirection, m.avgCar, m.avgHGV);
			}

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

		Mapping m = new Mapping(name,
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

	private List<Predicate<Link>> createRoadTypeFilter(List<String> types) {

		List<Predicate<Link>> filter = new ArrayList<>();

		for (String type : types) {
			Pattern pattern = Pattern.compile(type, Pattern.CASE_INSENSITIVE);

			Predicate<Link> p = link -> {
				var attr = NetworkUtils.getHighwayType(link);
				return pattern.matcher(attr).find();
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

		return Math.abs(angle) < Math.PI / 2;
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

	private record Mapping(String station, Id<Link> toDirection, Id<Link> fromDirection, int avgCar, int avgHGV) {

		public boolean hasBothDirections() {
			return fromDirection != null && toDirection != null;
		}
	}
}
