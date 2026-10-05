package org.matsim.prepare.population;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AssignSpatialIncomeTest {

	@BeforeEach
	void printTestName(TestInfo testInfo) {
		System.out.println();
		System.out.println("=== " + testInfo.getTestMethod().orElseThrow().getName() + " ===");
	}

	@Test
	void normalizeLorIdsFromExcelAndShapeValues() {
		assertThat(AssignSpatialIncome.normalizeLorId("01100101")).isEqualTo("01100101");
		assertThat(AssignSpatialIncome.normalizeLorId("1100101.0")).isEqualTo("01100101");
		assertThat(AssignSpatialIncome.normalizeLorId(1100101)).isEqualTo("01100101");
	}

	@Test
	void recognizeIncomeWorkbookLorHeader() {
		assertThat(AssignSpatialIncome.isLorHeader("raumid")).isTrue();
	}

	@Test
	void parseGermanFormattedIncome() {
		assertThat(AssignSpatialIncome.parseGermanNumber("4.895 €")).isEqualTo(4895);
		assertThat(AssignSpatialIncome.parseGermanNumber("4.895,50 €")).isEqualTo(4895.5);
		assertThat(AssignSpatialIncome.parseGermanNumber("4895")).isEqualTo(4895);
	}

	@Test
	void calculateMedianForEvenAndOddNumberOfValues() {
		assertThat(AssignSpatialIncome.median(List.of(1., 3., 2.))).isEqualTo(2);
		assertThat(AssignSpatialIncome.median(List.of(1., 4., 2., 3.))).isEqualTo(2.5);
	}

	@Test
	void calculateMean() {
		assertThat(AssignSpatialIncome.mean(List.of(1., 2., 3.))).isEqualTo(2);
	}

	@Test
	void reconstructEquivalentIncomeBoundsFromHouseholdClass() {
		double[] householdBounds = {0, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, 5600};

		AssignSpatialIncome.IncomeBounds bounds = AssignSpatialIncome.incomeBounds(
			2733, 1.5, 249, householdBounds);
		System.out.printf("Reconstructed equivalent-income interval: [%.2f, %.2f) EUR%n",
			bounds.lower(), bounds.upper());

		assertThat(bounds.sourceLower()).isEqualTo(2400);
		assertThat(bounds.sourceUpper()).isCloseTo(4600 / 1.5, within(1e-9));
		assertThat(bounds.lower()).isEqualTo(bounds.sourceLower());
		assertThat(bounds.upper()).isEqualTo(bounds.sourceUpper());
		assertThat(bounds.minimumRelaxed()).isFalse();
	}

	@Test
	void preserveLowestClassWhenMinimumIncomeIsIncompatible() {
		double[] householdBounds = {0, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, 5600};

		AssignSpatialIncome.IncomeBounds bounds = AssignSpatialIncome.incomeBounds(
			249, 2.5, 249, householdBounds);
		System.out.printf("Minimum relaxed=%s; preserved lowest-class interval: [%.2f, %.2f) EUR%n",
			bounds.minimumRelaxed(), bounds.lower(), bounds.upper());

		assertThat(bounds.lower()).isEqualTo(100);
		assertThat(bounds.upper()).isEqualTo(200);
		assertThat(bounds.minimumRelaxed()).isTrue();
	}

	@Test
	void keepBoundaryCalibratedLowestClassIncomePositive() {
		double[] householdBounds = {0, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, 5600};
		AssignSpatialIncome.IncomeBounds bounds = AssignSpatialIncome.incomeBounds(
			249, 2.5, 249, householdBounds);
		List<AssignSpatialIncome.BoundedDraw> draws = List.of(
			new AssignSpatialIncome.BoundedDraw(bounds.lower(), bounds.upper(), 0.2),
			new AssignSpatialIncome.BoundedDraw(bounds.lower(), bounds.upper(), 0.5),
			new AssignSpatialIncome.BoundedDraw(bounds.lower(), bounds.upper(), 0.8)
		);

		AssignSpatialIncome.CalibrationResult result = AssignSpatialIncome.calibrateMedianDraws(
			draws, new AssignSpatialIncome.LogNormalFit(Math.log(1000), 0.55), 50);

		assertThat(result.clipped()).isTrue();
		assertThat(result.achievedMedian()).isEqualTo(100);
		assertThat(result.incomes()).allSatisfy(income -> {
			assertThat(income).isPositive();
			assertThat(income).isLessThan(200);
		});
	}

	@Test
	void drawInsideLowestOpenLogNormalTail() {
		double income = AssignSpatialIncome.drawTruncatedLogNormal(
			Math.log(1000), 0.7, new AssignSpatialIncome.BoundedDraw(0, 200, 0.5));
		System.out.printf("Draw from lowest income class: %.2f EUR%n", income);

		assertThat(income).isPositive();
		assertThat(income).isLessThan(200);
	}

	@Test
	void drawInsideHighestOpenIncomeClass() {
		double income = AssignSpatialIncome.drawTruncatedLogNormal(
			Math.log(100), 0.7, new AssignSpatialIncome.BoundedDraw(5000, Double.POSITIVE_INFINITY, 0.5));
		System.out.printf("Draw from open highest income class: %.2f EUR%n", income);

		assertThat(Double.isFinite(income)).isTrue();
		assertThat(income).isGreaterThanOrEqualTo(5000);
	}

	@Test
	void useClassBoundariesWhenTruncatedTailProbabilitiesUnderflow() {
		AssignSpatialIncome.BoundedDraw draw = new AssignSpatialIncome.BoundedDraw(1500, 2000, 0.5);

		double lowerTail = AssignSpatialIncome.drawTruncatedLogNormal(-1_000_000, 0.55, draw);
		double upperTail = AssignSpatialIncome.drawTruncatedLogNormal(1_000_000, 0.55, draw);
		System.out.printf("Numerical tail fallback: lower=%.2f, upper=%.2f EUR%n", lowerTail, upperTail);

		assertThat(lowerTail).isEqualTo(1500);
		assertThat(upperTail).isEqualTo(Math.nextDown(2000.));
	}

	@Test
	void calibrateMedianVeryCloseToClassBoundaryWithoutFailing() {
		List<AssignSpatialIncome.BoundedDraw> draws = List.of(
			new AssignSpatialIncome.BoundedDraw(1500, 2000, 0.2),
			new AssignSpatialIncome.BoundedDraw(1500, 2000, 0.5),
			new AssignSpatialIncome.BoundedDraw(1500, 2000, 0.8)
		);

		AssignSpatialIncome.CalibrationResult result = AssignSpatialIncome.calibrateMedianDraws(
			draws, new AssignSpatialIncome.LogNormalFit(7.495025965431403, 0.5546181337034363),
			1500.000001);
		System.out.printf("Near-boundary calibration: requested=%.9f, applied=%.9f, achieved=%.9f, clipped=%s%n",
			result.requestedTarget(), result.appliedTarget(), result.achievedMedian(), result.clipped());

		assertThat(result.achievedMedian()).isGreaterThanOrEqualTo(1500);
		assertThat(result.achievedMedian()).isLessThan(2000);
		assertThat(result.incomes()).allSatisfy(income -> {
			assertThat(income).isGreaterThanOrEqualTo(1500);
			assertThat(income).isLessThan(2000);
		});
	}

	@Test
	void keepOpenTopClassFiniteWhenUsingUpperMedianBoundary() {
		List<AssignSpatialIncome.BoundedDraw> draws = List.of(
			new AssignSpatialIncome.BoundedDraw(1000, 1500, 0.2),
			new AssignSpatialIncome.BoundedDraw(1000, 1500, 0.8),
			new AssignSpatialIncome.BoundedDraw(1500, 2000, 0.5),
			new AssignSpatialIncome.BoundedDraw(2000, Double.POSITIVE_INFINITY, 0.3),
			new AssignSpatialIncome.BoundedDraw(2000, Double.POSITIVE_INFINITY, 0.7)
		);

		AssignSpatialIncome.CalibrationResult result = AssignSpatialIncome.calibrateMedianDraws(
			draws, new AssignSpatialIncome.LogNormalFit(Math.log(1800), 0.55), 5000);
		System.out.printf("Upper-boundary calibration: applied=%.2f, achieved=%.2f, incomes=%s%n",
			result.appliedTarget(), result.achievedMedian(), result.incomes());

		assertThat(result.clipped()).isTrue();
		assertThat(result.incomes()).allSatisfy(income -> assertThat(Double.isFinite(income)).isTrue());
		assertThat(result.incomes()).noneMatch(income -> income == Double.MAX_VALUE);
		assertThat(result.achievedMedian()).isCloseTo(2000, within(1e-9));
	}

	@Test
	void calibrateFeasibleLorMedianWithoutLeavingIncomeClasses() {
		List<AssignSpatialIncome.BoundedDraw> draws = List.of(
			new AssignSpatialIncome.BoundedDraw(1000, 2000, 0.2),
			new AssignSpatialIncome.BoundedDraw(1000, 2000, 0.5),
			new AssignSpatialIncome.BoundedDraw(2000, 3000, 0.8)
		);

		AssignSpatialIncome.CalibrationResult result = AssignSpatialIncome.calibrateMedianDraws(
			draws, new AssignSpatialIncome.LogNormalFit(Math.log(1800), 0.5), 1900);
		System.out.printf("Feasible calibration: requested=%.2f, applied=%.2f, achieved=%.2f, shift=%.6f, incomes=%s%n",
			result.requestedTarget(), result.appliedTarget(), result.achievedMedian(),
			result.locationShift(), result.incomes());

		assertThat(result.clipped()).isFalse();
		assertThat(result.achievedMedian()).isCloseTo(1900, within(1e-7));
		for (int i = 0; i < draws.size(); i++) {
			assertThat(result.incomes().get(i)).isGreaterThanOrEqualTo(draws.get(i).lower());
			assertThat(result.incomes().get(i)).isLessThan(draws.get(i).upper());
		}
	}

	@Test
	void clipInfeasibleLorMedianToClassLimits() {
		List<AssignSpatialIncome.BoundedDraw> draws = List.of(
			new AssignSpatialIncome.BoundedDraw(1000, 1500, 0.2),
			new AssignSpatialIncome.BoundedDraw(1000, 1500, 0.8),
			new AssignSpatialIncome.BoundedDraw(2000, 3000, 0.5)
		);

		AssignSpatialIncome.CalibrationResult result = AssignSpatialIncome.calibrateMedianDraws(
			draws, new AssignSpatialIncome.LogNormalFit(Math.log(1800), 0.5), 2500);
		System.out.printf("Infeasible calibration: requested=%.2f, clipped-to=%.2f, achieved=%.2f, incomes=%s%n",
			result.requestedTarget(), result.appliedTarget(), result.achievedMedian(), result.incomes());

		assertThat(result.clipped()).isTrue();
		assertThat(result.appliedTarget()).isCloseTo(1500, within(1e-9));
		assertThat(result.achievedMedian()).isCloseTo(1500, within(1e-9));
		for (int i = 0; i < draws.size(); i++) {
			assertThat(result.incomes().get(i)).isGreaterThanOrEqualTo(draws.get(i).lower());
			assertThat(result.incomes().get(i)).isLessThan(draws.get(i).upper());
		}
	}
}
