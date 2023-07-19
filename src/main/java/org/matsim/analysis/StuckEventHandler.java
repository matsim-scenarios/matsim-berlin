package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StuckEventHandler implements PersonStuckEventHandler {

    private final List<StuckEventsData> stuckEventsData;

    public StuckEventHandler(List<StuckEventsData> stuckEventsData) {
        this.stuckEventsData = stuckEventsData;
    }

    public static void main (String args []) throws IOException {

        EventsManager manager = EventsUtils.createEventsManager();
        List<StuckEventsData> listOfStuckEvents = new ArrayList<>();
        manager.addHandler(new StuckEventHandler(listOfStuckEvents));
        manager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(manager);
        matsimEventsReader.readFile("/Users/gregorr/Desktop/Test/TryToWriteAPaper/berlinMobBudget/output/mobBUdget1000_secondPlanAndPunishmentForStuckEvents/mobBUdget1000_secondPlanAndPunishmentForStuckEvents.output_events.xml.gz");
        manager.finishProcessing();
        writeResults("/Users/gregorr/Desktop/Test/TryToWriteAPaper/berlinMobBudget/output/mobBUdget1000_secondPlanAndPunishmentForStuckEvents/", listOfStuckEvents);
    }


    @Override
    public void handleEvent(PersonStuckEvent personStuckEvent) {
        StuckEventsData stuckEvent = new StuckEventsData(personStuckEvent.getPersonId(), personStuckEvent.getLinkId());
        stuckEventsData.add(stuckEvent);
    }

    @Override
    public void reset(int iteration) {
        PersonStuckEventHandler.super.reset(iteration);
    }


    record StuckEventsData (Id<Person> personId, Id<Link> linkId) {}


    private static void writeResults(String output, List<StuckEventsData> stuckEventsData) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter(output + "/stuckEvents.tsv");
        writer.write("personId"+ "\t" +"linkId" );
        writer.newLine();
        for (int i = 0; i < stuckEventsData.size(); i++) {
            StuckEventsData se = stuckEventsData.get(i);
            writer.write(se.personId + "\t" + se.linkId);
            writer.newLine();
        }
        writer.close();
        System.out.println("Nr of stuck Agents: " + stuckEventsData.size());
    }

}