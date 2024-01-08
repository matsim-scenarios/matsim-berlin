package org.matsim.run.scoring;

import org.matsim.core.scoring.functions.ModeUtilityParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mode utility with separate marginalUtilityOfDistance_m per distance group.
 */
public class DistanceGroupModeUtilityParameters extends ModeUtilityParameters {

	private final List<DistanceGroup> groups = new ArrayList<>();

	/**
	 * Constructor which copies the base params from given modeParams.
	 */
	public DistanceGroupModeUtilityParameters(ModeUtilityParameters modeParams,
											  List<Integer> dists, VspScoringConfigGroup.ModeParams params) {
		super(modeParams.marginalUtilityOfTraveling_s, modeParams.marginalUtilityOfDistance_m, modeParams.monetaryDistanceCostRate,
			modeParams.constant, modeParams.dailyMoneyConstant, modeParams.dailyUtilityConstant);

		// Nothing to do if no distance groups are defined.
		if (dists.isEmpty()) {
			return;
		}

		List<Integer> copy = new ArrayList<>(dists);

		if (copy.get(0) != 0)
			copy.add(0, 0);

		// Effectively no distance groups present
		if (copy.size() <= 1)
			return;

		for (int i = 0; i < copy.size() - 1; i++) {

			int dist = copy.get(i);
			double util = params.getDistUtil(dist).orElseThrow();

			double constant;
			if (i == 0)
				constant = 0;
			else {
				DistanceGroup prev = groups.get(groups.size() - 1);
				constant = prev.constant + prev.util_m * (dist - prev.dist);
			}

			groups.add(new DistanceGroup(dist, constant, util));
		}
	}


	/**
	 * Calculate the utility for given distance.
	 */
	public double calcDistUtility(double dist) {

		if (groups.isEmpty())
			return marginalUtilityOfDistance_m * dist;

		DistanceGroup group = groups.get(0);
		for (int i = 1; i < groups.size(); i++) {
			if (groups.get(i).dist > dist)
				break;

			group = groups.get(i);
		}

		return group.constant + group.util_m * (dist - group.dist);
	}

	/**
	 * Store distance group
	 * @param dist lower bound for distance group
	 * @param constant added constant
	 * @param util_m utility per meter, i.e. slope of linear function
	 */
	record DistanceGroup(double dist, double constant, double util_m) implements Comparable<DistanceGroup> {
		@Override
		public int compareTo(DistanceGroup o) {
			return Double.compare(dist, o.dist);
		}
	}

}
