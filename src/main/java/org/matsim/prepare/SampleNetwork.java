package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "sample-network",
	description = "Sample nodes and junctions ids from network"
)
@CommandSpec(requireNetwork = true, produces = {"intersections.txt", "links.txt"})
public class SampleNetwork implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(SampleNetwork.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(SampleNetwork.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(SampleNetwork.class);

	@CommandLine.Option(names = "--sample-size", description = "Number of samples to collect for each category.", defaultValue = "2000")
	private int sample;

	public static void main(String[] args) {
		new SampleNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();

		Map<String, ? extends List<? extends Node>> byType = network.getNodes().values().stream().collect(Collectors.groupingBy(
			n -> (String) n.getAttributes().getAttribute("type"), Collectors.toList()
		));

		SplittableRandom rnd = new SplittableRandom(0);

		try (BufferedWriter intersections = Files.newBufferedWriter(output.getPath("intersections.txt"))) {

			for (Map.Entry<String, ? extends List<? extends Node>> e : byType.entrySet()) {

				List<? extends Node> list = e.getValue();

				log.info("Sampling {} out of {} intersections for type {}", sample, list.size(), e.getKey());

				for (int i = 0; i < sample; i++) {
					Node n = list.remove(rnd.nextInt(0, list.size()));
					intersections.write(n.getId().toString() + "\n");
				}
			}
		}

		Map<Double, ? extends List<? extends Link>> bySpeed = network.getLinks().values().stream().collect(Collectors.groupingBy(
			n -> (Double) n.getAttributes().getAttribute("allowed_speed"), Collectors.toList()
		));

		try (BufferedWriter links = Files.newBufferedWriter(output.getPath("links.txt"))) {

			for (Map.Entry<Double, ? extends List<? extends Link>> e : bySpeed.entrySet()) {

				List<? extends Link> list = e.getValue();

				log.info("Sampling {} out of {} links for speed {}", sample / 10, list.size(), e.getKey());

				// Use longest link segments
				list.sort(Comparator.comparingDouble(l -> -l.getLength()));

				for (int i = 0; i < sample / 10 && i < list.size(); i++) {
					Link link = list.get(i);
					links.write(link.getId().toString() + "\n");
				}
			}
		}

		return 0;
	}
}
