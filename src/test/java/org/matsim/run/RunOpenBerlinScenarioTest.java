package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.testcases.MatsimTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class RunOpenBerlinScenarioTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void pct1() {

		int code = MATSimApplication.execute(RunOpenBerlinScenario.class,
			"--1pct",
			"--output", utils.getOutputDirectory(),
			"--iterations", "2"
		);

		assertThat(code).isEqualTo(0);

	}
}
