package org.matsim.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CountsOptions;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.prepare.counts.NetworkIndex;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Measurable;
import org.matsim.counts.MeasurementLocation;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@CommandLine.Command(
		name = "counts-from-vmz",
		description = "Create counts from the now deprecated (VMZ) data"
)
public class CreateCountsFromVMZ implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateCountsFromVMZ.class);

	@CommandLine.Option(names = "--excel", description = "Path to excel file containing the counts")
	private Path excel;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private Path network;

	@CommandLine.Option(names = "--network-geometries", description = "path to *linkGeometries.csv")
	private Path networkGeometries;

	@CommandLine.Option(names = "--output", description = "Base path for the output")
	private String output;

	@CommandLine.Mixin
	private final CrsOptions crs = new CrsOptions();

	@CommandLine.Mixin
	private final CountsOptions counts = new CountsOptions();

	private final Map<Integer, BerlinCount> stations = new HashMap<>();
	private final List<BerlinCount> unmatched = new ArrayList<>();

	public static void main(String[] args) {
		new CreateCountsFromVMZ().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		readExcelFile(excel.toString());
		matchWithNetwork(network);
		createCountsFile(output);

		return 0;
	}

	private void matchWithNetwork(Path network) throws TransformException, IOException {

		/*
		 * TODO
		 *  how to handle count station outside of berlin?
		 * */

		Network net;
		{
			Network unfiltered = NetworkUtils.readNetwork(network.toString());
			NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());
			manager.addLinkFilter(l -> !l.getId().toString().startsWith("pt_"));

			net = manager.applyFilters();
		}

		CoordinateTransformation transformation = crs.getTransformation();

		NetworkIndex.GeometryGetter<BerlinCount> getter = toMatch -> {
			Coord coord = toMatch.coord;
			Coord transform = transformation.transform(coord);
			return MGC.coord2Point(transform);
		};

		NetworkIndex<BerlinCount> index = networkGeometries != null ?
				new NetworkIndex<>(net, NetworkIndex.readGeometriesFromSumo(networkGeometries.toString(), IdentityTransform.create(2)), 100, getter):
				new NetworkIndex<>(net, 100, getter);

		index.addLinkFilter((link, berlinCounts) -> {
			String orientation = berlinCounts.orientation;

			Coord from = link.link().getFromNode().getCoord();
			Coord to = link.link().getToNode().getCoord();

			String linkDir = "";
			if (to.getY() > from.getY()) {
				linkDir += "nord";
			} else
				linkDir += "süd";

			if (to.getX() > from.getX()) {
				linkDir += "ost";
			} else
				linkDir += "west";

			Pattern pattern = Pattern.compile(orientation, Pattern.CASE_INSENSITIVE);
			return pattern.matcher(linkDir).find();
		});

		Map<Id<Link>, ? extends Link> links = net.getLinks();

		for (var it = stations.entrySet().iterator(); it.hasNext();) {

			Map.Entry<Integer, BerlinCount> next = it.next();
			BerlinCount station = next.getValue();

			if (counts.isIgnored(String.valueOf(next.getKey()))) {
				it.remove();
				continue;
			}

			Id<Link> manuallyMatched = counts.isManuallyMatched(String.valueOf(next.getKey()));
			if (manuallyMatched != null) {

				if (links.containsKey(manuallyMatched)) {
					station.linkId = manuallyMatched;
					index.remove(links.get(manuallyMatched));
				} else {
					log.warn("Manually matched station link {}={} not found in network", next.getKey(), manuallyMatched);
					it.remove();
				}

			} else {

				Link link = index.query(station);

				if (link == null) {
					it.remove();
					unmatched.add(station);
				} else {
					station.linkId = link.getId();
					index.remove(link);
				}

			}
		}

		log.info("Could not match {} stations.", unmatched.size());
	}

	private void readExcelFile(String excel) {
		XSSFWorkbook wb;

		try {
			wb = new XSSFWorkbook(excel);
		} catch (IOException e) {
			log.error("Error reading excel file", e);
			throw new RuntimeException("Error reading excel file", e);
		}

		extractStations(wb.getSheet("Stammdaten"));
		extractCarVolumes(wb.getSheet("DTVW_KFZ"));
		extractFreightVolumes(wb.getSheet("DTVW_LKW"));
		extractHourlyDistribution(wb.getSheet("typ.Ganglinien_Mo-Do"));
		extractFreightShare(wb.getSheet("LKW-Anteile"));
	}

	private void createCountsFile(String outputFile) {
		log.info("Create count files.");
		Counts<Link> counts = new Counts<>();
		counts.setYear(2018);
		counts.setDescription("data from the berliner senate to matsim counts");

		int counter = 0;

		for (BerlinCount station : stations.values()) {

			MeasurementLocation<Link> location = counts.createAndAddMeasureLocation(station.linkId,
				station.id + "_" + station.position + "_" + station.orientation);

			Measurable carVolumes = location.createVolume(TransportMode.car);

			//create hour volumes from 'Tagesganglinie'
			double[] carShareAtHour = station.carShareAtHour;
			for (int i = 0; i < 24; i++) {
				carVolumes.setAtHour(i, (station.totalVolume - station.freightVolume) * carShareAtHour[i]);
			}
			if (station.hasFreightShare) {

				Measurable truckVolumes = location.createVolume(TransportMode.truck);

				double[] freightShareAtHour = station.freightShareAtHour;
				for (int i = 0; i < 24; i++) {
					truckVolumes.setAtHour(i, station.freightVolume * freightShareAtHour[i]);
				}
			}

			counter++;
		}

		log.info("Write down {} count stations to file", counter);
		new CountsWriter(counts).write(outputFile);

		log.info("Write down {} unmatched count stations to file", unmatched.size());
		try (CSVPrinter printer = CSVFormat.Builder.create().setHeader("id", "position", "x", "y").build()
				.print(Path.of(outputFile + "unmatched_stations.csv"), Charset.defaultCharset())) {

			CoordinateTransformation transformation = crs.getTransformation();

			for (BerlinCount count : unmatched) {
				printer.print(count.id);
				printer.print(count.position);
				Coord transform = transformation.transform(count.coord);
				printer.print(transform.getX());
				printer.print(transform.getY());
				printer.println();
			}

		} catch (IOException e) {
			log.error("Error printing unmatched stations", e);
		}
	}

	private void extractStations(Sheet sheet) {
		Iterator<Row> it = sheet.iterator();
		//skip header row
		it.next();

		while (it.hasNext()) {
			Row row = it.next();
			int id = (int) (row.getCell(0).getNumericCellValue());
			BerlinCount station = new BerlinCount(id);

			//get coord
			double x = row.getCell(4).getNumericCellValue();
			double y = row.getCell(5).getNumericCellValue();
			station.coord = new Coord(x, y);

			String position = row.getCell(1).getStringCellValue();
			station.position = replaceUmlaute(position);

			station.orientation = row.getCell(3).getStringCellValue();

			this.stations.put(id, station);
		}
	}

	private void extractCarVolumes(Sheet sheet) {
		Iterator<Row> it = sheet.iterator();
		//skip header row
		it.next();

		while (it.hasNext()) {
			Row row = it.next();
			int id = (int) row.getCell(0).getNumericCellValue();
			BerlinCount station = this.stations.get(id);
			station.totalVolume = (int) row.getCell(1).getNumericCellValue();
		}
	}

	private void extractFreightVolumes(Sheet sheet) {
		Iterator<Row> it = sheet.iterator();
		//skip header row
		it.next();

		while (it.hasNext()) {
			Row row = it.next();
			int id = (int) row.getCell(0).getNumericCellValue();
			BerlinCount station = this.stations.get(id);
			station.freightVolume = (int) row.getCell(1).getNumericCellValue();
		}
	}

	private void extractHourlyDistribution(Sheet sheet) {
		Iterator<Row> it = sheet.iterator();
		//skip header row
		it.next();

		while (it.hasNext()) {
			Row row = it.next();
			int id = (int) row.getCell(0).getNumericCellValue();
			BerlinCount station = this.stations.get(id);
			int hour = (int) row.getCell(1).getNumericCellValue();
			double carShareAtHour = 0.0;
			double freightShareAtHour = 0.0;

			if (row.getCell(2) != null) {
				carShareAtHour = row.getCell(2).getNumericCellValue();
			}
			if (row.getCell(4) != null) {
				freightShareAtHour = row.getCell(4).getNumericCellValue();
			}

			station.carShareAtHour[hour] = carShareAtHour;
			station.freightShareAtHour[hour] = freightShareAtHour;
		}
	}

	private void extractFreightShare(Sheet sheet) {
		Iterator<Row> it = sheet.iterator();
		//skip header row
		it.next();

		while (it.hasNext()) {
			Row row = it.next();
			int id = (int) row.getCell(0).getNumericCellValue();
			BerlinCount station = stations.get(id);
			station.hasFreightShare = true;
		}
	}

	private static String replaceUmlaute(String str) {
		str = str.replace("ü", "ue")
				.replace("ö", "oe")
				.replace("ä", "ae")
				.replace("ß", "ss")
				.replaceAll("Ü(?=[a-zäöüß ])", "Ue")
				.replaceAll("Ö(?=[a-zäöüß ])", "Oe")
				.replaceAll("Ä(?=[a-zäöüß ])", "Ae")
				.replaceAll("Ü", "UE")
				.replaceAll("Ö", "OE")
				.replaceAll("Ä", "AE");
		return str;
	}

	/**
	 * The BerlinCount object to save the scanned data for further processing.
	 */
	private static final class BerlinCount {

		private final int id;
		private int totalVolume;
		private int freightVolume;
		private final double[] carShareAtHour = new double[24];
		private final double[] freightShareAtHour = new double[24];
		private Id<Link> linkId;
		private String position;
		private String orientation;
		private boolean hasFreightShare = false;

		private Coord coord;

		BerlinCount(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "BerlinCount{" +
					"MQ_ID=" + id +
					", linkid=" + linkId.toString() +
					", position='" + position + '\'' +
					", orientation='" + orientation + '\'' +
					'}';
		}
	}

}
