package org.matsim.analysis.hundekopf;

import org.matsim.analysis.AgentAnalysisFilter;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.analysis.modalSplit.ModeAnalysis;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RunKopfModalSplitAnalysisA75 {

    public static void main(String[] args) throws IOException {

        for (int i = 1; i < 5; i++) {

            final String runId = "ScenarioC75";
            final String runDirectory = "/net/ils/kreuschner/output/output_ScenarioC75";

            final String outputDirectory = "/net/ils/kreuschner/analysis/modal-split-analysis-superblocksCat" + i + "-C75/";

            Config config = ConfigUtils.createConfig();
            config.network().setInputFile(null);
            config.plans().setInputFile(runDirectory + "/" + runId + ".output_plans.xml.gz");
            config.controler().setRunId(runId);
            config.global().setCoordinateSystem("EPSG:31468");
            config.vehicles().setVehiclesFile(null);
            config.transit().setTransitScheduleFile(null);
            config.transit().setVehiclesFile(null);
            config.facilities().setInputFile(null);
            Scenario scenario = ScenarioUtils.loadScenario(config);

            AgentAnalysisFilter filter = new AgentAnalysisFilter("A");

            filter.setSubpopulation("person");

//        filter.setPersonAttribute("berlin");
//        filter.setPersonAttributeName("home-activity-zone");

            filter.setZoneFile("/net/ils/kreuschner/Superblocks_Shapefiles/SuperblocksCat" + i + ".shp");
            filter.setRelevantActivityType("home");

            filter.preProcess(scenario);

            ModeAnalysis analysis = new ModeAnalysis(scenario, filter, null, new DefaultAnalysisMainModeIdentifier());
            analysis.run();

            File directory = new File(outputDirectory);
            directory.mkdirs();

            analysis.writeModeShares(outputDirectory);
            analysis.writeTripRouteDistances(outputDirectory);
            analysis.writeTripEuclideanDistances(outputDirectory);

            final List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
            distanceGroups.add(new Tuple<>(0., 1000.));
            distanceGroups.add(new Tuple<>(1000., 3000.));
            distanceGroups.add(new Tuple<>(3000., 5000.));
            distanceGroups.add(new Tuple<>(5000., 10000.));
            distanceGroups.add(new Tuple<>(10000., 20000.));
            distanceGroups.add(new Tuple<>(20000., 100000.));
            distanceGroups.add(new Tuple<>(100000., 999999999999.));
            analysis.writeTripRouteDistances(outputDirectory, distanceGroups);
            analysis.writeTripEuclideanDistances(outputDirectory, distanceGroups);
        }
    }
}
