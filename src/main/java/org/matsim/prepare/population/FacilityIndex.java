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

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Spatial index for facilities.
 */
public final class FacilityIndex {

	private static final Logger log = LogManager.getLogger(FacilityIndex.class);
	/**
	 * Maps activity type to spatial index.
	 */
	public final Map<String, STRtree> index = new HashMap<>();
	final ActivityFacilities all = FacilitiesUtils.createActivityFacilities();

	public FacilityIndex(String facilityPath, String crs) {
		this(facilityPath, f -> true, crs);
	}

	/**
	 * Creates spatial index for facilities.
	 *
	 * @param f predicate to filter or transform facilities
	 */
	public FacilityIndex(String facilityPath, Predicate<ActivityFacility> f, String crs) {

		new MatsimFacilitiesReader(crs, crs, all).readFile(facilityPath);

		Set<String> activities = all.getFacilities().values().stream()
			.filter(f)
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
	 * Samples from list of candidates until one option is not rejected.
	 */
	public static ActivityFacility sample(List<ActivityFacility> candidates, SplittableRandom rnd) {
		if (candidates.isEmpty())
			return null;

		return candidates.get(rnd.nextInt(candidates.size()));
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

	/**
	 * Groups candidates first using classifier. Then does a weighted sample on the groups. THen samples a facility by weight from the chosen group.
	 * The groups are typical zones which might have certain OD relations.
	 */
	public static ActivityFacility sampleByWeightWithGrouping(List<AttributedActivityFacility> candidates,
															  Function<AttributedActivityFacility, String> classifier,
															  Function<Map.Entry<String, List<AttributedActivityFacility>>, Double> groupWeight,
															  Function<AttributedActivityFacility, Double> facilityWeight, SplittableRandom rnd) {

		if (candidates.isEmpty())
			return null;

		// Entries which produce a null key are discarded
		Map<String, List<AttributedActivityFacility>> map = candidates.stream()
			.filter(af -> classifier.apply(af) != null)
			.collect(Collectors.groupingBy(classifier));

		List<Map.Entry<String, List<AttributedActivityFacility>>> grouped = map.entrySet().stream().toList();

		double totalWeight = 0.0;
		double[] weights = new double[grouped.size()];

		for (int i = 0; i < grouped.size(); ++i) {
			double w = groupWeight.apply(grouped.get(i));
			totalWeight += w;
			weights[i] = totalWeight;
		}

		// No weights, sample uniformly
		if (totalWeight == 0.0) {
			List<AttributedActivityFacility> list = grouped.get(rnd.nextInt(grouped.size())).getValue();
			return list.get(rnd.nextInt(list.size()));
		}

		double r = rnd.nextDouble(0.0, totalWeight);
		int idx = Arrays.binarySearch(weights, r);
		if (idx < 0) {
			idx = -idx - 1;
		}

		// First get the samples group
		List<AttributedActivityFacility> list = grouped.get(idx).getValue();

		double totalGroupWeight = 0;
		double[] groupWeights = new double[list.size()];

		for (int i = 0; i < list.size(); i++) {
			double w = facilityWeight.apply(list.get(i));
			totalGroupWeight += w;
			groupWeights[i] = totalGroupWeight;
		}

		int idx2 = Arrays.binarySearch(groupWeights, rnd.nextDouble(0, totalGroupWeight));
		if (idx2 < 0) {
			idx2 = -idx2 - 1;
		}

		// Sample random facility from the zone
		return list.get(idx2);
	}

	/**
	 * Groups candidates first using classifier. Then does a weighted sample on the groups and selects a random facility.
	 * The groups are typical zones which might have certain OD relations.
	 */
	public static ActivityFacility sampleWithGrouping(List<AttributedActivityFacility> candidates,
													  Function<AttributedActivityFacility, String> classifier,
													  Function<Map.Entry<String, List<AttributedActivityFacility>>, Double> groupWeight,
													  SplittableRandom rnd) {

		if (candidates.isEmpty())
			return null;

		Map<String, List<AttributedActivityFacility>> map = candidates.stream()
			.collect(Collectors.groupingBy(classifier));

		List<Map.Entry<String, List<AttributedActivityFacility>>> grouped = map.entrySet().stream().toList();

		double totalWeight = 0.0;
		double[] weights = new double[grouped.size()];

		for (int i = 0; i < grouped.size(); ++i) {
			double w = groupWeight.apply(grouped.get(i));
			totalWeight += w;
			weights[i] = totalWeight;
		}

		// No weights, sample uniformly
		if (totalWeight == 0.0) {
			List<AttributedActivityFacility> list = grouped.get(rnd.nextInt(grouped.size())).getValue();
			return list.get(rnd.nextInt(list.size()));
		}

		double r = rnd.nextDouble(0.0, totalWeight);
		int idx = Arrays.binarySearch(weights, r);
		if (idx < 0) {
			idx = -idx - 1;
		}

		// First sample a random group.
		List<AttributedActivityFacility> list = grouped.get(idx).getValue();

		// Sample random facility from the zone
		return list.get(rnd.nextInt(list.size()));
	}

}
