package org.matsim.run.scoring;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.junit.jupiter.api.Test;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceGroupModeUtilityParametersTest {

	private final ModeUtilityParameters base = new ModeUtilityParameters(
		0, -1, 0,
		0, 0, 0
	);

	private static DistanceGroupModeUtilityParameters params(ModeUtilityParameters base,
															 List<Integer> dists, DoubleList utils) {
		return new DistanceGroupModeUtilityParameters(
			base,
			new DistanceGroupModeUtilityParameters.DeltaBuilder(),
			IndividualPersonScoringParameters.calcDistanceGroups(dists, utils));
	}

	@Test
	void empty() {

		DistanceGroupModeUtilityParameters m = params(base, List.of(), DoubleList.of());

		// Delta will be 0 for any distance
		assertThat(m.calcUtilityDistDelta(1000)).isEqualTo(0);
		assertThat(m.calcUtilityDistDelta(0)).isEqualTo(0);
		assertThat(m.calcUtilityDistDelta(5000)).isEqualTo(0);

	}

	@Test
	void manyGroups() {
		List<Integer> dists = List.of(1000, 5000, 10000);

		DistanceGroupModeUtilityParameters m = params(base, dists, DoubleList.of(-1d, -0.5d, -0.1d));

		assertThat(m.calcUtilityDistDelta(0)).isEqualTo(0);
		assertThat(m.calcUtilityDistDelta(500)).isEqualTo(-0.5);
		assertThat(m.calcUtilityDistDelta(1000)).isEqualTo(-1);

		assertThat(m.calcUtilityDistDelta(3000)).isEqualTo(-0.75);

		assertThat(m.calcUtilityDistDelta(5000)).isEqualTo(-0.5);

		assertThat(m.calcUtilityDistDelta(10000)).isEqualTo(-0.1d);
		assertThat(m.calcUtilityDistDelta(20000)).isEqualTo(-0.2d);

	}

	@Test
	void mixedSigns() {

		List<Integer> dists = List.of(1000, 2000, 3000);

		DistanceGroupModeUtilityParameters m = params(base, dists, DoubleList.of(-1d, 1d, 0));

		assertThat(m.calcUtilityDistDelta(0)).isEqualTo(0);
		assertThat(m.calcUtilityDistDelta(500)).isEqualTo(-0.5);
		assertThat(m.calcUtilityDistDelta(1000)).isEqualTo(-1);
		assertThat(m.calcUtilityDistDelta(1500)).isEqualTo(0);

		assertThat(m.calcUtilityDistDelta(2000)).isEqualTo(1);

		assertThat(m.calcUtilityDistDelta(3000)).isEqualTo(0);
		assertThat(m.calcUtilityDistDelta(5000)).isEqualTo(0);


	}

	@Test
	void oneGroup() {

		DistanceGroupModeUtilityParameters m = params(base, List.of(1000), DoubleList.of(-1));

		assertThat(m.calcUtilityDistDelta(0)).isEqualTo(0);
		assertThat(m.calcUtilityDistDelta(500)).isEqualTo(-0.5);
		assertThat(m.calcUtilityDistDelta(1000)).isEqualTo(-1);
		assertThat(m.calcUtilityDistDelta(2000)).isEqualTo(-2);

	}
}
