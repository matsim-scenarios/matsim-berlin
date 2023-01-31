package org.matsim.synthetic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import picocli.CommandLine;

@CommandLine.Command(
		name = "brandenburg-population",
		description = "Create synthetic population for berlin."
)
public class CreateBrandenburgPopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateBerlinPopulation.class);

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@Override
	public Integer call() throws Exception {
		return null;
	}
}
