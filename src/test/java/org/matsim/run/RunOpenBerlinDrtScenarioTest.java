package org.matsim.run;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.run.policies.OpenBerlinDrtScenario;
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
		assertThat(count).isGreaterThan(1000);


	}
}
