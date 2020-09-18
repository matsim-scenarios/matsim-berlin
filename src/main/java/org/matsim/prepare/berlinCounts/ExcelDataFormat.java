package org.matsim.prepare.berlinCounts;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.HashMap;

public class ExcelDataFormat {

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
                berlinCounts.setLinkid((int) sheet.getRow(j).getCell(6).getNumericCellValue());
            }
        }
        return berlinCountsMap;
    }

}
