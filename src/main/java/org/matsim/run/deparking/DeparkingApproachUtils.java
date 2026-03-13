package org.matsim.run.deparking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

final class DeparkingApproachUtils {
	static final double TARGET_RELATIVE_OCCUPANCY = 1.0;
	static final double MAX_RELATIVE_OCCUPANCY = 4.0;

	private DeparkingApproachUtils() {
	}

	static int bin(double from, double to) {
		return (int) Math.floor(from / (to - from));
	}

	static double weightedRelativeOccupancy(ParkingAnalyzer analyzer, Id<Link> linkId, int iteration, double from, double to, double availableSpots) {
		double binSizeInSeconds = to - from;
		double weightedOccupancy = analyzer.occupancy(iteration, linkId, from, to).stream()
			.mapToDouble(o -> (o.toTime() - o.fromTime()) * o.occupancy())
			.sum() / binSizeInSeconds;
		return sanitizeRelativeOccupancy(weightedOccupancy / availableSpots);
	}

	static double sanitizeRelativeOccupancy(double relativeOccupancy) {
		if (Double.isNaN(relativeOccupancy) || relativeOccupancy < 0) {
			return 0.0;
		}
		if (Double.isInfinite(relativeOccupancy)) {
			return MAX_RELATIVE_OCCUPANCY;
		}
		return Math.min(relativeOccupancy, MAX_RELATIVE_OCCUPANCY);
	}
}
