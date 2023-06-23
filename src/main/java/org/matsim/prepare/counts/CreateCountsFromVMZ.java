package org.matsim.prepare.counts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.xssf.usermodel.XSSFSheet;
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
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.prepare.berlinCounts.NetworkGeometryParser;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
	private final CrsOptions crs = new CrsOptions("EPSG:31468");

	private final HashMap<Integer, BerlinCounts> berlinCountsMap = new HashMap<>();

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

		Map<Id<Link>, LineString> geometries = new NetworkGeometryParser(networkGeometries).parse();

		NetworkIndex<BerlinCounts> index = new NetworkIndex<>(net, 50, toMatch -> {
			BerlinCounts count = (BerlinCounts) toMatch;

			Coord coord = count.getCoord();
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

		for(var it = berlinCountsMap.entrySet().iterator(); it.hasNext();){

			Map.Entry<Integer, BerlinCounts> next = it.next();
			BerlinCounts station = next.getValue();

			Link link = index.query(station);

			if(link == null)
				it.remove();
		}
	}

	/**
	 * reads the given excel file and creates an BerlinCounts object for every count
	 *
	 * @param excel
	 */
	private void readExcelFile(String excel) {
		try {
			XSSFWorkbook wb = new XSSFWorkbook(excel);
			for (int i = 0; i < wb.getNumberOfSheets(); i++) {
				XSSFSheet sheet = wb.getSheetAt(i);
				if (sheet.getRow(0).getCell(0).getStringCellValue().equals("MQ_ID")) {
					ExcelDataFormat.handleSheet(berlinCountsMap, i, sheet);
				} else {
					log.warn("sheets should start with MQ_ID, skipping sheet number: {}", i);
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * uses the BerlinCounts object to create a car and a truck counts file for matsim
	 *
	 * @param outputFile
	 */
	private void createCountsFile(String outputFile) {
		Counts<Link> countsPkw = new Counts();
		countsPkw.setYear(2018);
		countsPkw.setDescription("data from the berliner senate to matsim counts");
		Counts<Link> countsLkw = new Counts();
		countsLkw.setYear(2018);
		countsLkw.setDescription("data from the berliner senate to matsim counts");

		for (BerlinCounts berlinCounts : berlinCountsMap.values()) {
			if (!berlinCounts.isUsing()) {
				continue;
			}
			countsPkw.createAndAddCount(Id.createLinkId(berlinCounts.getLinkid()), berlinCounts.getMQ_ID() + "_" + berlinCounts.getPosition() + "_" + berlinCounts.getOrientation());
			double[] PERC_Q_PKW_TYPE = berlinCounts.getPERC_Q_KFZ_TYPE();
			for (int i = 1; i < 25; i++) {
				countsPkw.getCount(Id.createLinkId(berlinCounts.getLinkid())).createVolume(i, (berlinCounts.getDTVW_KFZ() * PERC_Q_PKW_TYPE[i - 1]));
			}
			if (berlinCounts.isLKW_Anteil()) {
				countsLkw.createAndAddCount(Id.createLinkId(berlinCounts.getLinkid()), berlinCounts.getMQ_ID() + "_" + berlinCounts.getPosition() + "_" + berlinCounts.getOrientation());
				double[] PERC_Q_LKW_TYPE = berlinCounts.getPERC_Q_LKW_TYPE();
				for (int i = 1; i < 25; i++) {
					countsLkw.getCount(Id.createLinkId(berlinCounts.getLinkid())).createVolume(i, (berlinCounts.getDTVW_LKW() * PERC_Q_LKW_TYPE[i - 1]));
				}
			}
		}
		new CountsWriter(countsPkw).write(outputFile + "car_counts_from_vmz.xml");
		new CountsWriter(countsLkw).write(outputFile + "freight_counts_from_vmz.xml");;
	}

	/**
	 * reads a given csv file and adds the data to the BerlinCounts object
	 *
	 * @param file
	 */
	private void readMappingFile(String file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String headerLine = br.readLine();
			String line;
			while ((line = br.readLine()) != null) {
				String[] information = line.split(";");
				if (information.length < 3) {
					continue;
				}
				int MQ_ID = Integer.parseInt(information[0]);
				int linkid = -999;
				if (!information[1].isBlank()) {
					linkid = Integer.parseInt(information[1]);
				}
				String using = information[2];
				BerlinCounts count = berlinCountsMap.get(MQ_ID);
				count.setLinkid(linkid);
				if (using.equals("x")) {
					count.setUsing(true);
				}

				if (linkid == -999) {
					berlinCountsMap.remove(MQ_ID);
					log.warn("No link id: {}", count);
				}
			}
		} catch (IOException e) {
			log.error(e);
		}
	}


	/**
	 * a description for every excel sheet that is given in the input file to process the data correctly
	 */
	private static final class ExcelDataFormat {

		public static HashMap<Integer, BerlinCounts> handleSheet(HashMap<Integer, BerlinCounts> berlinCountsMap, int i, XSSFSheet sheet) {
			if (i == 0) {
				for (int j = 1; j <= sheet.getLastRowNum(); j++) {
					BerlinCounts berlinCounts = new BerlinCounts((int) sheet.getRow(j).getCell(0).getNumericCellValue());
					berlinCounts.setDTVW_KFZ((int) sheet.getRow(j).getCell(1).getNumericCellValue());
					berlinCountsMap.put(berlinCounts.getMQ_ID(), berlinCounts);
				}
			} else if (i == 1) {
				for (int j = 1; j <= sheet.getLastRowNum(); j++) {
					BerlinCounts berlinCounts = berlinCountsMap.get((int) sheet.getRow(j).getCell(0).getNumericCellValue());
					berlinCounts.setDTVW_LKW((int) sheet.getRow(j).getCell(1).getNumericCellValue());
				}
			} else if (i == 2) {
				for (int j = 1; j <= sheet.getLastRowNum(); j++) {
					BerlinCounts berlinCounts = berlinCountsMap.get((int) sheet.getRow(j).getCell(0).getNumericCellValue());
					berlinCounts.setPERC_LKW(sheet.getRow(j).getCell(1).getNumericCellValue());
					berlinCounts.setLKW_Anteil(true);
				}
			} else if (i == 3) {
				for (int j = 1; j <= sheet.getLastRowNum(); j++) {
					BerlinCounts berlinCounts = berlinCountsMap.get((int) sheet.getRow(j).getCell(0).getNumericCellValue());
					int hour = (int) sheet.getRow(j).getCell(1).getNumericCellValue();
					double PERC_Q_KFZ_TYPE = 0.0;
					double PERC_Q_PKW_TYPE = 0.0;
					double PERC_Q_LKW_TYPE = 0.0;
					if (sheet.getRow(j).getCell(2) != null) {
						PERC_Q_KFZ_TYPE = sheet.getRow(j).getCell(2).getNumericCellValue();
					}
					if (sheet.getRow(j).getCell(3) != null) {
						PERC_Q_PKW_TYPE = sheet.getRow(j).getCell(3).getNumericCellValue();
					}
					if (sheet.getRow(j).getCell(4) != null) {
						PERC_Q_LKW_TYPE = sheet.getRow(j).getCell(4).getNumericCellValue();
					}
					berlinCounts.setArrays(hour, PERC_Q_KFZ_TYPE, PERC_Q_PKW_TYPE, PERC_Q_LKW_TYPE);
				}
			} else if (i == 4) {

				for (Row row : sheet) {
					int id = (int) row.getCell(0).getNumericCellValue();
					BerlinCounts station = berlinCountsMap.get(id);

					//get coord
					double x = row.getCell(4).getNumericCellValue();
					double y = row.getCell(5).getNumericCellValue();
					station.setCoord(new Coord(x, y));

					String position = row.getCell(1).getStringCellValue();
					station.setPosition(replaceUmlaute(position));

					String orientation = row.getCell(3).getStringCellValue();
					station.setPosition(replaceUmlaute(orientation));
				}
			}
			return berlinCountsMap;
		}

		/**
		 * replaces the german umlauts
		 *
		 * @param str
		 * @return
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
	 * the BerlinCounts object to save the scanned data for further processing
	 */
	private static final class BerlinCounts {

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

		public BerlinCounts(int MQ_ID) {
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
			return "BerlinCounts{" +
					"MQ_ID=" + MQ_ID +
					", linkid=" + linkid +
					", position='" + position + '\'' +
					", orientation='" + orientation + '\'' +
					'}';
		}
	}

}
