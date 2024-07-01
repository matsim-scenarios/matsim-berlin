package org.matsim.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.prepare.facilities.AttributedActivityFacility;
import org.matsim.run.OpenBerlinScenario;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Spatial index for facilities.
 */
final class FacilityIndex {

	private static final Logger log = LogManager.getLogger(FacilityIndex.class);

	final ActivityFacilities all = FacilitiesUtils.createActivityFacilities();

	/**
	 * Maps activity type to spatial index.
	 */
	final Map<String, STRtree> index = new HashMap<>();

	FacilityIndex(String facilityPath) {

		new MatsimFacilitiesReader(OpenBerlinScenario.CRS, OpenBerlinScenario.CRS, all)
			.readFile(facilityPath);

		Set<String> activities = all.getFacilities().values().stream()
			.flatMap(a -> a.getActivityOptions().keySet().stream())
			.collect(Collectors.toSet());

		log.info("Found activity types: {}", activities);

		for (String act : activities) {

			NavigableMap<Id<ActivityFacility>, ActivityFacility> afs = all.getFacilitiesForActivityType(act);
			for (ActivityFacility af : afs.values()) {
				STRtree idx = this.index.computeIfAbsent(act, k -> new STRtree());
				idx.insert(MGC.coord2Point(af.getCoord()).getEnvelopeInternal(), new AttributedActivityFacility(af));
			}
		}

		// Build all trees
		index.values().forEach(STRtree::build);
	}

	/**
	 * Sample facility weighted by specific attribute.
	 */
	public static int sampleByWeight(List<AttributedActivityFacility> candidates, Function<AttributedActivityFacility, Double> getter, SplittableRandom rnd) {

		double totalWeight = 0;
		double[] weights = new double[candidates.size()];

		for (int i = 0; i < candidates.size(); i++) {
			double w = getter.apply(candidates.get(i));
			totalWeight += w;
			weights[i] = totalWeight;
		}
		double r = rnd.nextDouble(0, totalWeight);
		int idx = Arrays.binarySearch(weights, r);

		if (idx < 0) {
			idx = -idx - 1;
		}
		return idx;
	}

	/**
	 * Samples from list of candidates using weight until one option is not rejected.
	 *
	 * @return null if all options are rejected
	 */
	public static ActivityFacility sampleByWeightWithRejection(List<AttributedActivityFacility> candidates, Predicate<ActivityFacility> filter,
															   Function<AttributedActivityFacility, Double> getter, SplittableRandom rnd) {

		double totalWeight = 0;
		double[] weights = new double[candidates.size()];

		for (int i = 0; i < candidates.size(); i++) {
			double w = getter.apply(candidates.get(i));
			totalWeight += w;
			weights[i] = totalWeight;
		}

		for (int i = 0; i < candidates.size(); i++) {
			double r = rnd.nextDouble(0, totalWeight);
			int idx = Arrays.binarySearch(weights, r);

			if (idx < 0) {
				idx = -idx - 1;
			}

			AttributedActivityFacility af = candidates.get(idx);
			if (filter.test(af)) {
				return af;
			} else {
				double w = getter.apply(af);
				totalWeight -= w;

				// Update weights
				for (int j = idx; j < weights.length; j++) {
					weights[j] -= w;
				}
			}
		}

		return null;
	}

}
