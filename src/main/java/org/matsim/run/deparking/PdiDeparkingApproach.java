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
		double targetRelativeOccupancy = DeparkingApproachUtils.targetRelativeOccupancy(config);
		// Error is measured against the configured target relative occupancy:
		// error = relativeOccupancy - targetRelativeOccupancy.
		double error = relativeOccupancy - targetRelativeOccupancy;
		// Integral term:
		// cumulativeError_t = cumulativeError_(t-1) + error_t
		// For the first observation of a link/bin, the previous cumulative error is 0.
		double cumulativeError = cumulativeErrorCache.getOrDefault(linkId, Map.of()).getOrDefault(bin, 0.0) + error;
		// Derivative term input:
		// previousError = error_(t-1)
		// For the first observation of a link/bin, the previous error is 0.
		double previousError = previousErrorCache.getOrDefault(linkId, Map.of()).getOrDefault(bin, 0.0);
		double newCost = calcCost(error, cumulativeError, previousError);

		cumulativeErrorCache.computeIfAbsent(linkId, id -> new HashMap<>()).put(bin, cumulativeError);
		previousErrorCache.computeIfAbsent(linkId, id -> new HashMap<>()).put(bin, error);

		return newCost;
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
	double calcCost(double error, double cumulativeError, double previousError) {
		DeparkingConfigGroup configGroup = ConfigUtils.addOrGetModule(config, DeparkingConfigGroup.class);
		double rawCost = configGroup.getK_p() * error
			+ configGroup.getK_i() * cumulativeError
			+ configGroup.getK_d() * (error - previousError);
		return Math.max(0.0, Math.min(configGroup.getMaxCost(), rawCost));
	}

	private double getParkingSpots(Id<Link> linkId) {
		return parkingSpotCache.computeIfAbsent(linkId, id -> (Double) network.getLinks().get(id).getAttributes().getAttribute(RunAutofreiPolicyDeparking.PARKING_SPOTS_ATTR));
	}
}
