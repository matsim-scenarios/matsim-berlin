package org.matsim.prepare.berlinCounts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOption;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@CommandLine.Command(name = "create-counts", description = "creates MATSim counts from FIS Broker count data")
public class CreateCountsFromOpenData implements MATSimAppCommand {

	@CommandLine.Option(names = "--network", description = "Network file path", required = true)
	String networkFilePath;

	@CommandLine.Option(names = "--road-type", description = "road type patterns to filter the network")
	List<String> roadTypes = List.of("motorway", "trunk", "primary");

	@CommandLine.Option(names = "--output", description = "output folder", required = true)
	String output;

	@CommandLine.Mixin
	CountsOption countsOption = new CountsOption();

	@CommandLine.Mixin
	ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	CrsOptions crs = new CrsOptions();

	Counts<Link> car = new Counts<>();
	Counts<Link> freight = new Counts<>();

	Logger logger = LogManager.getLogger(CreateCountsFromOpenData.class);

	public static void main(String[] args) {
		new CreateCountsFromOpenData().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if(!shp.isDefined())
			throw new RuntimeException("Please enter the FIS-Broker shape file path!");

		logger.info("Read shape file.");
		List<SimpleFeature> features = shp.readFeatures();
		List<Predicate<Link>> roadTypeFilter = createRoadTypeFilter(roadTypes);

		logger.info("Read network and apply filters");
		Network filteredNetwork;
		{
			Network network = NetworkUtils.readNetwork(networkFilePath);
			NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
			filter.addLinkFilter(link -> link.getAllowedModes().contains(TransportMode.car));
			filter.addLinkFilter(link -> roadTypeFilter.stream().anyMatch(predicate -> predicate.test(link)));

			filteredNetwork = filter.applyFilters();
		}

		logger.info("Build Index.");
		NetworkIndex index = new NetworkIndex(filteredNetwork, 10);
		MathTransform transformation = getCoordinateTransformation(crs);

		logger.info("Processing simple features.");
		int counter = 0;
		for(SimpleFeature feature: features){

			counter++;
			String id = (String) feature.getAttribute("link_id");
			if(countsOption.getIgnored().contains(id))
				continue;

			if(!(feature.getDefaultGeometry() instanceof MultiLineString ls)) {
				throw new RuntimeException("Geometry #" + counter + " is no LineString. Please check your shape file!.");
			}

			MultiLineString transformed = (MultiLineString) JTS.transform(ls, transformation);

			Link matched = index.query(transformed);
			if(matched == null) {
				logger.warn("Could not match feature {}. Maybe try with a bigger search range?", id);
			} else {
				index.remove(matched);

				long carDTV = (Long) feature.getAttribute("dtvw_kfz");
				long freightDTV = (Long) feature.getAttribute("dtvw_lkw");

				String name = (String) feature.getAttribute("str_name");
				car.createAndAddCount(matched.getId(), name).createVolume(1, carDTV);
				freight.createAndAddCount(matched.getId(), name).createVolume(1, freightDTV);
			}
		}

		logger.info("Write results to {}", output);
		new CountsWriter(car).write(output + "car-counts.xml.gz");
		new CountsWriter(freight).write(output + "freight-counts.xml.gz");

		return 0;
	}

	MathTransform getCoordinateTransformation(CrsOptions crs){
		CoordinateReferenceSystem inputCRS;
		CoordinateReferenceSystem targetCRS;

		MathTransform transformation;

		try {
			inputCRS = CRS.decode(crs.getInputCRS(), true);
			targetCRS = CRS.decode(crs.getTargetCRS(), true);

			transformation = CRS.findMathTransform(inputCRS, targetCRS);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException("Please check the coordinate systems!");
		}

		return transformation;
	}

	private List<Predicate<Link>> createRoadTypeFilter(List<String> types) {

		List<Predicate<Link>> filter = new ArrayList<>();

		for (String type : types) {

			Predicate<Link> p = link -> {
				var attr = link.getAttributes().getAttribute("type");
				if(attr == null)
					return true;

				Pattern pattern = Pattern.compile(type, Pattern.CASE_INSENSITIVE);
				return pattern.matcher(attr.toString()).find();
			};

			filter.add(p);
		}
		return filter;
	}

	private static class NetworkIndex{

		private final STRtree index = new STRtree();
		private final GeometryFactory factory = new GeometryFactory();

		double range;

		public NetworkIndex(Network network, double range){

			this.range = range;

			for (Link link : network.getLinks().values()) {
				Envelope env = getLinkEnvelope(link);
				index.insert(env, link);
			}

			index.build();
		}

		public Link query(MultiLineString ls) {

			Envelope searchArea = ls.buffer(this.range).getEnvelopeInternal();

			List<Link> result = index.query(searchArea);

			if (result.isEmpty()) return null;
			if (result.size() == 1) return result.get(0);

			// Find the closest link matching the direction
			Link closest = result.stream().findFirst().get();

			for (Link l : result) {

				double distance = link2LineString(l).distance(ls);
				double curClosest = link2LineString(closest).distance(ls);

				if (distance < curClosest) closest = l;
			}

			return closest;
		}

		private Envelope getLinkEnvelope(Link link) {
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

			return factory.createLineString(coordinates).getEnvelopeInternal();
		}

		private LineString link2LineString(Link link) {

			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

			return factory.createLineString(coordinates);
		}

		public void remove(Link link) {
			Envelope env = getLinkEnvelope(link);
			index.remove(env, link);
		}
	}
}
