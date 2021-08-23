package org.matsim.analysis.emissions;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * by antonstock
 */

public class RunCalculation {

    //events file
    private static final String eventsFile = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\nullfall\\berlin.emission.events.offline.xml.gz";

    private static final String outputFilePath = "C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\nullfall\\totalCO2emissions-nullfall.csv";
    public static void main(String[] args) {
        var manager = EventsUtils.createEventsManager();
        var pollutionHandler = new CalculateTotalPollution();

        manager.addHandler(pollutionHandler);

        new EmissionEventsReader(manager).readFile(eventsFile);

        printCSV(outputFilePath, pollutionHandler.counter, pollutionHandler.emissionCounter);

        System.out.println("Handled "+pollutionHandler.counter+" events.");
        System.out.println("Total emissions: "+pollutionHandler.emissionCounter);
    }

    private static void printCSV(String outputFilePath, int counter, double emissionCounter){

        PrintWriter pWriter = null;
        try {
            pWriter = new PrintWriter(
                    new BufferedWriter(new FileWriter(outputFilePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //header
        pWriter.println("number of events"+";"+"total value of CO2 emissions");
        //content
        pWriter.println(counter+";"+emissionCounter);

        pWriter.close();
    }
}

