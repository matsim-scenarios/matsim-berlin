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

		return group.constant() + group.util_m() * (dist - group.dist());
	}

	public static final class DeltaBuilder {

		private final DoubleList utilsDistance = new DoubleArrayList();
		double constant;
		double dailyUtilityConstant;

		public DoubleList getUtilsDistance() {
			return utilsDistance;
		}

		/**
		 * Sum delta of distance utilities.
		 */
		public void addUtilsDistance(AdvancedScoringConfigGroup.ModeParams params) {
			if (params.deltaUtilsDistance != null && !params.deltaUtilsDistance.isEmpty()) {
				if (utilsDistance.isEmpty()) {
					utilsDistance.addAll(params.deltaUtilsDistance);
					return;
				}

				if (utilsDistance.size() != params.deltaUtilsDistance.size()) {
					throw new IllegalArgumentException("Distance utility parameters do not match");
				}

				for (int i = 0; i < utilsDistance.size(); i++) {
					utilsDistance.set(i, utilsDistance.getDouble(i) + params.deltaUtilsDistance.get(i));
				}
			}
		}
	}
}
