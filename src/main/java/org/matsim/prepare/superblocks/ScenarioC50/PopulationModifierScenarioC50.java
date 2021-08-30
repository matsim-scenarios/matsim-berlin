package org.matsim.prepare.superblocks.ScenarioC50;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PopulationModifierScenarioC50 {

    public static void main(String[] args) throws IOException {
        // Input and output files
        String plansInputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/E_Shapefiles/Shapefiles/berlin-v5.5-10pct.plans.xml.gz";
        String plansOutputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/B_Coding/Coding/git/plans-modified-carSuperblocks50.xml.gz";


        // Get population
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansInputFile);

        // Loop for different shapefiles
        for (int i = 1; i < 160; i++) {

            // Superblocks that are not in the directory
            //if(i==3){
            //    continue;
            //}

            // Store relevant area of city as geometry
            ShapeFileReader ShapeFileReader = new ShapeFileReader();
            Collection<SimpleFeature> features = ShapeFileReader.readFileAndInitialize("/Users/moritzkreuschner/Desktop/Master Thesis/E_Shapefiles/Shapefiles/Superblocks_Shapefiles/25percent/S000" + i + ".shp");
            //continue;
            Map<String, Geometry> zoneGeometries = new HashMap<>();
            for (SimpleFeature feature : features) {
                zoneGeometries.put((String) feature.getAttribute("Name"),
                        (Geometry) feature.getDefaultGeometry());
            }
            Geometry areaGeometry = zoneGeometries.get("Superblock" + i);



            //List
            ArrayList<String> personSuperblockIDsList = new ArrayList<>();
            //

            // Substitute car mode by carSuperblock mode for people inside relevant area
            for (Person person : scenario.getPopulation().getPersons().values()) {
                // for only Residents
                Activity homeActivity = (Activity) person.getPlans().get(0).getPlanElements().get(0);
                Point homeActAsPoint = MGC.xy2Point(homeActivity.getCoord().getX(), homeActivity.getCoord().getY());

                //freightAgent !
                if (areaGeometry.contains(homeActAsPoint)&&(!person.getId().toString().contains("freight"))) {

                    person.getAttributes().putAttribute("subpopulation", "personSuperblock");

                    //print AgentID to List
                    personSuperblockIDsList.add(person.getId().toString());

                    for (PlanElement pe : person.getPlans().get(0).getPlanElements()) {
                        if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            if (leg.getMode().equals(TransportMode.car)) {
                                leg.setMode("carSuperblock");
                                leg.getAttributes().putAttribute("routingMode", "carSuperblock");
                            }
                            if (leg.getMode().equals(TransportMode.walk) && leg.getAttributes().getAttribute("routingMode").equals(TransportMode.car)) {
                                leg.getAttributes().putAttribute("routingMode", "carSuperblock");
                            }
                        } else if (pe instanceof Activity) {
                            Activity activity = (Activity) pe;
                            if (activity.getType().equals("car interaction")) {
                                activity.setType("carSuperblock interaction");
                            }
                        }
                    }
                }
            }

            //List Txt
            //print the AgentIDs of No Car Zone
            // Insert Path to Output File Here!
            BufferedWriter bw = new BufferedWriter(new FileWriter("/Users/moritzkreuschner//Desktop/Master Thesis/B_Coding/Coding/git/POPULATIONS/50percent/personIDsList_Superblock" + i + ".txt"));
            // traverses the collection
            for (String s : personSuperblockIDsList) {
                // write data
                bw.write(s);
                bw.newLine();
                bw.flush();
            }
            // release resource
            bw.close();
            //

            // Write modified population to file
            PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
            populationWriter.write(plansOutputFile);
        }
    }
}
