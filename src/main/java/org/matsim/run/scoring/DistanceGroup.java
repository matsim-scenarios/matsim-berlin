package org.matsim.run.scoring;

/**
 * Store distance group
 *
 * @param dist     lower bound for distance group
 * @param constant added constant
 * @param util_m   utility per meter, i.e. slope of linear function
 */
record DistanceGroup(double dist, double constant, double util_m) implements Comparable<DistanceGroup> {
	@Override
	public int compareTo(DistanceGroup o) {
		return Double.compare(dist, o.dist);
	}
}
