package org.matsim.prepare.parking;

import org.locationtech.jts.geom.Geometry;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class ParkingSupplyPerZone {

    public static void main(String[] args) throws IOException {

        Collection<SimpleFeature> parkingFeatures = ShapeFileReader.getAllFeatures(("/Users/gregorr/Desktop/Test/Parking/Uebergabe_Runde_1/TAM_Berlin_2020_Parking_counts_EPSG25833.shp"));
        Collection<SimpleFeature> berlinZones = ShapeFileReader.getAllFeatures(("/Users/gregorr/Desktop/Test/Parking/lor_planungsraeume_2021/lor_planungsraeume_2021.shp"));
        HashMap<String, Integer> lorToParkingCapacity = new HashMap<>();

        //iterate lor
        for (SimpleFeature berlinZone : berlinZones) {
            Geometry zone = (Geometry) berlinZone.getDefaultGeometry();
            int capacity = 0;

            // iterate parking spots
            for (SimpleFeature parkingFeature : parkingFeatures) {
                Geometry geom = (Geometry) parkingFeature.getDefaultGeometry();

                //check if parking spot is in zone
                if (zone.contains(geom)) {

                    if (parkingFeature.getAttribute("Count") instanceof Number) {
                        capacity = ((Number) parkingFeature.getAttribute("Count")).intValue() + capacity;
                    }
                }

            }
            lorToParkingCapacity.put((String) berlinZone.getAttribute("PLR_NAME"), capacity);
        }
        writeParkingCapacityPerLink("parkingSupply.tsv", lorToParkingCapacity);

    }


    private static void writeParkingCapacityPerLink(String outputFolder, HashMap<String, Integer> map) throws IOException {

        BufferedWriter parkingWriter = IOUtils.getBufferedWriter(outputFolder);
        parkingWriter.write("lor_id" + "\t" + "capacity");
        parkingWriter.newLine();
        for (String id: map.keySet()) {
            parkingWriter.write(id + "\t" + map.get(id).toString());
            parkingWriter.newLine();
        }
        parkingWriter.close();

    }
}
