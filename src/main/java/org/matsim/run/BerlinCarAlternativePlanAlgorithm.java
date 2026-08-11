package org.matsim.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;

/**
 * Algorithm for differentiation between agents which may use carExpensive only vs agents which may choose between carExpensive and "normal" car.
 */
public final class BerlinCarAlternativePlanAlgorithm implements PlanAlgorithm {

	private final SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;

	@Inject
	BerlinCarAlternativePlanAlgorithm(SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup) {
		this.subtourModeChoiceConfigGroup = subtourModeChoiceConfigGroup;
	}

	@Override
	public void run(Plan plan) {
//		TODO: if attr richness = rich, do not allow to switch to normal car in smc. so: if car trip, manually match it to carExpensive in this method
//		for all other agents normal mode choice set

	}
}
