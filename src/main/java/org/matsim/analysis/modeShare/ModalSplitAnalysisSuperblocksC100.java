package org.matsim.analysis.modeShare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;
import java.util.ArrayList;

public class ModalSplitAnalysisSuperblocksC100 {

    public static void main(String[] args) throws IOException {

        //-------------For Input and Output Files-------------

        // PlanC100
        String InputFile_planFile = "//net/ils/kreuschner/output/output_ScenarioC100/ScenarioC100.output_plans.xml";
        String OutputFile_Results = "//net/ils/kreuschner/analysis/C100/ScenarioC100_Results.txt";

        //For Input and Output Files-------------//

        //——————Input——————
        String Superblocks = "//net/ils/kreuschner/analysis/C100/ModifiedLinksInSuperblocks100.txt";
        //——————Input——————

        //——————Input As List——————
        // Superblocks
        BufferedReader bfrSuperblocks = new BufferedReader(new FileReader(Superblocks));
        ArrayList<String> SuperblocksList = new ArrayList<>();
        while (true) {
            String s = bfrSuperblocks.readLine();
            if (s == null) {
                break;
            }
            SuperblocksList.add(s);
        }
        bfrSuperblocks.close();
        System.out.println(SuperblocksList);
        System.out.println(SuperblocksList.size());

        String PersonInternalIDs = "//net/ils/kreuschner/analysis/C100/Person_Superblock_C100.txt";
        // ResindentsIDs
        BufferedReader bfrpersonInternalIDs = new BufferedReader(new FileReader(PersonInternalIDs));
        ArrayList<String> personInternalIDsList = new ArrayList<>();
        while (true) {
            String s = bfrpersonInternalIDs.readLine();
            if (s == null) {
                break;
            }
            personInternalIDsList.add(s);
        }
        bfrpersonInternalIDs.close();
        System.out.println(personInternalIDsList);
        System.out.println(personInternalIDsList.size());


        //************************for AffectedAgentHandler
        String inputFile = "//net/ils/kreuschner/output/output_ScenarioC100/ScenarioC100.output_events.xml.gz";
        String outputFile1 = "//net/ils/kreuschner/analysis/C100/resident_and_worker_Agents_C100.txt";
        String outputFile2 = "//net/ils/kreuschner/analysis/C100/affected_Vehicles_C100.txt";


        EventsManager eventsManager = EventsUtils.createEventsManager();

        // SimpleEventHandler eventHandler = new SimpleEventHandler();
        AffectedAgentHandler eventHandler = new AffectedAgentHandler(SuperblocksList);
        eventsManager.addHandler(eventHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(inputFile);

        eventHandler.printResults(outputFile1, outputFile2);
        //************************
        // AffectedAgentsIDs
        BufferedReader bfrAffectedAgentsIDs = new BufferedReader(new FileReader(outputFile2));
        ArrayList<String> AffectedAgentsIDsList = new ArrayList<>();
        while (true) {
            String s = bfrAffectedAgentsIDs.readLine();
            if (s == null) {
                break;
            }
            AffectedAgentsIDsList.add(s);
        }
        bfrAffectedAgentsIDs.close();
        System.out.println(AffectedAgentsIDsList);
        System.out.println(AffectedAgentsIDsList.size());

        // Get population
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(InputFile_planFile);

        //Counter for AffectedResidents
        double carCounter_AffectedResidents = 0;
        double freightCounter_AffectedResidents = 0;
        double rideCounter_AffectedResidents = 0;
        double bicycleCounter_AffectedResidents = 0;
        double walkCounter_AffectedResidents = 0;
        double ptCounter_AffectedResidents = 0;
        //Counter for Residents
        double carCounter_Residents = 0;
        double freightCounter_Residents = 0;
        double rideCounter_Residents = 0;
        double bicycleCounter_Residents = 0;
        double walkCounter_Residents = 0;
        double ptCounter_Residents = 0;
        //Counter for NoneResidentsAffectedAgents
        double carCounter_NoneResidentsAffectedAgents = 0;
        double freightCounter_NoneResidentsAffectedAgents = 0;
        double rideCounter_NoneResidentsAffectedAgents = 0;
        double bicycleCounter_NoneResidentsAffectedAgents = 0;
        double walkCounter_NoneResidentsAffectedAgents = 0;
        double ptCounter_NoneResidentsAffectedAgents = 0;
        //Counter for AffectedAgents
        double carCounter_AffectedAgents = 0;
        double freightCounter_AffectedAgents = 0;
        double rideCounter_AffectedAgents = 0;
        double bicycleCounter_AffectedAgents = 0;
        double walkCounter_AffectedAgents = 0;
        double ptCounter_AffectedAgents = 0;
        //Counter for OtherAgents
        double carCounter_OtherAgents = 0;
        double freightCounter_OtherAgents = 0;
        double rideCounter_OtherAgents = 0;
        double bicycleCounter_OtherAgents = 0;
        double walkCounter_OtherAgents = 0;
        double ptCounter_OtherAgents = 0;

        double AffectedResidentsCounter = 0;
        ArrayList<String> AffectedResidentsIDsList = new ArrayList<>();
        double ResidentsCounter = 0;
        //ArrayList<String> ResidentsIDsList = new ArrayList<>();
        double NoneResidentsAffectedAgentsCounter = 0;
        ArrayList<String> NoneResidentsAffectedAgentsIDsList = new ArrayList<>();
        double AffectedAgentsCounter = 0;
        //ArrayList<String> AffectedAgentsIDsList = new ArrayList<>();
        double OtherAgentsCounter = 0;
        ArrayList<String> OtherAgentsIDsList = new ArrayList<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {

            if (personInternalIDsList.contains(person.getId().toString()) && (!AffectedAgentsIDsList.contains(person.getId().toString()))) {
                for (Plan pa : person.getPlans()) {
                    for (PlanElement pe : pa.getPlanElements()) {
                        if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            if (leg.getMode().equals(TransportMode.car)) {
                                carCounter_Residents++;
                                carCounter_AffectedAgents++;
                            } else if (leg.getMode().equals("freight")) {
                                freightCounter_Residents++;
                                freightCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.ride)) {
                                rideCounter_Residents++;
                                rideCounter_AffectedAgents++;
                            } else if (leg.getMode().equals("bicycle")) {
                                bicycleCounter_Residents++;
                                bicycleCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.walk)) {
                                walkCounter_Residents++;
                                walkCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.pt)) {
                                ptCounter_Residents++;
                                ptCounter_AffectedAgents++;
                            } else {
                                throw new RuntimeException("there are not only mode 'car, carInternal, freight, ride, bicycle, walk, pt', but also something else mode! ");
                            }

                        } else if (pe instanceof Activity) {

                        } else {
                            throw new RuntimeException("Plan element can either be activity or leg.");
                        }
                    }
                }

