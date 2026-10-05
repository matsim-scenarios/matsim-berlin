package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * Bake the calibrated mode constants into the config: apply the yaml the ASC calibration writes for its best trial,
 * the same way the calibration runs apply it ({@code --yaml}), and write the result as the run config of the
 * version the pipeline produces.
 * <p>
 * The config is written without comments and with only the parameters that differ from their defaults; the
 * template it was generated from (input/run-config-template.xml) is where the comments live. Relative paths are
 * kept as they are, so the output has to stay in the directory of the input config to refer to the same files.
 */
@CommandLine.Command(
	name = "write-run-config",
	description = "Write the run config with the calibrated mode constants baked in."
)
public class WriteRunConfig implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(WriteRunConfig.class);

	@CommandLine.Option(names = "--config", description = "Config the calibration ran, i.e. without the calibrated constants", required = true)
	private Path config;

	@CommandLine.Option(names = "--yaml", description = "Calibrated parameters, as written by calibrate.py", required = true)
	private Path yaml;

	@CommandLine.Option(names = "--run-id", description = "Run id of the run config; the output directory is ./output/<run id>", required = true)
	private String runId;

	@CommandLine.Option(names = "--output", description = "Path to the run config", required = true)
	private Path output;

	@Override
	public Integer call() throws Exception {

		Config cfg = ConfigUtils.loadConfig(config.toString());

		ApplicationUtils.applyConfigUpdate(cfg, yaml);
		log.info("Applied {} to {}", yaml, config);

		cfg.controller().setRunId(runId);
		cfg.controller().setOutputDirectory("./output/" + runId);

		new ConfigWriter(cfg, ConfigWriter.Verbosity.minimal).write(output.toString());
		log.info("Written run config {}", output);

		return 0;
	}
}
