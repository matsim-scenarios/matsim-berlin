package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts a population from the earlier encoding of the typical durations, a {@code typicalDuration} attribute on
 * every main activity, to the {@value PersonUtils#TYPICAL_DURATIONS} person attribute,
 * and removes the activity attributes. The values are taken over unchanged, from the selected plan; the other plans
 * must carry the same sequence. Persons without the activity attributes are left as they are.
 */
@CommandLine.Command(
	name = "typical-durations-to-person",
	description = "Move the typical durations from the activities' typicalDuration attributes to the person's typicalDurations attribute."
)
public class MoveTypicalDurationsToPerson implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(MoveTypicalDurationsToPerson.class);

	private static final String ACTIVITY_ATTRIBUTE = ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE;

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	public static void main(String[] args) {
		new MoveTypicalDurationsToPerson().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		int moved = 0;
		for (Person person : population.getPersons().values()) {
			if (move(person)) {
				moved++;
			}
		}
		log.info("Moved the typical durations of {} of {} persons onto the person.", moved, population.getPersons().size());

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * @return whether the person had typical durations on their activities
	 */
	static boolean move(Person person) {
		if (person.getSelectedPlan() == null) {
			return false;
		}
		List<PersonUtils.TypicalDuration> selected = entries(person, person.getSelectedPlan());
		for (Plan plan : person.getPlans()) {
			List<PersonUtils.TypicalDuration> entries = entries(person, plan);
			if (!Objects.equals(entries, selected)) {
				throw new IllegalStateException("The plans of person " + person.getId() + " carry different typical durations: " + entries + " and " + selected);
			}
		}

		for (Plan plan : person.getPlans()) {
			for (Activity act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
				act.getAttributes().removeAttribute(ACTIVITY_ATTRIBUTE);
			}
		}

		if (selected == null) {
			return false;
		}
		PersonUtils.setTypicalDurations(person, selected);
		return true;
	}

	/**
	 * The typical durations on the plan's main activities, or null if none of them has one.
	 */
	private static List<PersonUtils.TypicalDuration> entries(Person person, Plan plan) {
		List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		List<PersonUtils.TypicalDuration> entries = new ArrayList<>(activities.size());
		int missing = 0;
		for (Activity act : activities) {
			Object value = act.getAttributes().getAttribute(ACTIVITY_ATTRIBUTE);
			if (value instanceof Number n) {
				entries.add(new PersonUtils.TypicalDuration(act.getType(), n.doubleValue()));
			} else {
				missing++;
			}
		}
		if (missing == activities.size()) {
			return null;
		}
		if (missing > 0) {
			throw new IllegalStateException("Person " + person.getId() + " has a typical duration on some main activities of a plan but not on all: " + activities);
		}
		return entries;
	}
}
