package org.matsim.analysis;

import org.checkerframework.checker.units.qual.N;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class FilterNetworkLinks implements MATSimAppCommand {
	@CommandLine.Option(names = "--input", description = "the input network file", required = true)
	private String input;

	@CommandLine.Option(names = "--output-berlin", description = "the output csv file for berlin links", required = true)
	private String outputBerlin;

	@CommandLine.Option(names = "--output-hundekopf", description = "the output csv file for hundekopf links", required = true)
	private String outputHundekopf;

	@CommandLine.Option(names = "--shp-berlin", description = "Shapefile of Berlin boundary", required = true)
	private String shpBerlin;

	@CommandLine.Option(names = "--shp-hundekopf", description = "Shapefile of hundekop area", defaultValue = "-8.586")
	private String shpHundekopf;


	public static void main(String[] args) {
		new FilterNetworkLinks().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(input);
		ShpOptions shpOptions = new ShpOptions();
		ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpBerlin)));

		writeLinksToCsv(filterOutLinks(network, ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpBerlin)))), outputBerlin);
		writeLinksToCsv(filterOutLinks(network, ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpHundekopf)))), outputHundekopf);


		Set<? extends Link> linksBerlin = filterOutLinks(network, ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpBerlin))));

		Network berlinNetwork = NetworkUtils.createNetwork();

		for (Link link : linksBerlin) {
			berlinNetwork.addNode(link.getFromNode());
			berlinNetwork.addNode(link.getToNode());
			berlinNetwork.addLink(link);
		}
		NetworkUtils.writeNetwork(berlinNetwork, "/Users/gregorr/Documents/work/Paper/heartParking/berlin_links.xml.gz");

		Network hundekopfNetwork = NetworkUtils.createNetwork();
		Set<? extends Link> linksHundekopf = filterOutLinks(network, ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpHundekopf))));
		for (Link link : linksHundekopf) {
			hundekopfNetwork.addNode(link.getFromNode());
			hundekopfNetwork.addNode(link.getToNode());
			hundekopfNetwork.addLink(link);
		}

		NetworkUtils.writeNetwork(hundekopfNetwork, "/Users/gregorr/Documents/work/Paper/heartParking/hundekopf_links.xml.gz");

		return 0;
	}

	/**
	 * Reduce the network to links that are in the bound
	 *
	 * @param network    the network to be modified
	 * @param geometries the geometries defining the area
	 * @return
	 */
	private static Set<? extends Link> filterOutLinks(Network network, List<PreparedGeometry> geometries) {
		Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
			//filter car links
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			//spatial filter
			.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
			.collect(Collectors.toSet());
		return carLinksInArea;
	}

	/**
	 * Write the filtered links to a csv file
	 *
	 * @param links      the links to be written
	 * @param outputPath the path to the output csv file
	 */
	private static void writeLinksToCsv(Set<? extends Link> links, String outputPath) {
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputPath)) {
			writer.write("linkId,fromNodeId,toNodeId,length,freespeed,capacity,numberOfLanes,allowedModes\n");
			for (Link link : links) {
				writer.write(String.format("%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%s\n",
						link.getId(),
						link.getFromNode().getId(),
						link.getToNode().getId(),
						link.getLength(),
						link.getFreespeed(),
						link.getCapacity(),
						link.getNumberOfLanes(),
						String.join("|", link.getAllowedModes())
				));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
