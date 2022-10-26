package org.matsim.synthetic;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;


import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
		name = "create-population",
		description = "Create a synthetic population."
)
public class CreatePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreatePopulation.class);

	@CommandLine.Option(names = "--input", description = "Path to input csv data", required = true)
	private Path input;

	@CommandLine.Mixin
	private LanduseOptions landuse = new LanduseOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	public static void main(String[] args) {
		new CreatePopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file with LOR zones is required.");
			return 2;
		}

		List<SimpleFeature> fts = shp.readFeatures();

		Map<String, MultiPolygon> lors = new HashMap<>();

		// Collect all LORs
		for (SimpleFeature ft : fts) {
			lors.put((String) ft.getAttribute("SCHLUESSEL"), (MultiPolygon) ft.getDefaultGeometry());
		}

		System.out.println(lors);

		try (CSVParser reader = new CSVParser(Files.newBufferedReader(input, Charset.forName("windows-1252")), CSVFormat.DEFAULT.withDelimiter(';'))) {
			for (CSVRecord record : reader) {
				//System.out.println(record);
			}
		}

		return 0;
	}
}
