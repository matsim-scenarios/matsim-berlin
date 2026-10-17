package org.matsim.run.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonTypicalDurationCalculatorTest {

	private static final String CAR_INTERACTION = "car interaction";

	/** Main activities of the day and their typical durations; "work" twice, with different typical durations. */
	private static final List<TypicalDurations.Entry> DAY = List.of(
		new TypicalDurations.Entry("home_morning", 25200),
		new TypicalDurations.Entry("work", 15000),
		new TypicalDurations.Entry("shop_daily", 1800),
		new TypicalDurations.Entry("work", 14000),
		new TypicalDurations.Entry("home_evening", 37000));

	private final ScoringParameters params = params();

	/**
	 * The person attribute gives the same score as the per-activity attribute it replaces, with the same pairing of
	 * executed and planned activities.
	 */
	@Test
	void scoresLikeThePerActivityAttribute() {
		Person old = person(plan(true));
		double before = score(new CharyparNagelActivityScoring(params, new ActivityAttributeTypicalDurationCalculator(), old), executedDay());

		Person person = person(plan(false));
		TypicalDurations.set(person, DAY);
		double after = score(new CharyparNagelActivityScoring(params, new PersonTypicalDurationCalculator(person, true), person), executedDay());

		assertThat(after).isEqualTo(before);
		// and the per-activity values make a difference
		assertThat(after).isNotEqualTo(score(new CharyparNagelActivityScoring(params), executedDay()));
	}

	@Test
	void requiredWithoutAttributeAborts() {
		Person person = person(plan(false));
		assertThatThrownBy(() -> score(new CharyparNagelActivityScoring(params, new PersonTypicalDurationCalculator(person, true), person), executedDay()))
			.hasMessageContaining("no typicalDurations attribute");
	}

	@Test
	void requiredWithWrongTypeAborts() {
		Person person = person(plan(false));
		TypicalDurations.set(person, List.of(DAY.get(0), DAY.get(1), new TypicalDurations.Entry("leisure", 1800), DAY.get(3), DAY.get(4)));
		assertThatThrownBy(() -> score(new CharyparNagelActivityScoring(params, new PersonTypicalDurationCalculator(person, true), person), executedDay()))
			.hasMessageContaining("is for type leisure");
	}

	/**
	 * When the executed activities cannot be paired with the plan, the core hands the calculator the executed
	 * activity, which is not in the plan.
	 */
	@Test
	void requiredWithBrokenPairingAborts() {
		Person person = person(plan(false));
		TypicalDurations.set(person, DAY);
		List<Activity> executed = executedDay();
		// the plan's second work activity executed as a shop activity
		executed.set(5, executed("shop_daily", 12 * 3600 + 3000, 17 * 3600));
		assertThatThrownBy(() -> score(new CharyparNagelActivityScoring(params, new PersonTypicalDurationCalculator(person, true), person), executed))
			.hasMessageContaining("could not be paired");
	}

	/**
	 * Without the requirement (other subpopulations, or the config switch), activities without a typical duration are
	 * scored against the config values, as without the calculator.
	 */
	@Test
	void notRequiredFallsBackToConfig() {
		Person person = person(plan(false));
		double stock = score(new CharyparNagelActivityScoring(params), executedDay());
		assertThat(score(new CharyparNagelActivityScoring(params, new PersonTypicalDurationCalculator(person, false), person), executedDay()))
			.isEqualTo(stock);
	}

	private static ScoringParameters params() {
		Config config = ConfigUtils.createConfig();
		config.scenario().setSimulationPeriodInDays(1.125);
		ScoringConfigGroup scoring = config.scoring();
		scoring.setPerforming_utils_hr(6.0);
		for (String type : List.of("home_morning", "work", "shop_daily", "home_evening")) {
			scoring.addActivityParams(new ScoringConfigGroup.ActivityParams(type).setTypicalDuration(8 * 3600));
		}
		scoring.addActivityParams(new ScoringConfigGroup.ActivityParams(CAR_INTERACTION).setScoringThisActivityAtAll(false));
		return new ScoringParameters.Builder(scoring, scoring.getScoringParameters(null), config.scenario()).build();
	}

	private static Person person(Plan plan) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		PopulationUtils.putSubpopulation(person, "person");
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		return person;
	}

	/** The planned day; with the old per-activity attributes or without. */
	private static Plan plan(boolean activityAttributes) {
		Plan plan = PopulationUtils.createPlan();
		int main = 0;
		for (String type : List.of("home_morning", CAR_INTERACTION, CAR_INTERACTION, "work", "shop_daily", "work", "home_evening")) {
			if (!plan.getPlanElements().isEmpty()) {
				plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
			}
			Activity act = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId("l"));
			if (!type.equals(CAR_INTERACTION)) {
				if (activityAttributes) {
					act.getAttributes().putAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE, DAY.get(main).seconds());
				}
				main++;
			}
			plan.addActivity(act);
		}
		return plan;
	}

	/** The day as the events hand it to the scoring: times, no attributes, stage activities included. */
	private static List<Activity> executedDay() {
		return new java.util.ArrayList<>(List.of(
			executed("home_morning", Double.NaN, 7 * 3600),
			executed(CAR_INTERACTION, 7 * 3600 + 300, 7 * 3600 + 300),
			executed(CAR_INTERACTION, 7 * 3600 + 2400, 7 * 3600 + 2400),
			executed("work", 7 * 3600 + 2700, 12 * 3600),
			executed("shop_daily", 12 * 3600 + 600, 12 * 3600 + 2400),
			executed("work", 12 * 3600 + 3000, 17 * 3600),
			executed("home_evening", 17 * 3600 + 1800, Double.NaN)));
	}

	private static Activity executed(String type, double start, double end) {
		Activity act = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId("l"));
		if (!Double.isNaN(start)) {
			act.setStartTime(start);
		}
		if (!Double.isNaN(end)) {
			act.setEndTime(end);
		}
		return act;
	}

	private static double score(CharyparNagelActivityScoring scoring, List<Activity> executed) {
		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(scoring);
		executed.forEach(sf::handleActivity);
		sf.finish();
		return sf.getScore();
	}
}
