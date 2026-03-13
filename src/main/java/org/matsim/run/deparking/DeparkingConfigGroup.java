package org.matsim.run.deparking;

import org.matsim.core.config.ReflectiveConfigGroup;

public class DeparkingConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "deparking";

	public DeparkingConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	private double K_p = 2.0;
	@Parameter
	private double K_i = 0.25;
	@Parameter
	private double K_d = 0.5;

	@Parameter
	private double maxCost = 20.0;

	@Parameter
	private int writeInterval = 50;

	@Parameter
	private DeParkingApproachType deParkingApproachType = DeParkingApproachType.PDI;

	public enum DeParkingApproachType {
		INVERSE_LINEAR,
		PDI
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

	public int getWriteInterval() {
		return writeInterval;
	}

	public void setWriteInterval(int writeInterval) {
		this.writeInterval = writeInterval;
	}
}
