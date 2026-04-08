package org.matsim.prepare.population;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.prepare.RunOpenBerlinCalibration;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import static org.matsim.prepare.population.AgeDistribution.Gender.*;

@CommandLine.Command(
	name = "berlin-population",
	description = "Create synthetic population for berlin."
)
public class CreateBerlinPopulation implements MATSimAppCommand {

	private static final NumberFormat FMT = NumberFormat.getInstance(Locale.GERMAN);

	private static final Logger log = LogManager.getLogger(CreateBerlinPopulation.class);
	private final CoordinateTransformation ct = new GeotoolsTransformation("EPSG:25833", "EPSG:25832");

	@CommandLine.Option(names = "--input", description = "Path to input csv data", required = true)
	private Path input;

	@CommandLine.Mixin
	private FacilityOptions facilities = new FacilityOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--year", description = "Year to use statistics from", defaultValue = "2019")
	private int year;

	@CommandLine.Option(names = "--sample", description = "Sample size to generate", defaultValue = "0.25")
	private double sample;

	private Map<String, MultiPolygon> lors;

	private SplittableRandom rnd;

	private Population population;

	private Map<AgeGroup, Map<AgeDistribution.Gender, AgeDistribution>> ageDistributions;

	public static void main(String[] args) {
		new CreateBerlinPopulation().execute(args);
	}

	/**
	 * Generate a new unique id within population.
	 */
	public static Id<Person> generateId(Population population, String prefix, SplittableRandom rnd) {

		Id<Person> id;
		byte[] bytes = new byte[4];
		do {
			rnd.nextBytes(bytes);
			id = Id.createPersonId(prefix + "_" + HexFormat.of().formatHex(bytes));

		} while (population.getPersons().containsKey(id));

		return id;
	}

	/**
	 * Samples a home coordinates from geometry and landuse.
	 */
	public static Coord sampleHomeCoordinate(MultiPolygon geometry, String crs, FacilityOptions facilities, SplittableRandom rnd, int tries) {

		Envelope bbox = geometry.getEnvelopeInternal();

		int i = 0;
		Coord coord;
		do {
			coord = facilities.select(crs, () -> new Coord(
				bbox.getMinX() + (bbox.getMaxX() - bbox.getMinX()) * rnd.nextDouble(),
				bbox.getMinY() + (bbox.getMaxY() - bbox.getMinY()) * rnd.nextDouble()
			));

			i++;

		} while (!geometry.contains(MGC.coord2Point(coord)) && i < tries);

		if (i == 1500)
			log.warn("Invalid coordinate generated");

		return RunOpenBerlinCalibration.roundCoord(coord);
	}

