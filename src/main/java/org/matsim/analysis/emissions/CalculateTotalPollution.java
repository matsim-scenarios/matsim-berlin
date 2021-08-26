package org.matsim.analysis.emissions;

import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import java.util.Map;

/**
 * by antonstock
 */

public class CalculateTotalPollution implements WarmEmissionEventHandler, ColdEmissionEventHandler {
    //set the emission type of interest e.g. "CO", "CO2_TOTAL", "NOx" etc.
    static final String emissionType ="CO2_TOTAL";
    int counter=0;
    public double totalValue=0;

    @Override
    public void handleEvent (WarmEmissionEvent warmEvent) {
        for (Map.Entry<Pollutant, Double> pollutant : warmEvent.getWarmEmissions().entrySet()) {
                var key = pollutant.getKey();
                var value = pollutant.getValue();
                counter++;
                if (emissionType.equals(key.toString())) {
                    totalValue = totalValue+value;
                }
        }
    }
    @Override
    public void handleEvent (ColdEmissionEvent coldEvent) {
        for (Map.Entry<Pollutant, Double> pollutant : coldEvent.getColdEmissions().entrySet()) {
            var key = pollutant.getKey();
            var value = pollutant.getValue();
            counter++;
            if (emissionType.equals(key.toString())) {
                totalValue = totalValue+value;
            }
        }
    }
}