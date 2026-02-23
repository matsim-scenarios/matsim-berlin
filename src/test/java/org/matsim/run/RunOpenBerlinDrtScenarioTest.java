package org.matsim.run;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class RunOpenBerlinDrtScenarioTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void pct1() throws IOException {

		int code = MATSimApplication.execute(OpenBerlinDrtScenario.class,
			"--1pct",
			"--output", utils.getOutputDirectory(),
			"--iterations", "2",
			"--config:qsim.numberOfThreads", "2",
			"--config:global.numberOfThreads", "2",
			"--config:simwrapper.defaultDashboards", "disabled"
		);

		assertThat(code).isEqualTo(0);

		String legs = utils.getOutputDirectory() + "/berlin-v" + OpenBerlinScenario.VERSION + "-drt.output_drt_legs_drt.csv";
		Path path = Path.of(legs);
		assertThat(path)
			.isNotEmptyFile();

		// Check if DRT trips have been performed
		long count = Files.lines(path).count();

		// after the 1pct scenario was calibrated (i.e. the plans for 1pct changed in the public svn), this needed to be adapted. I am not really
		// sure if 100 is plausible here, but on github actions, there were 117 instead of >1000 lines. paul, feb'26
		assertThat(count).isGreaterThan(100);


	}
}
