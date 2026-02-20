package org.matsim.run.deparking;

public class InverseLinearDeParkingApproach implements DeParkingApproach {
	private static final double TARGET_RELATIVE_OCCUPANCY = 1.0;
	private static final double LEARNING_RATE = 0.5;
	private static final double MIN_ADJUSTMENT_BASE = 1.0;
	private static final double MAX_RELATIVE_OCCUPANCY = 4.0;

	@Override
	public double newParkingCost(double previousRelativeOccupancy, double previousCost) {
		// Ensure that previousCost is non-negative.
		double safePreviousCost = Math.max(0.0, previousCost);
		// Ensure that previousRelativeOccupancy is between 0 and MAX_RELATIVE_OCCUPANCY.
		double safeRelativeOccupancy = sanitizeRelativeOccupancy(previousRelativeOccupancy);
		// Ensure that previousCost = 0 is no absorbing state.
		double adjustmentBase = Math.max(safePreviousCost, MIN_ADJUSTMENT_BASE);
		double delta = (safeRelativeOccupancy - TARGET_RELATIVE_OCCUPANCY) * adjustmentBase * LEARNING_RATE;
		return Math.max(0.0, safePreviousCost + delta);
	}

	// Returns value between 0 and MAX_RELATIVE_OCCUPANCY
	private double sanitizeRelativeOccupancy(double relativeOccupancy) {
		if (Double.isNaN(relativeOccupancy) || relativeOccupancy < 0) {
			return 0.0;
		}
		if (Double.isInfinite(relativeOccupancy)) {
			return MAX_RELATIVE_OCCUPANCY;
		}
		return Math.min(relativeOccupancy, MAX_RELATIVE_OCCUPANCY);
	}
}
