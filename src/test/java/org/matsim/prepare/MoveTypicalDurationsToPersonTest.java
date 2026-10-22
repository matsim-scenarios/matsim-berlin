package org.matsim.prepare;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveTypicalDurationsToPersonTest {

	private static final String ACTIVITY_ATTRIBUTE = "typicalDuration";

	@Test
	void movesTheValuesOntoThePersonAndClearsTheActivities() {
		Person person = person(plan(25200, 30600, 41400), plan(25200, 30600, 41400));

		assertThat(MoveTypicalDurationsToPerson.move(person)).isTrue();

		assertThat(PersonUtils.getTypicalDurations(person)).containsExactly(
			new PersonUtils.TypicalDuration("home_morning", 25200),
			new PersonUtils.TypicalDuration("work", 30600),
			new PersonUtils.TypicalDuration("home_evening", 41400));
		for (Plan plan : person.getPlans()) {
			for (Activity act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
				assertThat(act.getAttributes().getAttribute(ACTIVITY_ATTRIBUTE)).isNull();
			}
		}
	}

	@Test
	void personsWithoutActivityAttributesAreLeftAlone() {
		Person person = person(plan());

		assertThat(MoveTypicalDurationsToPerson.move(person)).isFalse();
		assertThat(PersonUtils.getTypicalDurations(person)).isNull();
	}

	@Test
	void plansMustAgree() {
		Person person = person(plan(25200, 30600, 41400), plan(25200, 29000, 41400));

		assertThatThrownBy(() -> MoveTypicalDurationsToPerson.move(person)).hasMessageContaining("different typical durations");
	}

	private static Person person(Plan selected, Plan... others) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		person.addPlan(selected);
		for (Plan other : others) {
			person.addPlan(other);
		}
		person.setSelectedPlan(selected);
		return person;
	}

	/** home_morning, work, home_evening, with the given typical durations as activity attributes (none if empty). */
	private static Plan plan(double... typicalDurations) {
		Plan plan = PopulationUtils.createPlan();
		String[] types = {"home_morning", "work", "home_evening"};
		for (int i = 0; i < types.length; i++) {
			if (i > 0) {
				plan.addLeg(PopulationUtils.createLeg(TransportMode.walk));
				plan.addActivity(PopulationUtils.createActivityFromLinkId("walk interaction", Id.createLinkId("l")));
				plan.addLeg(PopulationUtils.createLeg(TransportMode.walk));
			}
			Activity act = PopulationUtils.createActivityFromLinkId(types[i], Id.createLinkId("l"));
			if (typicalDurations.length > 0) {
				act.getAttributes().putAttribute(ACTIVITY_ATTRIBUTE, typicalDurations[i]);
			}
			plan.addActivity(act);
		}
		return plan;
	}
}
