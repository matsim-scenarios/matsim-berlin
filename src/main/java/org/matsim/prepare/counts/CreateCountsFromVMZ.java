package org.matsim.prepare.counts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.LineString;
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
import org.matsim.prepare.berlinCounts.NetworkGeometryParser;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
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

	private final HashMap<Integer, BerlinCount> stations = new HashMap<>();

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

	private void matchWithNetwork(Path network){

		Network net;

		{
			Network unfiltered = NetworkUtils.readNetwork(network.toString());
			NetworkFilterManager manager = new NetworkFilterManager(unfiltered, new NetworkConfigGroup());
			manager.addLinkFilter(l -> !l.getId().toString().startsWith("pt_"));

			net = manager.applyFilters();
		}

		CoordinateTransformation transformation = crs.getTransformation();

		//TODO check geometry crs!
		Map<Id<Link>, LineString> geometries = new NetworkGeometryParser(networkGeometries).parse();

		Map<Link, LineString> geometriesWithLinks = new HashMap<>();

		for(var entry: geometries.entrySet())
			geometriesWithLinks.put(net.getLinks().get(entry.getKey()), entry.getValue());

		NetworkIndex<BerlinCount> index = new NetworkIndex<>(geometriesWithLinks, 50, toMatch -> {

			Coord coord = toMatch.getCoord();
			Coord transform = transformation.transform(coord);
			return MGC.coord2Point(transform);
		});

		index.addLinkFilter((link, berlinCounts) -> {
			String orientation = berlinCounts.getOrientation();

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

		for(var it = stations.entrySet().iterator(); it.hasNext();){

			Map.Entry<Integer, BerlinCount> next = it.next();
			BerlinCount station = next.getValue();

			Link link = index.query(station);

			if(link == null)
				it.remove();
		}
	}

	/**
	 * reads the given Excel file and creates an BerlinCount object for every count
	 *
	 */
	private void readExcelFile(String excel) {
		try {
			XSSFWorkbook wb = new XSSFWorkbook(excel);
			for (Sheet sheet : wb)
				ExcelDataFormat.handleSheet(stations, sheet);

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * uses the BerlinCount object to create a car and a truck counts file for matsim
	 */
	private void createCountsFile(String outputFile) {
		log.warn("Create count files.");
		Counts<Link> countsPkw = new Counts<>();
		countsPkw.setYear(2018);
		countsPkw.setDescription("data from the berliner senate to matsim counts");
		Counts<Link> countsLkw = new Counts<>();
		countsLkw.setYear(2018);
		countsLkw.setDescription("data from the berliner senate to matsim counts");

		int counter = 0;

		for (BerlinCount station : stations.values()) {
			if (!station.isUsing()) {
				continue;
			}
			Id<Link> linkId = Id.createLinkId(station.getLinkid());
			Count<Link> car = countsPkw.createAndAddCount(linkId, station.getMQ_ID() + "_" + station.getPosition() + "_" + station.getOrientation());
			//create hour volumes from 'Tagesganglinie'
			double[] PERC_Q_PKW_TYPE = station.getPERC_Q_KFZ_TYPE();
			for (int i = 1; i < 25; i++) {
				car.createVolume(i, (station.getDTVW_KFZ() * PERC_Q_PKW_TYPE[i - 1]));
			}
			if (station.isLKW_Anteil()) {
				Count<Link> freight = countsLkw.createAndAddCount(linkId, station.getMQ_ID() + "_" + station.getPosition() + "_" + station.getOrientation());
				double[] PERC_Q_LKW_TYPE = station.getPERC_Q_LKW_TYPE();
				for (int i = 1; i < 25; i++) {
					freight.createVolume(i, (station.getDTVW_LKW() * PERC_Q_LKW_TYPE[i - 1]));
				}
			}

			counter++;
		}

		log.info("Write down {} count stations to file", counter);
		new CountsWriter(countsPkw).write(outputFile + "car_counts_from_vmz.xml");
		new CountsWriter(countsLkw).write(outputFile + "freight_counts_from_vmz.xml");
	}

	/**
	 * a description for every excel sheet that is given in the input file to process the data correctly
	 */
	private static final class ExcelDataFormat {

		public static void handleSheet(HashMap<Integer, BerlinCount> berlinCountsMap, Sheet sheet) {
			log.info("Sheet contains {} rows.", sheet.getLastRowNum());
			String sheetName = sheet.getSheetName();
			switch(sheetName){
				case "DTVW_KFZ":
					for (Row row : sheet) {
						int id = (int) row.getCell(0).getNumericCellValue();
						BerlinCount station = new BerlinCount(id);
						station.setDTVW_KFZ((int) row.getCell(1).getNumericCellValue());
						berlinCountsMap.put(station.getMQ_ID(), station);
					}
					break;
				case "DTVW_LKW":
					for (Row row: sheet) {
						int id = (int) row.getCell(0).getNumericCellValue();
						BerlinCount station = berlinCountsMap.get(id);
						station.setDTVW_LKW((int) row.getCell(1).getNumericCellValue());
					}
					break;
				case "LKW-Anteile":
					for (Row row: sheet) {
						int id = (int) row.getCell(0).getNumericCellValue();
						BerlinCount station = berlinCountsMap.get(id);
						station.setPERC_LKW(row.getCell(1).getNumericCellValue());
						station.setLKW_Anteil(true);
					}
					break;
				case "typ.Ganglinien_Mo-Do":
					for (Row row: sheet) {
						int id = (int) row.getCell(0).getNumericCellValue();
						BerlinCount station = berlinCountsMap.get(id);
						int hour = (int) row.getCell(1).getNumericCellValue();
						double PERC_Q_KFZ_TYPE = 0.0;
						double PERC_Q_PKW_TYPE = 0.0;
						double PERC_Q_LKW_TYPE = 0.0;
						if (row.getCell(2) != null) {
							PERC_Q_KFZ_TYPE = row.getCell(2).getNumericCellValue();
						}
						if (row.getCell(3) != null) {
							PERC_Q_PKW_TYPE = row.getCell(3).getNumericCellValue();
						}
						if (row.getCell(4) != null) {
							PERC_Q_LKW_TYPE = row.getCell(4).getNumericCellValue();
						}
						station.setArrays(hour, PERC_Q_KFZ_TYPE, PERC_Q_PKW_TYPE, PERC_Q_LKW_TYPE);
					}
					break;
				case "Stammdaten":
					for (Row row : sheet) {
						int id = (int) row.getCell(0).getNumericCellValue();
						BerlinCount station = berlinCountsMap.get(id);

						//get coord
						double x = row.getCell(4).getNumericCellValue();
						double y = row.getCell(5).getNumericCellValue();
						station.setCoord(new Coord(x, y));

						String position = row.getCell(1).getStringCellValue();
						station.setPosition(replaceUmlaute(position));

						String orientation = row.getCell(3).getStringCellValue();
						station.setPosition(replaceUmlaute(orientation));
					}
					break;
				default:

			}
		}

		/**
		 * replaces the german umlauts
		 */
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

	}

	/**
	 * the BerlinCount object to save the scanned data for further processing
	 */
	private static final class BerlinCount {

		private int MQ_ID;
		private int DTVW_KFZ;
		private int DTVW_LKW;
		private double PERC_LKW;
		private double[] PERC_Q_KFZ_TYPE = new double[24];
		private double[] PERC_Q_PKW_TYPE = new double[24];
		private double[] PERC_Q_LKW_TYPE = new double[24];
		private int linkid;
		private String position;
		private String orientation;
		private boolean LKW_Anteil = false;
		private boolean using = false;

		private Coord coord;

		public BerlinCount(int MQ_ID) {
			this.MQ_ID = MQ_ID;
		}

		public boolean isUsing() {
			return using;
		}

		public Coord getCoord() {
			return coord;
		}

		public void setCoord(Coord coord) {
			this.coord = coord;
		}

		public void setUsing(boolean using) {
			this.using = using;
		}

		public String getPosition() {
			return position;
		}

		public void setPosition(String position) {
			this.position = position;
		}

		public String getOrientation() {
			return orientation;
		}

		public void setOrientation(String orientation) {
			this.orientation = orientation;
		}

		public boolean isLKW_Anteil() {
			return LKW_Anteil;
		}

		public void setLKW_Anteil(boolean LKW_Anteil) {
			this.LKW_Anteil = LKW_Anteil;
		}

		public int getMQ_ID() {
			return MQ_ID;
		}

		public void setMQ_ID(int MQ_ID) {
			this.MQ_ID = MQ_ID;
		}

		public int getDTVW_KFZ() {
			return DTVW_KFZ;
		}

		public void setDTVW_KFZ(int DTVW_KFZ) {
			this.DTVW_KFZ = DTVW_KFZ;
		}

		public int getDTVW_LKW() {
			return DTVW_LKW;
		}

		public void setDTVW_LKW(int DTVW_LKW) {
			this.DTVW_LKW = DTVW_LKW;
		}

		public double getPERC_LKW() {
			return PERC_LKW;
		}

		public void setPERC_LKW(double PERC_LKW) {
			this.PERC_LKW = PERC_LKW;
		}

		public double[] getPERC_Q_KFZ_TYPE() {
			return PERC_Q_KFZ_TYPE;
		}

		public void setPERC_Q_KFZ_TYPE(double[] PERC_Q_KFZ_TYPE) {
			this.PERC_Q_KFZ_TYPE = PERC_Q_KFZ_TYPE;
		}

		public double[] getPERC_Q_PKW_TYPE() {
			return PERC_Q_PKW_TYPE;
		}

		public void setPERC_Q_PKW_TYPE(double[] PERC_Q_PKW_TYPE) {
			this.PERC_Q_PKW_TYPE = PERC_Q_PKW_TYPE;
		}

		public double[] getPERC_Q_LKW_TYPE() {
			return PERC_Q_LKW_TYPE;
		}

		public void setPERC_Q_LKW_TYPE(double[] PERC_Q_LKW_TYPE) {
			this.PERC_Q_LKW_TYPE = PERC_Q_LKW_TYPE;
		}

		public int getLinkid() {
			return linkid;
		}

		public void setLinkid(int linkid) {
			this.linkid = linkid;
		}

		public void setArrays(int i, double PERC_Q_KFZ_TYPE, double PERC_Q_PKW_TYPE, double PERC_Q_LKW_TYPE) {
			this.PERC_Q_KFZ_TYPE[i] = PERC_Q_KFZ_TYPE;
			this.PERC_Q_PKW_TYPE[i] = PERC_Q_PKW_TYPE;
			this.PERC_Q_LKW_TYPE[i] = PERC_Q_LKW_TYPE;
		}

		@Override
		public String toString() {
			return "BerlinCount{" +
					"MQ_ID=" + MQ_ID +
					", linkid=" + linkid +
					", position='" + position + '\'' +
					", orientation='" + orientation + '\'' +
					'}';
		}
	}

}
