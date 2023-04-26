package org.matsim.synthetic.opt;


import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.apache.commons.math3.util.FastMath;
import org.optaplanner.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import org.optaplanner.core.api.score.calculator.IncrementalScoreCalculator;

import java.math.BigDecimal;

public class ScoreCalculator implements IncrementalScoreCalculator<PlanAssignmentProblem, SimpleBigDecimalScore> {

	private static final double C = 15.0;
	/**
	 * Error metric.
	 */
	private BigDecimal error = BigDecimal.ZERO;
	/**
	 * Real counts
	 */
	private int[] counts;
	/**
	 * Observed counts from sim.
	 */
	private int[] observed;

	private ErrorMetric metric;

	static BigDecimal diffChange(ErrorMetric err, int count, int old, int update) {

		// Floating point arithmetic still leads to score corruption in full assert mode
		// logarithm can not even be efficiently calculated using big decimal, the corruption needs to be accepted as this point

		return switch (err) {
			case abs_error -> BigDecimal.valueOf(Math.abs(count - update) - Math.abs(count - old));
			case log_error -> BigDecimal.valueOf(FastMath.abs(FastMath.log((update + C) / (count + C))) - FastMath.abs(FastMath.log((old + C) / (count + C))));
			case symmetric_percentage_error -> BigDecimal.valueOf(FastMath.abs((double) (update - count) / (update + count + 2 * C) / 2.) -
					FastMath.abs((double) (old - count) / (old + count + 2 * C) / 2.));
		};
	}


	@Override
	public void resetWorkingSolution(PlanAssignmentProblem problem) {

		observed = new int[problem.counts.length];
		counts = problem.counts;
		metric = problem.metric;

		for (PlanPerson person : problem) {
			for (Int2IntMap.Entry e : person.selected().int2IntEntrySet()) {
				observed[e.getIntKey()] += e.getIntValue();
			}
		}

		calcScoreInternal();
	}

	private void calcScoreInternal() {
		double error = 0;

		// Log score needs to shift counts by 1.0 to avoid log 0

		if (metric == ErrorMetric.abs_error)
			for (int j = 0; j < counts.length; j++)
				error += Math.abs(counts[j] - observed[j]);
		else if (metric == ErrorMetric.log_error)
			for (int j = 0; j < counts.length; j++)
				error += FastMath.abs(Math.log((observed[j] + C) / (counts[j] + C)));
		else if (metric == ErrorMetric.symmetric_percentage_error) {
			for (int j = 0; j < counts.length; j++)
				error += FastMath.abs((double) (observed[j] - counts[j]) / (observed[j] + counts[j] + 2 * C) / 2);
		}

		this.error = BigDecimal.valueOf(error);
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

			int old = observed[e.getIntKey()];
			int update = observed[e.getIntKey()] -= e.getIntValue();

			error = error.add(diffChange(metric, counts[e.getIntKey()], old, update));
		}

	}

	@Override
	public void afterVariableChanged(Object entity, String variableName) {

		assert variableName.equals("k");
		PlanPerson person = (PlanPerson) entity;

		// add this persons contribution to the score
		for (Int2IntMap.Entry e : person.selected().int2IntEntrySet()) {

			int old = observed[e.getIntKey()];
			int update = observed[e.getIntKey()] += e.getIntValue();

			error = error.add(diffChange(metric, counts[e.getIntKey()], old, update));
		}
	}

	@Override
	public void beforeEntityRemoved(Object entity) {
	}

	@Override
	public void afterEntityRemoved(Object entity) {

	}

	@Override
	public SimpleBigDecimalScore calculateScore() {
		return SimpleBigDecimalScore.of(error.negate());
	}

	double scoreEntry(Int2IntMap.Entry e) {

		int idx = e.getIntKey();

		// Calculate impact compared to a plan without the observations of this plan
		// old can not get negative

		BigDecimal score = diffChange(metric, counts[idx], Math.max(0, observed[idx] - e.getIntValue()), observed[idx]);

		return -score.doubleValue();
	}
}
