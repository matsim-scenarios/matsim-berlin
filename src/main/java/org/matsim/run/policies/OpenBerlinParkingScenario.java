package org.matsim.run.policies;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.run.scoring.parking.ParkingModule;


/**
 * This class extends the Berlin scenario by parking search times. Currently, only one iteration is run based on that, scoring is performed.
 * By default, it uses the Belloche parking search model with a kernel of 500m distance.
 */
public class OpenBerlinParkingScenario extends OpenBerlinScenario {
	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);

		config.controller().setLastIteration(1);
		config.network().setInputFile("with-parking");
		config.controller().setOutputDirectory(config.controller().getOutputDirectory() + "-parking");
		config.controller().setRunId(config.controller().getRunId() + "-parking");

		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		controler.addOverridingModule(new ParkingModule());
	}
}
