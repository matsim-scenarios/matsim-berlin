package org.matsim.prepare.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@CommandLine.Command(name = "modify-network", description = "Remove or add network elements")
public class ModifyNetwork implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ModifyNetwork.class);

	@CommandLine.Option(names = {"--network"}, description = "Path to the network file", required = true)
	private String networkPath;

	@CommandLine.Option(names = {"--remove-links"}, description = "Path to the CSV file with link ids to remove")
	private Path removeCSV;

	@CommandLine.Option(names = {"--matching-distance"}, description = "Distance in meters to match links from shapefile to network nodes", defaultValue = "10.0")
	private double matchingDistance;

	@CommandLine.Option(names = "--output", description = "Path to the output network file", required = true)
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	private Network network;
	private STRtree nodeIndex;

	public static void main(String[] args) {
		new ModifyNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		network = NetworkUtils.readNetwork(networkPath);

		if (removeCSV != null) {
			removeLinks(network);
		}

		if (shp.isDefined()) {
			addLinksFromShape();
		}

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(Set.of(TransportMode.car));
		cleaner.run(Set.of(TransportMode.bike));

		NetworkUtils.writeNetwork(network, output.toString());

		return 0;
	}

	private void removeLinks(Network network) throws IOException {

		int removed = 0;

		for (String line : Files.readAllLines(removeCSV)) {
			Id<Link> linkId = Id.createLinkId(line);
			network.removeLink(linkId);
			removed++;
		}

		// Remove links that are not connected
		List<Id<Node>> toRemove = network.getNodes().values().stream().filter(n -> n.getInLinks().isEmpty() && n.getOutLinks().isEmpty())
			.map(Node::getId)
			.toList();

		log.info("Removed %d links and %d nodes".formatted(removed, toRemove.size()));

		toRemove.forEach(network::removeNode);
	}

	/**
	 * Read line geometry from a shapefile and add links to the network.
	 */
	private void addLinksFromShape() {

		nodeIndex = createIndex(network);

		int added = 0;
		List<SimpleFeature> features = shp.readFeatures();
		for (SimpleFeature feature : features) {
			// Find the nearest node
			LineString geom = (LineString) feature.getDefaultGeometry();

			String linkId = feature.getID();
			Node fromNode = matchNode(nodeIndex, geom.getCoordinateN(0), "from_" + linkId);
			Node toNode = matchNode(nodeIndex, geom.getCoordinateN(geom.getNumPoints() - 1), "to_" + linkId);

			if (fromNode == toNode) {
				throw new IllegalArgumentException("Link %s would have the same start and end node. Check the shapefile and improve matching to nodes.".formatted(linkId));
			}

			addLink(linkId, feature, fromNode, toNode);
			if (feature.getAttribute("is_oneway") != Boolean.TRUE) {
				addLink(linkId + "_r", feature, toNode, fromNode);
			}

			added++;
		}

		log.info("Added %d links".formatted(added));
	}

	private STRtree createIndex(Network network) {
		STRtree index = new STRtree();
		for (Node node : network.getNodes().values()) {
			index.insert(MGC.coord2Point(node.getCoord()).getEnvelopeInternal(), node);
		}
		index.build();
		return index;
	}

	@SuppressWarnings("unchecked")
	private Node matchNode(STRtree index, Coordinate coord, String nodeId) {

		Point point = MGC.coordinate2Point(coord);

		ToDoubleFunction<Object> distance = n -> NetworkUtils.getEuclideanDistance(((Node) n).getCoord(), MGC.coordinate2Coord(coord));

		List<Node> query = index.query(point.buffer(matchingDistance * 2).getEnvelopeInternal());

		List<Node> result = query
			.stream()
			.map(Node.class::cast)
			.filter(n -> distance.applyAsDouble(n) < matchingDistance)
			.sorted(Comparator.comparingDouble(distance))
			.toList();

		if (result.isEmpty()) {

			Id<Node> id = Id.createNodeId(nodeId);
			Node node = network.getFactory().createNode(id, MGC.coordinate2Coord(coord));
			network.addNode(node);
			nodeIndex = createIndex(network);

			return node;
		}

		return result.getFirst();
	}

	private void addLink(String id, SimpleFeature feature, Node fromNode, Node toNode) {

		Id<Link> linkId = Id.createLinkId(id);
		Link link = network.getFactory().createLink(linkId, fromNode, toNode);

		String modes = (String) feature.getAttribute("Allowed Transport Mode");
		if (modes != null) {
			String[] m = modes.split(",");
			link.setAllowedModes(Arrays.stream(m).collect(Collectors.toSet()));
		} else {
			link.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike, TransportMode.truck, "freight"));
		}

		Object freespeed = feature.getAttribute("Free Speed");
		if (freespeed != null)
			link.setFreespeed((Double) freespeed);
		else
			link.setFreespeed(30.0 / 3.6);

		Object allowedSpeed = feature.getAttribute("allowed_speed");
		if (allowedSpeed != null)
			link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);

		Object capacity = feature.getAttribute("Capacity");
		if (capacity != null)
			link.setCapacity((Double) capacity);
		else
			link.setFreespeed(800);

		Object numberOfLanes = feature.getAttribute("Number of Lanes");
		if (numberOfLanes != null)
			link.setNumberOfLanes((Double) numberOfLanes);
		else
			link.setNumberOfLanes(1);

		network.addLink(link);
	}

}
