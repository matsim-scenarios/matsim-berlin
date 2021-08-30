package org.matsim.prepare.superblocks.ScenarioA;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


public class NetworkModifierScenarioA {
    public static void main(String[] args) throws IOException {
        // Input and output files
        String networkInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        String networkOutputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/B_Coding/Coding/network-modified-scenarioA.xml.gz";

        //——————Input——————


        //Input as List - Residential
        List<Integer> ResidentialList = new ArrayList<Integer>();
        Scanner scanner = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/B_Coding/Coding/residential_street.rtf"));
        while(scanner.hasNextInt())
        {
            Integer id = scanner.nextInt();
            ResidentialList.add(id);

        }
        scanner.close();

        //Input as List - Living
        List<Integer> LivingList = new ArrayList<Integer>();
        Scanner scanner2 = new Scanner(new File("/Users/moritzkreuschner/Desktop/Master Thesis/B_Coding/Coding/living_street.rtf"));
        while(scanner2.hasNextInt())
        {
            Integer id = scanner2.nextInt();
            LivingList.add(id);

        }
        scanner2.close();

        // Get network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        // setting free speed

        // for residential
        for(int i=0; i< ResidentialList.size(); i++){

            scenario.getNetwork().getLinks().get(Id.createLinkId(ResidentialList.get(i))).setFreespeed(1.38888889);
        }
        // for living_street
        for (int i = 0; i < LivingList.size(); i++) {
            scenario.getNetwork().getLinks().get(Id.createLinkId(LivingList.get(i))).setFreespeed(1.38888889);
        }

        // Write modified network to file
        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }

}
