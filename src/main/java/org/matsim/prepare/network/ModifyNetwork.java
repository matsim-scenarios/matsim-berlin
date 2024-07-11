package org.matsim.prepare.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

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

	public static void main(String[] args) {
		new ModifyNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(networkPath);

		if (removeCSV != null) {
			removeLinks(network);
		}

		if (shp.isDefined()) {
			addLinksFromShape(network);
		}

		return 0;
	}

	private void removeLinks(Network network) throws IOException {
		for (String line : Files.readAllLines(removeCSV)) {
			Id<Link> linkId = Id.createLinkId(line);
			network.removeLink(linkId);
		}

		// Remove links that are not connected
		List<Id<Node>> toRemove = network.getNodes().values().stream().filter(n -> n.getInLinks().isEmpty() && n.getOutLinks().isEmpty())
			.map(Node::getId)
			.toList();

		toRemove.forEach(network::removeNode);
	}

	/**
	 * Read line geometry from a shapefile and add links to the network.
	 */
	private void addLinksFromShape(Network network) {

		STRtree nodeIndex = new STRtree();
		for (Node node : network.getNodes().values()) {
			nodeIndex.insert(MGC.coord2Point(node.getCoord()).getEnvelopeInternal(), node);
		}
		nodeIndex.build();

		List<SimpleFeature> features = shp.readFeatures();
		for (SimpleFeature feature : features) {
			// Find the nearest node
			LineString geom = (LineString) feature.getDefaultGeometry();

			Id<Link> linkId = Id.createLinkId(feature.getID());
			Node fromNode = matchNode(nodeIndex, geom.getCoordinateN(0));
			Node toNode = matchNode(nodeIndex, geom.getCoordinateN(geom.getNumPoints() - 1));

			if (fromNode == toNode) {
				throw new IllegalArgumentException("Link %s would have the same start and end node. Check the shapefile and improve matching to zones.".formatted(linkId));
			}

			Link link = network.getFactory().createLink(linkId, fromNode, toNode);

			// TODO: Set link attributes

			network.addLink(link);
		}
	}

	@SuppressWarnings("unchecked")
	private Node matchNode(STRtree index, Coordinate coord) {

		Point point = MGC.coordinate2Point(coord);

		ToDoubleFunction<Object> distance = n -> NetworkUtils.getEuclideanDistance(((Node) n).getCoord(), MGC.coordinate2Coord(coord));

		List<Node> result = index.query(point.buffer(matchingDistance).getEnvelopeInternal())
			.stream()
			.map(Node.class::cast)
			.filter(n -> distance.applyAsDouble(n) < matchingDistance)
			.sorted(Comparator.comparingDouble(distance))
			.toList();

		if (result.isEmpty()) {
			throw new IllegalArgumentException("No node found for coordinate %s".formatted(coord));
		}

		return result.getFirst();
	}

}
