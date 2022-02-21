/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.matsim.contrib.emissions.Pollutant.*;

/**
* @author ikaddoura
*/

public class RunOfflineAirPollutionAnalysisB75 {
    private static final Logger log = Logger.getLogger(RunOfflineAirPollutionAnalysisB75.class);

    private final String runDirectory;
    private final String runId;
    private final String hbefaWarmFile;
    private final String hbefaColdFile;
    private final String analysisOutputDirectory;

    static List<Pollutant> pollutants2Output = Arrays.asList(CO2_TOTAL, NOx, PM, PM_non_exhaust);

    public RunOfflineAirPollutionAnalysisB75(String runDirectory, String runId, String hbefaFileWarm, String hbefaFileCold, String analysisOutputDirectory) {
        if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
        this.runDirectory = runDirectory;

        this.runId = runId;
        this.hbefaWarmFile = hbefaFileWarm;
        this.hbefaColdFile = hbefaFileCold;

        if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
        this.analysisOutputDirectory = analysisOutputDirectory;
    }


    public static void main(String[] args) throws IOException {

        //TODO: Please set MATSIM_DECRYPTION_PASSWORD as envrionment variable to decrypt the files.

        final String hbefaFileCold = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";
        final String hbefaFileWarm = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

        final String runId = "ScenarioA75" ;
        String runDirectory = "/net/ils/kreuschner/output/output_ScenarioA75/";
        RunOfflineAirPollutionAnalysisB75 analysis = new RunOfflineAirPollutionAnalysisB75(
                runDirectory,
                runId,
                hbefaFileWarm,
                hbefaFileCold,
                runDirectory + "emission-analysis-hbefa-v4.1-2020");
        try {
            analysis.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void run() throws IOException {

        Config config = ConfigUtils.createConfig();
        config.vehicles().setVehiclesFile( runDirectory + runId + ".output_allVehicles.xml.gz");
        config.network().setInputFile( runDirectory + runId + ".output_network.xml.gz");
        config.transit().setTransitScheduleFile( runDirectory +runId + ".output_transitSchedule.xml.gz");
        config.transit().setVehiclesFile( runDirectory + runId + ".output_transitVehicles.xml.gz");
        config.global().setCoordinateSystem("EPSG:31468");
        config.plans().setInputFile(null);
        config.parallelEventHandling().setNumberOfThreads(null);
        config.parallelEventHandling().setEstimatedNumberOfEvents(null);
        config.global().setNumberOfThreads(1);

        EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.directlyTryAverageTable);
        eConfig.setAverageColdEmissionFactorsFile(hbefaColdFile);
        eConfig.setAverageWarmEmissionFactorsFile(hbefaWarmFile);
        eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
        eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);
        eConfig.setWritingEmissionsEvents(true);

        File folder = new File(analysisOutputDirectory);
        folder.mkdirs();

        final String eventsFile = runDirectory + runId + ".output_events.xml.gz";

        final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
        final String linkEmissionAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLink.csv";
        final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLinkPerM.csv";
//      final String vehicleTypeFile = analysisOutputDirectory + runId + ".emissionVehicleInformation.csv";

        Scenario scenario = ScenarioUtils.loadScenario(config);
        for (Link link : scenario.getNetwork().getLinks().values()) {

            double freespeed = Double.NaN;

            if (link.getFreespeed() <= 25.5 / 3.6) {
                freespeed = link.getFreespeed() * 2;
                // for non motorway roads, the free speed level was reduced
            } else {
                freespeed = link.getFreespeed();
                // for motorways, the original speed levels seems ok.
            }

            if(freespeed <= 8.333333333){ //30kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
            } else if(freespeed <= 11.111111111){ //40kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
            } else if(freespeed <= 13.888888889){ //50kmh
                double lanes = link.getNumberOfLanes();
                if(lanes <= 1.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
                } else if(lanes <= 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
                } else if(lanes > 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
                } else{
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if(freespeed <= 16.666666667){ //60kmh
                double lanes = link.getNumberOfLanes();
                if(lanes <= 1.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
                } else if(lanes <= 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
                } else if(lanes > 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
                } else{
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if(freespeed <= 19.444444444){ //70kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
            } else if(freespeed <= 22.222222222){ //80kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
            } else if(freespeed > 22.222222222){ //faster
                link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
            } else{
                throw new RuntimeException("Link not considered...");
            }
        }

        // vehicles

        // 1.) Set all vehicletypes to "Non-HBEFA"
        for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
            EngineInformation engineInformation = type.getEngineInformation();
            // TODO: Check! Is this a zero emission vehicle?!
            VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
            VehicleUtils.setHbefaTechnology( engineInformation, "average" );
            VehicleUtils.setHbefaSizeClass( engineInformation, "average" );
            VehicleUtils.setHbefaEmissionsConcept( engineInformation, "average" );
        }

        //Now set the "car" vehicle types
        VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class));
        EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );

        //Now set the "freight" vehicle types
        VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("freight", VehicleType.class));
        EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology( freightEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( freightEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( freightEngineInformation, "average" );

        //As a result all non-car and non-freight vehicles are iognored in the emissions analysis. This are mostly the pt relates veh types.


        //------------------------------------------------------------------------------

        // the following is copy paste from the example...
        EventsManager eventsManager = EventsUtils.createEventsManager();
        AbstractModule module = new AbstractModule(){
            @Override
            public void install(){
                bind( Scenario.class ).toInstance( scenario );
                bind( EventsManager.class ).toInstance( eventsManager );
                bind( EmissionModule.class ) ;
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        EmissionsOnLinkHandler emissionsEventHandler = new EmissionsOnLinkHandler();
        eventsManager.addHandler(emissionsEventHandler);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        log.info("Done reading the events file.");
        log.info("Finish processing...");
        eventsManager.finishProcessing();

       log.info("Closing events file...");
       emissionEventWriter.closeFile();

        writeOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, scenario, emissionsEventHandler);

        int totalVehicles = scenario.getVehicles().getVehicles().size();
        log.info("Total number of vehicles: " + totalVehicles);


        scenario.getVehicles().getVehicles().values().stream()
                .map(vehicle -> vehicle.getType())
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()))
                .entrySet()
                .forEach(entry -> log.info("nr of " + VehicleUtils.getHbefaVehicleCategory(entry.getKey().getEngineInformation()) + " vehicles running on " + VehicleUtils.getHbefaEmissionsConcept(entry.getKey().getEngineInformation())
                        +" = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "% overall)"));
    }



    private void writeOutput(String linkEmissionAnalysisFile, String linkEmissionPerMAnalysisFile, Scenario scenario, EmissionsOnLinkHandler emissionsEventHandler) throws IOException {

        log.info("Emission analysis completed.");

        log.info("Writing output...");

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(4);
        nf.setGroupingUsed(false);

        {
            File file1 = new File(linkEmissionAnalysisFile);

            BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

            bw1.write("linkId");

            for (Pollutant pollutant : pollutants2Output) {
                bw1.write(";" + pollutant);
            }
            bw1.newLine();

            Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

            for (Id<Link> linkId : link2pollutants.keySet()) {
                bw1.write(linkId.toString());

                for (Pollutant pollutant : pollutants2Output) {
                    double value = 0.;
                    if (link2pollutants.get(linkId).get(pollutant) != null) {
                        value = link2pollutants.get(linkId).get(pollutant);
                    }
                    bw1.write(";" + nf.format(value));
                }
                bw1.newLine();
            }

            bw1.close();
            log.info("Output written to " + linkEmissionAnalysisFile);
        }

        {
            File file2 = new File(linkEmissionPerMAnalysisFile);

            BufferedWriter bw1 = new BufferedWriter(new FileWriter(file2));

            bw1.write("linkId");

            for (Pollutant pollutant : pollutants2Output) {
                bw1.write(";" + pollutant + " [g/m]");
            }
            bw1.newLine();

            Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

            for (Id<Link> linkId : link2pollutants.keySet()) {
                bw1.write(linkId.toString());

                for (Pollutant pollutant : pollutants2Output) {
                    double emission = 0.;
                    if (link2pollutants.get(linkId).get(pollutant) != null) {
                        emission = link2pollutants.get(linkId).get(pollutant);
                    }

                    double emissionPerM = Double.NaN;
                    Link link = scenario.getNetwork().getLinks().get(linkId);
                    if (link != null) {
                        emissionPerM = emission / link.getLength();
                    }
                    bw1.write(";" + nf.format(emissionPerM));
                }
                bw1.newLine();
            }

            bw1.close();
            log.info("Output written to " + linkEmissionPerMAnalysisFile);
        }
    }
}

