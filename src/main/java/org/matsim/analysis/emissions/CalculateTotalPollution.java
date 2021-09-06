package org.matsim.analysis.emissions;

import org.matsim.analysis.speedlimit.ShapeFileAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.network.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * by antonstock
 */

public class CalculateTotalPollution implements WarmEmissionEventHandler, ColdEmissionEventHandler {
    //set the emission type of interest e.g. "CO", "CO2_TOTAL", "NOx" etc.
    static final String emissionType ="CO2_TOTAL";
    private int warmEventCounter =0;
    private int coldEventCounter = 0;
    private double totalValueOnePercentSample =0;
    private String shapeFilePath = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\analysis\\shapefiles\\ring\\Berlin_S-Bahn-Ring.shp";
    //private String shapeFilePath = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\analysis\\shapefiles\\gesamt-Berlin\\Berlin_Bezirke.shp";
    private String networkFilePath = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\ring\\berlin.output_network_with_hbefa.xml";

    private HashMap<Id<Link>,Double> pollutionOnLinks = new HashMap<>();
    private ShapeFileAnalyzer shapeFileAnalyzer =  new ShapeFileAnalyzer(shapeFilePath);
    private Network network = NetworkUtils.readNetwork(networkFilePath);

    @Override
    public void handleEvent (WarmEmissionEvent warmEvent) {

        for (Map.Entry<Pollutant, Double> pollutant : warmEvent.getWarmEmissions().entrySet()) {

            var key = pollutant.getKey();
            Double pollution = pollutant.getValue();
            warmEventCounter++;
            if (emissionType.equals(key.toString())) {

                if (!shapeFileAnalyzer.isLinkInGeometry(findLinkbyId(warmEvent.getLinkId()))){
                    return;
                }

                totalValueOnePercentSample = totalValueOnePercentSample +pollution;
                pollutionOnLinks.merge(warmEvent.getLinkId(), pollution, Double::sum);
            }
        }

        if (warmEventCounter % 1000000 == 0) System.out.println("WarmEventCounter: " + warmEventCounter);
    }

    @Override
    public void handleEvent (ColdEmissionEvent coldEvent) {

        for (Map.Entry<Pollutant, Double> pollutant : coldEvent.getColdEmissions().entrySet()) {

            var key = pollutant.getKey();
            Double pollution  = pollutant.getValue();
            coldEventCounter++;
            if (emissionType.equals(key.toString())) {

                if (!shapeFileAnalyzer.isLinkInGeometry(findLinkbyId(coldEvent.getLinkId()))){
                    return;
                }

                totalValueOnePercentSample = totalValueOnePercentSample + pollution;
                pollutionOnLinks.merge(coldEvent.getLinkId(), pollution, Double::sum);
            }
        }

        if (coldEventCounter % 10000 == 0) System.out.println("ColdEventCounter: " + coldEventCounter);
    }

    public void printSummary(){

        System.out.println("+++++++Sumary of Pollution Analysis+++++++");
        System.out.println("Total number of Emission Events: " + warmEventCounter);
        System.out.println("Total emissions in g: " + totalValueOnePercentSample);
        System.out.println("Total emissions according to the HashMap: " + sumUp());
        System.out.println("totalValueOnePercentSample == sumUp() ? --> " + ((Double) totalValueOnePercentSample).equals(sumUp()));
        System.out.println("++++++++++++++++++++++++++++++++++++++++++");
    }

    public int getWarmEventCounter() {
        return warmEventCounter;
    }

    public int getColdEventCounter() {
        return coldEventCounter;
    }

    public double getTotalValueOnePercentSample() {
        return totalValueOnePercentSample;
    }

    public HashMap<Id<Link>, Double> getPollutionOnLinks() {
        return pollutionOnLinks;
    }

    private Link findLinkbyId(Id<Link> linkId){

        return network.getLinks().get(linkId);
    }

    private double sumUp(){

        double total = 0.0;

        for (Double emission: pollutionOnLinks.values()){
            total += emission;
        }

        return total;
    }

    public void setShapeFilePath(String shapeFilePath) {
        this.shapeFilePath = shapeFilePath;
        shapeFileAnalyzer.setShapeFilePath(shapeFilePath);

    }

    public void setNetworkFilePath(String networkFilePath) {
        this.networkFilePath = networkFilePath;
        this.network = NetworkUtils.readNetwork(networkFilePath);
    }
}