package org.matsim.prepare.berlinCounts;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ReadExcelFile {

    public static void main(String[] args) {

        String excel = "Datenexport_2018_TU_Berlin_LKW_Abweichungen.xlsx";
//        String excel = "D:/Arbeit/vsp/Datenexport_2018_TU_Berlin_LKW_Abweichungen.xlsx";
        HashMap<Integer, BerlinCounts> berlinCountsMap = new HashMap<>();

        try {
            XSSFWorkbook wb = new XSSFWorkbook(excel);
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                XSSFSheet sheet = wb.getSheetAt(i);
                if (sheet.getRow(0).getCell(0).getStringCellValue().equals("MQ_ID")) {
                    ExcelDataFormat.handleSheet(berlinCountsMap, i, sheet);
                } else {
                    System.err.println("sheets should start with MQ_ID, skipping sheet number: " + i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Counts<Link> counts = new Counts();
        for (BerlinCounts berlinCounts : berlinCountsMap.values()) {
            counts.createAndAddCount(Id.createLinkId(berlinCounts.getLinkid() + "_PKW" ),berlinCounts.getPosition() + "_" + berlinCounts.getOrientation());
            double[] PERC_Q_PKW_TYPE = berlinCounts.getPERC_Q_KFZ_TYPE();
            for (int i = 1; i < 25; i++) {
                counts.getCount(Id.createLinkId(berlinCounts.getLinkid() + "_PKW")).createVolume(i, (int) (berlinCounts.getDTVW_KFZ() * PERC_Q_PKW_TYPE[i - 1]));
            }
            if (berlinCounts.isLKW_Anteil()) {
                counts.createAndAddCount(Id.createLinkId(berlinCounts.getLinkid() + "_LKW"), berlinCounts.getPosition() + "_" + berlinCounts.getOrientation());
                double[] PERC_Q_LKW_TYPE = berlinCounts.getPERC_Q_LKW_TYPE();
                for (int i = 1; i < 25; i++) {
                    counts.getCount(Id.createLinkId(berlinCounts.getLinkid() + "_LKW")).createVolume(i, (int) (berlinCounts.getDTVW_KFZ() * PERC_Q_LKW_TYPE[i - 1]));
                }
            }
        }
        CountsWriter writer = new CountsWriter(counts);
        writer.write("counts.xml");

    }
}

class ExcelDataFormat {

    private static final String[] sheet0 = {"MQ_ID","DTVW_KFZ","QUALITY"};
    private static final String[] sheet1 = {"MQ_ID","DTVW_LKW"};
    private static final String[] sheet2 = {"MQ_ID","PERC_LKW"};
    private static final String[] sheet3 = {"MQ_ID","HOUR","PERC_Q_KFZ_TYPE","PERC_Q_PKW_TYPE","PERC_Q_LKW_TYPE"};
    private static final String[] sheet4 = {"MQ_ID","POSITION","DETAIL","ORIENTATION","X_GK4","Y_GK4","linkid"};
    private static final String[][] sheets = {sheet0, sheet1, sheet2, sheet3, sheet4};

    public static  HashMap<Integer, BerlinCounts> handleSheet(HashMap<Integer, BerlinCounts> berlinCountsMap, int i, XSSFSheet sheet) {
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
                if (berlinCounts == null) {
                    System.out.println();
                }
                berlinCounts.setArrays(hour, PERC_Q_KFZ_TYPE, PERC_Q_PKW_TYPE, PERC_Q_LKW_TYPE);
            }
        } else if (i == 4) {
            for (int j = 1; j <= sheet.getLastRowNum(); j++) {
                BerlinCounts berlinCounts = berlinCountsMap.get((int) sheet.getRow(j).getCell(0).getNumericCellValue());
                berlinCounts.setPosition(sheet.getRow(j).getCell(1).getStringCellValue());
                berlinCounts.setOrientation(sheet.getRow(j).getCell(3).getStringCellValue());
                berlinCounts.setLinkid((int) sheet.getRow(j).getCell(6).getNumericCellValue());
            }
        }
        return berlinCountsMap;
    }
}

class BerlinCounts {

    public final List<String> informations = Arrays.asList("MQ_ID","DTVW_KFZ","DTVW_LKW","PERC_LKW","PERC_Q_KFZ_TYPE","PERC_Q_PKW_TYPE","PERC_Q_LKW_TYPE","linkid");

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
}