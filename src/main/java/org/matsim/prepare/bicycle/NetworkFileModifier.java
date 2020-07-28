package org.matsim.prepare.bicycle;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NetworkFileModifier {

    public static void main(String[] args) {
        String inputNetworkFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        String outputNetworkFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network-with-bicycles.xml.gz";

        String BICYCLE = "bicycle";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        networkReader.readFile(inputNetworkFile);

        // Add bicycles to all link which can be traversed by cars
        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (link.getAllowedModes().contains(TransportMode.car)) {
                Set<String> updatedAllowedModes = new HashSet<>(link.getAllowedModes());
                updatedAllowedModes.add(BICYCLE);
                link.setAllowedModes(updatedAllowedModes);
            }
        }

        // Add bicycle infrastructure speed factor for all links
        scenario.getNetwork().getLinks().values().parallelStream()
                .filter(link -> link.getAllowedModes().contains(BICYCLE))
                .forEach(link -> link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.0));

        NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
        networkWriter.write(outputNetworkFile);
    }
}