package org.matsim.analysis;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(name = "link-speeds", description = "Export link speeds by link type")
@CommandSpec(
	requireRunDirectory = true,
	group = "network"
)
public class LinkSpeedAnalysis implements MATSimAppCommand {

	private static final String[] LINK_COLUMNS = {
		"linkId", "freespeed_m_s", "freespeed_km_h", "allowed_speed_m_s", "allowed_speed_km_h"
	};

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(LinkSpeedAnalysis.class);

//	@CommandLine.Mixin
//	private final OutputOptions output = OutputOptions.ofCommand(LinkSpeedAnalysis.class);

	public static void main(String[] args) {
		new LinkSpeedAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Network network = NetworkUtils.readNetwork(String.valueOf(ApplicationUtils.globFile(input.getRunDirectory(), "*output_network*")));
		Map<String, List<LinkSpeed>> byType = new TreeMap<>();

		for (Link link : network.getLinks().values()) {
			String id = link.getId().toString();
			if (id.startsWith("pt_"))
				continue;

			String type = NetworkUtils.getHighwayType(link);
			Double allowedSpeed = getAllowedSpeed(link);
			byType.computeIfAbsent(type, ignored -> new ArrayList<>())
				.add(new LinkSpeed(id, link.getFreespeed(), allowedSpeed));
		}

		for (Map.Entry<String, List<LinkSpeed>> entry : byType.entrySet()) {
			String fileName = "link_speeds_by_type_" + safeFileName(entry.getKey()) + ".csv";
			writeTypeFile(input.getRunDirectory().resolve(fileName), entry.getValue());
		}

		writeSummary(input.getRunDirectory().resolve("link_speed_summary.csv"), byType);
		return 0;
	}

	private static Double getAllowedSpeed(Link link) {
		Object value = link.getAttributes().getAttribute("allowed_speed");
		if (value instanceof Number number)
			return number.doubleValue();
		if (value instanceof String string) {
			try {
				return Double.parseDouble(string);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private static void writeTypeFile(Path path, List<LinkSpeed> links) throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writeRow(writer, LINK_COLUMNS);
			for (LinkSpeed link : links) {
				writeRow(writer, link.id, number(link.freeSpeed), number(link.freeSpeed * 3.6),
					link.allowedSpeed == null ? "" : number(link.allowedSpeed),
					link.allowedSpeed == null ? "" : number(link.allowedSpeed * 3.6));
			}
		}
	}

	private static void writeSummary(Path path, Map<String, List<LinkSpeed>> byType) throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writeRow(writer, "link_type", "mean_freespeed_m_s", "mean_freespeed_km_h",
				"mean_allowed_speed_m_s", "mean_allowed_speed_km_h");
			for (Map.Entry<String, List<LinkSpeed>> entry : byType.entrySet()) {
				List<LinkSpeed> links = entry.getValue();
				double meanFreeSpeed = links.stream().mapToDouble(LinkSpeed::freeSpeed).average().orElse(Double.NaN);
				double meanAllowedSpeed = links.stream().filter(link -> link.allowedSpeed != null)
					.mapToDouble(link -> link.allowedSpeed).average().orElse(Double.NaN);
				writeRow(writer, entry.getKey(), number(meanFreeSpeed), number(meanFreeSpeed * 3.6),
					number(meanAllowedSpeed), number(meanAllowedSpeed * 3.6));
			}
		}
	}

	private static String safeFileName(String value) {
		String safe = value.replaceAll("[^A-Za-z0-9._-]", "_");
		return safe.isBlank() ? "unknown" : safe;
	}

	private static String number(double value) {
		return Double.isFinite(value) ? String.format(Locale.ROOT, "%.2f", value) : "";
	}

	private static void writeRow(BufferedWriter writer, String... values) throws Exception {
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				writer.write(',');
			String value = values[i] == null ? "" : values[i];
			if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
				writer.write('"');
				writer.write(value.replace("\"", "\"\""));
				writer.write('"');
			} else {
				writer.write(value);
			}
		}
		writer.newLine();
	}

	private record LinkSpeed(String id, double freeSpeed, Double allowedSpeed) {
	}
}
