package org.matsim.run.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.TypicalDurationCalculator;

/**
 * Picks the typical duration calculator for a person, so the scenario and the calibration agree on how activities
 * are scored. See {@link org.matsim.prepare.EncodeTypicalDuration} for where the attribute comes from.
 */
public final class TypicalDurations {

	private TypicalDurations() {
	}

	/**
	 * Person-subpopulation activities must carry a survey-derived typical duration; everything else
	 * (freight, commercial) falls back to the config typical durations by design.
	 */
	public static TypicalDurationCalculator forPerson(Config config, Person person) {
		BerlinScoringConfigGroup berlinScoring = ConfigUtils.addOrGetModule(config, BerlinScoringConfigGroup.class);
		return "person".equals(PopulationUtils.getSubpopulation(person)) && !berlinScoring.isAllowConfigTypicalDurations()
			? new RequiredTypicalDurationCalculator(person)
			: new ActivityAttributeTypicalDurationCalculator();
	}
}
