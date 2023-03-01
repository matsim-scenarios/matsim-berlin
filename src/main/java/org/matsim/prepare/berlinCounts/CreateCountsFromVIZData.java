package org.matsim.prepare.berlinCounts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(name = "counts-from-viz", description = "creates MATSim counts from VIZ data.")
public class CreateCountsFromVIZData implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "input excel file", required = true)
	private Path input;

	@CommandLine.Option(names = "--network", description = "network file path", required = true)
	private Path networkFilePath;

	@CommandLine.Option(names = "--search-range", description = "network file path", required = true)
	private double searchRange;

	@CommandLine.Option(names = "--output", description = "output folder", required = true)
	private Path output;

	@CommandLine.Mixin
	CrsOptions crs = new CrsOptions();

	private final Logger logger = LogManager.getLogger(CreateCountsFromVIZData.class);

	private final HashMap<String, Station> stations = new HashMap<>();

	public static void main(String[] args) {
		new CreateCountsFromVIZData().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		logger.info("Read excel data from {}", input.toString());
		XSSFWorkbook wb = new XSSFWorkbook(input.toString());

		extractStations(stations, wb.getSheet("Stammdaten"));
		extractDtv(stations, wb.getSheet("DTVW_KFZ"), wb.getSheet("DTVW_LKW"));

		logger.info("Read and filter network.");
		Network network;
		{
			Network unfiltered = NetworkUtils.readNetwork(networkFilePath.toString());
			NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());
			manager.addLinkFilter(l -> !l.getId().toString().startsWith("pt"));
		//	manager.addLinkFilter(l -> !(l.getAllowedModes().size() == 1) && !l.getAllowedModes().contains(TransportMode.bike));

			network = manager.applyFilters();
		}

		logger.info("Start matching.");
		NetworkIndex<Station> index = new NetworkIndex<>(network, searchRange, o -> {

			Coord coord = ((Station) o).getCoord();
			Coord transform = crs.getTransformation().transform(coord);
			return MGC.coord2Point(transform);
		});

		for(Station station: stations.values())
			match(station, index);

		Counts<Link> car = new Counts<>();
		Counts<Link> freight = new Counts<>();

		logger.info("Map stations to counts");
		mapStationsToCounts(stations, car, freight);

		logger.info("Write output count files into {}", output.toString());
		new CountsWriter(car).write(output.toString() + "\\car-counts.xml");
		new CountsWriter(freight).write(output.toString() + "\\freight-counts.xml");

		return 0;
	}

	private void mapStationsToCounts(HashMap<String, Station> stations, Counts<Link> car, Counts<Link> freight){

		car.setYear(2018);
		freight.setYear(2018);

		car.setDescription("Car counts from closed data provided by the Verkehrsinformationszentrale Berlin");
		freight.setDescription("Freight counts from closed data provided by the Verkehrsinformationszentrale Berlin");

		car.setName("Car");
		freight.setName("Freight");

		int counter = 0;
		for (Station station : stations.values()) {

			if(!station.isMatched()){
				counter++;
				continue;
			}

			car.createAndAddCount(station.getMatched().getId(), station.id).createVolume(1, station.getCar());
			freight.createAndAddCount(station.getMatched().getId(), station.id).createVolume(1, station.getFreight());
		}

		logger.info("Done. Stations without a matched link: {}", counter);
	}

	private void match(Station station, NetworkIndex<Station> index) {

		List<Link> result = index.query(station);

		if(result == null) {
			station.setIsNotMatched();
			return;
		}

		String direction = station.getDirection();
		Geometry geometry = index.getGeometry(station);

		Pattern pattern = Pattern.compile(direction, Pattern.CASE_INSENSITIVE);

		List<Link> filtered = result.stream()
				.filter(link -> pattern.matcher(getLinkDirection(link)).find())
				.collect(Collectors.toList());

		if(filtered.isEmpty()){
			//logger.warn("Results for station {} have no matching directions!", station.getId());
			station.setIsNotMatched();
			return;
		}

		Link closest = filtered.stream().findFirst().get();
		for(Link l: filtered){
			String linkDirection = getLinkDirection(l);

			if(linkDirection.contains(direction)){
				double distance = index.link2LineString(l).distance(geometry);
				double curClosest = index.link2LineString(closest).distance(geometry);

				if (distance < curClosest) closest = l;
			}
		}

		station.setMatched(closest);
		index.remove(closest);
	}

	private String getLinkDirection(Link l) {

		String dir = "";
		double fromX = l.getFromNode().getCoord().getX();
		double fromY = l.getFromNode().getCoord().getY();

		double toX = l.getToNode().getCoord().getX();
		double toY = l.getToNode().getCoord().getY();

		if(fromY < toY){
			dir += "nord";
		} else
			dir += "süd";

		if(fromX < toX) {
			dir += "ost";
		} else
			dir += "west";

		return dir;
	}

	private void extractDtv(HashMap<String, Station> stations, XSSFSheet car, XSSFSheet freight) {

		Iterator<Row> carIterator = car.iterator();
		carIterator.next();

		while (carIterator.hasNext()) {

			Row next = carIterator.next();
			String id = String.valueOf(next.getCell(0).getNumericCellValue()).substring(0,5);
			double dtv = next.getCell(1).getNumericCellValue();

			stations.get(id).setCarDTV(dtv);
		}

		Iterator<Row> freightIterator = freight.iterator();
		freightIterator.next();
		while (freightIterator.hasNext()) {
			Row next = freightIterator.next();
			String id = String.valueOf(next.getCell(0).getNumericCellValue()).substring(0,5);
			double dtv = next.getCell(1).getNumericCellValue();

			stations.get(id).setFreight(dtv);
		}
	}

	private void extractStations(HashMap<String, Station> stations, XSSFSheet sheet){
		Map<String, Integer> header = new HashMap<>();
		header.put("id", 0);
		header.put("direction", 3);
		header.put("x", 4);
		header.put("y", 5);


		Iterator<Row> it = sheet.iterator();
		it.next();
		while (it.hasNext()) {

			Row next = it.next();
			String id = String.valueOf(next.getCell(header.get("id")).getNumericCellValue()).substring(0,5);
			String direction = next.getCell(header.get("direction")).getStringCellValue();
			double x = next.getCell(header.get("x")).getNumericCellValue();
			double y = next.getCell(header.get("y")).getNumericCellValue();

			Station station = new Station(id, direction, x, y);
			stations.put(station.getId(), station);
		}
	}

	private static final class Station{

		String id;
		String direction;
		Coord coord;

		double car;
		double freight;

		boolean isMatched = true;

		Link matched;

		Station(String id, String direction, double x, double y){
			this.id = id;
			this.direction = direction;

			this.coord = new Coord(x, y);
		}

		public double getFreight() {
			return freight;
		}

		public double getCar() {
			return car;
		}

		public Coord getCoord() {
			return coord;
		}

		public void setCarDTV(double dtv) {
			this.car = dtv;
		}

		public void setFreight(double dtv) {
			this.freight = dtv;
		}

		public void setIsNotMatched(){
			this.isMatched = false;
		}

		public void setMatched(Link link) {
			this.matched = link;
		}

		public Link getMatched() {
			return matched;
		}

		public String getId() {
			return id;
		}

		public boolean isMatched(){
			return isMatched;
		}

		public String getDirection() {
			return direction;
		}
	}

	private enum Direction{

		Nord,
		Nordost,
		Nordwest,
		Ost,
		Süd,
		Südost,
		Südwest,
		West,
		unbekannt,
	}
}
