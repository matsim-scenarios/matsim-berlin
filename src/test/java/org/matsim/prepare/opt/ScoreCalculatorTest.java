package org.matsim.prepare.opt;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ScoreCalculatorTest {

	@Test
	public void absChange() {

		ErrorMetric e = ErrorMetric.abs_error;
		// some count value
		int c = 5;

		assertThat(ScoreCalculator.diffChange(e, c, 3, 5))
				.isEqualTo(-2);

		assertThat(ScoreCalculator.diffChange(e, c, 6, 5))
				.isEqualTo(-1);

		assertThat(ScoreCalculator.diffChange(e, c, 4, 6))
				.isEqualTo(0);

		assertThat(ScoreCalculator.diffChange(e, c, 5, 3))
				.isEqualTo(2);

		assertThat(ScoreCalculator.diffChange(e, c, 10, 15))
				.isEqualTo(5);

	}

	@Test
	public void logChange() {

		ErrorMetric e = ErrorMetric.log_error;

		assertThat(ScoreCalculator.diffChange(e, 5, 3, 5))
				.isEqualTo(-0.10536051565782628);

		assertThat(ScoreCalculator.diffChange(e, 5, 5, 3))
				.isEqualTo(0.10536051565782628);

		// Does not hold anymore, because of the constant
		assertThat(ScoreCalculator.diffChange(e, 10, 5, 20))
				.isEqualTo(0);

	}

}
