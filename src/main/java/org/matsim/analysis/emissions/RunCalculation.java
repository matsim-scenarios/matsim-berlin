package org.matsim.analysis.emissions;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.events.EventsUtils;

import java.io.IOException;


/**
 * by antonstock and hzoerner
 */

public class RunCalculation {

    //events file
    private static final String eventsFile = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\gesamt-Berlin\\berlin-30kmh-gesBLN.emission.events.offline.xml.gz";

    public static void main(String[] args) throws IOException {
        var manager = EventsUtils.createEventsManager();
        var pollutionHandler = new CalculateTotalPollution();
        //use the setter methods to set filepaths to the shapefile and network data!

        manager.addHandler(pollutionHandler);

        new EmissionEventsReader(manager).readFile(eventsFile);

        var linkOfInterest = Id.createLinkId("160133");

        pollutionHandler.printSummary();

        System.out.println("CO2_TOTAL on link 160133: "+pollutionHandler.getPollutionOnLinks().get(linkOfInterest));
    }
}