package org.matsim.run.scoring;

import org.junit.jupiter.api.Test;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceGroupModeUtilityParametersTest {

	private final ModeUtilityParameters base = new ModeUtilityParameters(
		0, -1, 0,
		0, 0, 0
	);

	private static VspScoringConfigGroup.ModeParams params(List<Integer> dists, List<Double> utils) {
		VspScoringConfigGroup.ModeParams p = new VspScoringConfigGroup.ModeParams();

		p.setDistUtil(0, utils.get(0));

		for (int i = 0; i < utils.size() - 1; i++) {
			p.setDistUtil(dists.get(i), utils.get(i + 1));
		}

		return p;
	}

	@Test
	void empty() {

		DistanceGroupModeUtilityParameters m = new DistanceGroupModeUtilityParameters(base, List.of(), new VspScoringConfigGroup.ModeParams());

		assertThat(m.calcDistUtility(1000)).isEqualTo(-1000);
		assertThat(m.calcDistUtility(0)).isEqualTo(0);
		assertThat(m.calcDistUtility(5000)).isEqualTo(-5000);

	}

	@Test
	void groups() {

		List<Integer> dists = List.of(1000, 5000, 10000);
		DistanceGroupModeUtilityParameters m = new DistanceGroupModeUtilityParameters(
			base,
			dists,
			params(dists, List.of(-1d, -0.5d, -0.1d, -0.001d))
		);


		assertThat(m.calcDistUtility(0)).isEqualTo(0);
		assertThat(m.calcDistUtility(500)).isEqualTo(-500);
		assertThat(m.calcDistUtility(1000)).isEqualTo(-1000);

		assertThat(m.calcDistUtility(1500)).isEqualTo(-1250d);
		assertThat(m.calcDistUtility(2000)).isEqualTo(-1500d);

		assertThat(m.calcDistUtility(5000)).isEqualTo(-3000d);
		assertThat(m.calcDistUtility(8000)).isEqualTo(-3300d);

		assertThat(m.calcDistUtility(10000)).isEqualTo(-3500d);
		assertThat(m.calcDistUtility(15000)).isEqualTo(-3505d);


	}
}
