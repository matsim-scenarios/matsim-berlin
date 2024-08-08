package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CommandLine.Command(name = "reproject-network", description = "Change the CRS of a network")
public class ReprojectNetwork implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "Path to input network", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Desired output path", required = true)
	private Path output;

	@CommandLine.Option(names = "--mode", description = "Remap existing modes in the network", required = false, split = ",")
	private Map<String, String> remapModes;

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

		// Scenario loader does the reprojection for the network
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Node node : scenario.getNetwork().getNodes().values()) {
			node.setCoord(CoordUtils.round(node.getCoord()));
		}

		if (!remapModes.isEmpty()) {
			for (Link link : scenario.getNetwork().getLinks().values()) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());

				// Only add mode without removing
				remapModes.forEach((oldMode, newMode) -> {
					if (modes.contains(oldMode)) {
						modes.add(newMode);
					}
				});
				link.setAllowedModes(modes);
			}
		}

		NetworkUtils.writeNetwork(scenario.getNetwork(), output.toString());

		return 0;
	}
}
