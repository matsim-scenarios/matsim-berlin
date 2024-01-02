package org.matsim.run;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.testcases.MatsimTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class RunOpenBerlinScenarioTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void pct1() {

		int code = MATSimApplication.execute(RunOpenBerlinScenario.class,
			"--1pct",
			"--output", utils.getOutputDirectory(),
			"--iterations", "2",
			"--config:simwrapper.defaultDashboards", "disabled"
		);

		assertThat(code).isEqualTo(0);

	}
}
