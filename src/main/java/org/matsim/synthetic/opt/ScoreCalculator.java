package org.matsim.synthetic.opt;


import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;

public class ScoreCalculator implements IncrementalScoreCalculator<PlanAssignmentProblem, SimpleScore> {

	/**
	 * Total absolute difference.
	 */
	private int totalDiff = 0;

	/**
	 * Array of differences to the counts.
	 */
	private int[] diff;

	public int[] getDiff() {
		return diff;
	}

	@Override
	public void resetWorkingSolution(PlanAssignmentProblem problem) {

		diff = new int[problem.counts.length];
		System.arraycopy(problem.counts, 0, diff, 0, problem.counts.length);
		for (PlanPerson person : problem) {
			for (Int2IntMap.Entry e : person.selected().int2IntEntrySet()) {
				diff[e.getIntKey()] -= e.getIntValue();
			}
		}

		totalDiff = 0;
		for (int j = 0; j < problem.counts.length; j++) {
			totalDiff += Math.abs(diff[j]);
		}
	}

	@Override
	public void beforeEntityAdded(Object entity) {
	}

	@Override
	public void afterEntityAdded(Object entity) {
	}

	@Override
	public void beforeVariableChanged(Object entity, String variableName) {

		assert variableName.equals("k");
		PlanPerson person = (PlanPerson) entity;

		// remove this persons plan from the calculation
		for (Int2IntMap.Entry e : person.selected().int2IntEntrySet()) {

			int old = diff[e.getIntKey()];
			int update = diff[e.getIntKey()] += e.getIntValue();

			totalDiff += diffChange(old, update);
		}
	}

	static int diffChange(int old, int update) {
		return Math.abs(update) - Math.abs(old);
	}

	@Override
	public void afterVariableChanged(Object entity, String variableName) {

		assert variableName.equals("k");
		PlanPerson person = (PlanPerson) entity;

		// add this persons contribution to the score
		for (Int2IntMap.Entry e : person.selected().int2IntEntrySet()) {

			int old = diff[e.getIntKey()];
			int update = diff[e.getIntKey()] -= e.getIntValue();

			totalDiff += diffChange(old, update);
		}
	}

	@Override
	public void beforeEntityRemoved(Object entity) {
	}

	@Override
	public void afterEntityRemoved(Object entity) {

	}

	@Override
	public SimpleScore calculateScore() {
		return SimpleScore.of(-totalDiff);
	}
}
