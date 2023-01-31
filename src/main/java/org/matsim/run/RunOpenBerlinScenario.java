package org.matsim.run;

import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.synthetic.CreateBerlinPopulation;
import picocli.CommandLine;

@CommandLine.Command(name = "RunOpenBerlinScenario")
@MATSimApplication.Prepare({CreateLandUseShp.class, CreateBerlinPopulation.class})
public class RunOpenBerlinScenario extends MATSimApplication {

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinScenario.class, args);
	}

}
