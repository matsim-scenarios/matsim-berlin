package org.matsim.prepare.superblocks.ScenarioC75;

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

public class NetworkModifierScenarioC75 {

    private static final Logger LOG = Logger.getLogger(org.matsim.prepare.superblocks.ScenarioC75.NetworkModifierScenarioC75.class);

    public static void main(String[] args) throws IOException {
        // Input and output files
        String networkInputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/berlin-v5.5-network.xml.gz";
        String networkOutputFile = "/Users/moritzkreuschner/Desktop/Master Thesis/02_Coding/git/matsim-berlin-kreuschner/superblock_input_data/Network-modifiedC75.xml.gz";

        Path filePath = Paths.get("/Users/moritzkreuschner/Desktop/Master Thesis/01_Shapefiles/Shapefiles/Superblocks_Shapefiles/75percent/NOTin75percent.txt");
        Scanner scanner = new Scanner(filePath);
        List<Integer> NOTin75list = new ArrayList<>();
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                NOTin75list.add(scanner.nextInt());
            } else {
                scanner.next();
            }
        }

        // Get network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        // Get only non-pt links
        Set<Link> set = new HashSet<>();
        for (Link link1 : scenario.getNetwork().getLinks().values()) {
            if (!link1.getAllowedModes().contains(TransportMode.pt)) {
                set.add(link1);
            }
        }
        Set<? extends Link> nonptlinks = set;

        // Loop for different shapefiles
        for (int i = 1; i < 160; i++) {

            // Superblocks that are not in the directory
            if (NOTin75list.contains(i)) {
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
                        link.setFreespeed(1.3888889);
                    }
                }

                LOG.info("Superblock " + i + " is ready");
            }
        }
        LOG.info("Finished modifying freespeed");

        // Get car subnetwork and clean it
        Scenario carScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MultimodalNetworkCleaner(carScenario.getNetwork()).run(Set.of(TransportMode.car));

        LOG.info("Finished creating and cleaning car subnetwork");

        // Write modified network to file
        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }
}
