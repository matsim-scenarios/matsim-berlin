package org.matsim.prepare.network;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
import org.matsim.application.options.OutputOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
	name = "network-freespeed",
	description = "Start server for freespeed optimization."
)
@CommandSpec(
	requireNetwork = true,
	requires = {"routes-validation.csv"}
)
public class FreeSpeedOptimizer implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(FreeSpeedOptimizer.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(FreeSpeedOptimizer.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(FreeSpeedOptimizer.class);

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
		validationSet = readValidation();
		mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		for (Link link : network.getLinks().values()) {
			speeds.put(link.getId(), link.getFreespeed());
		}

		log.info("Initial score:");
		DoubleDoublePair init = evaluateNetwork(null);

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

	private DoubleDoublePair evaluateNetwork(Request request) {

		if (request != null)
			for (Link link : network.getLinks().values()) {

				double allowedSpeed = NetworkUtils.getAllowedSpeed(link);
				double speed = speeds.getDouble(link.getId());

				if (allowedSpeed <= 51 / 3.6)
					link.setFreespeed(speed * request.b50);
				else if (allowedSpeed <= 91 / 3.6)
					link.setFreespeed(speed * request.b90);
			}

		FreeSpeedTravelTime tt = new FreeSpeedTravelTime();
		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);
		LeastCostPathCalculator router = new FastDijkstraFactory(false).createPathCalculator(network, util, tt);

		SummaryStatistics rmse = new SummaryStatistics();
		SummaryStatistics mse = new SummaryStatistics();

		for (Object2DoubleMap.Entry<Entry> e : validationSet.object2DoubleEntrySet()) {

			Entry r = e.getKey();

			LeastCostPathCalculator.Path path = router.calcLeastCostPath(network.getNodes().get(r.fromNode), network.getNodes().get(r.toNode), 0, null, null);

			double distance = path.links.stream().mapToDouble(Link::getLength).sum();
			double speed = distance / path.travelTime;

			rmse.addValue(Math.pow(e.getDoubleValue() - speed, 2));
			mse.addValue(Math.abs((e.getDoubleValue() - speed) * 3.6));
		}


		log.info("{}, rmse: {}, mse: {}", request, rmse.getMean(), mse.getMean());

		return DoubleDoublePair.of(rmse.getMean(), mse.getMean());
	}

	/**
	 * Collect highest observed speed.
	 */
	private Object2DoubleMap<Entry> readValidation() throws IOException {

		Object2DoubleMap<Entry> result = new Object2DoubleOpenHashMap<>();

		try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(input.getPath("routes-validation.csv"))),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

			for (CSVRecord r : parser) {
				Entry e = new Entry(Id.createNodeId(r.get("from_node")), Id.createNodeId(r.get("to_node")));
				result.mergeDouble(e, Double.parseDouble(r.get("dist")) / Double.parseDouble(r.get("travel_time")), Double::min);
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

		double b50;
		double b90;

		@Override
		public String toString() {
			return "Request{" +
				"b50=" + b50 +
				", b90=" + b90 +
				'}';
		}
	}

	private final class Backend extends HttpServlet {

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

			Request request = mapper.readValue(req.getInputStream(), Request.class);

			DoubleDoublePair stats = evaluateNetwork(request);

			resp.setStatus(200);

			PrintWriter writer = resp.getWriter();

			// rmse
			writer.println(stats.leftDouble());

			writer.close();
		}
	}

}
