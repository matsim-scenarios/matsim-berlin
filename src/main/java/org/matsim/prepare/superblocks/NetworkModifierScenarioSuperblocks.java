package org.matsim.prepare.superblocks;

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


public class NetworkModifierScenarioSuperblocks {

    private static final Logger LOG = Logger.getLogger(NetworkModifierScenarioSuperblocks.class);

    public static void main(String[] args) throws IOException {
        // Input and output files
        String networkInputFile = "/Users/moritzkreuschner/Desktop/MasterThesis/coding/berlin-v5.5-network.xml.gz";
        String networkOutputFile = "/Users/moritzkreuschner/Desktop/MasterThesis/coding/modified-networks/A/Network-modifiedA25.xml.gz";
        String shapeFileDirectory = "/Users/moritzkreuschner/Desktop/MasterThesis/coding/shapefiles/superblocks/";

        Path filePath = Paths.get("/Users/moritzkreuschner/Desktop/MasterThesis/coding/number_superblocks/NOTin25percent.txt");
        int nrOfSuperBlocks = 160;

        Scanner scanner = new Scanner(filePath);
        List<Integer> NOTinlist = new ArrayList<>();
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) {
                NOTinlist.add(scanner.nextInt());
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
        for (int i = 0; i < nrOfSuperBlocks; i++) {

            // Superblocks that are not in list
            if (NOTinlist.contains(i)) {
                continue;
            } else {

                // Store relevant area of city as geometry
                ShapeFileReader ShapeFileReader = new ShapeFileReader();
                Collection<SimpleFeature> features = ShapeFileReader.readFileAndInitialize(shapeFileDirectory + "S000" + (i + 1) + ".shp");
                //continue;
                Map<String, Geometry> zoneGeometries = new HashMap<>();
                for (SimpleFeature feature : features) {
                    zoneGeometries.put((String) feature.getAttribute("Name"),
                            (Geometry) feature.getDefaultGeometry());
                }
                Geometry areaGeometry = zoneGeometries.get("Superblock" + (i + 1));

                // Modify the car network
                for (Link link : nonptlinks) {

                    Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());

                    if (areaGeometry.contains(linkCenterAsPoint)) {
                        // Approach A
                        //link.setFreespeed(1.3888889);
                        // Approach B
                        Set<String> modes = new HashSet<String>(link.getAllowedModes());
                        modes.remove(TransportMode.car);
                        link.setAllowedModes(modes);
                    }
                }

                LOG.info("Superblock " + (i + 1) + " is ready");
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
