package org.matsim.analysis.mainmode;

import org.apache.commons.csv.CSVFormat;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

public class RunMainModeAnalysis {
private static final String workDir = "C:\\Users\\anton\\IdeaProjects\\Hausaufgaben\\HA2\\Analysis\\";
private static final String eventsfile = workDir+"output\\events.xml.gz";
private static final String outputCSV = workDir+"analysis-output.csv";

    public static void main(String[] args) {

        var manager = EventsUtils.createEventsManager();
        var handler = new org.matsim.analysis.mainmode.MainModeHandler();
        manager.addHandler(handler);
        EventsUtils.readEvents(manager, eventsfile);

        var personTrips = handler.getPersonTrips();
        var modes = personTrips.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(mode -> mode, mode -> 1, Integer::sum));

        var totalTrips = modes.values().stream()
                .mapToDouble(d -> d)
                .sum();

        try (var writer = Files.newBufferedWriter(Paths.get(outputCSV)); var printer = CSVFormat.DEFAULT.withDelimiter(',').withHeader("Mode"+";"+"Count"+";"+"Share").print(writer)) {

            for (var entry : modes.entrySet()) {
                printer.printRecord(entry.getKey()+";"+entry.getValue()+";"+entry.getValue() / totalTrips);
            }

            printer.printRecord("total"+";"+totalTrips+";"+1.0);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
