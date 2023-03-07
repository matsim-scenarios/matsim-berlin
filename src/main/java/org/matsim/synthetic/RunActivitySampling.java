package org.matsim.synthetic;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@CommandLine.Command(
		name = "activity-sampling",
		description = "Create activities by sampling from survey data"
)
public class RunActivitySampling implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(RunActivitySampling.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Output path for population", required = true)
	private Path output;

	@CommandLine.Option(names = "--persons", description = "Path to person table", required = true)
	private Path personsPath;

	@CommandLine.Option(names = "--activities", description = "Path to activity table", required = true)
	private Path activityPath;

	@CommandLine.Option(names = "--seed", description = "Seed used to sample plans", defaultValue = "1")
	private long seed;
	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	private final Map<Key, IntList> groups = new HashMap<>();

	private final Int2ObjectMap<CSVRecord> persons = new Int2ObjectOpenHashMap<>();
	/**
	 * Maps person index to list of activities.
	 */
	private final Int2ObjectMap<List<CSVRecord>> activities = new Int2ObjectOpenHashMap<>();

	private ThreadLocal<Context> ctxs;

	private PopulationFactory factory;

	public static void main(String[] args) {
		new RunActivitySampling().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		try (CSVParser parser = csv.createParser(personsPath)) {
			buildSubgroups(parser);
		}

		try (CSVParser parser = csv.createParser(activityPath)) {
			readActivities(parser);
		}

		ctxs = ThreadLocal.withInitial(() -> new Context(new SplittableRandom(seed)));
		factory = population.getFactory();

		ParallelPersonAlgorithmUtils.run(population, 8, this);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Create subpopulations for sampling.
	 */
	private void buildSubgroups(CSVParser csv) {

		int i = 0;

		for (CSVRecord record : csv) {

			int idx = Integer.parseInt(record.get("idx"));
			int regionType = Integer.parseInt(record.get("region_type"));
			String gender = record.get("gender");
			String employment = record.get("employment");
			int age = Integer.parseInt(record.get("age"));

			Stream<Key> keys = createKey(gender, age, regionType, employment);
			keys.forEach(key -> groups.computeIfAbsent(key, (k) -> new IntArrayList()).add(idx));
			persons.put(idx, record);
			i++;
		}

		log.info("Read {} persons from csv.", i);
	}

	private void readActivities(CSVParser csv) {

		int currentId = -1;
		List<CSVRecord> current = null;

		int i = 0;
		for (CSVRecord record : csv) {

			int pId = Integer.parseInt(record.get("p_id"));

			if (pId != currentId) {
				if (current != null)
					activities.put(currentId, current);

				currentId = pId;
				current = new ArrayList<>();
			}

			current.add(record);
			i++;
		}

		if (current != null && !current.isEmpty()) {
			activities.put(currentId, current);
		}

		log.info("Read {} activities for {} persons", i, activities.size());
	}

	private Stream<Key> createKey(String gender, int age, int regionType, String employment) {
		if (age < 6) {
			return IntStream.rangeClosed(0, 5).mapToObj(i -> new Key(null, i, regionType, null));
		}
		if (age <= 10) {
			return IntStream.rangeClosed(6, 10).mapToObj(i -> new Key(null, i, regionType, null));
		}
		if (age < 18) {
			return IntStream.rangeClosed(11, 18).mapToObj(i -> new Key(gender, i, regionType, null));
		}

		Boolean isEmployed = age > 65 ? null : !employment.equals("unemployed");
		int min = Math.max(18, age - 6);
		int max = Math.min(65, age + 6);

		// larger groups for older people
		if (age > 65) {
			min = Math.max(66, age - 10);
			max = Math.min(99, age + 10);
		}

		return IntStream.rangeClosed(min, max).mapToObj(i -> new Key(gender, i, regionType, isEmployed));
	}

	private Key createKey(Person person) {

		Integer age = PersonUtils.getAge(person);
		String gender = PersonUtils.getSex(person);
		if (age <= 10)
			gender = null;

		Boolean employed = PersonUtils.isEmployed(person);
		if (age < 18 || age > 65)
			employed = null;

		int regionType = (int) person.getAttributes().getAttribute(Attributes.RegioStaR7);

		// Region types have been reduced to 1 and 3
		if (regionType != 1)
			regionType = 3;

		return new Key(gender, age, regionType, employed);
	}

	@Override
	public void run(Person person) {

		SplittableRandom rnd = ctxs.get().rnd;

		Key key = createKey(person);

		IntList subgroup = groups.get(key);
		if (subgroup == null) {
			log.error("No subgroup found for key {}", key);
			throw new IllegalStateException("Invalid entry");
		}

		if (subgroup.size() < 30) {
			log.warn("Group {} has low sample size: {}", key, subgroup.size());
		}

		int idx = subgroup.getInt(rnd.nextInt(subgroup.size()));
		CSVRecord record = persons.get(idx);

		PersonUtils.setCarAvail(person, record.get("car_avail").equals("True") ? "always" : "never");
		PersonUtils.setLicence(person, record.get("driving_license").toLowerCase());

		person.getAttributes().putAttribute(Attributes.BIKE_AVAIL, record.get("bike_avail").equals("True") ? "always" : "never");
		person.getAttributes().putAttribute(Attributes.PT_ABO_AVAIL, record.get("pt_abo_avail").equals("True") ? "always" : "never");

		person.getAttributes().putAttribute(Attributes.EMPLOYMENT, record.get("employment"));
		person.getAttributes().putAttribute(Attributes.RESTRICTED_MOBILITY, record.get("restricted_mobility").equals("True"));
		person.getAttributes().putAttribute(Attributes.ECONOMIC_STATUS, record.get("economic_status"));
		person.getAttributes().putAttribute(Attributes.HOUSEHOLD_SIZE, Integer.parseInt(record.get("n_persons")));

		List<CSVRecord> activities = this.activities.get(idx);

		if (activities != null && !activities.isEmpty()) {
			person.removePlan(person.getSelectedPlan());

			Plan plan = createPlan(Attributes.getHomeCoord(person), activities);

			person.addPlan(plan);
			person.setSelectedPlan(plan);
		}
	}

	private Plan createPlan(Coord homeCoord, List<CSVRecord> activities) {
		Plan plan = factory.createPlan();

		Activity a = null;
		String lastMode = null;

		double startTime = 0;

		for (int i = 0; i < activities.size(); i++) {

			CSVRecord act = activities.get(i);

			String actType = act.get("type");

			// First and last activities that are other are changed to home
			if (actType.equals("other") && (i == 0 || i == activities.size() - 1))
				actType = "home";

			int duration = Integer.parseInt(act.get("duration"));

			if (actType.equals("home")) {
				a = factory.createActivityFromCoord("home", homeCoord);
			} else
				a = factory.createActivityFromLinkId(actType, Id.createLinkId("unassigned"));

			double legDuration = Double.parseDouble(act.get("leg_duration"));

			if (plan.getPlanElements().isEmpty()) {
				a.setEndTime(duration * 60);
				startTime += duration * 60;
			} else if (duration < 1440) {

				startTime += legDuration * 60;

				// Flexible modes are represented with duration
				// otherwise start and end time
				if (RunOpenBerlinCalibration.FLEXIBLE_MODES.contains(actType))
					a.setMaximumDuration(duration * 60);
				else {
					a.setStartTime(startTime);
					a.setEndTime(startTime + duration * 60);
				}

				startTime += duration * 60;

			}

			if (i > 0) {
				a.getAttributes().putAttribute("orig_dist", Double.parseDouble(act.get("leg_dist")));
				a.getAttributes().putAttribute("orig_duration", legDuration);
			}

			if (!plan.getPlanElements().isEmpty()) {
				lastMode = act.get("leg_mode");

				// other mode is initialized as walk
				if (lastMode.equals("other"))
					lastMode = "walk";

				plan.addLeg(factory.createLeg(lastMode));
			}

			plan.addActivity(a);
		}

		// Last activity has no end time and duration
		if (a != null) {

			if (!RunOpenBerlinCalibration.FLEXIBLE_MODES.contains(a.getType())) {
				a.setEndTimeUndefined();
				a.setMaximumDurationUndefined();
			} else {

				// End open activities
				a.setMaximumDuration(30 * 60);
				plan.addLeg(factory.createLeg(lastMode));
				plan.addActivity(factory.createActivityFromCoord("home", homeCoord));

				//log.warn("Last activity of type {}", a.getType());
			}
		}

		return plan;
	}

	/**
	 * Key used for sampling activities.
	 */
	private record Key(String gender, int age, int regionType, Boolean employed) {
	}

	private record Context(SplittableRandom rnd) {
	}

}
