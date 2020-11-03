package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier;

public class RunDirectWalkAnalysis {

    private static final Logger log = Logger.getLogger(RunDirectWalkAnalysis.class);

    public static void main(String[] args) {

        String runDirectory = "/home/gregor/tmp/open-berlin-intermodal/Z127e";
        String runId = "Z127e"; // 15979 direct walk trips of total 55204 trips.
//        String runDirectory = "/home/gregor/tmp/open-berlin-intermodal/Z148e";
//        String runId = "Z148e"; // 20642 direct walk trips of total 59952 trips.
        run(runDirectory, runId);
    }

    private static void run(String runDirectory, String runId) {
        Scenario scenario = loadScenario(runDirectory, runId);
        OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();

        long counterTrips = scenario.getPopulation().getPersons().values().stream().
                map(person -> person.getSelectedPlan()).
                map(plan -> TripStructureUtils.getTrips(plan)).
                flatMap(trips -> trips.stream()).
                filter(trip -> TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0)).equals(TransportMode.pt)).
                count();

        long counterDirectWalkTrips = scenario.getPopulation().getPersons().values().stream().
                map(person -> person.getSelectedPlan()).
                map(plan -> TripStructureUtils.getTrips(plan)).
                flatMap(trips -> trips.stream()).
                filter(trip ->
                        trip.getLegsOnly().size() == 1 &&
                                trip.getLegsOnly().get(0).getMode().equals(TransportMode.walk) &&
                                ( /*!*/ TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0)).equals(TransportMode.pt))).
                count();

        log.info(counterDirectWalkTrips + " direct walk trips of total " + counterTrips + " trips.");
    }

    private static Scenario loadScenario(String runDirectory, String runId) {
        log.info("Loading scenario...");

        if (runDirectory == null || runDirectory.equals("") || runDirectory.equals("null")) {
            return null;
        }

        if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

        String networkFile;
        String populationFile;
        String configFile;

        configFile = runDirectory + runId + ".output_config.xml";
        networkFile = runId + ".output_network.xml.gz";
        populationFile = runId + ".output_plans.xml.gz";

        Config config = ConfigUtils.loadConfig(configFile);

        if (!runId.equals(config.controler().getRunId()))
            throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");

        config.controler().setOutputDirectory(runDirectory);
        config.plans().setInputFile(populationFile);
        config.network().setInputFile(networkFile);
        config.vehicles().setVehiclesFile(null);
        config.transit().setTransitScheduleFile(null);
        config.transit().setVehiclesFile(null);

        return ScenarioUtils.loadScenario(config);
    }
}
