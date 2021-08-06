package org.matsim.analysis.speedlimit;

import org.matsim.api.core.v01.Coord;

import java.io.*;
import java.util.*;

public class FilterTripsCSV {

    private static final String workingDirectory = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_2\\Analyse\\";
    private static final String tripsFilepath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_2\\Output\\" +
            "S-ring_trips.csv\\berlin-30kmh-ring.output_trips.csv";
    private static final String shapeFilePath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_2\\Input\\Shapefile\\Berlin_S-Bahn-Ring.shp";
    private static final String outputFilePath0 = "tripsWithinZone_S-ring-only";
    private static final String outputFilePath1 = "tripsIntoZone_S-ring-only";
    private static final String outputFilePath2 = "tripsOutoZone_S-ring-only";

    private static  ShapeFileAnalyzer analyzer = new ShapeFileAnalyzer(shapeFilePath);

    private static ArrayList<Trip> tripsList = new ArrayList();
    private static ArrayList<Trip> tripsWithinZone = new ArrayList();
    private static ArrayList<Trip> tripsIntoZone = new ArrayList();
    private static ArrayList<Trip> tripsOutoZone = new ArrayList();

    private static int counter = 0;

    public static void main(String[] args) {

        var tripList = readFile(tripsFilepath);

        for (var trip: tripList){
            int direction = getDirection(trip);

            switch (direction) {
                case -1: tripsOutoZone.add(trip); break;
                case 0: tripsWithinZone.add(trip); break;
                case 1: tripsIntoZone.add(trip); break;

                default: break;
            }

            if (counter++ % 10000 == 0) {
                System.out.println("Actual trip number: " + counter + "\n" + (tripList.size() - counter) +
                        " trips left.");
            }
        }

        printCSV(workingDirectory+outputFilePath0+".csv",tripsWithinZone);
        printCSV(workingDirectory+outputFilePath1+".csv",tripsIntoZone);
        printCSV(workingDirectory+outputFilePath2+".csv",tripsOutoZone);
    }

    private static List<Trip> readFile(String filepath) {
        //refers to OOP TUT #11 or 12

        File inputFile = new File(filepath);
        List<Trip> trips = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(inputFile))) {

            String[] header = in.readLine().split(";"); //read in the headline
            String line ;
            while ((line = in.readLine()) != null) {
                String[] trip_attributes = line.split(";");

                HashMap<String, String> currentTrip = new HashMap<>();

                for (int i = 0; i < header.length-2; i++){
                    //we create a hashmap, so we can later decide which attributes we want to have in the later csv
                    currentTrip.put(header[i],trip_attributes[i]);
                }

                trips.add(new Trip(currentTrip));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trips;
    }

    private static void printCSV(String outputFilePath, ArrayList<Trip> tripsList) {

        if (tripsList.isEmpty()) {

            System.out.println("ARRAY IS EMPTY");
            return;
        }

        String header = new String();

        //get the whole keySet as a String with ';' for the csv data

        for (String key: tripsList.get(0).getTrip_attributes().keySet()){
            if (!key.contains("x") && !key.contains("y")) {
                header += key + ";";
            }
        }

        PrintWriter pWriter = null;
        try {
            pWriter = new PrintWriter(
                    new BufferedWriter(new FileWriter(outputFilePath)));


        } catch (IOException e) {
            e.printStackTrace();
        }

        pWriter.println(header);

        for (var trip: tripsList){
            pWriter.println(trip.toString());
        }

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

        if (tripStartsInZone && tripEndsInZone) return 0; // if trip is completly inside the zone
        if (tripStartsInZone && tripEndsInZone == false) return -1; //if trip ends outside the zone but starts inside
        if (tripStartsInZone == false && tripEndsInZone) return 1; //if trip ends within in the zone but starts outside
        return 2; //return 2 if trip is happening outside the shapefile area
    }

    private static class Trip{
        private HashMap<String,String> trip_attributes = new HashMap();

        public Trip(String personID, double distance, String mainMode, String legList, Coord deptCoord, Coord arrCoord){
            //old constructor, probably without any use by now

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
            trip_attributes.put("traveltime", computeTimeInSeconds(tripAsHashMap.get("trav_time")));

            trip_attributes.put("start_x", tripAsHashMap.get("start_x"));
            trip_attributes.put("start_y", tripAsHashMap.get("start_y"));
            trip_attributes.put("end_x", tripAsHashMap.get("end_x"));
            trip_attributes.put("end_y", tripAsHashMap.get("end_y"));

        }

        private String computeTimeInSeconds(String travelTimeInOrginalFormat){
            if(!travelTimeInOrginalFormat.contains("")) return "-1";

            String[] s = travelTimeInOrginalFormat.split(":");

            int timeInSeconds = 0;

            for (int i = 0; i < s.length; i++){
                timeInSeconds += Integer.parseInt(s[i])*60^(2-i);
            }

            return String.valueOf(timeInSeconds);
        }

        public HashMap<String, String> getTrip_attributes() {
            return trip_attributes;
        }

        @Override
        public String toString() {
            String tripAsStringForCSV = "";

            //its important, that we iterate by the order of the keys, otherwise the attributes will be in the wrong column in the csv later

            for (String key: trip_attributes.keySet()){
                if (!key.contains("x") && !key.contains("y")) {

                    if (trip_attributes.get(key).length() == 0){
                        tripAsStringForCSV += "NA"; continue;
                    }

                    tripAsStringForCSV += trip_attributes.get(key) + ";";
                }
            }

            return tripAsStringForCSV;
        }
    }
}
