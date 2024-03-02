package org.matsim.run.scoring;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

/**
 * Mode utility with separate marginalUtilityOfDistance_m per distance group.
 */
public class DistanceGroupModeUtilityParameters extends ModeUtilityParameters {

	private final DistanceGroup[] groups;

	/**
	 * Constructor which copies the base params from given modeParams.
	 */
	DistanceGroupModeUtilityParameters(ModeUtilityParameters modeParams, DeltaBuilder delta, DistanceGroup[] groups) {
		super(modeParams.marginalUtilityOfTraveling_s,
			modeParams.marginalUtilityOfDistance_m,
			modeParams.monetaryDistanceCostRate,
			modeParams.constant + delta.constant,
			modeParams.dailyMoneyConstant,
			modeParams.dailyUtilityConstant + delta.dailyUtilityConstant);

		this.groups = groups;
	}

	/**
	 * Calculate the utility for given distance.
	 */
	public double calcUtilityDistDelta(double dist) {

		if (groups == null)
			return 0;

		DistanceGroup group = groups[0];
		for (int i = 1; i < groups.length; i++) {
			if (groups[i].dist() > dist)
				break;

			group = groups[i];
		}

		return group.constant() + group.utilPerM() * (dist - group.dist());
	}

	static final class DeltaBuilder {

		private final DoubleList perDistGroup = new DoubleArrayList();
		double constant;
		double dailyUtilityConstant;

		public DoubleList getPerDistGroup() {
			return perDistGroup;
		}

		/**
		 * Sum delta of distance utilities.
		 */
		public void addUtilsDistance(AdvancedScoringConfigGroup.ModeParams params) {
			if (params.deltaPerDistGroup != null && !params.deltaPerDistGroup.isEmpty()) {
				if (perDistGroup.isEmpty()) {
					perDistGroup.addAll(params.deltaPerDistGroup);
					return;
				}

				if (perDistGroup.size() != params.deltaPerDistGroup.size()) {
					throw new IllegalArgumentException("Distance utility parameters do not match");
				}

				for (int i = 0; i < perDistGroup.size(); i++) {
					perDistGroup.set(i, perDistGroup.getDouble(i) + params.deltaPerDistGroup.get(i));
				}
			}
		}
	}
}
