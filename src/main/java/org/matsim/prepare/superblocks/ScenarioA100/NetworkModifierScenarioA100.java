package org.matsim.prepare.superblocks.ScenarioA;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

import java.util.*;

public class NetworkModifierScenarioB100 {

    private static final Logger LOG = Logger.getLogger(NetworkModifierScenarioB100.class);

    public static void main(String[] args) {
        // Input and output files
        String networkInputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/E_Shapefiles/Shapefiles/berlin-v5.5-network.xml.gz";
        String networkOutputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/B_Coding/Coding/git/matsim-berlin-kreuschner/superblock_input_data/Input_B100/Network/Network-modifiedB100.xml.gz";



        // Get network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        // Get pt subnetwork
        Scenario ptScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransportModeNetworkFilter transportModeNetworkFilterPt = new TransportModeNetworkFilter(scenario.getNetwork());
        transportModeNetworkFilterPt.filter(ptScenario.getNetwork(), new HashSet<>(Arrays.asList(TransportMode.pt)));

        // Loop for different shapefiles
        for (int i = 1; i < 160; i++) {


            // Store relevant area of city as geometry


            ShapeFileReader ShapeFileReader = new ShapeFileReader();
            Collection<SimpleFeature> features = ShapeFileReader.readFileAndInitialize("/Users/moritzkreuschner/Desktop/Master Thesis/E_Shapefiles/Shapefiles/Superblocks_Shapefiles/S000" + i + ".shp");
            //continue;
            Map<String, Geometry> zoneGeometries = new HashMap<>();
            for (SimpleFeature feature : features) {
                zoneGeometries.put((String) feature.getAttribute("Name"),
                        (Geometry) feature.getDefaultGeometry());
            }
            Geometry areaGeometry = zoneGeometries.get("Superblock" + i);


            // Modify the car network
            for (Link link : scenario.getNetwork().getLinks().values()) {
                Set<String> allowedModesBefore = link.getAllowedModes();
                Set<String> allowedModesAfter = new HashSet<>();

                Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());

                for (String mode : allowedModesBefore) {
                    if (mode.equals(TransportMode.car)) {
                        allowedModesAfter.add(TransportMode.bike);
                        allowedModesAfter.add(TransportMode.walk);
                        allowedModesAfter.add(TransportMode.car);
                        if (areaGeometry.contains(linkCenterAsPoint)) {
                            allowedModesAfter.add(TransportMode.bike);
                            allowedModesAfter.add(TransportMode.walk);
                            allowedModesAfter.remove(TransportMode.car);
                        }
                    } else {
                        allowedModesAfter.add(mode);
                    }
                }
                link.setAllowedModes(allowedModesAfter);

            }

            LOG.info("Superblock " + i + " is ready");
        }

        LOG.info("Finished modifying car and freespeed");

        // Get car subnetwork and clean it
        Scenario carScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransportModeNetworkFilter transportModeNetworkFilterCar = new TransportModeNetworkFilter(scenario.getNetwork());
        transportModeNetworkFilterCar.filter(carScenario.getNetwork(), new HashSet<>(Arrays.asList(TransportMode.car)));
        (new NetworkCleaner()).run(carScenario.getNetwork());
        LOG.info("Finished creating and cleaning car subnetwork");


        // Add pt back into the other network
        // Note: Customized attributes are not considered here
        NetworkFactory factory = carScenario.getNetwork().getFactory();
        for (Node node : ptScenario.getNetwork().getNodes().values()) {
            Node node2 = factory.createNode(node.getId(), node.getCoord());
            carScenario.getNetwork().addNode(node2);
        }
        for (Link link : ptScenario.getNetwork().getLinks().values()) {
            Node fromNode = carScenario.getNetwork().getNodes().get(link.getFromNode().getId());
            Node toNode = carScenario.getNetwork().getNodes().get(link.getToNode().getId());
            Link link2 = factory.createLink(link.getId(), fromNode, toNode);
            link2.setAllowedModes(link.getAllowedModes());
            link2.setCapacity(link.getCapacity());
            link2.setFreespeed(link.getFreespeed());
            link2.setLength(link.getLength());
            link2.setNumberOfLanes(link.getNumberOfLanes());
            carScenario.getNetwork().addLink(link2);
        }
        LOG.info("Finished merging pt network layer back into network");


        // Write modified network to file
        NetworkWriter writer = new NetworkWriter(carScenario.getNetwork());
        writer.write(networkOutputFile);

    }
}
