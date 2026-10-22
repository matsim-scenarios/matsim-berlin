package org.matsim.prepare.network;

import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges OSM files into one and optionally applies a tag transform to the result. The last stage of the
 * OSM filtering the network is built from, after the layers have been cut by {@link FilterOsmWays}.
 * <p>
 * Replaces an {@code osmosis --rb --rb --merge [--tag-transform] --wx} invocation. The inputs have to be
 * sorted by entity type and id, which is what the other stages write.
 */
@CommandLine.Command(
	name = "merge-osm",
	description = "Merge OSM files into one, optionally applying a tag transform."
)
public class MergeOsm implements MATSimAppCommand {

	@CommandLine.Parameters(arity = "2..*", paramLabel = "INPUT", description = "Paths of the .osm.pbf or .osm files to merge")
	private List<Path> inputs;

	@CommandLine.Option(names = "--output", description = "Path to write; .osm is xml, anything else pbf", required = true)
	private Path output;

	@CommandLine.Option(names = "--tag-transform", description = "Optional osmosis translation file applied to the merged data")
	private Path tagTransform;

	public static void main(String[] args) {
		new MergeOsm().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		for (Path input : inputs) {
			if (!Files.exists(input))
				throw new IllegalArgumentException("Input file does not exist: " + input);
		}
		if (tagTransform != null && !Files.exists(tagTransform))
			throw new IllegalArgumentException("Tag transform file does not exist: " + tagTransform);

		Files.createDirectories(output.toAbsolutePath().getParent());

		List<String> args = new ArrayList<>();

		for (Path input : inputs) {
			args.add(OsmosisPipeline.readerFor(input));
			args.add("file=" + input);
		}

		// each merge consumes two sources and produces one, so n inputs need n-1 of them
		for (int i = 1; i < inputs.size(); i++)
			args.add("--merge");

		if (tagTransform != null) {
			args.add("--tag-transform");
			args.add("file=" + tagTransform);
		}

		args.add(OsmosisPipeline.writerFor(output));
		args.add("file=" + output);

		OsmosisPipeline.run(args.toArray(String[]::new));

		return 0;
	}
}
