package org.matsim.analysis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ShapeFile {

    public static void main(String[] args) throws IOException {

        String plansInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";
        // Get network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansInputFile);

        Path filePath = Paths.get("//net/ils/kreuschner/Shapefiles/Superblocks_Shapefiles/25percent/NOTin25percent.txt");
        Scanner scanner = new Scanner(filePath);
        List<Integer> NOTin25list = new ArrayList<>();
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                NOTin25list.add(scanner.nextInt());
            } else {
                scanner.next();
            }
        }

        Path filePath1 = Paths.get("//net/ils/kreuschner/Superblocks_Shapefiles/50percent/NOTin50percent.txt");
        Scanner scanner1 = new Scanner(filePath1);
        List<Integer> NOTin50List = new ArrayList<>();
        while (scanner1.hasNext()) {
            if (scanner1.hasNextInt()) {
                NOTin50List.add(scanner1.nextInt());
            } else {
                scanner1.next();
            }
        }

        Path filePath2 = Paths.get("//net/ils/kreuschner/Superblocks_Shapefiles/75percent/NOTin75percent.txt");
        Scanner scanner2 = new Scanner(filePath2);
        List<Integer> NOTin75list = new ArrayList<>();
        while (scanner2.hasNext()) {
            if (scanner2.hasNextInt()) {
                NOTin75list.add(scanner2.nextInt());
            } else {
                scanner2.next();
            }
        }

        Path filePath3 = Paths.get("//net/ils/kreuschner/Superblocks_Shapefiles/100percent/NOTin100percent.txt");
        Scanner scanner3 = new Scanner(filePath3);
        List<Integer> NOTin100list = new ArrayList<>();
        while (scanner3.hasNext()) {
            if (scanner3.hasNextInt()) {
                NOTin100list.add(scanner3.nextInt());
            } else {
                scanner3.next();
            }
        }

        ArrayList<Id> ResidentsList25 = new ArrayList<Id>();
        ArrayList<Id> ResidentsList50 = new ArrayList<Id>();
        ArrayList<Id> ResidentsList75 = new ArrayList<Id>();
        ArrayList<Id> ResidentsList100 = new ArrayList<Id>();

        for (int i = 1; i < 160; i++) {
            if (NOTin25list.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader1 = new ShapeFileReader();
                Collection<SimpleFeature> features1 = ShapeFileReader1.readFileAndInitialize("//net/ils/kreuschner/Superblocks_Shapefiles/S000" + i + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries1 = new HashMap<>();
                for (SimpleFeature feature1 : features1) {
                    zoneGeometries1.put((String) feature1.getAttribute("Name"),
                            (Geometry) feature1.getDefaultGeometry());
                }
                Geometry areaGeometry1 = zoneGeometries1.get("Superblock" + i);

                // Modify the car network
                for (Person person : scenario.getPopulation().getPersons().values()) {

                    Activity homeActivity1 = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                    Point linkCenterAsPoint1 = MGC.xy2Point(homeActivity1.getCoord().getX(), homeActivity1.getCoord().getY());
                    if (areaGeometry1.contains(linkCenterAsPoint1)) {
                        ResidentsList25.add(person.getId());
                    }
                }
            }
        }

        for (int i = 1; i < 160; i++) {
            if (NOTin50List.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader2 = new ShapeFileReader();
                Collection<SimpleFeature> features2 = ShapeFileReader2.readFileAndInitialize("//net/ils/kreuschner/Superblocks_Shapefiles/S000" + i + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries2 = new HashMap<>();
                for (SimpleFeature feature2 : features2) {
                    zoneGeometries2.put((String) feature2.getAttribute("Name"),
                            (Geometry) feature2.getDefaultGeometry());
                }
                Geometry areaGeometry2 = zoneGeometries2.get("Superblock" + i);

                // Modify the car network
                for (Person person : scenario.getPopulation().getPersons().values()) {

                    Activity homeActivity2 = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                    Point linkCenterAsPoint2 = MGC.xy2Point(homeActivity2.getCoord().getX(), homeActivity2.getCoord().getY());
                    if (areaGeometry2.contains(linkCenterAsPoint2)) {
                        ResidentsList50.add(person.getId());
                    }
                }
            }
        }

        for (int i = 1; i < 160; i++) {
            if (NOTin75list.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader3 = new ShapeFileReader();
                Collection<SimpleFeature> features3 = ShapeFileReader3.readFileAndInitialize("//net/ils/kreuschner/Superblocks_Shapefiles/S000" + i + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries3 = new HashMap<>();
                for (SimpleFeature feature3 : features3) {
                    zoneGeometries3.put((String) feature3.getAttribute("Name"),
                            (Geometry) feature3.getDefaultGeometry());
                }
                Geometry areaGeometry3 = zoneGeometries3.get("Superblock" + i);

                // Modify the car network
                for (Person person : scenario.getPopulation().getPersons().values()) {

                    Activity homeActivity3 = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                    Point linkCenterAsPoint3 = MGC.xy2Point(homeActivity3.getCoord().getX(), homeActivity3.getCoord().getY());
                    if (areaGeometry3.contains(linkCenterAsPoint3)) {
                        ResidentsList75.add(person.getId());
                    }
                }
            }
        }

        for (int i = 1; i < 160; i++) {
            if (NOTin100list.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader4 = new ShapeFileReader();
                Collection<SimpleFeature> features4 = ShapeFileReader4.readFileAndInitialize("//net/ils/kreuschner/Superblocks_Shapefiles/S000" + i + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries4 = new HashMap<>();
                for (SimpleFeature feature4 : features4) {
                    zoneGeometries4.put((String) feature4.getAttribute("Name"),
                            (Geometry) feature4.getDefaultGeometry());
                }
                Geometry areaGeometry4 = zoneGeometries4.get("Superblock" + i);

                // Modify the car network
                for (Person person : scenario.getPopulation().getPersons().values()) {

                    Activity homeActivity4 = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                    Point linkCenterAsPoint4 = MGC.xy2Point(homeActivity4.getCoord().getX(), homeActivity4.getCoord().getY());
                    if (areaGeometry4.contains(linkCenterAsPoint4)) {
                        ResidentsList100.add(person.getId());
                    }
                }
            }
        }

        // Insert Path to Output File Here!
        BufferedWriter bw1 = new BufferedWriter(new FileWriter("//net/ils/kreuschner/ResidentsList25.txt"));
        // traverses the collection
        for (Id s : ResidentsList25) {
            // write data
            bw1.write(String.valueOf(s));
            bw1.newLine();
            bw1.flush();
        }
        // release resource
        bw1.close();

        // Insert Path to Output File Here!
        BufferedWriter bw2 = new BufferedWriter(new FileWriter("//net/ils/kreuschner/ResidentsList50.txt"));
        // traverses the collection
        for (Id s : ResidentsList50) {
            // write data
            bw2.write(String.valueOf(s));
            bw2.newLine();
            bw2.flush();
        }
        // release resource
        bw2.close();

        // Insert Path to Output File Here!
        BufferedWriter bw3 = new BufferedWriter(new FileWriter("//net/ils/kreuschner/ResidentsList75.txt"));
        // traverses the collection
        for (Id s : ResidentsList75) {
            // write data
            bw3.write(String.valueOf(s));
            bw3.newLine();
            bw3.flush();
        }
        // release resource
        bw3.close();

        // Insert Path to Output File Here!
        BufferedWriter bw4 = new BufferedWriter(new FileWriter("//net/ils/kreuschner/ResidentsList100.txt"));
        // traverses the collection
        for (Id s : ResidentsList100) {
            // write data
            bw4.write(String.valueOf(s));
            bw4.newLine();
            bw4.flush();
        }
        // release resource
        bw4.close();
    }
}