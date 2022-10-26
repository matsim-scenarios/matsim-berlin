package org.matsim.run;

import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.synthetic.CreatePopulation;
import picocli.CommandLine;

@CommandLine.Command(name = "RunOpenBerlinScenario")
@MATSimApplication.Prepare({CreateLandUseShp.class, CreatePopulation.class})
public class RunOpenBerlinScenario extends MATSimApplication {

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinScenario.class, args);
	}

}
