package org.matsim.analysis.speedlimit;

import org.matsim.api.core.v01.Coord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterTripsCSV {

    private static final String filepath = "";
    private static final String shapeFilePath = "";
    private static final String outputFilePath0 = "";
    private static final String outputFilePath1 = "";
    private static final String outputFilePath2 = "";

    private static String csvHeader;

    private static ArrayList tripsList = new ArrayList();
    private static ArrayList tripsWithinZone = new ArrayList();
    private static ArrayList tripsIntoZone = new ArrayList();
    private static ArrayList tripsOutoZone = new ArrayList();

    public static void main(String[] args) {

        var tripList = readFile(filepath);

        for (var trip: tripList){
            int direction = getDirection(trip);

            switch (direction) {
                case -1: tripsOutoZone.add(trip.toString()); break;
                case 0: tripsWithinZone.add(trip.toString()); break;
                case 1: tripsIntoZone.add(trip.toString()); break;

                default: break;
            }

        }


    }

    private static List<Trip> readFile(String filepath) {
        File inputFile = new File(filepath);
        List<Trip> trips = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(inputFile))) {

            csvHeader = in.readLine();
            String[] header = csvHeader.split(";");
            String line ;
            while ((line = in.readLine()) != null) {
                String[] trip_attributes = line.split(";");

                HashMap<String, String> currentTrip = new HashMap<>();

                for (int i = 0; i < header.length; i++){
                    currentTrip.put(header[i],trip_attributes[i]);
                }

                trips.add(new Trip(currentTrip));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trips;
    }

    private static void printCSV(String outputFilePath, ArrayList tripsList){

        PrintWriter pWriter = null;
        try {
            pWriter = new PrintWriter(
                    new BufferedWriter(new FileWriter(outputFilePath)));


        } catch (IOException e) {
            e.printStackTrace();
        }

        pWriter.println(csvHeader);
        for (var trip: tripsList){
            pWriter.println(trip.toString());
        }
        /**Reicht das so?*/
        pWriter.close();
    }

    private static int getDirection(Trip trip){
        ShapeFileAnalyzer analyzer = new ShapeFileAnalyzer(shapeFilePath);
        HashMap<String, String> tripAttributes = trip.trip_attributes;

        var deptCoord = new Coord(Double.parseDouble(tripAttributes.get("start_x")),
                Double.parseDouble(tripAttributes.get("start_y")));
        var arrCoord = new Coord(Double.parseDouble(tripAttributes.get("end_x")),
                Double.parseDouble(tripAttributes.get("end_y")));

        boolean tripStartsInZone = analyzer.isInGeometry(deptCoord);
        boolean tripEndsInZone = analyzer.isInGeometry(arrCoord);

        if (tripStartsInZone && tripEndsInZone) return 0;
        if (tripStartsInZone && !tripEndsInZone) return -1;
        if (!tripStartsInZone && tripEndsInZone) return 1;
        return 2; //return 2 if trip is happening outside the shapefile area
    }

    private static class Trip{
        private HashMap<String,String> trip_attributes = new HashMap();

        public Trip(String personID, double distance, String mainMode, String legList, Coord deptCoord, Coord arrCoord){
            trip_attributes.put("personID",personID);
            trip_attributes.put("traveled_distance", Double.toString(distance));
            trip_attributes.put("mainMode",mainMode);
            trip_attributes.put("legList",legList);
            trip_attributes.put("start_x", Double.toString(deptCoord.getX()));
            trip_attributes.put("start_y", Double.toString(deptCoord.getY()));
            trip_attributes.put("end_x", Double.toString(arrCoord.getX()));
            trip_attributes.put("end_y",Double.toString(arrCoord.getY()));
        }

        public Trip(HashMap<String,String> tripAsHashMap){
            trip_attributes.put("personID",tripAsHashMap.get("person"));
            trip_attributes.put("traveled_distance", tripAsHashMap.get("traveled_distance"));
            trip_attributes.put("mainMode",tripAsHashMap.get("longest_distance_mode"));
            trip_attributes.put("legList",tripAsHashMap.get("modes"));
            trip_attributes.put("start_x", tripAsHashMap.get("start_x"));
            trip_attributes.put("start_y", tripAsHashMap.get("start_y"));
            trip_attributes.put("end_x", tripAsHashMap.get("end_x"));
            trip_attributes.put("end_y",tripAsHashMap.get("end_y"));
        }

        public HashMap<String, String> getTrip_attributes() {
            return trip_attributes;
        }

        @Override
        public String toString() {
            String tripAsStringForCSV = "";

            for (String attribute: trip_attributes.values()){
                tripAsStringForCSV += attribute + ";";
            }

            return tripAsStringForCSV;
        }
    }
}
