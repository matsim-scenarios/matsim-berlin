package org.matsim.dashboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperListener;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@CommandLine.Command(
	name = "simwrapper",
	description = "Run SimWrapper on existing folders and generate dashboard files."
)
public class BerlinSimWrapperRunner implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(BerlinSimWrapperRunner.class);

	@CommandLine.Parameters( description = "Path to folder for which dashboards should be generated.")
	private Path inputPath;

	@CommandLine.Option(names = "--config", description = "Path to MATSim config that should be used. If not given tries to use output config.", required = false)
	private String configPath;

	@CommandLine.Option(names = "--exclude", split = ",", description = "Exclusion that will be added to the config.")
	private Set<String> exclude;

	@CommandLine.Option(names = "--include", split = ",", description = "Use only the dashboards which classnames match.")
	private Set<String> include;

	public static void main(String[] args) {
		new BerlinSimWrapperRunner().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		log.info("Running on {}, writing to {}", inputPath);

		Config config;
		if (configPath != null)
			config = ConfigUtils.loadConfig(configPath);
		else {

			try {
				Path path = ApplicationUtils.matchInput("config.xml", inputPath);
				config = ConfigUtils.loadConfig(path.toString());
			} catch (IllegalArgumentException e) {
				log.warn("No output config found in {}, and no config given via --config", inputPath);
				return 1;
			}
		}

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		if (exclude != null)
			simWrapperConfigGroup.setExclude(exclude);

		if (include != null)
			simWrapperConfigGroup.setInclude(include);

		SimWrapperListener listener = new SimWrapperListener(SimWrapper.create(config), config);
		try {
			listener.run(inputPath);
		} catch (IOException e) {
			log.error("Error creating dashboards on {}", inputPath, e);
		}

		return 0;
	}

}
