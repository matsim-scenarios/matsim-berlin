package org.matsim.synthetic.opt;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeFactory;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;

import java.util.*;

@PlanningSolution(solutionCloner = PlanAssignmentProblem.Cloner.class)
public class PlanAssignmentProblem implements Iterable<PlanPerson> {

	private final int maxK;

	@PlanningEntityCollectionProperty
	private final List<PlanPerson> persons;

	final int[] counts;

	@PlanningScore
	private SimpleScore score;

	public PlanAssignmentProblem(int maxK, List<PlanPerson> persons, int[] counts) {
		this.maxK = maxK;
		this.persons = persons;
		this.counts = counts;
		this.score = SimpleScore.ofUninitialized(-1, -1);

		persons.sort(new PlanPerson.DifficultyComparator());
		Collections.reverse(persons);
	}


	private PlanAssignmentProblem(int maxK, List<PlanPerson> persons, int[] counts, SimpleScore score) {
		this.maxK = maxK;
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

	public SimpleScore getScore() {
		return score;
	}

	public void setScore(SimpleScore score) {
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

	public void iterate(int n) {

		ScoreCalculator calc = new ScoreCalculator();
		calc.resetWorkingSolution(this);
		score = calc.calculateScore();

		Random rnd = new Random(0);

		for (int i = 0; i < n; i++) {

			calc.resetWorkingSolution(this);
			score = calc.calculateScore();

			for (PlanPerson person : persons) {

				if (rnd.nextDouble() < 0.2) {
					person.setScore(calc.getDiff());
					person.setK(person.changePlanExpBeta(rnd));
				}
			}
		}
	}

	public static final class Cloner implements SolutionCloner<PlanAssignmentProblem> {
		@Override
		public PlanAssignmentProblem cloneSolution(PlanAssignmentProblem original) {
			List<PlanPerson> personsCopy = new ArrayList<>();
			for (PlanPerson person : original.persons) {
				personsCopy.add(person.copy());
			}
			return new PlanAssignmentProblem(original.maxK, personsCopy, original.counts, original.score);
		}
	}
}
