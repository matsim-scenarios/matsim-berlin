package org.matsim.prepare.counts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.prepare.counts.NetworkIndex;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.opengis.referencing.operation.TransformException;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

@CommandLine.Command(
		name = "counts-from-vmz",
		description = "Create counts from the now deprecated (VMZ) data"
)
public class CreateCountsFromVMZ implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateCountsFromVMZ.class);

	@CommandLine.Option(names = "--excel", description = "Path to excel file containing the counts")
	private Path excel;

	@CommandLine.Option(names = "--csv", description = "Path to csv file with the mapping")
	private Path csv;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private Path network;

	@CommandLine.Option(names = "--network-geometries", description = "path to *linkGeometries.csv", required = true)
	private Path networkGeometries;

	@CommandLine.Option(names = "--output", description = "Base path for the output")
	private String output;

	@CommandLine.Mixin
	private final CrsOptions crs = new CrsOptions();

	private final Map<Integer, BerlinCount> stations = new HashMap<>();

	public static void main(String[] args) {
		new CreateCountsFromVMZ().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		readExcelFile(excel.toString());
//		readMappingFile(csv.toString());
		matchWithNetwork(network);
		createCountsFile(output);

		return 0;
	}

	private void matchWithNetwork(Path network) throws TransformException, IOException {

		Network net;
		{
			Network unfiltered = NetworkUtils.readNetwork(network.toString());
			NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());
			manager.addLinkFilter(l -> !l.getId().toString().startsWith("pt_"));

			net = manager.applyFilters();
		}

		CoordinateTransformation transformation = crs.getTransformation();

		/*int id = 0;
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("network-geometries");
		typeBuilder.setSRS("EPSG:25832");
		SimpleFeatureType type = typeBuilder.buildFeatureType();
		List<SimpleFeature> features = new ArrayList<>();

		for(var g: geometries.values()){
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
			SimpleFeature feature = builder.buildFeature(String.valueOf(id++));
			feature.setDefaultGeometry(g);
			features.add(feature);
		}
		 */


		NetworkIndex<BerlinCount> index = new NetworkIndex<>(net, NetworkIndex.readGeometriesFromSumo(networkGeometries.toString(), IdentityTransform.create(2)), 50, toMatch -> {
			Coord coord = toMatch.coord;
			Coord transform = transformation.transform(coord);
			return MGC.coord2Point(transform);
		});

		index.addLinkFilter((link, berlinCounts) -> {
			String orientation = berlinCounts.orientation;

			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();

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

		int counter = 0;

		for (var it = stations.entrySet().iterator(); it.hasNext();) {

			Map.Entry<Integer, BerlinCount> next = it.next();
			BerlinCount station = next.getValue();

			Link link = index.query(station);

			if (link == null) {
				it.remove();
				counter++;
			} else
				station.linkId = link.getId();
		}

		log.info("Could not match {} stations.", counter);
	}

	private void readExcelFile(String excel) {
		XSSFWorkbook wb;

		try {
			wb = new XSSFWorkbook(excel);
		} catch (Exception e) {
			log.error("Error reading excel file", e);
			throw new RuntimeException("Error reading excel file");
		}

		extractStations(wb.getSheet("Stammdaten"));
		extractCarVolumes(wb.getSheet("DTVW_KFZ"));
		extractFreightVolumes(wb.getSheet("DTVW_LKW"));
		extractHourlyDistribution(wb.getSheet("typ.Ganglinien_Mo-Do"));
		extractFreightShare(wb.getSheet("LKW-Anteile"));
	}

	private void createCountsFile(String outputFile) {
		log.info("Create count files.");
		Counts<Link> countsPkw = new Counts<>();
		countsPkw.setYear(2018);
		countsPkw.setDescription("data from the berliner senate to matsim counts");
		Counts<Link> countsLkw = new Counts<>();
		countsLkw.setYear(2018);
		countsLkw.setDescription("data from the berliner senate to matsim counts");

		int counter = 0;

		for (BerlinCount station : stations.values()) {
			if (!station.using) {
				continue;
			}
			Count<Link> car = countsPkw.createAndAddCount(station.linkId, station.id + "_" + station.position + "_" + station.orientation);
			//create hour volumes from 'Tagesganglinie'
			double[] carShareAtHour = station.carShareAtHour;
			for (int i = 1; i < 25; i++) {
				car.createVolume(i, (station.carVolume * carShareAtHour[i - 1]));
			}
			if (station.hasFreightShare) {
				Count<Link> freight = countsLkw.createAndAddCount(station.linkId, station.id + "_" + station.position + "_" + station.orientation);
				double[] freightShareAtHour = station.freightShareAtHour;
				for (int i = 1; i < 25; i++) {
					freight.createVolume(i, (station.freightVolume * freightShareAtHour[i - 1]));
				}
			}

			counter++;
		}

		log.info("Write down {} count stations to file", counter);
		new CountsWriter(countsPkw).write(outputFile + "car_counts_from_vmz.xml");
		new CountsWriter(countsLkw).write(outputFile + "freight_counts_from_vmz.xml");
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

			String orientation = row.getCell(3).getStringCellValue();
			station.orientation = replaceUmlaute(orientation);

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
			station.carVolume = (int) row.getCell(1).getNumericCellValue();
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
			if (row.getCell(3) != null) {
				carShareAtHour = row.getCell(3).getNumericCellValue();
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

	private String replaceUmlaute(String str) {
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
		private int carVolume;
		private int freightVolume;
		private final double[] carShareAtHour = new double[24];
		private final double[] freightShareAtHour = new double[24];
		private Id<Link> linkId;
		private String position;
		private String orientation;
		private boolean hasFreightShare = false;
		private final boolean using = false;

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
