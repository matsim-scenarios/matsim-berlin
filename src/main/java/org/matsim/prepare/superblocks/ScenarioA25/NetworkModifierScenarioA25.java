package org.matsim.prepare.superblocks.ScenarioA25;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NetworkModifierScenarioA25 {

    private static final Logger LOG = Logger.getLogger(org.matsim.prepare.superblocks.ScenarioA25.NetworkModifierScenarioA25.class);

    public static void main(String[] args) throws IOException {
        // Input and output files
        String networkInputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/berlin-v5.5-network.xml.gz";
        String networkOutputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/02_Coding/git/matsim-berlin-kreuschner/superblock_input_data/Network-modifiedA25.xml.gz";

        Path filePath = Paths.get("/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/25percent/NOTin25percent.txt");
        Scanner scanner = new Scanner(filePath);
        List<Integer> NOTin25list = new ArrayList<>();
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                NOTin25list.add(scanner.nextInt());
            } else {
                scanner.next();
            }
        }

        // Get network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        Set<Link> set = new HashSet<>();
        for (Link link1 : scenario.getNetwork().getLinks().values()) {
            if (!link1.getAllowedModes().contains(TransportMode.pt)) {
                set.add(link1);
            }
        }
        Set<? extends Link> nonptlinks = set;

        // Loop for different shapefiles
        // Superblocks that are not in the directory
        for (int i = 1; i < 160; i++) {
            if (NOTin25list.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader = new ShapeFileReader();
                Collection<SimpleFeature> features = ShapeFileReader.readFileAndInitialize("/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/S000" + i + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries = new HashMap<>();
                for (SimpleFeature feature : features) {
                    zoneGeometries.put((String) feature.getAttribute("Name"),
                            (Geometry) feature.getDefaultGeometry());
                }
                Geometry areaGeometry = zoneGeometries.get("Superblock" + i);

                // Modify the car network
                for (Link link : nonptlinks) {

                    Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
                    if (areaGeometry.contains(linkCenterAsPoint)) {
                        link.setFreespeed(0.00001);
                        link.setCapacity(0);
                    }
                }

                LOG.info("Superblock " + i + " is ready");
            }
        }

        LOG.info("Finished modifying freespeed and capacity");

        // Get car subnetwork and clean it
        Scenario carScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MultimodalNetworkCleaner(carScenario.getNetwork()).run(Set.of(TransportMode.car));
        /*TransportModeNetworkFilter transportModeNetworkFilterCar = new TransportModeNetworkFilter(scenario.getNetwork());
        transportModeNetworkFilterCar.filter(carScenario.getNetwork(), new HashSet<>(Arrays.asList(TransportMode.car)));
        (new NetworkCleaner()).run(carScenario.getNetwork());*/

        LOG.info("Finished cleaning car subnetwork");

        // Write modified network to file
        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);

    }
}
