package org.matsim.prepare.population;

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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.prepare.RunOpenBerlinCalibration;
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
	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	/**
	 * Maps person index to list of activities.
	 */
	private final Map<String, List<CSVRecord>> activities = new HashMap<>();

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
	private ThreadLocal<Context> ctxs;

	private PopulationFactory factory;
	private PersonMatcher matcher;

	public static void main(String[] args) {
		new RunActivitySampling().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		matcher = new PersonMatcher("idx", personsPath);

		try (CSVParser parser = csv.createParser(activityPath)) {
			readActivities(parser);
		}

		ctxs = ThreadLocal.withInitial(() -> new Context(new SplittableRandom(seed)));
		factory = population.getFactory();

		ParallelPersonAlgorithmUtils.run(population, 8, this);

		PopulationUtils.writePopulation(population, output.toString());

		double atHome = 0;
		for (Person person : population.getPersons().values()) {
			List<Leg> legs = TripStructureUtils.getLegs(person.getSelectedPlan());
			if (legs.isEmpty())
				atHome++;
		}

		int size = population.getPersons().size();
		double mobile = (size - atHome) / size;

		log.info("Processed {} persons, mobile persons: {}%", size, 100 * mobile);

		return 0;
	}

	private void readActivities(CSVParser csv) {

		String currentId = null;
		List<CSVRecord> current = null;

		int i = 0;
		for (CSVRecord r : csv) {

			String pId = r.get("p_id");

			if (!Objects.equals(pId, currentId)) {
				if (current != null)
					activities.put(currentId, current);

				currentId = pId;
				current = new ArrayList<>();
			}

			current.add(r);
			i++;
		}

		if (current != null && !current.isEmpty()) {
			activities.put(currentId, current);
		}

		log.info("Read {} activities for {} persons", i, activities.size());
	}



	@Override
	public void run(Person person) {

		SplittableRandom rnd = ctxs.get().rnd;

		String  idx = matcher.matchPerson(person, rnd);
		CSVRecord row = matcher.getPerson(idx);

		PersonUtils.setCarAvail(person, row.get("car_avail").equals("True") ? "always" : "never");
		PersonUtils.setLicence(person, row.get("driving_license").toLowerCase());
		PersonUtils.setIncome(person, Math.max(499, Double.parseDouble(row.get("income"))));

		person.getAttributes().putAttribute(Attributes.BIKE_AVAIL, row.get("bike_avail").equals("True") ? "always" : "never");
		person.getAttributes().putAttribute(Attributes.PT_ABO_AVAIL, row.get("pt_abo_avail").equals("True") ? "always" : "never");

		person.getAttributes().putAttribute(Attributes.EMPLOYMENT, row.get("employment"));
		person.getAttributes().putAttribute(Attributes.RESTRICTED_MOBILITY, row.get("restricted_mobility").equals("True"));
		person.getAttributes().putAttribute(Attributes.ECONOMIC_STATUS, row.get("economic_status"));
		person.getAttributes().putAttribute(Attributes.HOUSEHOLD_SIZE, Integer.parseInt(row.get("n_persons")));


		String mobile = row.get("mobile_on_day");

		// ensure mobile agents have a valid plan
		switch (mobile.toLowerCase()) {

			case "true" -> {
				List<CSVRecord> activities = this.activities.get(idx);

				if (activities == null)
					throw new AssertionError("No activities for mobile person " + idx);

				if (activities.size() == 0)
					throw new AssertionError("Activities for mobile agent can not be empty.");

				person.removePlan(person.getSelectedPlan());
				Plan plan = createPlan(Attributes.getHomeCoord(person), activities, rnd);

				person.addPlan(plan);
				person.setSelectedPlan(plan);
			}

			case "false" -> {
				// Keep the stay home plan
			}

			default -> throw new AssertionError("Invalid mobile_on_day attribute " + mobile);
		}
	}

	/**
	 * Randomize the duration slightly, depending on total duration.
	 */
	private int randomizeDuration(int minutes, SplittableRandom rnd) {
		if (minutes <= 10)
			return minutes * 60;

		if (minutes <= 60)
			return minutes * 60 + rnd.nextInt(300) - 150;

		if (minutes <= 240)
			return minutes * 60 + rnd.nextInt(600) - 300;

		return minutes * 60 + rnd.nextInt(1200) - 600;
	}

	private Plan createPlan(Coord homeCoord, List<CSVRecord> activities, SplittableRandom rnd) {
		Plan plan = factory.createPlan();

		Activity a = null;
		String lastMode = null;

		double startTime = 0;

		// Track the distance to the first home activity
		double homeDist = 0;
		boolean arrivedHome = false;

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
				// Add little
				int seconds = randomizeDuration(duration, rnd);

				a.setEndTime(seconds);
				startTime += seconds;

			} else if (duration < 1440) {

				startTime += legDuration * 60;

				// Flexible modes are represented with duration
				// otherwise start and end time
				int seconds = randomizeDuration(duration, rnd);

				if (RunOpenBerlinCalibration.FLEXIBLE_ACTS.contains(actType))
					a.setMaximumDuration(seconds);
				else {
					a.setStartTime(startTime);
					a.setEndTime(startTime + seconds);
				}

				startTime += seconds;
			}

			double legDist = Double.parseDouble(act.get("leg_dist"));

			if (i > 0) {
				a.getAttributes().putAttribute("orig_dist", legDist);
				a.getAttributes().putAttribute("orig_duration", legDuration);
			}

			if (!plan.getPlanElements().isEmpty()) {
				lastMode = act.get("leg_mode");

				// other mode is initialized as walk
				if (lastMode.equals("other"))
					lastMode = "walk";

				plan.addLeg(factory.createLeg(lastMode));
			}

			if (!arrivedHome) {
				homeDist += legDist;
			}

			if (a.getType().equals("home")) {
				arrivedHome = true;
			}

			plan.addActivity(a);
		}

		// First activity contains the home distance
		Activity act = (Activity) plan.getPlanElements().get(0);
		act.getAttributes().putAttribute("orig_dist", homeDist);

		// Last activity has no end time and duration
		if (a != null) {

			if (!RunOpenBerlinCalibration.FLEXIBLE_ACTS.contains(a.getType())) {
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

	private record Context(SplittableRandom rnd) {
	}

}
