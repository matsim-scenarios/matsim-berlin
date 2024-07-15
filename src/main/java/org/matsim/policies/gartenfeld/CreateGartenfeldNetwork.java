package org.matsim.policies.gartenfeld;

import org.matsim.application.MATSimAppCommand;
import org.matsim.prepare.network.ModifyNetwork;
import picocli.CommandLine;

@CommandLine.Command(name = "create-gartenfeld-network", description = "Create the network for the Gartenfeld scenario.")
public class CreateGartenfeldNetwork implements MATSimAppCommand {

	public static void main(String[] args) {
		new CreateGartenfeldNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		new ModifyNetwork().execute(
			"--network", "input/v6.3/berlin-v6.3-network-with-pt.xml.gz",
			"--remove-links", "input/gartenfeld/DNG_LinksToDelete.txt",
			"--shp", "input/gartenfeld/DNG_network.gpkg",
			"--output", "input/gartenfeld/gartenfeld-network.xml.gz"
		);

		return 0;
	}

}
