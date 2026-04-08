package org.matsim.prepare.population;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import static org.matsim.prepare.download.CalculateEmployedPopulation.Employment;
import static org.matsim.prepare.download.CalculateEmployedPopulation.Entry;
import static org.matsim.prepare.population.CreateBerlinPopulation.generateId;

@CommandLine.Command(
		name = "brandenburg-population",
		description = "Create synthetic population for brandenburg."
)
public class CreateBrandenburgPopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateBerlinPopulation.class);

	private static final int MAX_AGE = 100;

	@CommandLine.Option(names = "--population", description = "Path to population csv (Regional statistic)", required = true)
	private Path stats;

	@CommandLine.Option(names = "--employees", description = "Path to employees json (See CalculateEmployedPopulation).", required = true)
	private Path employedPath;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--sample", description = "Sample size to generate", defaultValue = "0.25")
	private double sample;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private FacilityOptions facilities = new FacilityOptions();

	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	private SplittableRandom rnd;
	private Population population;

	private Map<Integer, Employment> employed;

	public static void main(String[] args) {
		new CreateBrandenburgPopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file with Gemeinden and ARS/AGS codes is required.");
			return 2;
		}

		employed = new JsonMapper().readerFor(new TypeReference<Map<Integer, Employment>>() {})
				.readValue(employedPath.toFile());

		// Filter for Brandenburg
		Map<String, SimpleFeature> zones = shp.readFeatures().stream()
				.filter(ft -> ft.getAttribute("SN_L").equals("12"))
				.collect(Collectors.toMap(ft -> (String) ft.getAttribute("AGS"), ft -> ft));

		Set<String> found = new HashSet<>();

		rnd = new SplittableRandom(0);
		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		try (CSVParser parser = csv.createParser(stats)) {

			for (CSVRecord row : ProgressBar.wrap(parser.getRecords(), "Processing persons")) {

				String code = row.get("code");

				// Some cities are LK level
				if (!zones.containsKey(code) && code.length() == 5)
					code += "000";

				if (zones.containsKey(code)) {

					addPersons(row, code, (String) zones.get(code).getAttribute("ARS"), (MultiPolygon) zones.get(code).getDefaultGeometry());

					found.add(code);
				}
			}
		}

		for (Map.Entry<String, SimpleFeature> zone : zones.entrySet()) {
			if (!found.contains(zone.getKey()))
				log.warn("Zone not found in population statistic: {} ({})", zone.getValue().getAttribute("GEN"), zone.getKey());
		}

		log.info("Generated {} persons", population.getPersons().size());

		PopulationUtils.sortPersons(population);

		ProjectionUtils.putCRS(population, OpenBerlinScenario.CRS);
		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Add number of persons to the population according to entry.
	 */
	private void addPersons(CSVRecord r, String code, String ars, MultiPolygon geom) {

		int n = Integer.parseInt(r.get("n"));

		// Convert to english gender
		String gender = r.get("gender").equals("m") ? "m" : "f";
		String[] group = r.get("age").split(" - ");

		int low = Integer.parseInt(group[0]);
		int high = group[1].equals("inf") ? MAX_AGE : Integer.parseInt(group[1]);

		AgeDistribution ageDist = new AgeDistribution(low, high, gender.equals("m") ? AgeDistribution.Gender.MALE : AgeDistribution.Gender.FEMALE,
			Long.parseLong(ars) * 31 + gender.hashCode());

		// Landkreis
		int lk = Integer.parseInt(code.substring(0, code.length() - 3));
		if (!employed.containsKey(lk)) {
			log.error("No employment for {}", code);
			return;
		}

		Entry employed = gender.equals("m") ? this.employed.get(lk).men : this.employed.get(lk).women;

		PopulationFactory f = population.getFactory();

		for (int i = 0; i < n * sample; i++) {

			Person person = f.createPerson(generateId(population, "bb", rnd));

			int age = ageDist.sample();

			PersonUtils.setSex(person, gender);
			PersonUtils.setAge(person, age);
			PopulationUtils.putSubpopulation(person, "person");

			// All persons will be employed until employed population is empty.
			PersonUtils.setEmployed(person, employed.subtract(1 / sample, age));

			Coord coord = CreateBerlinPopulation.sampleHomeCoordinate(geom, OpenBerlinScenario.CRS, facilities, rnd, 1500);

			person.getAttributes().putAttribute(Attributes.HOME_X, coord.getX());
			person.getAttributes().putAttribute(Attributes.HOME_Y, coord.getY());

			person.getAttributes().putAttribute(Attributes.GEM, Integer.parseInt(code));
			person.getAttributes().putAttribute(Attributes.ARS, Long.parseLong(ars));

			Plan plan = f.createPlan();
			plan.addActivity(f.createActivityFromCoord("home", coord));

			person.addPlan(plan);
			person.setSelectedPlan(plan);

			population.addPerson(person);

		}
	}

}
