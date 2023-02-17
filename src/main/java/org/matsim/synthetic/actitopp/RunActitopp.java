package org.matsim.synthetic.actitopp;

import edu.kit.ifv.mobitopp.actitopp.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.synthetic.Attributes;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.SplittableRandom;

@CommandLine.Command(
		name = "actitopp",
		description = "Run actiTopp activity generation"
)
public class RunActitopp implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(RunActitopp.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp;

	private PopulationFactory factory;
	private int index;

	private ThreadLocal<Context> tl;

	public static void main(String[] args) throws InvalidPatternException {
		new RunActitopp().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Activity shape file is required");
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());
		factory = population.getFactory();

		log.info("Generating activity chains...");

		tl = ThreadLocal.withInitial(Context::new);

		ParallelPersonAlgorithmUtils.run(population, 8, this);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {

		// actitopp can only create correct activity schedules for persons aged 10 years and older!
		if (PersonUtils.getAge(person) < 10)
			return;

		Context ctx = tl.get();

		ActitoppPerson ap = convertPerson(person, ctx.rnd);

		if (PersonUtils.isEmployed(person)) {
			ap.setCommutingdistance_work((double) person.getAttributes().getAttribute(Attributes.COMMUTE_KM));
		}

		boolean scheduleOK = false;
		int tries = 0;
		while (!scheduleOK) {
			try {

				Coord homeCoord = new Coord((Double) person.getAttributes().getAttribute(Attributes.HOME_X),
						(Double) person.getAttributes().getAttribute(Attributes.HOME_Y));

				ap.generateSchedule(ctx.fileBase, ctx.rng);

				// choose random tuesday, wednesday, thursday
				HWeekPattern week = ap.getWeekPattern();
				HDay day = week.getDay(ctx.rnd.nextInt(2, 5));

				if (!day.isHomeDay()) {

					Plan plan = factory.createPlan();

					convertDay(day.getWeekday(), week.getAllActivities(), plan, homeCoord);

					person.removePlan(person.getSelectedPlan());
					person.addPlan(plan);
					person.setSelectedPlan(plan);
				}

				scheduleOK = true;
			} catch (InvalidPatternException e) {
				// Re-try
				tries++;
				if (tries > 100) {
					log.warn("No chain generated for person {} after 100 attempts", person.getId());
					break;
				}
			}
		}
	}

	/**
	 * Convert actitopp day into matsim schedule.
	 */
	private void convertDay(int weekDay, List<HActivity> activities, Plan plan, Coord homeCoord) {

		Activity a = null;

		for (HActivity act : activities) {

			if (act.getDay().getWeekday() != weekDay)
				continue;

			if (plan.getPlanElements().isEmpty() && !act.isHomeActivity()) {
				Activity home = factory.createActivityFromCoord("home", homeCoord);
				plan.addActivity(home);
			}

			if (act.isHomeActivity()) {
				a = factory.createActivityFromCoord("home", homeCoord);
			} else
				a = factory.createActivityFromLinkId(getActivityType(act.getActivityType()), Id.createLinkId("unassigned"));

			a.setStartTime(act.getStartTime() * 60);
			a.setMaximumDuration(act.getDuration() * 60);

			// No leg needed for first activity
			if (!plan.getPlanElements().isEmpty())
				plan.addLeg(factory.createLeg("walk"));

			plan.addActivity(a);
		}

		// Last home activity has no end time and duration
		if (a != null && a.getType().equals("home")) {
			a.setEndTimeUndefined();
			a.setMaximumDurationUndefined();
		}
	}

	private String getActivityType(ActivityType activityType) {
		if (activityType == ActivityType.TRANSPORT || activityType == ActivityType.UNKNOWN) {
			return "other";
		}
		return activityType.name().toLowerCase();
	}

	/**
	 * Convert actitopp person into matsim.
	 */
	private ActitoppPerson convertPerson(Person p, SplittableRandom rnd) {
		// TODO: from data
		int children0_10 = rnd.nextInt(0, 2);
		int children_u18 = rnd.nextInt(0, 2);

		int age = PersonUtils.getAge(p);
		// gender type Coding: 1 - male 2 - female
		int gender = PersonUtils.getSex(p).equals("m") ? 1 : 2;

		// main occupation status of the person Coding:
		// 1 - full-time occupied 2 - half-time occupied 3 - not occupied
		// 4 - student (school or university) 5 - worker in vocational program 6 -housewife, househusband
		// 7 - retired person / pensioner
		int employment = 3;
		if (PersonUtils.isEmployed(p)) {
			employment = 1;
			// random half time: TODO: from data
			if (rnd.nextDouble() < 0.2)
				employment = 2;

		} else {
			if (age < 18)
				employment = 4;

			// random students TODO: from data
			if (age < 28 && rnd.nextDouble() < 0.1) {
				employment = 4;
			}

			if (age > 65)
				employment = 7;
		}

		// https://bmdv.bund.de/SharedDocs/DE/Artikel/G/regionalstatistische-raumtypologie.html
		// Raumtyp Coding: 1 - rural 2 - provincial 3 - cityoutskirt 4 - metropolitan 5 - conurbation
		int regioStar = (int) p.getAttributes().getAttribute(Attributes.RegioStaR7);
		int areaType = switch (regioStar) {
			case 71 -> 4;
			case 72, 75 -> 5;
			case 73 -> 3;
			case 74, 76 -> 2;
			case 77 -> 1;
			default -> throw new IllegalStateException("Unknown regioStar type: " + regioStar);
		};

		// TODO from data
		int numberofcarsinhousehold = rnd.nextInt(0, 3);

		// TODO: edu commuting distances

		return new ActitoppPerson(index++, children0_10, children_u18, age, employment,
				gender, areaType, numberofcarsinhousehold);
	}

	/**
	 * Context for one thread.
	 */
	private static final class Context {

		private final ModelFileBase fileBase = new ModelFileBase();
		;
		private final RNGHelper rng = new RNGHelper(1);
		private final SplittableRandom rnd = new SplittableRandom(1);

	}

}
