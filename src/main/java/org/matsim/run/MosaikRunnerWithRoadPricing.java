package org.matsim.run;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.jdeqsim.Road;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MosaikRunnerWithRoadPricing {

    public static void main(String[] args) {

        var roadPricincConfig = new RoadPricingConfigGroup();
        var config = RunBerlinScenario.prepareConfig(args, roadPricincConfig);
        config.global().setCoordinateSystem("EPSG:25833");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        var scenario = RunBerlinScenario.prepareScenario(config);
        loadScheme(scenario);

        var controler = RunBerlinScenario.prepareControler(scenario);
        var module = new RoadPricingModule(RoadPricingUtils.getRoadPricingScheme(scenario));
        controler.addOverridingModule(module);

        controler.run();
    }

    private static void loadScheme(Scenario scenario) {

        var scheme = (RoadPricingSchemeImpl) RoadPricingUtils.addOrGetRoadPricingScheme(scenario);
        var network = scenario.getNetwork();

        try (var reader = Files.newBufferedReader(Paths.get("some path")); var parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (var record : parser) {

                var id = Id.createLinkId(record.get("id"));
                var time = Double.parseDouble(record.get("time"));
                var value = Double.parseDouble(record.get("emission [g/m3]"));

                var link = network.getLinks().get(id);
                var volume = 1000; // this should be flexible but our showcase grid is 10x10x10=1000m3
                var costFactor = 1000;
                var toll = value * volume * costFactor / link.getLength(); // this should be toll per link/m

                RoadPricingUtils.addLinkSpecificCost(scheme, id, time, time + 3600, toll);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
