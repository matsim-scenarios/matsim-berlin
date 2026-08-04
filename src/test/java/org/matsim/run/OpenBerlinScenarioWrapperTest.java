package org.matsim.run;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.testcases.MatsimTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenBerlinScenarioWrapperTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void pct1() {

		int code = MATSimApplication.execute(OpenBerlinScenario.class,
			"--config", String.format("input/v%s/berlin-v%s-1pct.config.xml", OpenBerlinScenario.VERSION, OpenBerlinScenario.VERSION),
			"--output", utils.getOutputDirectory(),
			"--iterations", "2",
			// the checked-in 1pct population still encodes its typical durations in the activity type
			// (home_86400, ...) and carries no typicalDuration attribute; drop once it is re-preprocessed.
			"--allow-config-typical-durations",
			"--config:qsim.numberOfThreads", "2",
			"--config:global.numberOfThreads", "2",
			"--config:simwrapper.defaultDashboards", "disabled"
		);

		assertThat(code).isEqualTo(0);

	}
}
