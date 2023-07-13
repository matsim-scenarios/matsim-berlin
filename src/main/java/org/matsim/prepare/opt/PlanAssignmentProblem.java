package org.matsim.prepare.opt;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeFactory;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;

import java.math.BigDecimal;
import java.util.*;

/**
 * Planning problem containing all entities and information.
 */
@PlanningSolution(solutionCloner = PlanAssignmentProblem.Cloner.class)
public final class PlanAssignmentProblem implements Iterable<PlanPerson> {

	final int[] counts;
	final ErrorMetric metric;
	private final int maxK;
	@PlanningEntityCollectionProperty
	private final List<PlanPerson> persons;
	@PlanningScore
	private SimpleBigDecimalScore score;

	public PlanAssignmentProblem(int maxK, ErrorMetric metric, List<PlanPerson> persons, int[] counts) {
		this.maxK = maxK;
		this.metric = metric;
		this.persons = persons;
		this.counts = counts;
		this.score = SimpleBigDecimalScore.ofUninitialized(-1, BigDecimal.ZERO);

		persons.sort(new PlanPerson.DifficultyComparator());
		Collections.reverse(persons);
	}


	private PlanAssignmentProblem(int maxK, ErrorMetric metric, List<PlanPerson> persons, int[] counts, SimpleBigDecimalScore score) {
		this.maxK = maxK;
		this.metric = metric;
		this.persons = persons;
		this.counts = counts;
		this.score = score;
	}

	public int getMaxK() {
		return maxK;
	}

	public List<PlanPerson> getPersons() {
		return persons;
	}

	public int getSize() {
		return persons.size();
	}

	public SimpleBigDecimalScore getScore() {
		return score;
	}

	public void setScore(SimpleBigDecimalScore score) {
		this.score = score;
	}

	@ValueRangeProvider(id = "numPlans")
	public CountableValueRange<Integer> getPlanRange() {
		return ValueRangeFactory.createIntValueRange(0, maxK);
	}

	@Override
	public Iterator<PlanPerson> iterator() {
		return persons.iterator();
	}

	/**
	 * Iterative pre optimization using change plan exp beta logic.
	 */
	public void iterate(int n, double prob, double beta, double w) {

		ScoreCalculator calc = new ScoreCalculator();
		calc.resetWorkingSolution(this);
		score = calc.calculateScore();

		RunCountOptimization.log.info("Iterating {} iters with prob {} and beta {}", n, prob, beta);

		SplittableRandom rnd = new SplittableRandom(0);

		double step = prob / n;

		for (int i = 0; i < n; i++) {

			calc.resetWorkingSolution(this);
			score = calc.calculateScore();

			if (n % 500 == 0)
				RunCountOptimization.log.info("Iteration {} score: {}", n, score);

			// Best p and beta are not known, so it will be annealed
			double p = prob - step * i;
			double b = beta - (beta / n) * i;

			for (PlanPerson person : persons) {

				if (rnd.nextDouble() < p) {
					person.setScore(calc);
					person.setK(person.changePlanExpBeta(b, w, rnd));
				}
			}
		}
	}

	/**
	 * Create a clone of a solution.
	 */
	public static final class Cloner implements SolutionCloner<PlanAssignmentProblem> {
		@Override
		public PlanAssignmentProblem cloneSolution(PlanAssignmentProblem original) {
			List<PlanPerson> personsCopy = new ArrayList<>();
			for (PlanPerson person : original.persons) {
				personsCopy.add(person.copy());
			}
			return new PlanAssignmentProblem(original.maxK, original.metric, personsCopy, original.counts, original.score);
		}
	}

}
