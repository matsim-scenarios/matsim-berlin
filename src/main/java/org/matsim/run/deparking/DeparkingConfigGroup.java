package org.matsim.run.deparking;

import org.matsim.core.config.ReflectiveConfigGroup;

public class DeparkingConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "deparking";

	private static final String K_P = "K_p";
	private static final String K_I = "K_i";
	private static final String K_D = "K_d";
	private static final String MAX_COST = "maxCost";
	private static final String TARGET_RELATIVE_OCCUPANCY = "targetRelativeOccupancy";
	private static final String WRITE_INTERVAL = "writeInterval";
	private static final String DEPARKING_APPROACH_TYPE = "deParkingApproachType";

	public DeparkingConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	private double K_p = 1.0; // [€/h] used in relativeError * K_p
	@Parameter
	private double K_i = 0.25; // [€/h] used in integral * K_i, where integral is the sum of all past relative errors
	@Parameter
	private double K_d = 0.25; // [€/h] used in derivative * K_d, where derivative is the change of the relative error compared to the last iteration

	@Parameter
	private double maxCost = 3.0; // = 3 €/h, which is the normal parking cost in Berlin.

	@Parameter
	private double targetRelativeOccupancy = 1.0;

	@Parameter
	private int writeInterval = 50;

	@Parameter
	private DeParkingApproachType deParkingApproachType = DeParkingApproachType.PDI;

	public enum DeParkingApproachType {
		INVERSE_LINEAR,
		PDI
	}

	@StringGetter(DEPARKING_APPROACH_TYPE)
	public DeParkingApproachType getDeParkingApproachType() {
		return deParkingApproachType;
	}

	@StringSetter(DEPARKING_APPROACH_TYPE)
	public void setDeParkingApproachType(DeParkingApproachType deParkingApproachType) {
		this.deParkingApproachType = deParkingApproachType;
	}

	@StringGetter(K_P)
	public double getK_p() {
		return K_p;
	}

	@StringSetter(K_P)
	public void setK_p(double k_p) {
		K_p = k_p;
	}

	@StringGetter(K_I)
	public double getK_i() {
		return K_i;
	}

	@StringSetter(K_I)
	public void setK_i(double k_i) {
		K_i = k_i;
	}

	@StringGetter(K_D)
	public double getK_d() {
		return K_d;
	}

	@StringSetter(K_D)
	public void setK_d(double k_d) {
		K_d = k_d;
	}

	@StringGetter(MAX_COST)
	public double getMaxCost() {
		return maxCost;
	}

	@StringSetter(MAX_COST)
	public void setMaxCost(double maxCost) {
		this.maxCost = maxCost;
	}

	@StringGetter(TARGET_RELATIVE_OCCUPANCY)
	public double getTargetRelativeOccupancy() {
		return targetRelativeOccupancy;
	}

	@StringSetter(TARGET_RELATIVE_OCCUPANCY)
	public void setTargetRelativeOccupancy(double targetRelativeOccupancy) {
		this.targetRelativeOccupancy = targetRelativeOccupancy;
	}

	@StringGetter(WRITE_INTERVAL)
	public int getWriteInterval() {
		return writeInterval;
	}

	@StringSetter(WRITE_INTERVAL)
	public void setWriteInterval(int writeInterval) {
		this.writeInterval = writeInterval;
	}
}
