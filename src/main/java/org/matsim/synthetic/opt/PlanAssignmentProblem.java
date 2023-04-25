package org.matsim.synthetic.opt;

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

@PlanningSolution(solutionCloner = PlanAssignmentProblem.Cloner.class)
public class PlanAssignmentProblem implements Iterable<PlanPerson> {

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

	public void iterate(int n, double prob) {

		ScoreCalculator calc = new ScoreCalculator();
		calc.resetWorkingSolution(this);
		score = calc.calculateScore();

		Random rnd = new Random(0);

		double step = prob / n;

		for (int i = 0; i < n; i++) {

			calc.resetWorkingSolution(this);
			score = calc.calculateScore();

			// Best p and beta are not known, so it will be annealed
			double p = prob - step * i;
			double b = 1 - i / (double) n;

			for (PlanPerson person : persons) {

				if (rnd.nextDouble() < p) {
					person.setScore(calc);
					person.setK(person.changePlanExpBeta(b, rnd));
				}
			}
		}
	}

	/**
	 * Error metric to calculate.
	 */
	enum ErrorMetric {
		abs_error,
		log_error,
		symetric_percentage_error
	}

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
