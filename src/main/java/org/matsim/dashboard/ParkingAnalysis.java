package org.matsim.dashboard;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@CommandLine.Command(
        name = "parking"
)
@CommandSpec(
        requireEvents = true,
        produces = {
                "parking_search_times_density.csv",
                "parking_search_times.csv",
                "parking_search_times_per_link.csv",
                "total_parking_search_time_per_link.csv",
                "parking_search_times_per_person.csv",
                "total_parking_search_time_per_person.csv",
                "parking_search_times_time_of_day.csv"
        },
        group = "parking"
)
public class ParkingAnalysis implements MATSimAppCommand {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ParkingAnalysis.class);

    @CommandLine.Mixin
    private final InputOptions input = InputOptions.ofCommand(ParkingAnalysis.class);

    @CommandLine.Mixin
    private final OutputOptions output = OutputOptions.ofCommand(ParkingAnalysis.class);

    @Override
    public Integer call() throws Exception {
        log.info("Running parking analysis");

        EventsManager eventsManager = EventsUtils.createEventsManager();
        ParkingEventHandler handler = new ParkingEventHandler();

        eventsManager.addHandler(handler);
        eventsManager.initProcessing();
        EventsUtils.readEvents(eventsManager, input.getEventsPath());
        eventsManager.finishProcessing();

        writeCsv(handler);

        log.info("Parking analysis finished");

        return 0;
    }

    public static void main(String[] args) {
        new ParkingAnalysis().execute(args);
    }

    private void writeCsv(ParkingEventHandler handler) {

        Map<Id<Person>, Double> totalParkingSearchTimePerPerson = new HashMap<>();
        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("total_parking_search_time_per_person.csv").toString()
            );
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader(new String[]{"person_id", "total_parking_search_time"}).build());
            for (Map.Entry<Id<Person>, Double> entry : handler.totalParkingSearchTimePerPerson().entrySet()) {
                csvPrinter.printRecord(entry.getKey(), entry.getValue());
            }
            csvPrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("parking_search_times_per_link.csv").toString()
            );

            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader("link_id", "search_time")
                    .build()
            );

            for (Map.Entry<Id<Link>, List<Double>> entry : handler.parkingSearchTimesPerLink().entrySet()) {
                Id<Link> linkId = entry.getKey();
                for (Double searchTime : entry.getValue()) {
                    csvPrinter.printRecord(linkId, searchTime);
                }
            }
            csvPrinter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("total_parking_search_time_per_link.csv").toString()
            );
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader(new String[]{"link_id", "total_parking_search_time"}).build());
            for (Map.Entry<Id<Link>, Double> entry : handler.totalParkingSearchTimePerLink.entrySet()) {
                csvPrinter.printRecord(entry.getKey(), entry.getValue());
            }
            csvPrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //write CSV with header search_time
        ArrayList<Double> searchTimes = handler.parkingSearchTimesList();
        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("parking_search_times.csv").toString()
            );
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader(new String[]{"search_time"}).build());
            for (Double searchTime : searchTimes) {
                csvPrinter.printRecord(searchTime);
            }
            csvPrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<Id<Person>, List<Double>> parkingSearchTimePerPerson = handler.parkingSearchTimesPerPerson();
        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("parking_search_times_per_person.csv").toString()
            );
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader(new String[]{"person_id", "search_time"}).build());
            for (Map.Entry<Id<Person>, List<Double>> entry : parkingSearchTimePerPerson.entrySet()) {
                for (Double searchTime : entry.getValue()) {
                    csvPrinter.printRecord(entry.getKey(), searchTime);
                }
            }
            csvPrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<Double, Double> density = handler.parkingSearchTimesDensity();
        // write CSV with header search_time, density
        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("parking_search_times_density.csv").toString()
            );
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader(new String[]{"search_time", "density"}).build());
            for (Map.Entry<Double, Double> entry : density.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                csvPrinter.printRecord(entry.getKey(), entry.getValue());
            }
            csvPrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //write out CSV with header time_of_day, person_id, search_time
        Collection<ParkingEventHandler.ParkingEvent> searchWithTimeOfDay = handler.parkingSearchAtTimeOfDay();
        try {
            BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(
                    output.getPath().resolve("parking_search_times_time_of_day.csv").toString()
            );
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
                    .setDelimiter(";")
                    .setHeader(new String[]{"time_of_day", "person_id", "search_time"}).build());
            for (ParkingEventHandler.ParkingEvent event : searchWithTimeOfDay) {
                csvPrinter.printRecord(event.time, event.personId, event.searchTime);
            }
            csvPrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
