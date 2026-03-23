package org.matsim.run.deparking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InverseLinearDeParkingApproachTest {
	private final InverseLinearDeParkingApproach approach = new InverseLinearDeParkingApproach();

	@Test
	void calcCosts_increasesWhenOccupancyIsHigh() {
		double newCost = approach.calcCosts(2.0, 10.0);
		Assertions.assertEquals(15.0, newCost, 1e-9);
	}

	@Test
	void calcCosts_decreasesWhenOccupancyIsLow() {
		double newCost = approach.calcCosts(0.0, 10.0);
		Assertions.assertEquals(5.0, newCost, 1e-9);
	}

	@Test
	void calcCosts_staysSameAtTargetOccupancy() {
		double newCost = approach.calcCosts(1.0, 10.0);
		Assertions.assertEquals(10.0, newCost, 1e-9);
	}

	@Test
	void calcCosts_increasesFromZeroWhenOverCapacity() {
		double newCost = approach.calcCosts(Double.POSITIVE_INFINITY, 0.0);
		Assertions.assertEquals(4999.5, newCost, 1e-9);
	}

	@Test
	void calcCosts_handlesNaNAsNoOccupancyPressure() {
		double newCost = approach.calcCosts(Double.NaN, 10.0);
		Assertions.assertEquals(5.0, newCost, 1e-9);
	}

	@Test
	void calcCosts_treatsNegativeOccupancyAsZeroPressure() {
		double newCost = approach.calcCosts(-3.0, 10.0);
		Assertions.assertEquals(5.0, newCost, 1e-9);
	}

	@Test
	void calcCosts_neverGoesNegative() {
		double newCost = approach.calcCosts(-3.0, -2.0);
		Assertions.assertEquals(0.0, newCost, 1e-9);
	}
}
