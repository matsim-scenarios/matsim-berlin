package org.matsim.run.deparking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InverseLinearDeParkingApproachTest {
	private final InverseLinearDeParkingApproach approach = new InverseLinearDeParkingApproach();

	@Test
	void newParkingCost_increasesWhenOccupancyIsHigh() {
		double newCost = approach.newParkingCost(2.0, 10.0);
		Assertions.assertEquals(15.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_decreasesWhenOccupancyIsLow() {
		double newCost = approach.newParkingCost(0.0, 10.0);
		Assertions.assertEquals(5.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_staysSameAtTargetOccupancy() {
		double newCost = approach.newParkingCost(1.0, 10.0);
		Assertions.assertEquals(10.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_increasesFromZeroWhenOverCapacity() {
		double newCost = approach.newParkingCost(Double.POSITIVE_INFINITY, 0.0);
		Assertions.assertEquals(1.5, newCost, 1e-9);
	}

	@Test
	void newParkingCost_handlesNaNAsNoOccupancyPressure() {
		double newCost = approach.newParkingCost(Double.NaN, 10.0);
		Assertions.assertEquals(5.0, newCost, 1e-9);
	}

	@Test
	void newParkingCost_neverGoesNegative() {
		double newCost = approach.newParkingCost(-3.0, -2.0);
		Assertions.assertEquals(0.0, newCost, 1e-9);
	}
}
