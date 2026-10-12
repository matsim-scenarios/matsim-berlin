package org.matsim.run.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.TypicalDurationCalculator;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * An {@link ActivityAttributeTypicalDurationCalculator} that fails hard when a scored activity carries no positive
 * typicalDuration attribute, instead of falling back to the activity type's config value. The experiment's premise is
 * that every (person-subpopulation) activity is scored against its survey-derived typical duration; a silent
 * config-typical fallback would corrupt exactly the quantity under study. Not used for subpopulations that score by
 * config typicals by design (freight/commercial) or for legacy type-encoded populations (see
 * {@link BerlinScoringConfigGroup#isAllowConfigTypicalDurations()}).
 */
public final class RequiredTypicalDurationCalculator implements TypicalDurationCalculator {

	private final ActivityAttributeTypicalDurationCalculator delegate = new ActivityAttributeTypicalDurationCalculator();
	/** For the error message only; may be null. */
	private final Person person;

	public RequiredTypicalDurationCalculator(Person person) {
		this.person = person;
	}

	@Override
	public OptionalTime getTypicalDuration(Activity act) {
		OptionalTime typicalDuration = delegate.getTypicalDuration(act);
		if (typicalDuration.isUndefined()) {
			throw new RuntimeException("Activity of type " + act.getType() + (person != null ? " of person " + person.getId() : "")
				+ " carries no positive typicalDuration attribute, but this run requires every scored activity to use its "
				+ "survey-derived typical duration. Either the population was not preprocessed (EncodeTypicalDuration), the "
				+ "attribute was lost, or the realized activities could not be aligned with the selected plan; for legacy "
				+ "type-encoded populations pass --allow-config-typical-durations.");
		}
		return typicalDuration;
	}

}
