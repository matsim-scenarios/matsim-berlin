package org.matsim.synthetic.opt;

import org.assertj.core.data.Offset;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


public class ScoreCalculatorTest {

	@Test
	public void absChange() {

		PlanAssignmentProblem.ErrorMetric e = PlanAssignmentProblem.ErrorMetric.abs_error;
		// some count value
		int c = 5;

		assertThat(ScoreCalculator.diffChange(e, c, 3, 5))
				.isEqualTo(BigDecimal.valueOf(-2));

		assertThat(ScoreCalculator.diffChange(e, c, 6, 5))
				.isEqualTo(BigDecimal.valueOf(-1));

		assertThat(ScoreCalculator.diffChange(e, c, 4, 6))
				.isEqualTo(BigDecimal.valueOf(0));

		assertThat(ScoreCalculator.diffChange(e, c, 5, 3))
				.isEqualTo(BigDecimal.valueOf(2));

		assertThat(ScoreCalculator.diffChange(e, c, 10, 15))
				.isEqualTo(BigDecimal.valueOf(5));

	}

	@Test
	public void logChange() {

		PlanAssignmentProblem.ErrorMetric e = PlanAssignmentProblem.ErrorMetric.log_error;

		assertThat(ScoreCalculator.diffChange(e, 5, 3, 5))
				.isEqualTo(BigDecimal.valueOf(-0.5108256237659907));

		assertThat(ScoreCalculator.diffChange(e, 5, 5, 3))
				.isEqualTo(BigDecimal.valueOf(0.5108256237659907));

		assertThat(ScoreCalculator.diffChange(e, 10, 5, 20))
				.isCloseTo(BigDecimal.valueOf(0), Offset.offset(BigDecimal.ZERO));

	}

}