	@Override
	@SuppressWarnings("IllegalCatch")
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			log.error("Shape file with LOR zones is required.");
			return 2;
		}

		List<SimpleFeature> fts = shp.readFeatures();

		rnd = new SplittableRandom(0);
		lors = new HashMap<>();
		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		ageDistributions = Map.of(
			AgeGroup.YOUNG, Map.of(MALE, new AgeDistribution(0, 18, MALE, 0), FEMALE, new AgeDistribution(0, 18, FEMALE, 0)),
			AgeGroup.MIDDLE, Map.of(MALE, new AgeDistribution(18, 65, MALE, 0), FEMALE, new AgeDistribution(18, 65, FEMALE, 0)),
			AgeGroup.OLD, Map.of(MALE, new AgeDistribution(65, 100, MALE, 0), FEMALE, new AgeDistribution(65, 100, FEMALE, 0))
		);

		// Collect all LORs
		for (SimpleFeature ft : fts) {
			// Support both old and new key for different shape files
			String key = ft.getAttribute("SCHLUESSEL") != null ? "SCHLUESSEL" : "PLR_ID";

			lors.put((String) ft.getAttribute(key), (MultiPolygon) ft.getDefaultGeometry());
		}

		log.info("Found {} LORs", lors.size());

		CSVFormat.Builder format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader().setSkipHeaderRecord(true);

		try (CSVParser reader = new CSVParser(Files.newBufferedReader(input, Charset.forName("windows-1252")), format.build())) {

			for (CSVRecord row : ProgressBar.wrap(reader.getRecords(), "Processing LORs")) {

				int year = Integer.parseInt(row.get("Jahr"));
				if (this.year != year)
					continue;

				try {
					processLOR(row);
				} catch (RuntimeException e) {
					log.error("Error processing lor", e);
					log.error(row.toString());
				}
			}
		}

		log.info("Generated {} persons", population.getPersons().size());

		PopulationUtils.sortPersons(population);

		ProjectionUtils.putCRS(population, OpenBerlinScenario.CRS);
		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	private void processLOR(CSVRecord row) throws ParseException {

		String raumID = row.get("RaumID");
		int n = Integer.parseInt(row.get("Einwohnerinnen und Einwohner (EW) insgesamt"));

		double young = FMT.parse(row.get("Anteil der unter 18-Jährigen an Einwohnerinnen und Einwohner (EW) gesamt")).doubleValue() / 100;
		double old = FMT.parse(row.get("Anteil der 65-Jährigen und älter an Einwohnerinnen und Einwohner (EW) gesamt")).doubleValue() / 100;

		// x women for 100 men
		double women = FMT.parse(row.get("Geschlechterverteilung")).doubleValue();
		double quota = women / (100 + women);

		// sometimes this entry is not set
		double unemployed;
		try {
			unemployed = FMT.parse(row.get("Anteil Arbeitslose nach SGB II und SGB III an Einwohnerinnen und Einwohner (EW) im Alter von 15 bis unter 65 Jahren")).doubleValue() / 100;
		} catch (ParseException e) {
			unemployed = 0;
			log.warn("LOR {} {} has no unemployment", raumID, row.get(1));
		}

		long lor = Integer.parseInt(raumID);

		var sex = new EnumeratedAttributeDistribution<>(Map.of("f", quota, "m", 1 - quota), lor);
		var employment = new EnumeratedAttributeDistribution<>(Map.of(true, 1 - unemployed, false, unemployed), lor * 2);
		var ageGroup = new EnumeratedAttributeDistribution<>(Map.of(
			AgeGroup.YOUNG, young,
			AgeGroup.MIDDLE, 1.0 - young - old,
			AgeGroup.OLD, old
		), lor * 3);

		if (!lors.containsKey(raumID)) {
			log.warn("LOR {} not found", raumID);
			return;
		}

		MultiPolygon geom = lors.get(raumID);

		PopulationFactory f = population.getFactory();

		for (int i = 0; i < n * sample; i++) {

			String gender = sex.sample();

			Person person = f.createPerson(generateId(population, "berlin", rnd));
			PersonUtils.setSex(person, gender);
			PopulationUtils.putSubpopulation(person, "person");

			AgeDistribution.Gender g = gender.equals("f") ? FEMALE : MALE;

			AgeGroup group = ageGroup.sample();
			PersonUtils.setAge(person, ageDistributions.get(group).get(g).sample());

			if (group == AgeGroup.MIDDLE) {
				PersonUtils.setEmployed(person, employment.sample());
			} else if (group == AgeGroup.YOUNG || group == AgeGroup.OLD) {
				PersonUtils.setEmployed(person, false);
			}

			Coord coord = ct.transform(sampleHomeCoordinate(geom, "EPSG:25833", facilities, rnd, 1500));

			person.getAttributes().putAttribute(Attributes.HOME_X, coord.getX());
			person.getAttributes().putAttribute(Attributes.HOME_Y, coord.getY());

			person.getAttributes().putAttribute(Attributes.GEM, 11000000);
			person.getAttributes().putAttribute(Attributes.ARS, 110000000000L);
			person.getAttributes().putAttribute(Attributes.LOR, (int) lor);
			person.getAttributes().putAttribute(Attributes.ZONE, raumID.substring(0, 2));

			Plan plan = f.createPlan();
			plan.addActivity(f.createActivityFromCoord("home", coord));

			person.addPlan(plan);
			person.setSelectedPlan(plan);

			population.addPerson(person);
		}
	}

	private enum AgeGroup {
		YOUNG,
		MIDDLE,
		OLD
	}

}
