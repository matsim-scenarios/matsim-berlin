package org.matsim.prepare.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.util.Map;

/**
 * This class won't be necesarry with a updated network.
 */
@CommandLine.Command(name = "fix-network-v5", description = "Apply corrects to the v5 network.")
@CommandSpec(
	requireNetwork = true,
	produces = "network.xml.gz"
)
@Deprecated
public class FixNetworkV5 implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(FixNetworkV5.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(FixNetworkV5.class);

	public static void main(String[] args) {
		new FixNetworkV5().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();

		fixLinks(network.getLinks());

		NetworkUtils.writeNetwork(network, output.getPath().toString());

		return 0;
	}

	private void fixLinks(Map<Id<Link>, ? extends Link> links) {

		// Subtract a bus lane manually
		links.get(Id.createLinkId("106347")).setNumberOfLanes(2);
		links.get(Id.createLinkId("138826")).setNumberOfLanes(2);
		links.get(Id.createLinkId("154291")).setNumberOfLanes(2);
		links.get(Id.createLinkId("7875")).setNumberOfLanes(3);

		links.get(Id.createLinkId("106330")).setNumberOfLanes(2);

	}

}
