package org.matsim.analysis.speedlimit;

import org.matsim.api.core.v01.Coord;

import java.io.*;
import java.util.*;

public class FilterTripsCSV {

    private static final String workingDirectory = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_1\\Kalibrierung\\V54\\";
    private static final String tripsFilepath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_2\\Analyse\\berlin.o" +
            "utput_trips.csv\\berlin.output_trips.csv";
    private static final String shapeFilePath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Bezirke_-_Berli" +
            "n-shp\\Berlin_Bezirke.shp";
    private static final String outputFilePath0 = "tripsWithinZone";
    private static final String outputFilePath1 = "tripsIntoZone";
    private static final String outputFilePath2 = "tripsOutoZone";

    private static  ShapeFileAnalyzer analyzer = new ShapeFileAnalyzer(shapeFilePath);

    private static String csvHeader;

    private static ArrayList tripsList = new ArrayList();
    private static ArrayList tripsWithinZone = new ArrayList();
    private static ArrayList tripsIntoZone = new ArrayList();
    private static ArrayList tripsOutoZone = new ArrayList();

    private static int counter = 0;

    public static void main(String[] args) {

        var tripList = readFile(tripsFilepath);

        for (var trip: tripList){
            int direction = getDirection(trip);

            switch (direction) {
                case -1: tripsOutoZone.add(trip.toString()); break;
                case 0: tripsWithinZone.add(trip.toString()); break;
                case 1: tripsIntoZone.add(trip.toString()); break;

                default: break;
            }

            System.out.println("Actual trip number: " + counter++);
        }

        printCSV(workingDirectory+outputFilePath0+".csv",tripsWithinZone);


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

                for (int i = 0; i < header.length-2; i++){
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
            String tempStartX = new String();
            String tempStartY = new String();
            String tempEndX = new String();
            String tempEndY = new String();

            trip_attributes.put("personID",tripAsHashMap.get("person"));
            trip_attributes.put("traveled_distance", tripAsHashMap.get("traveled_distance"));
            trip_attributes.put("mainMode",tripAsHashMap.get("longest_distance_mode"));
            trip_attributes.put("legList",tripAsHashMap.get("modes"));

            /*
            if (tripAsHashMap.get("start_x").contains(".")) {
                tempStartX = tripAsHashMap.get("start_x").replaceFirst("\\.","");
            }
            if (tripAsHashMap.get("start_y").contains(".")){
                tempStartY = tripAsHashMap.get("start_y").replaceFirst("\\.","");
            }
            if (tripAsHashMap.get("end_x").contains(".")) {
                tempEndX = tripAsHashMap.get("end_x").replaceFirst("\\.","");
            }
            if (tripAsHashMap.get("end_y").contains(".")) {
                tempEndY = tripAsHashMap.get("end_y").replaceFirst("\\.","");
            }
             */

            trip_attributes.put("start_x", tripAsHashMap.get("start_x"));
            trip_attributes.put("start_y", tripAsHashMap.get("start_y"));
            trip_attributes.put("end_x", tripAsHashMap.get("end_x"));
            trip_attributes.put("end_y", tripAsHashMap.get("end_y"));

            /*
            trip_attributes.put("start_x", tempStartX);
            trip_attributes.put("start_y", tempStartY);
            trip_attributes.put("end_x", tempEndX);
            trip_attributes.put("end_y", tempEndY);
             */
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
