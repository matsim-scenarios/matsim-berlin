package org.matsim.run.deparking;

import org.matsim.core.config.ReflectiveConfigGroup;

public class DeparkingConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "deparking";

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
	private int writeInterval = 1;

	@Parameter
	private DeParkingApproachType deParkingApproachType = DeParkingApproachType.PDI;

	@Parameter
	private IntegralApproach integralApproach = IntegralApproach.ALL;

	@Parameter
	private double smoothAlpha = 0.5; // only used if integralApproach is set to SMOOTHING, otherwise ignored

	@Parameter
	private double resetOnZeroThreshold = 0.1; // only used if integralApproach is set to RESET_ON_ZERO, otherwise ignored

	public enum DeParkingApproachType {
		INVERSE_LINEAR,
		PDI
	}

	public enum IntegralApproach {
		ALL, RESET_ON_ZERO, SMOOTHING
	}

	public double getResetOnZeroThreshold() {
		return resetOnZeroThreshold;
	}

	public void setResetOnZeroThreshold(double resetOnZeroThreshold) {
		this.resetOnZeroThreshold = resetOnZeroThreshold;
	}

	public IntegralApproach getIntegralApproach() {
		return integralApproach;
	}

	public void setIntegralApproach(IntegralApproach integralApproach) {
		this.integralApproach = integralApproach;
	}

	public double getSmoothAlpha() {
		return smoothAlpha;
	}

	public void setSmoothAlpha(double smoothAlpha) {
		this.smoothAlpha = smoothAlpha;
	}

	public DeParkingApproachType getDeParkingApproachType() {
		return deParkingApproachType;
	}

	public void setDeParkingApproachType(DeParkingApproachType deParkingApproachType) {
		this.deParkingApproachType = deParkingApproachType;
	}

	public double getK_p() {
		return K_p;
	}

	public void setK_p(double k_p) {
		K_p = k_p;
	}

	public double getK_i() {
		return K_i;
	}

	public void setK_i(double k_i) {
		K_i = k_i;
	}

	public double getK_d() {
		return K_d;
	}

	public void setK_d(double k_d) {
		K_d = k_d;
	}

	public double getMaxCost() {
		return maxCost;
	}

	public void setMaxCost(double maxCost) {
		this.maxCost = maxCost;
	}

	public double getTargetRelativeOccupancy() {
		return targetRelativeOccupancy;
	}

	public void setTargetRelativeOccupancy(double targetRelativeOccupancy) {
		this.targetRelativeOccupancy = targetRelativeOccupancy;
	}

	public int getWriteInterval() {
		return writeInterval;
	}

	public void setWriteInterval(int writeInterval) {
		this.writeInterval = writeInterval;
	}
}
