package org.matsim.run;

import org.matsim.application.MATSimApplication;
import picocli.CommandLine;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = RunOpenBerlinScenario.VERSION, mixinStandardHelpOptions = true)
@MATSimApplication.Prepare()
public class RunOpenBerlinScenario extends MATSimApplication {

	public static final String VERSION = "6.0";
	public static final String CRS = "EPSG:25832";

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinScenario.class, args);
	}

}
