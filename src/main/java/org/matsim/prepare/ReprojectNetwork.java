package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CommandLine.Command(name = "reproject-network", description = "Change the CRS of a network")
public class ReprojectNetwork implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "Path to input network", required = true)
	private Path input;

	@CommandLine.Option(names = "--transit-schedule", description = "Path to input transit schedule", required = true)
	private Path transitSchedule;

	@CommandLine.Option(names = "--output", description = "Desired output path", required = true)
	private Path output;

	@CommandLine.Option(names = "--mode", description = "Remap existing modes in the network", required = false, split = ",")
	private Map<String, String> remapModes;

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

		if (!remapModes.isEmpty()) {
			for (Link link : scenario.getNetwork().getLinks().values()) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				remapModes.forEach((oldMode, newMode) -> {
					if (modes.contains(oldMode)) {
						modes.remove(oldMode);
						modes.add(newMode);
					}
				});
				link.setAllowedModes(modes);
			}
		}

		NetworkUtils.writeNetwork(scenario.getNetwork(), output.toString());

		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(outputTransit.toString());


		return 0;
	}
}
