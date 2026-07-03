package org.matsim.prepare.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.wololo.jts2geojson.GeoJSONReader;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateNetworkWithParking implements MATSimAppCommand {

	@CommandLine.Option(names = {"-n", "--network"}, required = true)
	private String networkFile;

	@CommandLine.Option(names = "--parking-outside", required = true)
	private String parkingOutside;

	@CommandLine.Option(names = "--parking-inside", required = true)
	private String parkingInside;

	private static final Logger log = LogManager.getLogger(CreateNetworkWithParking.class);

	private final CoordinateTransformation transform =
		TransformationFactory.getCoordinateTransformation(
			TransformationFactory.WGS84,
			"EPSG:25832"
		);

	private final List<Double> distancesInside = new ArrayList<>();
	private final List<Double> distancesOutside = new ArrayList<>();

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateNetworkWithParking()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		log.info("Creating network with parking");

		Network network = NetworkUtils.readNetwork(networkFile);

		List<ParkingPolygon> insideParking =
			readParkingGeoJson(parkingInside, "errechnete_anzahl_parkplaetze");

		List<ParkingPolygon> outsideParking =
			readParkingGeoJson(parkingOutside, "anzahl_parkplaetze");

		log.info("Network links: {}", network.getLinks().size());
		log.info("Inside polygons: {}", insideParking.size());
		log.info("Outside polygons: {}", outsideParking.size());

		assignToNetwork(network, insideParking, "inside_parking", distancesInside);
		assignToNetwork(network, outsideParking, "outside_parking", distancesOutside);

		log.info("Spatial matching completed");

		log.info("==== MATCH QUALITY REPORT ====");
		logStats("INSIDE", distancesInside);
		logStats("OUTSIDE", distancesOutside);

		NetworkUtils.writeNetwork(network, "test-network-with-parking.xml.gz");

		return 0;
	}

	/**
	 * OPTION A: nearest-link matching using centroid
	 */
	private void assignToNetwork(
		Network network,
		List<ParkingPolygon> polygons,
		String attributeName,
		List<Double> distanceCollector
	) {

		int unmatched = 0;

		for (ParkingPolygon p : polygons) {

			Coord centroid = transformCoord(p.geometry.getCentroid().getCoordinate());

			Link bestLink = null;
			double bestDist = Double.MAX_VALUE;

			for (Link link : network.getLinks().values()) {

				if (!isValidLink(link)) continue;

				double d = distance(centroid, link.getCoord());

				if (d < bestDist) {
					bestDist = d;
					bestLink = link;
				}
			}

			if (bestLink != null) {

				distanceCollector.add(bestDist);

				Object existing = bestLink.getAttributes().getAttribute(attributeName);
				int current = existing == null ? 0 : Integer.parseInt(existing.toString());

				bestLink.getAttributes().putAttribute(attributeName, current + p.capacity);

			} else {
				unmatched++;
			}
		}

		log.info("Finished matching '{}', unmatched polygons: {}", attributeName, unmatched);
	}

	/**
	 * Filter to avoid highways / irrelevant links
	 */
	private boolean isValidLink(Link link) {

		if (!link.getAllowedModes().contains("car")) return false;
		Object type = link.getAttributes().getAttribute("type");
		if (type == null) return true;
		String t = type.toString();
		return !(t.contains("motorway")
			|| t.contains("trunk"));
	}

	private Coord transformCoord(Coordinate c) {
		return transform.transform(new Coord(c.getX(), c.getY()));
	}

	private double distance(Coord c1, Coord c2) {
		double dx = c1.getX() - c2.getX();
		double dy = c1.getY() - c2.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	private void logStats(String label, List<Double> dists) {

		if (dists.isEmpty()) {
			log.warn("{}: No distances collected!", label);
			return;
		}

		Collections.sort(dists);

		double mean = dists.stream().mapToDouble(d -> d).average().orElse(0);
		double max = dists.stream().mapToDouble(d -> d).max().orElse(0);

		double p50 = dists.get((int)(0.50 * dists.size()));
		double p90 = dists.get((int)(0.90 * dists.size()));
		double p95 = dists.get((int)(0.95 * dists.size()));

		log.info("---- {} ----", label);
		log.info("mean = {}", mean);
		log.info("p50  = {}", p50);
		log.info("p90  = {}", p90);
		log.info("p95  = {}", p95);
		log.info("max  = {}", max);
	}

	private List<ParkingPolygon> readParkingGeoJson(
		String file,
		String capacityField
	) throws Exception {

		List<ParkingPolygon> result = new ArrayList<>();

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(new File(file));

		JsonNode features = root.get("features");

		GeoJSONReader geoJsonReader = new GeoJSONReader();

		for (JsonNode feature : features) {

			JsonNode geometryNode = feature.get("geometry");
			JsonNode properties = feature.get("properties");

			if (geometryNode == null || properties == null) continue;

			Geometry geometry = geoJsonReader.read(geometryNode.toString());

			JsonNode capacityNode = properties.get(capacityField);

			int capacity = (capacityNode != null && !capacityNode.isNull())
				? capacityNode.asInt()
				: 0;

			result.add(new ParkingPolygon(geometry, capacity));
		}

		return result;
	}

	private static class ParkingPolygon {
		final Geometry geometry;
		final int capacity;

		ParkingPolygon(Geometry geometry, int capacity) {
			this.geometry = geometry;
			this.capacity = capacity;
		}
	}
}
