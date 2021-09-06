package org.matsim.prepare.emission;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * by antonstock
 */

public class AddRoadtype {
    public void run() {
        final String workingDirectory="C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\";
        final String runID="ring\\";
        final String networkInputPath=workingDirectory+runID+"berlin-30kmh-ring.output_network.xml.gz";
        final String networkOutputPath=workingDirectory+runID+"berlin.output_network_with_hbefa.xml";
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkInputPath);
        for (Link link : network.getLinks().values()) {
            if (LinkIsPTOnly(link)) {
                link.getAttributes().putAttribute("hbefa_road_type", "publictransport");
            } else {
                double freespeed = link.getFreespeed();
                if (freespeed <= 4.2) { //streets with 30kph speed limit have a free speed of ~4,167m/s
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");

                } else if (freespeed <= 7.0) { //streets with a 50kph speed limit have a free speed of ~6,945m/s
                    //urban main roads must be differentiated by their number of lanes
                    double lanes = link.getNumberOfLanes();
                    if (lanes <= 1.0) {
                        link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
                    } else if (lanes <= 2.0) {
                        link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
                    } else if (lanes > 2.0) {
                        link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
                    } else {
                        throw new RuntimeException("Number of lanes of link " + link.getId() + " is not defined properly.");
                    }
                } else if (freespeed <= 17.0) { //streets with 60kph speed limit have a free speed of ~16,68m/s
                    //urban main roads must be differentiated by their number of lanes
                    double lanes = link.getNumberOfLanes();
                    if (lanes <= 1.0) {
                        link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
                    } else if (lanes <= 2.0) {
                        link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
                    } else if (lanes > 2.0) {
                        link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
                    } else {
                        throw new RuntimeException("Number of lanes of link " + link.getId() + " is not defined properly.");
                    }
                } else if (freespeed <= 20.0) { //streets with 70kph speed limit have a free speed of ~19,4m/s
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
                } else if (freespeed <= 23.0) { //80kph
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
                } else if (freespeed <= 28.0) { //100kph
                    link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/100");
                } else if (freespeed <= 33.5) { //120kph
                    link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/120");
                } else if (freespeed > 33.5) { //faster than 120kph
                    link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
                } else {
                    throw new RuntimeException("Link " + link.getId() + " was not considered because freespeed is " + link.getFreespeed());
                }
            }
        }
        System.out.println("writing...");
        new NetworkWriter(network).write(networkOutputPath);
    }
    public static void main(String[] args) {
        new AddRoadtype().run();
    }
    //made by hzoerner: github.com/hzoerner
    private static boolean LinkIsPTOnly (Link link){
        String linkID = link.getId().toString();
        return linkID.startsWith("pt");
    }
}
