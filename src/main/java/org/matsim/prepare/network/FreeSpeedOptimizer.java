package org.matsim.prepare.network;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

@CommandLine.Command(
	name = "network-freespeed",
	description = "Start server for freespeed optimization."
)
@CommandSpec(
	requireNetwork = true
)
public class FreeSpeedOptimizer implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(FreeSpeedOptimizer.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(FreeSpeedOptimizer.class);

	@CommandLine.Option(names = "--output", description = "Path to output network")
	private Path output;

	@CommandLine.Option(names = "--params", description = "Apply params and write to output if given")
	private Path params;

	@CommandLine.Parameters(arity = "0..*", description = "Input validation files loaded from APIs")
	private List<String> validationFiles;

	private Network network;
	private Object2DoubleMap<Entry> validationSet;

	private ObjectMapper mapper;

	/**
	 * Original speeds.
	 */
	private Object2DoubleMap<Id<Link>> speeds = new Object2DoubleOpenHashMap<>();

	public static void main(String[] args) {
		new FreeSpeedOptimizer().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		network = input.getNetwork();
		mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

		for (Link link : network.getLinks().values()) {
			speeds.put(link.getId(), link.getFreespeed());
		}

		validationSet = readValidation();

		log.info("Initial score:");
		evaluateNetwork(null, "init");

		evaluateNetwork(new Request(0.5), "05");
		evaluateNetwork(new Request(0.75), "075");
		evaluateNetwork(new Request(0.9), "09");

		if (output != null && params != null) {
			Request p = mapper.readValue(params.toFile(), Request.class);
			evaluateNetwork(p, null);
			NetworkUtils.writeNetwork(network, output.toString());

			return 0;
		}

		Server server = new Server(9090);

		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);

		handler.addServletWithMapping(new ServletHolder(new Backend()), "/");

		try {
			server.start();
			server.join();
		} finally {
			server.destroy();
		}

		return 0;
	}

	private DoubleDoublePair evaluateNetwork(Request request, String save) throws IOException {

		if (request != null) {
			for (Link link : network.getLinks().values()) {

				double allowedSpeed = NetworkUtils.getAllowedSpeed(link);
				double speed = speeds.getDouble(link.getId());

				if (request.f == 0) {
					double speedFactor = (double) link.getAttributes().getAttribute("speed_factor");

					if (allowedSpeed <= 31 / 3.6) {
						link.setFreespeed(speed * request.b30);
						link.getAttributes().putAttribute("speed_factor", speedFactor * request.b30);

					} else if (allowedSpeed <= 51 / 3.6) {
						link.setFreespeed(speed * request.b50);
						link.getAttributes().putAttribute("speed_factor", speedFactor * request.b50);
					} else if (allowedSpeed <= 91 / 3.6) {
						link.setFreespeed(speed * request.b90);
						link.getAttributes().putAttribute("speed_factor", speedFactor * request.b90);
					}


				} else
					// Old MATSim freespeed logic
					link.setFreespeed(LinkProperties.calculateSpeedIfSpeedTag(allowedSpeed, request.f));
			}

			if (save != null)
				mapper.writeValue(new File(save + "-params.json"), request);
		}

		FreeSpeedTravelTime tt = new FreeSpeedTravelTime();
		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);
		LeastCostPathCalculator router = new FastDijkstraFactory(false).createPathCalculator(network, util, tt);

		SummaryStatistics rmse = new SummaryStatistics();
		SummaryStatistics mse = new SummaryStatistics();

		CSVPrinter csv = save != null ? new CSVPrinter(Files.newBufferedWriter(Path.of(save + "-eval.csv")), CSVFormat.DEFAULT) : null;

		if (csv != null)
			csv.printRecord("from_node", "to_node", "beeline_dist", "dist", "travel_time");

		for (Object2DoubleMap.Entry<Entry> e : validationSet.object2DoubleEntrySet()) {

			Entry r = e.getKey();

			Node fromNode = network.getNodes().get(r.fromNode);
			Node toNode = network.getNodes().get(r.toNode);
			LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, 0, null, null);

			double distance = path.links.stream().mapToDouble(Link::getLength).sum();
			double speed = distance / path.travelTime;

			rmse.addValue(Math.pow(e.getDoubleValue() - speed, 2));
			mse.addValue(Math.abs((e.getDoubleValue() - speed) * 3.6));

			if (csv != null)
				csv.printRecord(r.fromNode, r.toNode, (int) CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()),
					(int) distance, (int) path.travelTime);
		}

		if (csv != null)
			csv.close();

		log.info("{}, rmse: {}, mse: {}", request, rmse.getMean(), mse.getMean());

		return DoubleDoublePair.of(rmse.getMean(), mse.getMean());
	}

	/**
	 * Collect highest observed speed.
	 */
	private Object2DoubleMap<Entry> readValidation() throws IOException {

		// entry to hour and list of speeds
		Map<Entry, Int2ObjectMap<DoubleList>> entries = new LinkedHashMap<>();

		if (validationFiles != null)
			for (String file : validationFiles) {

				log.info("Loading {}", file);

				try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(file)),
					CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

					for (CSVRecord r : parser) {
						Entry e = new Entry(Id.createNodeId(r.get("from_node")), Id.createNodeId(r.get("to_node")));
						double speed = Double.parseDouble(r.get("dist")) / Double.parseDouble(r.get("travel_time"));

						if (!Double.isFinite(speed)) {
							log.warn("Invalid entry {}", r);
							continue;
						}

						Int2ObjectMap<DoubleList> perHour = entries.computeIfAbsent(e, (k) -> new Int2ObjectLinkedOpenHashMap<>());
						perHour.computeIfAbsent(Integer.parseInt(r.get("hour")), k -> new DoubleArrayList()).add(speed);
					}
				}
			}

		Object2DoubleMap<Entry> result = new Object2DoubleOpenHashMap<>();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Path.of("routes-ref.csv")), CSVFormat.DEFAULT)) {

			printer.printRecord("from_node", "to_node", "hour", "min", "max", "mean", "std");

			// Target values
			for (Map.Entry<Entry, Int2ObjectMap<DoubleList>> e : entries.entrySet()) {

				Int2ObjectMap<DoubleList> perHour = e.getValue();

				// Use avg from all values for 3:00 and 21:00
				double avg = DoubleStream.concat(perHour.get(3).doubleStream(), perHour.get(21).doubleStream())
					.average().orElseThrow();


				for (Int2ObjectMap.Entry<DoubleList> e2 : perHour.int2ObjectEntrySet()) {

					SummaryStatistics stats = new SummaryStatistics();
					// This is as kmh
					e2.getValue().forEach(v -> stats.addValue(v * 3.6));

					printer.printRecord(e.getKey().fromNode, e.getKey().toNode, e2.getIntKey(),
						stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
				}

				result.put(e.getKey(), avg);
			}
		}

		return result;
	}

	private record Entry(Id<Node> fromNode, Id<Node> toNode) {
	}

	/**
	 * JSON request containing desired parameters.
	 */
	private static final class Request {

		double b30;
		double b50;
		double b90;

		double f;

		public Request() {
		}

		public Request(double f) {
			this.f = f;
		}

		@Override
		public String toString() {
			if (f == 0)
				return "Request{" +
					"b30=" + b30 +
					", b50=" + b50 +
					", b90=" + b90 +
					'}';

			return "Request{f=" + f + "}";
		}
	}

	private final class Backend extends HttpServlet {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

			Request request = mapper.readValue(req.getInputStream(), Request.class);

			boolean save = req.getRequestURI().equals("/save");
			DoubleDoublePair stats = evaluateNetwork(request, save ? "network-opt" : null);

			if (save)
				NetworkUtils.writeNetwork(network, "network-opt.xml.gz");

			resp.setStatus(200);

			PrintWriter writer = resp.getWriter();

			// target value
			writer.println(stats.rightDouble());

			writer.close();
		}
	}

}
