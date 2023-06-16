package org.matsim.prepare.counts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.lang.String;

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

	@CommandLine.Option(names = "--output", description = "Base path for the output")
	private String output;

	private final HashMap<Integer, BerlinCounts> berlinCountsMap = new HashMap<>();

	public static void main(String[] args) {
		new CreateCountsFromVMZ().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		readExcelFile(excel.toString());
		readMappingFile(csv.toString());
		createCountsFile(output);

		return 0;
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
		CountsWriter writerPkw = new CountsWriter(countsPkw);
		CountsWriter writerLkw = new CountsWriter(countsLkw);
		writerPkw.write(outputFile);

		if (outputFile.contains("-car"))
			writerLkw.write(outputFile.replace("-car", "-hgv"));
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

		private static final String[] sheet0 = {"MQ_ID", "DTVW_KFZ", "QUALITY"};
		private static final String[] sheet1 = {"MQ_ID", "DTVW_LKW"};
		private static final String[] sheet2 = {"MQ_ID", "PERC_LKW"};
		private static final String[] sheet3 = {"MQ_ID", "HOUR", "PERC_Q_KFZ_TYPE", "PERC_Q_PKW_TYPE", "PERC_Q_LKW_TYPE"};
		private static final String[] sheet4 = {"MQ_ID", "POSITION", "DETAIL", "ORIENTATION", "X_GK4", "Y_GK4", "linkid"};

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
				for (int j = 1; j <= sheet.getLastRowNum(); j++) {
					BerlinCounts berlinCounts = berlinCountsMap.get((int) sheet.getRow(j).getCell(0).getNumericCellValue());
					berlinCounts.setPosition(replaceUmlaute(sheet.getRow(j).getCell(1).getStringCellValue()));
					if (sheet.getRow(j).getCell(2) != null) {
						berlinCounts.setDetail(replaceUmlaute(sheet.getRow(j).getCell(2).getStringCellValue()));
					} else {
						berlinCounts.setDetail("");
					}
					berlinCounts.setOrientation(replaceUmlaute(sheet.getRow(j).getCell(3).getStringCellValue()));
//                berlinCounts.setLinkid((int) sheet.getRow(j).getCell(6).getNumericCellValue());
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
		private String detail;
		private boolean LKW_Anteil = false;
		private boolean using = false;

		public boolean isUsing() {
			return using;
		}

		public void setUsing(boolean using) {
			this.using = using;
		}

		public String getDetail() {
			return detail;
		}

		public void setDetail(String detail) {
			this.detail = detail;
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

		public BerlinCounts(int MQ_ID) {
			this.MQ_ID = MQ_ID;
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
					", detail='" + detail + '\'' +
					'}';
		}
	}

}
