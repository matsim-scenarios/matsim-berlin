package org.matsim.prepare.network;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

@CommandLine.Command(
	name = "network-params", description = "Apply network parameters for capacity and speed."
)
@CommandSpec(
	requireNetwork = true,
	requires = "features.csv",
	produces = "network.xml.gz"
)
public class PrepareNetworkParams implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PrepareNetworkParams.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PrepareNetworkParams.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PrepareNetworkParams.class);

	private int warn = 0;

	public static void main(String[] args) {
		new PrepareNetworkParams().execute(args);
	}

	/**
	 * Theoretical capacity.
	 */
	private static double capacityEstimate(double v) {

		// headway
		double tT = 1.2;

		// car length
		double lL = 7.0;

		double Qc = v / (v * tT + lL);

		return 3600 * Qc;
	}

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();

		Map<Id<Link>, Object2DoubleMap<String>> features = new IdMap<>(Link.class, network.getLinks().size());
		Map<Id<Link>, String> types = new IdMap<>(Link.class, network.getLinks().size());

		try (CSVParser reader = new CSVParser(IOUtils.getBufferedReader(input.getPath("features.csv")),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

			List<String> header = reader.getHeaderNames();

			for (CSVRecord row : reader) {

				Id<Link> id = Id.createLinkId(row.get("edgeId"));

				Object2DoubleOpenHashMap<String> ft = new Object2DoubleOpenHashMap<>();
				ft.defaultReturnValue(Double.NaN);

				for (String column : header) {
					String v = row.get(column);
					try {
						ft.put(column, Double.parseDouble(v));
					} catch (NumberFormatException e) {
						// every not equal to True will be false
						ft.put(column, Boolean.parseBoolean(v) ? 1 : 0);
					}
				}

				features.put(id, ft);
				types.put(id, row.get("junctionType"));
			}
		}

		for (Link link : network.getLinks().values()) {
			applyChanges(link, types.get(link.getId()), features.get(link.getId()));
		}

		log.warn("Observed {} warnings out of {} links", warn, network.getLinks().size());

		NetworkUtils.writeNetwork(network, output.getPath("network.xml.gz").toString());

		return 0;
	}

	/**
	 * Apply speed and capacity models and apply changes.
	 */
	private void applyChanges(Link link, String junctionType, Object2DoubleMap<String> features) {

		String type = NetworkUtils.getHighwayType(link);

		FeatureRegressor capacity = switch (junctionType) {
			case "traffic_light" -> new Capacity_traffic_light();
			case "right_before_left" -> new Capacity_right_before_left();
			case "priority" -> new Capacity_priority();
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};

		double perLane = capacity.predict(features);

		double cap = capacityEstimate(features.getDouble("speed"));

		boolean modified = false;

		// Minimum thresholds
		double threshold = switch (junctionType) {
			// traffic light can reduce capacity at least to 50% (with equal green split)
			case "traffic_light" -> 0.4;
			case "right_before_left" -> 0.6;
			// Motorways are kept at their max theoretical capacity
			case "priority" -> type.startsWith("motorway") ? 1 : 0.8;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};

		if (perLane < cap * threshold) {
			log.warn("Increasing capacity per lane on {} ({}, {}) from {} to {}", link.getId(), type, junctionType, perLane, cap * threshold);
			perLane = cap * threshold;
			modified = true;
		}

		link.setCapacity(link.getNumberOfLanes() * perLane);

		double speedFactor = 1.0;

		if (!type.startsWith("motorway")) {

			FeatureRegressor speedModel = switch (junctionType) {
				case "traffic_light" -> new Speedrelative_traffic_light();
				case "right_before_left" -> new Speedrelative_right_before_left();
				case "priority" -> new Speedrelative_priority();
				default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
			};

			speedFactor = speedModel.predict(features);

			if (speedFactor > 1) {
				log.warn("Reducing speed factor on {} from {} to 1", link.getId(), speedFactor);
				speedFactor = 1;
				modified = true;
			}

			// Threshold for very low speed factors
			if (speedFactor < 0.25) {
				log.warn("Increasing speed factor on {} from {} to 0.25", link, speedFactor);
				speedFactor = 0.25;
				modified = true;
			}
		}

		if (modified)
			warn++;

		link.setFreespeed((double) link.getAttributes().getAttribute("allowed_speed") * speedFactor);
		link.getAttributes().putAttribute("speed_factor", speedFactor);
	}

}
