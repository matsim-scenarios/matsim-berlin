package org.matsim.prepare.network;

import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the ways of one network layer and the nodes they use, optionally clipped to an area, and drops
 * everything else. One stage of the OSM filtering the network is built from; the layers are merged by
 * {@link MergeOsm} afterwards.
 * <p>
 * Replaces an {@code osmosis --rb --tf accept-ways ... [--bounding-polygon] --used-node --wb} invocation.
 */
@CommandLine.Command(
	name = "filter-osm-ways",
	description = "Keep the given highway types and the nodes they use, optionally clipped to an area."
)
public class FilterOsmWays implements MATSimAppCommand {

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the input .osm.pbf or .osm file")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to write; .osm is xml, anything else pbf", required = true)
	private Path output;

	@CommandLine.Option(names = "--highways", description = "Comma separated highway tag values to keep", required = true)
	private String highways;

	@CommandLine.Option(names = "--area", description = "Optional .poly file to clip to. Without it the whole input is kept.")
	private Path area;

	@CommandLine.Option(names = "--bicycle", description = "Optionally also keep ways with this bicycle tag value, e.g. designated")
	private String bicycle;

	public static void main(String[] args) {
		new FilterOsmWays().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input))
			throw new IllegalArgumentException("Input file does not exist: " + input);
		if (area != null && !Files.exists(area))
			throw new IllegalArgumentException("Area file does not exist: " + area);

		Files.createDirectories(output.toAbsolutePath().getParent());

		List<String> args = new ArrayList<>(List.of(
			OsmosisPipeline.readerFor(input), "file=" + input,
			"--tf", "accept-ways"
		));

		// the way filter is an or, so a bicycle value keeps those ways in addition to the highway types
		if (bicycle != null)
			args.add("bicycle=" + bicycle);

		args.add("highway=" + highways);

		if (area != null) {
			args.add("--bounding-polygon");
			args.add("file=" + area);
		}

		args.add("--used-node");
		args.add(OsmosisPipeline.writerFor(output));
		args.add("file=" + output);

		OsmosisPipeline.run(args.toArray(String[]::new));

		return 0;
	}
}
