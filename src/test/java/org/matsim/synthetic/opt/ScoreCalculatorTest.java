package org.matsim.synthetic.opt;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ScoreCalculatorTest {

	@Test
	public void diffChange() {

		assertThat(ScoreCalculator.diffChange(3, 5))
				.isEqualTo(2);

		assertThat(ScoreCalculator.diffChange(-5, -6))
				.isEqualTo(1);

		assertThat(ScoreCalculator.diffChange(-1, 1))
				.isEqualTo(0);

		assertThat(ScoreCalculator.diffChange(-1, 2))
				.isEqualTo(1);

		assertThat(ScoreCalculator.diffChange(5, -2))
				.isEqualTo(-3);

		assertThat(ScoreCalculator.diffChange(5, 1))
				.isEqualTo(-4);

		assertThat(ScoreCalculator.diffChange(-7, -6))
				.isEqualTo(-1);

	}
}
