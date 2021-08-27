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
    private int counter=0;
    private double totalValueOnePercentSample =0;
    private String shapeFilePath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Bezirke_-_Berlin-shp\\Berlin_Bezirke.shp";
    private String networkFilePath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Berlin_Scenario_output\\berlin-v5.5-1p" +
            "ct.output_network.xml.gz";

    private HashMap<Id<Link>,Double> pollutionOnLinks = new HashMap<>();
    private ShapeFileAnalyzer shapeFileAnalyzer =  new ShapeFileAnalyzer(shapeFilePath);
    private Network network = NetworkUtils.readNetwork(networkFilePath);

    @Override
    public void handleEvent (WarmEmissionEvent warmEvent) {

        if (!shapeFileAnalyzer.isLinkInGeometry(findLinkbyId(warmEvent.getLinkId()))){
            return;
        }

        for (Map.Entry<Pollutant, Double> pollutant : warmEvent.getWarmEmissions().entrySet()) {

            var key = pollutant.getKey();
            Double pollution = pollutant.getValue();
            counter++;
            if (emissionType.equals(key.toString())) {
                totalValueOnePercentSample = totalValueOnePercentSample +pollution;

                pollutionOnLinks.merge(warmEvent.getLinkId(), pollution, Double::sum);
            }
        }

        if (counter % 1000 == 0) System.out.println("Counter: " + counter);
    }

    @Override
    public void handleEvent (ColdEmissionEvent coldEvent) {

        if (!shapeFileAnalyzer.isLinkInGeometry(findLinkbyId(coldEvent.getLinkId()))){
            return;
        }

        for (Map.Entry<Pollutant, Double> pollutant : coldEvent.getColdEmissions().entrySet()) {

            var key = pollutant.getKey();
            Double pollution  = pollutant.getValue();
            counter++;
            if (emissionType.equals(key.toString())) {
                totalValueOnePercentSample = totalValueOnePercentSample + pollution;

                pollutionOnLinks.merge(coldEvent.getLinkId(), pollution, Double::sum);
            }
        }

        if (counter % 1000 == 0) System.out.println("Counter: " + counter);
    }

    public void printSummary(){

        System.out.println("+++++++Sumary of Pollution Analysis+++++++");
        System.out.println("Total number of Emission Events: " + counter);
        System.out.println("Total emissions in g: " + totalValueOnePercentSample);
        System.out.println("Total emissions according to the HashMap: " + sumUp());
        System.out.println("totalValueOnePercentSample == sumUp() ? --> " + ((Double) totalValueOnePercentSample).equals(sumUp()));
        System.out.println("++++++++++++++++++++++++++++++++++++++++++");
    }

    public int getCounter() {
        return counter;
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