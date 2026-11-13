package org.matsim.run;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.testcases.MatsimTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class OpenBerlinScenarioWrapperTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void pct1() {

		// the inputs are on shared-svn, behind the VSP login; CI gets it from the repository secrets, forks don't
		assumeTrue(HttpAuthentication.get(HttpAuthentication.USER) != null && HttpAuthentication.get(HttpAuthentication.PASSWORD) != null,
			"needs " + HttpAuthentication.USER + " and " + HttpAuthentication.PASSWORD + " for the shared-svn inputs");

		int code = MATSimApplication.execute(OpenBerlinScenario.class,
			"--config", "input/v7.2-pre-2.1/berlin-v7.2-pre-2.1-1pct.config.xml",
			"--output", utils.getOutputDirectory(),
			"--iterations", "2",
			"--config:qsim.numberOfThreads", "2",
			"--config:global.numberOfThreads", "2",
			"--config:simwrapper.defaultDashboards", "disabled"
		);

		assertThat(code).isEqualTo(0);

	}
}
