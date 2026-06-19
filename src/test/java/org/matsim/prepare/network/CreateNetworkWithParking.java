package org.matsim.prepare.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;
import org.wololo.jts2geojson.GeoJSONReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreateNetworkWithParking implements MATSimAppCommand {

	@CommandLine.Option(names = {"-n", "--network"}, required = true)
	private String network;

	@CommandLine.Option(names = "--parking-outside", required = true)
	private String parkingOutside;

	@CommandLine.Option(names = "--parking-inside", required = true)
	private String parkingInside;


	private static final Logger log = LogManager.getLogger(CreateNetworkWithParking.class);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateNetworkWithParking()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		log.info("Creating network with parking");

		Network network = NetworkUtils.readNetwork(this.network);

		List<ParkingPolygon> insideParking =
			readParkingGeoJson(parkingInside, "errechnete_anzahl_parkplaetze");

		List<ParkingPolygon> outsideParking =
			readParkingGeoJson(parkingOutside, "anzahl_parkplaetze");

		log.info("Network links: " + network.getLinks().size());

		log.info("Inside polygons: " + insideParking.size());
		log.info("Outside polygons: " + outsideParking.size());

		log.info(
			"Inside capacity: " +
				insideParking.stream().mapToInt(ParkingPolygon::capacity).sum()
		);

		log.info(
			"Outside capacity: " +
				outsideParking.stream().mapToInt(ParkingPolygon::capacity).sum()
		);

		// TODO:
		// spatial	matching to MATSim links

		return 0;
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

			if (geometryNode == null || properties == null) {
				continue;
			}

			Geometry geometry =
				geoJsonReader.read(geometryNode.toString());

			JsonNode capacityNode =
				properties.get(capacityField);

			int capacity = 0;

			if (capacityNode != null && !capacityNode.isNull()) {
				capacity = capacityNode.asInt();
			}

			result.add(new ParkingPolygon(geometry, capacity));
		}

		return result;
	}

	private record ParkingPolygon(
		Geometry geometry,
		int capacity
	) {
	}
}