                ResidentsCounter++;
                AffectedAgentsCounter++;

            } else if (personInternalIDsList.contains(person.getId().toString()) && (AffectedAgentsIDsList.contains(person.getId().toString()))) {

                for (Plan pa : person.getPlans()) {
                    for (PlanElement pe : pa.getPlanElements()) {
                        if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            if (leg.getMode().equals(TransportMode.car)) {
                                carCounter_AffectedResidents++;
                                carCounter_Residents++;
                                carCounter_AffectedAgents++;
                            } else if (leg.getMode().equals("freight")) {
                                freightCounter_AffectedResidents++;
                                freightCounter_Residents++;
                                freightCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.ride)) {
                                rideCounter_AffectedResidents++;
                                rideCounter_Residents++;
                                rideCounter_AffectedAgents++;
                            } else if (leg.getMode().equals("bicycle")) {
                                bicycleCounter_AffectedResidents++;
                                bicycleCounter_Residents++;
                                bicycleCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.walk)) {
                                walkCounter_AffectedResidents++;
                                walkCounter_Residents++;
                                walkCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.pt)) {
                                ptCounter_AffectedResidents++;
                                ptCounter_Residents++;
                                ptCounter_AffectedAgents++;
                            } else {
                                throw new RuntimeException("there are not only mode 'car, carInternal, freight, ride, bicycle, walk, pt', but also something else mode! ");
                            }

                        } else if (pe instanceof Activity) {

                        } else {
                            throw new RuntimeException("Plan element can either be activity or leg.");
                        }
                    }
                }

                AffectedResidentsCounter++;
                AffectedResidentsIDsList.add(person.getId().toString());

                ResidentsCounter++;
                AffectedAgentsCounter++;

            } else if ((!personInternalIDsList.contains(person.getId().toString())) && AffectedAgentsIDsList.contains(person.getId().toString())) {

                for (Plan pa : person.getPlans()) {
                    for (PlanElement pe : pa.getPlanElements()) {
                        if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            if (leg.getMode().equals(TransportMode.car)) {
                                carCounter_NoneResidentsAffectedAgents++;
                                carCounter_AffectedAgents++;
                            } else if (leg.getMode().equals("freight")) {
                                freightCounter_NoneResidentsAffectedAgents++;
                                freightCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.ride)) {
                                rideCounter_NoneResidentsAffectedAgents++;
                                rideCounter_AffectedAgents++;
                            } else if (leg.getMode().equals("bicycle")) {
                                bicycleCounter_NoneResidentsAffectedAgents++;
                                bicycleCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.walk)) {
                                walkCounter_NoneResidentsAffectedAgents++;
                                walkCounter_AffectedAgents++;
                            } else if (leg.getMode().equals(TransportMode.pt)) {
                                ptCounter_NoneResidentsAffectedAgents++;
                                ptCounter_AffectedAgents++;
                            } else {
                                throw new RuntimeException("there are not only mode 'car, carInternal, freight, ride, bicycle, walk, pt', but also something else mode! ");
                            }

                        } else if (pe instanceof Activity) {

                        } else {
                            throw new RuntimeException("Plan element can either be activity or leg.");
                        }
                    }
                }

                NoneResidentsAffectedAgentsCounter++;
                NoneResidentsAffectedAgentsIDsList.add(person.getId().toString());

                AffectedAgentsCounter++;

            } else {

                for (Plan pa : person.getPlans()) {
                    for (PlanElement pe : pa.getPlanElements()) {
                        if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            if (leg.getMode().equals(TransportMode.car)) {
                                carCounter_OtherAgents++;
                            } else if (leg.getMode().equals("freight")) {
                                freightCounter_OtherAgents++;
                            } else if (leg.getMode().equals(TransportMode.ride)) {
                                rideCounter_OtherAgents++;
                            } else if (leg.getMode().equals("bicycle")) {
                                bicycleCounter_OtherAgents++;
                            } else if (leg.getMode().equals(TransportMode.walk)) {
                                walkCounter_OtherAgents++;
                            } else if (leg.getMode().equals(TransportMode.pt)) {
                                ptCounter_OtherAgents++;
                            } else {
                                throw new RuntimeException("there are not only mode 'car, carInternal, freight, ride, bicycle, walk, pt', but also something else mode! ");
                            }

                        } else if (pe instanceof Activity) {

                        } else {
                            throw new RuntimeException("Plan element can either be activity or leg.");
                        }
                    }
                }

                OtherAgentsCounter++;
                OtherAgentsIDsList.add(person.getId().toString());
            }
        }

            //prepare
            //Counter for OtherAgents
            double carCounter = 0;
            carCounter = carCounter_Residents + carCounter_NoneResidentsAffectedAgents + carCounter_OtherAgents;
            System.out.println(carCounter + ", " + carCounter_Residents + ", " + carCounter_NoneResidentsAffectedAgents + ", " + carCounter_OtherAgents);
            double freightCounter = 0;
            freightCounter = freightCounter_Residents + freightCounter_NoneResidentsAffectedAgents + freightCounter_OtherAgents;
            System.out.println(freightCounter + ", " + freightCounter_Residents + ", " + freightCounter_NoneResidentsAffectedAgents + ", " + freightCounter_OtherAgents);
            double rideCounter = 0;
            rideCounter = rideCounter_Residents + rideCounter_NoneResidentsAffectedAgents + rideCounter_OtherAgents;
            System.out.println(rideCounter + ", " + rideCounter_Residents + ", " + rideCounter_NoneResidentsAffectedAgents + ", " + rideCounter_OtherAgents);
            double bicycleCounter = 0;
            bicycleCounter = bicycleCounter_Residents + bicycleCounter_NoneResidentsAffectedAgents + bicycleCounter_OtherAgents;
            System.out.println(bicycleCounter + ", " + bicycleCounter_Residents + ", " + bicycleCounter_NoneResidentsAffectedAgents + ", " + bicycleCounter_OtherAgents);
            double walkCounter = 0;
            walkCounter = walkCounter_Residents + walkCounter_NoneResidentsAffectedAgents + walkCounter_OtherAgents;
            System.out.println(walkCounter + ", " + walkCounter_Residents + ", " + walkCounter_NoneResidentsAffectedAgents + ", " + walkCounter_OtherAgents);
            double ptCounter = 0;
            ptCounter = ptCounter_Residents + ptCounter_NoneResidentsAffectedAgents + ptCounter_OtherAgents;
            System.out.println(ptCounter + ", " + ptCounter_Residents + ", " + ptCounter_NoneResidentsAffectedAgents + ", " + ptCounter_OtherAgents);


            double AllAgentsCounter = 0;
            AllAgentsCounter = ResidentsCounter + NoneResidentsAffectedAgentsCounter + OtherAgentsCounter;
            System.out.println(AllAgentsCounter + ", " + ResidentsCounter + ", " + NoneResidentsAffectedAgentsCounter + ", " + OtherAgentsCounter);


            double carProportion_AffectedResidents = 0;
            double carProportion_Residents = 0;
            double carProportion_NoneResidentsAffectedAgents = 0;
            double carProportion_AffectedAgents = 0;
            double carProportion_OtherAgents = 0;
            carProportion_AffectedResidents = carCounter_AffectedResidents / carCounter * 100;
            carProportion_Residents = carCounter_Residents / carCounter * 100;
            carProportion_NoneResidentsAffectedAgents = carCounter_NoneResidentsAffectedAgents / carCounter * 100;
            carProportion_AffectedAgents = carCounter_AffectedAgents / carCounter * 100;
            carProportion_OtherAgents = carCounter_OtherAgents / carCounter * 100;

            double freightProportion_AffectedResidents = 0;
            double freightProportion_Residents = 0;
            double freightProportion_NoneResidentsAffectedAgents = 0;
            double freightProportion_AffectedAgents = 0;
            double freightProportion_OtherAgents = 0;
            freightProportion_AffectedResidents = freightCounter_AffectedResidents / freightCounter * 100;
            freightProportion_Residents = freightCounter_Residents / freightCounter * 100;
            freightProportion_NoneResidentsAffectedAgents = freightCounter_NoneResidentsAffectedAgents / freightCounter * 100;
            freightProportion_AffectedAgents = freightCounter_AffectedAgents / freightCounter * 100;
            freightProportion_OtherAgents = freightCounter_OtherAgents / freightCounter * 100;

            double rideProportion_AffectedResidents = 0;
            double rideProportion_Residents = 0;
            double rideProportion_NoneResidentsAffectedAgents = 0;
            double rideProportion_AffectedAgents = 0;
            double rideProportion_OtherAgents = 0;
            rideProportion_AffectedResidents = rideCounter_AffectedResidents / rideCounter * 100;
            rideProportion_Residents = rideCounter_Residents / rideCounter * 100;
            rideProportion_NoneResidentsAffectedAgents = rideCounter_NoneResidentsAffectedAgents / rideCounter * 100;
            rideProportion_AffectedAgents = rideCounter_AffectedAgents / rideCounter * 100;
            rideProportion_OtherAgents = rideCounter_OtherAgents / rideCounter * 100;

            double bicycleProportion_AffectedResidents = 0;
            double bicycleProportion_Residents = 0;
            double bicycleProportion_NoneResidentsAffectedAgents = 0;
            double bicycleProportion_AffectedAgents = 0;
            double bicycleProportion_OtherAgents = 0;
            bicycleProportion_AffectedResidents = bicycleCounter_AffectedResidents / bicycleCounter * 100;
            bicycleProportion_Residents = bicycleCounter_Residents / bicycleCounter * 100;
            bicycleProportion_NoneResidentsAffectedAgents = bicycleCounter_NoneResidentsAffectedAgents / bicycleCounter * 100;
            bicycleProportion_AffectedAgents = bicycleCounter_AffectedAgents / bicycleCounter * 100;
            bicycleProportion_OtherAgents = bicycleCounter_OtherAgents / bicycleCounter * 100;

            double walkProportion_AffectedResidents = 0;
            double walkProportion_Residents = 0;
            double walkProportion_NoneResidentsAffectedAgents = 0;
            double walkProportion_AffectedAgents = 0;
            double walkProportion_OtherAgents = 0;
            walkProportion_AffectedResidents = walkCounter_AffectedResidents / walkCounter * 100;
            walkProportion_Residents = walkCounter_Residents / walkCounter * 100;
            walkProportion_NoneResidentsAffectedAgents = walkCounter_NoneResidentsAffectedAgents / walkCounter * 100;
            walkProportion_AffectedAgents = walkCounter_AffectedAgents / walkCounter * 100;
            walkProportion_OtherAgents = walkCounter_OtherAgents / walkCounter * 100;

            double ptProportion_AffectedResidents = 0;
            double ptProportion_Residents = 0;
            double ptProportion_NoneResidentsAffectedAgents = 0;
            double ptProportion_AffectedAgents = 0;
            double ptProportion_OtherAgents = 0;
            ptProportion_AffectedResidents = ptCounter_AffectedResidents / ptCounter * 100;
            ptProportion_Residents = ptCounter_Residents / ptCounter * 100;
            ptProportion_NoneResidentsAffectedAgents = ptCounter_NoneResidentsAffectedAgents / ptCounter * 100;
            ptProportion_AffectedAgents = ptCounter_AffectedAgents / ptCounter * 100;
            ptProportion_OtherAgents = ptCounter_OtherAgents / ptCounter * 100;


            //double Proportion_AffectedResidents = 0;
            double Proportion_Residents = 0;
            double Proportion_NoneResidentsAffectedAgents = 0;
            //double Proportion_AffectedAgents = 0;
            double Proportion_OtherAgents = 0;
            //Proportion_AffectedResidents = AffectedResidentsCounter / AllAgentsCounter * 100;
            Proportion_Residents = ResidentsCounter / AllAgentsCounter * 100;
            Proportion_NoneResidentsAffectedAgents = NoneResidentsAffectedAgentsCounter / AllAgentsCounter * 100;
            //Proportion_AffectedAgents = AffectedAgentsCounter / AllAgentsCounter * 100;
            Proportion_OtherAgents = OtherAgentsCounter / AllAgentsCounter * 100;

            double Proportion_AllAgents = 0;
            Proportion_AllAgents = Proportion_Residents + Proportion_NoneResidentsAffectedAgents + Proportion_OtherAgents;


            //print Txt
            // Insert Path to Output File Here!
            BufferedWriter ms = new BufferedWriter(new FileWriter(OutputFile_Results));
            // traverses the collection
            //for (String s : personInternalIDsList) {
            // write data
            ms.write("In this Txt File: !!! Residents means |those Agents|, who are residents and not used going through the restricted zone!");
            ms.newLine();
            ms.flush();
            ms.write("In this Txt File: !!! AffectedResidents means |those Agents|, who are residents and used going through the restricted zone!");
            ms.newLine();
            ms.flush();
            ms.write("In this Txt File: !!! AffectedAgents means |those Agents|, who are affected by our policy!(In orther word, who either lives in our restricted zone, or make activities in our restricted zone!)");
            ms.newLine();
            ms.flush();
            ms.write("In this Txt File: !!! NoneResidentsAffectedAgents means |those Agents|, who are affected by our policy but not the Residents!(In orther word, who only make activities in our restricted zone!)");
            ms.newLine();
            ms.newLine();
            ms.flush();
            ms.write("In this Txt File: !!! The Unit of Percent are %");
            ms.newLine();
            ms.newLine();
            ms.flush();


            ms.write("***************************************************************************************");
            ms.newLine();
            ms.flush();
            ms.write("the residents counted in initial Plan: " + personInternalIDsList.size() + "; the residents counted in output Plan: " + ResidentsCounter);
            ms.newLine();
            ms.newLine();
            ms.flush();


            ms.write("—————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————");
            ms.newLine();
            ms.flush();
            ms.write("AllAgents Number: " + AllAgentsCounter + "; Residents Number: " + ResidentsCounter + "; NoneResidentsAffectedAgents Number: " + NoneResidentsAffectedAgentsCounter + "; OtherAgents Number: " + OtherAgentsCounter);
            ms.newLine();
            ms.flush();
            ms.write("AllAgents Percent: " + Proportion_AllAgents + "; Residents Percent: " + Proportion_Residents + "; NoneResidentsAffectedAgents Percent: " + Proportion_NoneResidentsAffectedAgents + "; OtherAgents Percent: " + Proportion_OtherAgents);
            ms.newLine();
            ms.newLine();
            ms.flush();

            ms.newLine();
            ms.write("!!! the follow block is only for AffectedResidents !!!");
            ms.newLine();
            ms.write("AffectedResidents Number: " + AffectedResidentsCounter);
            ms.newLine();
            ms.flush();

            ms.newLine();
            ms.write("!!! the follow block is only for AffectedAgents !!!");
            ms.newLine();
            ms.write("AffectedAgents Number: " + AffectedAgentsCounter);
            ms.newLine();
            ms.newLine();
            ms.flush();


            ms.write("—————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————————");
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||car: " + "Residents Number: " + carCounter_Residents + "; NoneResidentsAffectedAgents Number: " + carCounter_NoneResidentsAffectedAgents + "; OtherAgents Number: " + carCounter_OtherAgents + "; Sum Number: " + carCounter);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||freight: " + "Residents Number: " + freightCounter_Residents + "; NoneResidentsAffectedAgents Number: " + freightCounter_NoneResidentsAffectedAgents + "; OtherAgents Number: " + freightCounter_OtherAgents + "; Sum Number: " + freightCounter);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||ride: " + "Residents Number: " + rideCounter_Residents + "; NoneResidentsAffectedAgents Number: " + rideCounter_NoneResidentsAffectedAgents + "; OtherAgents Number: " + rideCounter_OtherAgents + "; Sum Number: " + rideCounter);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||bicycle: " + "Residents Number: " + bicycleCounter_Residents + "; NoneResidentsAffectedAgents Number: " + bicycleCounter_NoneResidentsAffectedAgents + "; OtherAgents Number: " + bicycleCounter_OtherAgents + "; Sum Number: " + bicycleCounter);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||walk: " + "Residents Number: " + walkCounter_Residents + "; NoneResidentsAffectedAgents Number: " + walkCounter_NoneResidentsAffectedAgents + "; OtherAgents Number: " + walkCounter_OtherAgents + "; Sum Number: " + walkCounter);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||pt: " + "Residents Number: " + ptCounter_Residents + "; NoneResidentsAffectedAgents Number: " + ptCounter_NoneResidentsAffectedAgents + "; OtherAgents Number: " + ptCounter_OtherAgents + "; Sum Number: " + ptCounter);
            ms.newLine();
            ms.flush();

            ms.newLine();
            ms.write("!!! the follow block is only for AffectedResidents !!!");
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||car: " + "AffectedResidents Number: " + carCounter_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||freight: " + "AffectedResidents Number: " + freightCounter_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||ride: " + "AffectedResidents Number: " + rideCounter_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||bicycle: " + "AffectedResidents Number: " + bicycleCounter_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||walk: " + "AffectedResidents Number: " + walkCounter_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||pt: " + "AffectedResidents Number: " + ptCounter_AffectedResidents);
            ms.newLine();
            ms.newLine();
            ms.flush();

            ms.newLine();
            ms.write("!!! the follow block is only for AffectedAgents !!!");
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||car: " + "AffectedAgents Number: " + carCounter_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||freight: " + "AffectedAgents Number: " + freightCounter_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||ride: " + "AffectedAgents Number: " + rideCounter_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||bicycle: " + "AffectedAgents Number: " + bicycleCounter_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||walk: " + "AffectedAgents Number: " + walkCounter_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("Legs Number||pt: " + "AffectedAgents Number: " + ptCounter_AffectedAgents);
            ms.newLine();
            ms.newLine();
            ms.flush();


            ms.write("**********************************************************************************************************************************");
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||car: " + "Residents: " + carProportion_Residents + "; NoneResidentsAffectedAgents: " + carProportion_NoneResidentsAffectedAgents + "; OtherAgents: " + carProportion_OtherAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||freight: " + "Residents: " + freightProportion_Residents + "; NoneResidentsAffectedAgents: " + freightProportion_NoneResidentsAffectedAgents + "; OtherAgents: " + freightProportion_OtherAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||ride: " + "Residents: " + rideProportion_Residents + "; NoneResidentsAffectedAgents: " + rideProportion_NoneResidentsAffectedAgents + "; OtherAgents: " + rideProportion_OtherAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||bicycle: " + "Residents: " + bicycleProportion_Residents + "; NoneResidentsAffectedAgents: " + bicycleProportion_NoneResidentsAffectedAgents + "; OtherAgents: " + bicycleProportion_OtherAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||walk: " + "Residents: " + walkProportion_Residents + "; NoneResidentsAffectedAgents: " + walkProportion_NoneResidentsAffectedAgents + "; OtherAgents: " + walkProportion_OtherAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||pt: " + "Residents: " + ptProportion_Residents + "; NoneResidentsAffectedAgents: " + ptProportion_NoneResidentsAffectedAgents + "; OtherAgents: " + ptProportion_OtherAgents);
            ms.newLine();
            ms.flush();

            ms.newLine();
            ms.write("!!! the follow block is only for AffectedResidents !!!");
            ms.newLine();
            ms.write("ModalSplit||car: " + "AffectedResidents: " + carProportion_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||freight: " + "AffectedResidents: " + freightProportion_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||ride: " + "AffectedResidents: " + rideProportion_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||bicycle: " + "AffectedResidents: " + bicycleProportion_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||walk: " + "AffectedResidents: " + walkProportion_AffectedResidents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||pt: " + "AffectedResidents: " + ptProportion_AffectedResidents);
            ms.newLine();
            ms.flush();

            ms.newLine();
            ms.write("!!! the follow block is only for AffectedAgents !!!");
            ms.newLine();
            ms.write("ModalSplit||car: " + "AffectedAgents: " + carProportion_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||freight: " + "AffectedAgents: " + freightProportion_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||ride: " + "AffectedAgents: " + rideProportion_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||bicycle: " + "AffectedAgents: " + bicycleProportion_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||walk: " + "AffectedAgents: " + walkProportion_AffectedAgents);
            ms.newLine();
            ms.flush();
            ms.write("ModalSplit||pt: " + "AffectedAgents: " + ptProportion_AffectedAgents);
            ms.newLine();
            ms.flush();

            //}
            // release resource
            ms.close();
            //print Txt//

    }
}
