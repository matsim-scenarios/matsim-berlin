package org.matsim.run.deparking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.run.policies.autofrei.RunAutofreiPolicyDeparking;

import java.util.HashMap;
import java.util.Map;

public class InverseLinearDeParkingApproach implements DeParkingApproach {
	private static final double TARGET_RELATIVE_OCCUPANCY = DeparkingApproachUtils.TARGET_RELATIVE_OCCUPANCY;
	private static final double LEARNING_RATE = 0.5;
	private static final double MIN_ADJUSTMENT_BASE = 1.0;

	private final Map<Id<Link>, Double> parkingSpotCache = new HashMap<>();
	private final Map<Id<Link>, Map<Integer, Double>> previousCostCache = new HashMap<>();

	@Inject
	private Network network;

	InverseLinearDeParkingApproach() {
	}

	InverseLinearDeParkingApproach(Network network) {
		this.network = network;
	}

	@Override
	public double newParkingCost(ParkingAnalyzer analyzer, Id<Link> linkId, int iteration, double from, double to) {
		int bin = DeparkingApproachUtils.bin(from, to);
		double weightedRelativeOccupancy = DeparkingApproachUtils.weightedRelativeOccupancy(analyzer, linkId, iteration, from, to, getParkingSpots(linkId));
		double newCosts = calcCosts(weightedRelativeOccupancy, previousCostCache.getOrDefault(linkId, Map.of()).getOrDefault(bin, 0.));
		previousCostCache.computeIfAbsent(linkId, id -> new HashMap<>()).put(bin, newCosts);
		return newCosts;
	}

	private double getParkingSpots(Id<Link> linkId) {
		return parkingSpotCache.computeIfAbsent(linkId, id -> (Double) network.getLinks().get(id).getAttributes().getAttribute(RunAutofreiPolicyDeparking.PARKING_SPOTS_ATTR));
	}

	public double calcCosts(double previousRelativeOccupancy, double previousCost) {
		// Ensure that previousCost is non-negative.
		double safePreviousCost = Math.max(0.0, previousCost);
		// Ensure that previousRelativeOccupancy is between 0 and MAX_RELATIVE_OCCUPANCY.
		double safeRelativeOccupancy = DeparkingApproachUtils.sanitizeRelativeOccupancy(previousRelativeOccupancy);
		// Ensure that previousCost = 0 is no absorbing state.
		double adjustmentBase = Math.max(safePreviousCost, MIN_ADJUSTMENT_BASE);
		double delta = (safeRelativeOccupancy - TARGET_RELATIVE_OCCUPANCY) * adjustmentBase * LEARNING_RATE;
		return Math.max(0.0, safePreviousCost + delta);
	}
}
