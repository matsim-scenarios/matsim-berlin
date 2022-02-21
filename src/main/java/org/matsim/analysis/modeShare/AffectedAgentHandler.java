package org.matsim.analysis.modeShare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AffectedAgentHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {
        public Set<Id> SuperblockslinkIDlist = new HashSet<Id>();
        public Set<Id> residentAndWorkerAgentIDlist = new HashSet<Id>();
        public Set<Id> affectedAgentIDlist = new HashSet<Id>();

        public AffectedAgentHandler(ArrayList<String> outputFile) throws IOException{

            Scanner scanner3 = new Scanner(new File("//net/ils/kreuschner/analysis/A25/ModifiedLinksInSuperblocks25.txt"));
            while(scanner3.hasNextLine()) {
                String id = scanner3.nextLine();
                SuperblockslinkIDlist.add(Id.createLinkId(id));
            }
            scanner3.close();
        }



         @Override
        public void handleEvent(PersonDepartureEvent event){
            Id person = event.getPersonId();
            if(SuperblockslinkIDlist.contains(event.getLinkId())){
                residentAndWorkerAgentIDlist.add(person);
            }
        }

        @Override
        public void handleEvent(PersonArrivalEvent event){
            Id person = event.getPersonId();
            if(SuperblockslinkIDlist.contains(event.getLinkId())){
                residentAndWorkerAgentIDlist.add(person);
            }
        }

        @Override
        public void handleEvent(ActivityEndEvent event){
            Id linkID = event.getLinkId();
            if (SuperblockslinkIDlist.contains(linkID)){
                residentAndWorkerAgentIDlist.add(event.getPersonId());
            }
        }

        @Override
        public void handleEvent(LinkEnterEvent event){
            Id linkID = event.getLinkId();
            if(SuperblockslinkIDlist.contains(linkID)){
                affectedAgentIDlist.add(event.getVehicleId());
            }
        }

        public void printResults(String outputFile1, String outputFile2) throws IOException {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile1));
            BufferedWriter writer2 = new BufferedWriter(new FileWriter(outputFile2));
            System.out.println("******************************************");
            System.out.println("Total resident and worker agents: " + residentAndWorkerAgentIDlist.size());
            System.out.println("******************************************");
            System.out.println("******************************************");
            System.out.println("Total affected vehicles: " + affectedAgentIDlist.size());
            System.out.println("******************************************");

            for (Id person : residentAndWorkerAgentIDlist) {
                writer.write(person.toString()+ "\n");
            }
            writer.close();
            for (Id person : affectedAgentIDlist) {
                writer2.write(person.toString()+ "\n");
            }
            writer2.close();
        }

 }

