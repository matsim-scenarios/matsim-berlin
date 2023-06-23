package org.matsim.prepare.berlinCounts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for *network-linkGeometries.csv file.
 * */
public class NetworkGeometryParser {

	private final Path filename;
	private final CSVFormat format;
	private final Logger logger = LogManager.getLogger(NetworkGeometryParser.class);

	public NetworkGeometryParser(Path filename){
		this(filename, CSVFormat.newFormat('c'));
	}

	public NetworkGeometryParser(Path filename, CSVFormat format){
		this.filename = filename;
		this.format = format;

		if (!filename.endsWith(".csv"))
			throw new RuntimeException("Network geometries must be provided as *.csv file!");
	}

	/**
	 * Reads the geometries file and returns a map with link id as key and the geometry of the link provided as LinString as value.
	 * */
	public Map<Id<Link>, LineString> parse(){
		logger.info("Try to parse geometries from {}", this.filename.toString());

		Map<Id<Link>, LineString> network = new HashMap<>();

		GeometryFactory factory = new GeometryFactory();

		try {
			CSVParser records = this.format.parse(new FileReader(this.filename.toString()));

			for (CSVRecord r : records) {

				String idAsString = r.get("LinkId");
				String raw = r.get("Geometry");

				LineString link = parseCoordinates(raw, factory);
				Id<Link> linkId = Id.createLinkId(idAsString);

				network.put(linkId, link);

			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		return network;
	}

	private LineString parseCoordinates(String coordinateSequence, GeometryFactory factory) {

		String[] split = coordinateSequence.split("\\)");

		Coordinate[] coordinates = new Coordinate[split.length];

		for (int i = 0; i < split.length; i++) {
			String coord = split[i];
			int toRemove = coord.indexOf("(");

			String cleaned = coord.substring(toRemove + 1);

			String[] split1 = cleaned.split(",");

			Coordinate coordinate = new Coordinate();
			coordinate.setX(Double.parseDouble(split1[0]));
			coordinate.setY(Double.parseDouble(split1[1]));

			coordinates[i] = coordinate;
		}

		return factory.createLineString(coordinates);
	}

	/**
	 * Returns the initialized csv format for customization.
	 * */
	public CSVFormat getFormat() {
		return format;
	}
}
