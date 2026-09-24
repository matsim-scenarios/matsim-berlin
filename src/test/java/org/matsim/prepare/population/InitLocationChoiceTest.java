package org.matsim.prepare.population;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Activities are placed at a beeline distance, but the distance the pipeline is given (SrV) and the distance a run
 * reports are travelled distances, so everything depends on the conversion between the two. When it is off, the whole
 * distance distribution moves: with the fixed factor of 1.3 this class used before, a trip reported as 0.95 km was
 * placed 731 m away, which the network turns into about 1130 m - out of the 0-1 km group it was drawn for.
 */
class InitLocationChoiceTest {

	/**
	 * Routed over beeline distance, measured per beeline distance group on the trips of a 1 % run (Berlin residents,
	 * all modes, sum over sum), against the middle of the group.
	 */
	private static final double[][] MEASURED = {{500, 1.57}, {1500, 1.49}, {3500, 1.43}, {7500, 1.37}};

	/**
	 * What a trip placed at this beeline distance comes out as, by log-linear interpolation of the measurement. This
	 * is deliberately not the power law {@link InitLocationChoice} uses, so that the test checks the fit and not only
	 * that the code agrees with itself.
	 */
	private static double travelled(double beeline) {
		int i = 0;
		while (i < MEASURED.length - 2 && beeline > MEASURED[i + 1][0])
			i++;

		double[] lo = MEASURED[i], hi = MEASURED[i + 1];
		double slope = Math.log(hi[1] / lo[1]) / Math.log(hi[0] / lo[0]);

		return beeline * lo[1] * Math.pow(beeline / lo[0], slope);
	}

	/**
	 * The detour factor the conversion implies at the given travelled distance.
	 */
	private static double impliedDetour(double travelDist) {
		return travelDist * 1000 / InitLocationChoice.beelineDist(travelDist);
	}

	@Test
	void reproducesTheMeasuredDetour() {
		for (double[] point : MEASURED) {
			double beeline = point[0], detour = point[1];

			// a trip placed at this beeline distance is reported with this travelled distance, so asking for that
			// travelled distance has to give the beeline distance back
			assertThat(InitLocationChoice.beelineDist(beeline * detour / 1000))
				.as("beeline distance of a %.0f m trip with detour %.2f", beeline, detour)
				.isCloseTo(beeline, within(0.01 * beeline));
		}
	}

	@Test
	void placesTripsAtTheDistanceTheyWereReportedWith() {
		for (double travelDist = 0.3; travelDist <= 12; travelDist += 0.1) {
			double realized = travelled(InitLocationChoice.beelineDist(travelDist));

			assertThat(realized)
				.as("travelled distance of a trip drawn for %.1f km", travelDist)
				.isCloseTo(travelDist * 1000, within(0.02 * travelDist * 1000));
		}
	}

	/**
	 * The one the change is about: a trip the reference data reports below a kilometre has to stay below a kilometre,
	 * otherwise the 0-1 km group of the mode share dashboard empties into the one above it.
	 */
	@Test
	void keepsShortTripsShort() {
		for (double travelDist = 0.1; travelDist < 1; travelDist += 0.05) {
			assertThat(travelled(InitLocationChoice.beelineDist(travelDist)))
				.as("travelled distance of a trip drawn for %.2f km", travelDist)
				.isLessThan(1000);
		}
	}

	/**
	 * Outside the range the detour was measured in there is nothing to compare against, but the extrapolation still
	 * has to be a plausible detour and has to keep the conversion monotone.
	 */
	@Test
	void extrapolatesSensibly() {
		assertThat(impliedDetour(30)).isBetween(1.25, 1.35);
		assertThat(impliedDetour(70)).isBetween(1.2, 1.3);

		double last = 0;
		for (double travelDist = 0.1; travelDist <= 100; travelDist += 0.1) {
			assertThat(InitLocationChoice.beelineDist(travelDist)).isGreaterThan(last);
			last = InitLocationChoice.beelineDist(travelDist);
		}

		assertThat(InitLocationChoice.beelineDist(0)).isZero();
	}
}
