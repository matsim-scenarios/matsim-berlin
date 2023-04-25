package org.matsim.synthetic.opt;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

@PlanningEntity(difficultyComparatorClass = PlanPerson.DifficultyComparator.class)
public class PlanPerson {

	/**
	 * A plan that does not affect any count stations.
	 */
	public static final Int2IntMap NOOP_PLAN = Int2IntMaps.EMPTY_MAP;

	@PlanningId
	private final Id<Person> id;

	/**
	 * If 1, an empty plan was inserted at the front.
	 */
	private final int offset;

	/**
	 * Index of the selected plans.
	 */
	@PlanningVariable(valueRangeProviderRefs = "numPlans")
	private Integer k;

	/**
	 * Plans and their count increment.
	 */
	private final Int2IntMap[] plans;

	/**
	 * Scores of each plan.
	 */
	private final double[] scores;

	/**
	 * Maximum number of affected counts.
	 */
	final int maxImpact;

	public PlanPerson(Id<Person> id, int offset, Int2IntMap[] plans) {
		this.id = id;
		this.offset = offset;
		this.plans = plans;
		this.k = 0;

		int max = 0;
		for (Int2IntMap plan : plans) {
			max = Math.max(max, plan.values().intStream().sum());
		}
		this.scores = new double[plans.length];
		Arrays.fill(scores, Float.NaN);

		this.maxImpact = max;
	}

	/**
	 * Constructor for cloning.
	 */
	private PlanPerson(Integer k, Id<Person> id, int offset, Int2IntMap[] plans, double[] scores, int maxImpact) {
		this.k = k;
		this.id = id;
		this.offset = offset;
		this.plans = plans;
		this.scores = scores;
		this.maxImpact = maxImpact;
	}

	public Id<Person> getId() {
		return id;
	}

	public int getOffset() {
		return offset;
	}
	public Int2IntMap selected() {
		return plans[k];
	}

	public Int2IntMap get(int idx) {
		return plans[idx];
	}

	public void setK(int k) {
		this.k = k;
	}

	public int getK() {
		return k;
	}

	public void setScore(ScoreCalculator calc) {


		for (int i = 0; i < plans.length; i++) {

			double score = 0;

			Int2IntMap p = plans[i];

			for (Int2IntMap.Entry e : p.int2IntEntrySet()) {

				score += calc.scoreEntry(e);
			}

			scores[i] = score;
		}
	}

	/**
	 * Change plan with exp beta probability.
	 *
	 * @see org.matsim.core.replanning.strategies.ChangeExpBeta
	 */
	public int changePlanExpBeta(double beta, Random rnd) {

		int other = rnd.nextInt(scores.length);

		double currentPlan = scores[k];
		double otherPlan = scores[other];

		if (Double.isNaN(otherPlan))
			return other;

		if (Double.isNaN(currentPlan))
			return k;

		double weight = Math.exp(0.5 * beta * (otherPlan - currentPlan));

		// switch plan
		if (rnd.nextDouble() < 0.01 * weight) {
			return other;
		}

		return k;
	}

	PlanPerson copy() {
		return new PlanPerson(k, id, offset, plans, scores, maxImpact);
	}

	/**
	 * Compares plans by difficulty.
	 */
	public static final class DifficultyComparator implements Comparator<PlanPerson> {

		@Override
		public int compare(PlanPerson o1, PlanPerson o2) {
			return Float.compare(o1.maxImpact, o2.maxImpact);
		}
	}

}
