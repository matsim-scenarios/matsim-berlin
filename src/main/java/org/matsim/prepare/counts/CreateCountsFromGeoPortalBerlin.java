package org.matsim.prepare.counts;

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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOption;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.counts.NetworkIndex;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.run.RunOpenBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Data can be obtained at Data portal from Berlin:
 * <a href="https://fbinter.stadt-berlin.de/fb/?loginkey=alphaDataStart&alphaDataId=s_vmengen2019@senstadt">GeoPortal Berlin</a>
 */
@CommandLine.Command(name = "counts-from-geoportal", description = "Creates MATSim counts from Berlin FIS Broker count data")
public class CreateCountsFromGeoPortalBerlin implements MATSimAppCommand {


	/**
	 * TODO
	 * <p>
	 * Counts class has not the right structure
	 * <p>
	 * one station need to be matched to multiple links (because of both directions)
	 * <p>
	 * mapping.csv:
	 * <p>
	 * stationId;linkId
	 * <p>
	 * (station id can occur multiple times)
	 * <p>
	 * <p>
	 * counts.csv:
	 * <p>
	 * stationId;geometry;volume
	 * <p>
	 * <p>
	 * <p>
	 * Analysis Class:
	 * <p>
	 * mapping + counts als Input
	 * <p>
	 * comparison.csv:
	 * <p>
	 * stationId;observed_volume;simulated_volume
	 */


	private static final Logger log = LogManager.getLogger(CreateCountsFromGeoPortalBerlin.class);
	private final Counts<Link> car = new Counts<>();
	private final Counts<Link> freight = new Counts<>();
	@CommandLine.Option(names = "--network", description = "Network file path", required = true)
	private String networkFilePath;
	@CommandLine.Option(names = "--road-type", description = "road type patterns to filter the network")
	private List<String> roadTypes = List.of("motorway", "trunk", "primary");
	@CommandLine.Option(names = "--output", description = "output folder", required = true)
	private String output;
	@CommandLine.Option(names = "--network-geometries", description = "path to *linkGeometries.csv", required = true)
	private Path networkGeometries;
	@CommandLine.Mixin
	private CountsOption countsOption = new CountsOption();
	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

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
			Network network = NetworkUtils.readNetwork(networkFilePath);
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


		log.info("Processing simple features.");
		int counter = 0;
		int nullCounter = 0;
		for (SimpleFeature feature : features) {

			counter++;
			String id = (String) feature.getAttribute("link_id");
			if (countsOption.getIgnored().contains(id))
				continue;

			if (!(feature.getDefaultGeometry() instanceof MultiLineString ls)) {
				throw new RuntimeException("Geometry #" + counter + " is no LineString. Please check your shape file!.");
			}

			MultiLineString transformed = (MultiLineString) JTS.transform(ls, transformation);

			String direction = (String) feature.getAttribute("vricht");

			switch (direction) {
				case "R" -> handleMatch(feature, index.query(transformed, this::filterDirection), true);
				case "G" -> handleMatch(feature, index.query(transformed, this::filterOppositeDirection), false);
				case "B" -> {
					handleMatch(feature, index.query(transformed, this::filterDirection), true);
					handleMatch(feature, index.query(transformed, this::filterOppositeDirection), false);
				}
				default -> throw new IllegalStateException("Unknown direction " + direction);
			}

		}

		log.info("Could not match {} features", nullCounter);

		log.info("Write results to {}", output);

		// TODO: replace counts and write to csv

		new CountsWriter(car).write(output + "car-counts.xml.gz");
		new CountsWriter(freight).write(output + "freight-counts.xml.gz");

		return 0;
	}

	private void handleMatch(SimpleFeature feature, Link result, boolean inDirection) {

		// TODO: need different datastructure to store the results
		// check if there is already a match
		// one station can have two links (but only one per direction)

		long carDTV = (Long) feature.getAttribute("dtvw_kfz");
		long freightDTV = (Long) feature.getAttribute("dtvw_lkw");

		String name = (String) feature.getAttribute("str_name");
		car.createAndAddCount(result.getId(), name).createVolume(1, carDTV);
		freight.createAndAddCount(result.getId(), name).createVolume(1, freightDTV);

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

		return Math.abs(angle) < Math.PI / 2;
	}
}
