package org.matsim.analysis.emissions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * by antonstock
 */

public class CalculateTotalPollution implements WarmEmissionEventHandler, ColdEmissionEventHandler {
    //set the emission type of interest e.g. "CO", "CO2_TOTAL", "NOx" etc.
    static final String emissionType ="CO2_TOTAL";
    int counter=0;
    public double totalValueOnePercentSample =0;

    private HashMap<Id<Link>,Double> pollutionOnLinks = new HashMap<>();

    @Override
    public void handleEvent (WarmEmissionEvent warmEvent) {

        Double pollution = 0.0;

        for (Map.Entry<Pollutant, Double> pollutant : warmEvent.getWarmEmissions().entrySet()) {
                var key = pollutant.getKey();
                pollution = pollutant.getValue();
                counter++;
                if (emissionType.equals(key.toString())) {
                    totalValueOnePercentSample = totalValueOnePercentSample +pollution;
                }
        }

        pollutionOnLinks.put(warmEvent.getLinkId(),pollution);
    }
    @Override
    public void handleEvent (ColdEmissionEvent coldEvent) {

        Double pollution = 0.0;

        for (Map.Entry<Pollutant, Double> pollutant : coldEvent.getColdEmissions().entrySet()) {
            var key = pollutant.getKey();
            var value = pollutant.getValue();
            counter++;
            if (emissionType.equals(key.toString())) {
                totalValueOnePercentSample = totalValueOnePercentSample +value;
            }
        }

        pollutionOnLinks.put(coldEvent.getLinkId(),pollution);
    }

    public HashMap<Id<Link>, Double> getPollutionOnLinks() {
        return pollutionOnLinks;
    }
}