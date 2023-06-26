package org.matsim.prepare.berlinCounts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.jfree.data.io.CSV;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for *network-linkGeometries.csv file.
 * @deprecated part of network index. (TODO: remove before merge)
 * */
@Deprecated
public class NetworkGeometryParser {

	private final Path filename;
	private final CSVFormat format;
	private final Logger logger = LogManager.getLogger(NetworkGeometryParser.class);
	private MathTransform transformation = IdentityTransform.create(2);

	public NetworkGeometryParser(Path filename){
		this(filename, CSVFormat.newFormat(',').builder().setHeader().setSkipHeaderRecord(true).setQuote('\"').build());
	}

	public NetworkGeometryParser(Path filename, CSVFormat format){
		this.filename = filename;
		this.format = format;

		if (!filename.toString().endsWith(".csv"))
			throw new RuntimeException("Network geometries must be provided as *.csv file!");
	}

	public NetworkGeometryParser setCoordinateTransformation(String inputCrs, String targetCrs){
		this.transformation = getCoordinateTransformation(inputCrs, targetCrs);
		return this;
	}

	private MathTransform getCoordinateTransformation(String inputCrs, String targetCrs) {

		try {
			return CRS.findMathTransform(
					CRS.decode(inputCrs, true),
					CRS.decode(targetCrs, true)
			);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException("Please check the coordinate systems!");
		}
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

				network.put(linkId, this.transform(link));

			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		return network;
	}

	private LineString transform(LineString link) {

		try {
			return (LineString) JTS.transform(link, this.transformation);
		} catch (TransformException e) {
			logger.error("Error transforming linestring.", e);
			return null;
		}
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
