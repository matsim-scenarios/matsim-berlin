package org.matsim.prepare;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "sample-network",
	description = "Sample nodes and junctions ids from network"
)
@CommandSpec(requireNetwork = true, produces = {"intersections.txt", "links.txt", "routes.txt"})
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

	/**
	 * Coordinate as string.
	 */
	private static String toString(Coordinate c) {
		return BigDecimal.valueOf(c.x).setScale(2, RoundingMode.HALF_UP) + "," + BigDecimal.valueOf(c.y).setScale(2, RoundingMode.HALF_UP);
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

		Network cityNetwork = createCityNetwork(network);

		RandomizedTravelTime tt = new RandomizedTravelTime(rnd);

		LeastCostPathCalculator router = createRandomizedRouter(network, tt);

		sampleCityRoutes(cityNetwork, router, tt, rnd);

		return 0;
	}

	/**
	 * Samples routes from the network.
	 */
	private void sampleCityRoutes(Network network, LeastCostPathCalculator router, RandomizedTravelTime tt, SplittableRandom rnd) throws IOException {

		List<? extends Link> links = new ArrayList<>(network.getLinks().values());

		GeometryFactory f = new GeometryFactory();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.getPath("routes.txt")), CSVFormat.DEFAULT)) {

			csv.printRecord("fromEdge", "toEdge", "min_capacity", "travel_time", "geometry");

			for (int i = 0; i < sample; i++) {

				Link link = links.get(rnd.nextInt(0, links.size()));

				Coord dest = InitLocationChoice.rndCoord(rnd, 5000, link.getCoord());

				Link to = NetworkUtils.getNearestLink(network, dest);

				LeastCostPathCalculator.Path path = router.calcLeastCostPath(link.getFromNode(), to.getToNode(), 0, null, null);

				if (path.nodes.size() < 2) {
					i--;
					continue;
				}

				double minCapacity = path.links.stream().mapToDouble(Link::getCapacity).min().orElse(-1);

				LineString lineString = f.createLineString(path.nodes.stream().map(n -> MGC.coord2Point(n.getCoord()).getCoordinate()).toArray(Coordinate[]::new));

				Polygon polygon = (Polygon) lineString.buffer(60);
				// min capacity along the route

				Polygon simplified = (Polygon) TopologyPreservingSimplifier.simplify(polygon, 25);

				csv.print(link.getId());
				csv.print(to.getId());
				csv.print(minCapacity);
				csv.print(path.travelTime);
				csv.print(
					Arrays.stream(simplified.getCoordinates()).map(SampleNetwork::toString).collect(Collectors.joining(","))
				);

				csv.println();

				// Reset randomness
				tt.reset();
			}

		}
	}

	/**
	 * Create network without highways.
	 */
	private Network createCityNetwork(Network network) {

		NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
		filter.addLinkFilter(l -> !NetworkUtils.getHighwayType(l).startsWith("primary"));

		Network net = filter.applyFilters();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(net);
		cleaner.run(Set.of("car"));

		return net;
	}

	/**
	 * Router with randomization.
	 */
	private LeastCostPathCalculator createRandomizedRouter(Network network, TravelTime tt) {

		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);
		return new FastDijkstraFactory(false).createPathCalculator(network, util, tt);
	}

	private static final class RandomizedTravelTime implements TravelTime {

		private final FreeSpeedTravelTime tt = new FreeSpeedTravelTime();

		private final Object2DoubleMap<Link> factors = new Object2DoubleOpenHashMap<>();

		private final SplittableRandom rnd;

		public RandomizedTravelTime(SplittableRandom rnd) {
			this.rnd = rnd;
		}

		void reset() {
			factors.clear();
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return tt.getLinkTravelTime(link, time, person, vehicle) * factors.computeIfAbsent(link, l -> rnd.nextDouble(0.8, 1.2));
		}
	}

}
