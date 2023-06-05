package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "reproject-network", description = "Change the CRS of a network")
public class ReprojectNetwork implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "Path to input network", required = true)
	private Path input;

	@CommandLine.Option(names = "--transit-schedule", description = "Path to input transit schedule", required = true)
	private Path transitSchedule;

	@CommandLine.Option(names = "--output", description = "Desired output path", required = true)
	private Path output;

	@CommandLine.Option(names = "--output-transit", description = "Desired output path", required = true)
	private Path outputTransit;

	@CommandLine.Mixin
	private CrsOptions crs;

	public static void main(String[] args) {
		new ReprojectNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.createConfig();

		config.global().setCoordinateSystem(crs.getTargetCRS());
		config.network().setInputFile(input.toString());
		config.network().setInputCRS(crs.getInputCRS());

		config.transit().setInputScheduleCRS(crs.getInputCRS());
		config.transit().setTransitScheduleFile(transitSchedule.toString());

		// Scenario loader does the reprojection for the network
		Scenario scenario = ScenarioUtils.loadScenario(config);

		NetworkUtils.writeNetwork(scenario.getNetwork(), output.toString());

		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(outputTransit.toString());


		return 0;
	}
}
