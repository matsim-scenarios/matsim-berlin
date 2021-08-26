package org.matsim.analysis.emissions;

import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.events.EventsUtils;

import java.io.IOException;


/**
 * by antonstock
 */

public class RunCalculation {

    //events file
    private static final String eventsFile = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\nullfall\\berlin.emission.events.offline.xml.gz";

    public static void main(String[] args) throws IOException {
        var manager = EventsUtils.createEventsManager();
        var pollutionHandler = new CalculateTotalPollution();

        manager.addHandler(pollutionHandler);

        new EmissionEventsReader(manager).readFile(eventsFile);
        System.out.println(pollutionHandler.totalValue);

        System.out.println("CO2_TOTAL on link 160133: "+pollutionHandler.getPollutionOnLinks().get("160133"));
    }
}