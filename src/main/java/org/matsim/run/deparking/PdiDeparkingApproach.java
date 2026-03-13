package org.matsim.run.deparking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.run.policies.autofrei.RunAutofreiPolicyDeparking;

import java.util.HashMap;
import java.util.Map;

public class PdiDeparkingApproach implements DeParkingApproach {
	private final Map<Id<Link>, Double> parkingSpotCache = new HashMap<>();
	// Controller state is tracked independently per link and time bin so one crowded period
	// does not leak into another link or another bin on the same link.
	private final Map<Id<Link>, Map<Integer, Double>> cumulativeErrorCache = new HashMap<>();
	private final Map<Id<Link>, Map<Integer, Double>> previousErrorCache = new HashMap<>();

	@Inject
	private Config config;

	@Inject
	private Network network;

	PdiDeparkingApproach() {
	}

	PdiDeparkingApproach(Config config, Network network) {
		this.config = config;
		this.network = network;
	}

	@Override
	public double newParkingCost(ParkingAnalyzer analyzer, Id<Link> linkId, int iteration, double from, double to) {
		int bin = DeparkingApproachUtils.bin(from, to);
		double relativeOccupancy = DeparkingApproachUtils.weightedRelativeOccupancy(analyzer, linkId, iteration, from, to, getParkingSpots(linkId));
		DeparkingConfigGroup configGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		double targetRelativeOccupancy = DeparkingApproachUtils.targetRelativeOccupancy(config);
		// Error is measured against the configured target relative occupancy:
		// error = relativeOccupancy - targetRelativeOccupancy.
		double error = relativeOccupancy - targetRelativeOccupancy;
		// Integral term:
		// The exact update depends on the configured integral approach:
		// ALL: cumulativeError_t = cumulativeError_(t-1) + error_t
		// RESET_ON_ZERO: cumulativeError_t = 0 if |error_t| <= threshold, else cumulativeError_(t-1) + error_t
		// SMOOTHING: cumulativeError_t = alpha * cumulativeError_(t-1) + error_t
		// For the first observation of a link/bin, the previous cumulative error is 0.
		double previousCumulativeError = cumulativeErrorCache.getOrDefault(linkId, Map.of()).getOrDefault(bin, 0.0);
		double cumulativeError = updateCumulativeError(configGroup, previousCumulativeError, error);
		// Derivative term input:
		// previousError = error_(t-1)
		// For the first observation of a link/bin, the previous error is 0.
		double previousError = previousErrorCache.getOrDefault(linkId, Map.of()).getOrDefault(bin, 0.0);
		double newCost = calcCost(configGroup, error, cumulativeError, previousError);

		cumulativeErrorCache.computeIfAbsent(linkId, id -> new HashMap<>()).put(bin, cumulativeError);
		previousErrorCache.computeIfAbsent(linkId, id -> new HashMap<>()).put(bin, error);

		return newCost;
	}

	double updateCumulativeError(DeparkingConfigGroup configGroup, double previousCumulativeError, double error) {
		return switch (configGroup.getIntegralApproach()) {
			case ALL -> previousCumulativeError + error;
			case RESET_ON_ZERO -> Math.abs(error) <= configGroup.getResetOnZeroThreshold() ? 0.0 : previousCumulativeError + error;
			case SMOOTHING -> configGroup.getSmoothAlpha() * previousCumulativeError + error;
		};
	}

	// Direct PDI control law:
	// newCost = clamp(
	//     Kp * error
	//   + Ki * cumulativeError
	//   + Kd * (error - previousError),
	//   0,
	//   maxCost
	// )
	// The controller output is the full next parking cost, not an increment on the previous cost.
	double calcCost(DeparkingConfigGroup configGroup, double error, double cumulativeError, double previousError) {
		double rawCost = configGroup.getK_p() * error
			+ configGroup.getK_i() * cumulativeError
			+ configGroup.getK_d() * (error - previousError);
		return Math.max(0.0, Math.min(configGroup.getMaxCost(), rawCost));
	}

	private double getParkingSpots(Id<Link> linkId) {
		return parkingSpotCache.computeIfAbsent(linkId, id -> (Double) network.getLinks().get(id).getAttributes().getAttribute(RunAutofreiPolicyDeparking.PARKING_SPOTS_ATTR));
	}
}
