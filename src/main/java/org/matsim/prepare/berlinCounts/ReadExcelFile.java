package org.matsim.prepare.berlinCounts;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.util.HashMap;

public class ReadExcelFile {

    public static void main(String[] args) {

        String excel = "D:/Arbeit/vsp/Datenexport_2018_TU_Berlin_LKW_Abweichungen.xlsx";
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
            counts.createAndAddCount(Id.createLinkId(berlinCounts.getLinkid()),"");
            counts.getCount(Id.createLinkId(berlinCounts.getLinkid())).createVolume(1,berlinCounts.getPERC_LKW());
        }
        CountsWriter writer = new CountsWriter(counts);
        writer.write("counts.xml");

    }

}
