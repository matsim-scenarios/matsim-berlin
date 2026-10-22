package org.matsim.prepare.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.CorePluginLoader;
import org.openstreetmap.osmosis.core.TaskRegistrar;
import org.openstreetmap.osmosis.core.cli.CommandLineParser;
import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;

import java.nio.file.Path;

/**
 * Runs osmosis pipelines in-process, against the osmosis libraries from the pom, so that the OSM steps of
 * the scenario pipeline do not need an osmosis installation on the PATH.
 * <p>
 * The pipelines are given as osmosis' own command line, so that a stage can be compared to the osmosis
 * invocation it replaces, and to the osmosis documentation, one to one.
 */
final class OsmosisPipeline {

	private static final Logger log = LogManager.getLogger(OsmosisPipeline.class);

	private OsmosisPipeline() {
	}

	/**
	 * Runs one pipeline to completion, as {@code org.openstreetmap.osmosis.core.Osmosis#run(String[])} does.
	 * <p>
	 * The task factories are registered by hand instead of through {@link TaskRegistrar#initialize(java.util.List)},
	 * which would read every {@code osmosis-plugins.conf} on the classpath and then go through the JPF plugin
	 * framework and the osmosis plugin directories. Those conf files all sit at the jar root and would collide in
	 * the shaded jar, and a stray plugin directory next to the pipeline must not be able to change what the tasks
	 * do. The class names here are the ones those conf files list.
	 */
	static void run(String... args) {
		TaskRegistrar registrar = registrar();

		CommandLineParser parser = new CommandLineParser();
		parser.parse(args);

		log.info("Running osmosis {}", String.join(" ", args));

		// a merge is fed by two readers at once, so the pipeline has to run them in parallel
		Pipeline pipeline = new Pipeline(registrar.getFactoryRegister());
		pipeline.prepare(parser.getTaskInfoList());
		pipeline.execute();
		pipeline.waitForCompletion();
	}

	/**
	 * The osmosis tasks this package uses. Package private so that a test can check they all resolve, which is
	 * where a missing or wrongly scoped osmosis module shows up.
	 */
	static TaskRegistrar registrar() {
		TaskRegistrar registrar = new TaskRegistrar();
		registrar.loadPlugin(new CorePluginLoader());
		registrar.loadPlugin(new crosby.binary.osmosis.BinaryPluginLoader());
		registrar.loadPlugin(new org.openstreetmap.osmosis.xml.XmlPluginLoader());
		registrar.loadPlugin(new org.openstreetmap.osmosis.tagfilter.TagFilterPluginLoader());
		registrar.loadPlugin(new org.openstreetmap.osmosis.areafilter.AreaFilterPluginLoader());
		registrar.loadPlugin(new org.openstreetmap.osmosis.set.SetPluginLoader());
		registrar.loadPlugin(new org.openstreetmap.osmosis.tagtransform.TransformPlugin());
		return registrar;
	}

	/**
	 * The reader task for a file, picked from its extension, so that a stage does not need a format option.
	 */
	static String readerFor(Path file) {
		return isXml(file) ? "--rx" : "--rb";
	}

	/**
	 * The writer task for a file, picked from its extension. See {@link #readerFor(Path)}.
	 */
	static String writerFor(Path file) {
		return isXml(file) ? "--wx" : "--wb";
	}

	private static boolean isXml(Path file) {
		String name = file.getFileName().toString();
		return name.endsWith(".osm") || name.endsWith(".osm.gz") || name.endsWith(".osm.bz2") || name.endsWith(".xml");
	}
}
