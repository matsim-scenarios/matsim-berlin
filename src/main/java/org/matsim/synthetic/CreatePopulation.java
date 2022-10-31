package org.matsim.synthetic;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.IntStream;

@CommandLine.Command(
		name = "create-population",
		description = "Create a synthetic population."
)
public class CreatePopulation implements MATSimAppCommand {

	private final static NumberFormat FMT = NumberFormat.getInstance(Locale.GERMAN);

	private static final Logger log = LogManager.getLogger(CreatePopulation.class);

	@CommandLine.Option(names = "--input", description = "Path to input csv data", required = true)
	private Path input;

	@CommandLine.Mixin
	private LanduseOptions landuse = new LanduseOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--year", description = "Year to use statistics from", defaultValue = "2019")
	private int year;

	@CommandLine.Option(names = "--sample", description = "Sample size to generate", defaultValue = "0.25")
	private double sample;


	private Map<String, MultiPolygon> lors;

	private Random rnd;

	private Population population;
	long id;

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

		rnd = new Random(0);
		lors = new HashMap<>();
		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		id = 0;

		// Collect all LORs
		for (SimpleFeature ft : fts) {
			lors.put((String) ft.getAttribute("SCHLUESSEL"), (MultiPolygon) ft.getDefaultGeometry());
		}

		CSVFormat.Builder format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true);

		try (CSVParser reader = new CSVParser(Files.newBufferedReader(input, Charset.forName("windows-1252")), format.build())) {

			for (CSVRecord record : reader) {

				int year = Integer.parseInt(record.get("Jahr"));
				if (this.year != year)
					continue;

				try {
					processLOR(record);
				} catch (Exception e) {
					log.error("Error processing lor", e);
					log.error(record.toString());
				}
			}
		}

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	private void processLOR(CSVRecord record) throws ParseException {

		String raumID = record.get("RaumID");
		int n = Integer.parseInt(record.get("Einwohnerinnen und Einwohner (EW) insgesamt"));

		double young = FMT.parse(record.get("Anteil der unter 18-Jährigen an Einwohnerinnen und Einwohner (EW) gesamt")).doubleValue() / 100;
		double old = FMT.parse(record.get("Anteil der 65-Jährigen und älter an Einwohnerinnen und Einwohner (EW) gesamt")).doubleValue() / 100;

		// x women for 100 men
		double women = FMT.parse(record.get("Geschlechterverteilung")).doubleValue();
		double quota = women / (100 + women);

		// sometimes this entry is not set
		double unemployed;
		try {
			unemployed = FMT.parse(record.get("Anteil Arbeitslose nach SGB II und SGB III an Einwohnerinnen und Einwohner (EW) im Alter von 15 bis unter 65 Jahren")).doubleValue() / 100;
		} catch (ParseException e) {
			unemployed = 0;
			log.warn("LOR {} has no unemployment", record.get(1));
		}

		var sex = new EnumeratedAttributeDistribution<>(Map.of("f", quota, "m", 1 - quota));
		var employment = new EnumeratedAttributeDistribution<>(Map.of(true, 1 - unemployed, false, unemployed));
		var ageGroup = new EnumeratedAttributeDistribution<>(Map.of(
				AgeGroup.YOUNG, young,
				AgeGroup.MIDDLE, 1.0 - young - old,
				AgeGroup.OLD, old
		));

		MultiPolygon geom = lors.get(raumID);

		PopulationFactory f = population.getFactory();

		var youngDist = new UniformAttributeDistribution<>(IntStream.range(1, 18).boxed().toList());
		var middleDist = new UniformAttributeDistribution<>(IntStream.range(18, 65).boxed().toList());
		var oldDist = new UniformAttributeDistribution<>(IntStream.range(65, 100).boxed().toList());

		for (int i = 0; i < n * sample; i++) {

			Person person = f.createPerson(Id.createPersonId(id++));
			PersonUtils.setSex(person, sex.sample());

			AgeGroup group = ageGroup.sample();

			if (group == AgeGroup.MIDDLE) {
				PersonUtils.setAge(person, middleDist.sample());
				PersonUtils.setEmployed(person, employment.sample());
			} else if (group == AgeGroup.YOUNG) {
				PersonUtils.setAge(person, youngDist.sample());
				PersonUtils.setEmployed(person, false);
			} else if (group == AgeGroup.OLD) {
				PersonUtils.setAge(person, oldDist.sample());
				PersonUtils.setEmployed(person, false);
			}

			Coord coord = sampleHomeCoordinate(geom);

			person.getAttributes().putAttribute("home_x", coord.getX());
			person.getAttributes().putAttribute("home_y", coord.getY());

			Plan plan = f.createPlan();
			plan.addActivity(f.createActivityFromCoord("home", coord));

			person.addPlan(plan);

			population.addPerson(person);
		}
	}

	private Coord sampleHomeCoordinate(MultiPolygon geometry) {

		Envelope bbox = geometry.getEnvelopeInternal();

		int i = 0;
		Coord coord;
		do {
			coord = landuse.select("EPSG:25833", () -> new Coord(
					bbox.getMinX() + (bbox.getMaxX() - bbox.getMinX()) * rnd.nextDouble(),
					bbox.getMinY() + (bbox.getMaxY() - bbox.getMinY()) * rnd.nextDouble()
			));

			i++;

		} while (!geometry.contains(MGC.coord2Point(coord)) && i < 500);

		if (i == 500)
			log.warn("Invalid coordinate generated");

		return coord;
	}

	private enum AgeGroup {
		YOUNG,
		MIDDLE,
		OLD
	}

}
