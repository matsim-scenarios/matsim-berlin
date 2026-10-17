package org.matsim.run.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.TypicalDurationCalculator;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.List;

/**
 * Reads an activity's typical duration from the person's {@value TypicalDurations#ATTRIBUTE} attribute, by the
 * activity's position among the main activities of the plan.
 * <p>
 * To be used with {@code CharyparNagelActivityScoring(params, calculator, person)}: given the person, the core pairs
 * each executed activity with the main activity at the same position in the selected plan (checking the type) and
 * hands that plan activity to this calculator. The calculator finds its position in the selected plan and takes the
 * entry at that position. The plan only supplies the positions; the values come from the person.
 * <p>
 * In required mode, a scored activity without a typical duration aborts the run: the person has no attribute, the
 * entry's type differs, or the core could not pair the executed activity with the plan and handed the executed
 * activity itself. Otherwise those activities are scored against the config value of their type. One instance per
 * person and scoring function.
 */
public final class PersonTypicalDurationCalculator implements TypicalDurationCalculator {

	private final Person person;
	private final boolean required;

	/** Resolved at the first call, i.e. after replanning, like the core's pairing. */
	private List<Activity> plannedActivities;
	private List<TypicalDurations.Entry> entries;

	public PersonTypicalDurationCalculator(Person person, boolean required) {
		this.person = person;
		this.required = required;
	}

	@Override
	public OptionalTime getTypicalDuration(Activity act) {
		if (this.plannedActivities == null) {
			this.plannedActivities = TripStructureUtils.getActivities(this.person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			this.entries = TypicalDurations.get(this.person);
		}
		int index = indexOf(this.plannedActivities, act);
		if (index < 0) {
			return missing(act, "it is not an activity of the selected plan, i.e. the executed activities could not be paired with the plan");
		}
		return typicalDuration(index, act);
	}

	/**
	 * The typical duration of the main activity at the given position, checked against its type.
	 */
	OptionalTime typicalDuration(int index, Activity act) {
		if (this.entries == null) {
			return missing(act, "the person has no " + TypicalDurations.ATTRIBUTE + " attribute");
		}
		if (index >= this.entries.size()) {
			return missing(act, "the " + TypicalDurations.ATTRIBUTE + " attribute has only " + this.entries.size() + " entries, this is main activity " + index);
		}
		TypicalDurations.Entry entry = this.entries.get(index);
		if (!entry.type().equals(act.getType())) {
			return missing(act, "entry " + index + " of the " + TypicalDurations.ATTRIBUTE + " attribute is for type " + entry.type());
		}
		if (!(entry.seconds() > 0)) {
			return missing(act, "entry " + index + " of the " + TypicalDurations.ATTRIBUTE + " attribute is not positive");
		}
		return OptionalTime.defined(entry.seconds());
	}

	private static int indexOf(List<Activity> activities, Activity act) {
		for (int i = 0; i < activities.size(); i++) {
			if (activities.get(i) == act) {
				return i;
			}
		}
		return -1;
	}

	private OptionalTime missing(Activity act, String reason) {
		if (this.required) {
			throw new RuntimeException("Activity of type " + act.getType() + " of person " + this.person.getId() + " has no typical duration: "
				+ reason + ". This run requires every scored activity of a person to be scored against its own typical duration. "
				+ "Populations from before typical durations moved onto the person can be converted with "
				+ "'prepare typical-durations-to-person'; populations that encode them in the activity type need "
				+ "--allow-config-typical-durations.");
		}
		return OptionalTime.undefined();
	}
}
