package org.matsim.synthetic;

import it.unimi.dsi.fastutil.longs.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "assign-commuters", description = "Assign commuting destination and distance")
public class AssignCommuters implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(AssignCommuters.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to input population", required = true)
	private Path output;

	@CommandLine.Option(names = "--commuter", description = "Path to commuter.csv", required = true)
	private Path commuterPath;

	@CommandLine.Option(names = "--sample", description = "Sample size of the population", defaultValue = "0.25")
	private double sample;

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	private Random rnd = new Random(0);
	private Map<Long, SimpleFeature> zones;
	private Long2ObjectMap<Long2DoubleMap> commuter;

	/**
	 * Draw distances from this distribution. Fit with dists < 50km.
	 */
	private final RealDistribution distribution = new GammaDistribution(new Well19937c(1), 1.06639881, 1 / 0.07092311);

	public static void main(String[] args) {
		new AssignCommuters().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file with Gemeinden and ARS/AGS codes is required.");
			return 2;
		}

		zones = shp.readFeatures().stream()
				.collect(Collectors.toMap(ft -> Long.parseLong((String) ft.getAttribute("ARS")), ft -> ft));

		log.info("Read {} zones", zones.size());

		// outgoing commuters
		commuter = new Long2ObjectOpenHashMap<>();

		// read commuters
		try (CSVParser parser = csv.createParser(commuterPath)) {
			for (CSVRecord record : parser) {
				long from, to;
				try {
					from = Long.parseLong(record.get("from"));
					to = Long.parseLong(record.get("to"));
				} catch (NumberFormatException e) {
					continue;
				}

				String n = record.get("n");
				commuter.computeIfAbsent(from, k -> new Long2DoubleOpenHashMap())
						.mergeDouble(to, Integer.parseInt(n), Double::sum);
			}
		}

		log.info("Read commuters for {} districts", commuter.size());

		Population population = PopulationUtils.readPopulation(input.toString());

		List<Person> persons = new ArrayList<>(population.getPersons().values());

		Collections.shuffle(persons, rnd);

		persons.forEach(this::run);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {

		if (PersonUtils.isEmployed(person)) {

			long ars = (long) person.getAttributes().getAttribute(Attributes.ARS);

			MultiPolygon origin = (MultiPolygon) zones.get(ars).getDefaultGeometry();

			// Default is commute within same zone
			person.getAttributes().putAttribute(Attributes.COMMUTE, ars);

			double extent = origin.getEnvelopeInternal().maxExtent();
			double dist = distribution.sample();

			// Create commute distance within the region
			for (int i = 0; i < 10 && dist > extent; i++) {
				dist = distribution.sample();
			}

			// Default commute within zone and random diameter
			person.getAttributes().putAttribute(Attributes.COMMUTE_KM, dist);

			Long2DoubleMap comms = commuter.get(ars);

			if (comms == null) {
				log.warn("No commute data for {}", ars);
				return;
			}

			long to = selectTarget(ars, comms);

			// Same zone commuters are the default
			if (ars == to)
				return;

			if (!zones.containsKey(to)) {

				// TODO: some commute zones changed slightly and need to be remapped
				log.warn("No zone data for {}", to);
				return;
			}

			MultiPolygon destination = (MultiPolygon) zones.get(to).getDefaultGeometry();

			person.getAttributes().putAttribute(Attributes.COMMUTE, to);
			person.getAttributes().putAttribute(Attributes.COMMUTE_KM,
					origin.getCentroid().distance(destination.getCentroid()) / 1000);

		}
	}

	/**
	 * Select and return a commute target
	 *
	 * @param ars   origin
	 * @param comms map of destinations and number of commuters
	 */
	private long selectTarget(long ars, Long2DoubleMap comms) {

		if (comms.isEmpty())
			return ars;

		Optional<Long2DoubleMap.Entry> entry = comms.long2DoubleEntrySet().stream()
				.skip((int) (comms.size() * rnd.nextDouble()))
				.findFirst();

		// Should never happen
		if (entry.isEmpty())
			return ars;

		long to = entry.get().getLongKey();

		// subtract available commuters
		double newValue = entry.get().getDoubleValue() - (1 / sample);
		comms.put(to, newValue);

		if (newValue <= 0)
			comms.remove(to);

		return to;
	}

}
