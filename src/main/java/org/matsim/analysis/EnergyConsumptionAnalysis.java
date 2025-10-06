package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "energy-consumption",
        description = "Roughly estimates electric energy consumption assuming 100% electrification of cars within an area.")
@CommandSpec(
        requireRunDirectory = true,
        requirePopulation = true,
        produces = {"energy_consumption_residents.csv", "energy_consumption_non_residents.csv",
                "distance_stats_residents.csv", "distance_stats_non_residents.csv",
                "energy_consumption_area.gpkg"})
public class EnergyConsumptionAnalysis implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(EnergyConsumptionAnalysis.class);

    @CommandLine.Mixin
    private final InputOptions input = InputOptions.ofCommand(EnergyConsumptionAnalysis.class);
    @CommandLine.Mixin
    private final OutputOptions output = OutputOptions.ofCommand(EnergyConsumptionAnalysis.class);

    @CommandLine.Mixin
    private final ConfigOptions configOptions = new ConfigOptions();

    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private final CrsOptions crsOptions = new CrsOptions();


    @CommandLine.Mixin
    private final SampleOptions sampleOptions = new SampleOptions();

    private double averageConsumptionInKWhPer100km = 15.0;

    /**
     * Run the command logic.
     *
     * @return return code, 0 - success. 1 - general failure, 2 - user/input error
     */
    @Override
    public Integer call() throws Exception {

        log.info("Starting energy consumption analysis...");

        // Unfortunately, the Berlin scenario has activities without coordinates. Thus,
        // I need a scenario instance, to be able to call PopulationUtils.decideOnCoordForActivity(activity, scenario);
        // I do not want to load the full scenario, so I create an empty config and copy the paths to the population and the network.
        // There is no easy way to copy the GlobalConfigGroup (which is the only config reference in PopulationUtils.decideOnCoordForActivity(activity, scenario))
        // BUT: there is no setter for the relativePositionOnLink in the GlobalConfigGroup. It is deprecated, anyway. So we can use a standard instance here.
        // All of this to say that this is really clunky, and if activities had coordinates our lives would be much easier here.


        Config analysisConfig = ConfigUtils.createConfig();
        analysisConfig.global().setCoordinateSystem(crsOptions.getInputCRS());

        //TODO: normally, we should load the _experienced_ plans directly, but these do not contain person attributes ATM. PR#4315 addresses this issue. read also above.
//        //load _executed_ plans from output
//        String plansFile = ApplicationUtils.matchInput("experienced_plans", input.getRunDirectory()).toString();

        //this finds the 'regular' output plans file with all the attributes
        String plansFile = ApplicationUtils.matchInput("output_plans.xml.gz", input.getRunDirectory()).toString();

        analysisConfig.plans().setInputFile(plansFile);

        analysisConfig.network().setInputFile(ApplicationUtils.matchInput("output_network.xml.gz", input.getRunDirectory()).toString());
        analysisConfig.facilities().setInputFile(ApplicationUtils.matchInput("output_facilities.xml.gz", input.getRunDirectory()).toString());

        Scenario scenario;
        //this finds the experienced plans (which currently have no person attributes, see above)
//        scenario = ApplicationUtils.loadScenario(analysisConfig.controller().getRunId(), input.getRunDirectory(), crsOptions);

        scenario = ScenarioUtils.loadScenario(analysisConfig);

        Set<Id<Person>> inhabitants = new HashSet();
        Set<Id<Person>> agentsWithDestinationActs = new HashSet();

        if (shp.isDefined()) {
            Geometry geometry = shp.getGeometry();

            scenario.getPopulation().getPersons().values().stream()
                    .filter(person -> PopulationUtils.getSubpopulation(person).equals("person"))
                    .forEach(person -> {

                        //as long as executed plans have no person attributes, we need to reverse-engineer the home location in getHomeLocation()
                        HashSet<Activity> activitiesInShape =
                                TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream()
                                        .filter(act -> geometry.contains(MGC.coord2Point(PopulationUtils.decideOnCoordForActivity(act, scenario))))
//                                        .filter(act -> geometry.contains(MGC.coord2Point(act.getCoord())))
                                        .collect(Collectors.toCollection(HashSet::new));

                        if (activitiesInShape.size() > 0) {
                            //agents that have an activity of type "home*" are considered inhabitants
                            if (activitiesInShape.stream().anyMatch(act -> act.getType().startsWith("home"))) {
                                inhabitants.add(person.getId());
                            } else {
                                //all other agents with activities in the shape are considered non-inhabitants
                                agentsWithDestinationActs.add(person.getId());
                            }
                        }
                    });

            //copy shp file to output folder
            writeGeometryFile();

        } else {
            log.warn("No shape file defined. All 'person' agents in the executed plans will be treated as 'inhabitants'.");
            inhabitants.addAll(scenario.getPopulation().getPersons().values().stream()
                    .filter(person -> PopulationUtils.getSubpopulation(person).equals("person"))
                    .map(Person::getId)
                    .collect(Collectors.toCollection(HashSet::new)));
        }

        DescriptiveStatistics inhabitantsCarDistanceStats = new DescriptiveStatistics();
        DescriptiveStatistics destinationAgentsCarDistanceStats = new DescriptiveStatistics();

        //now we calculate the total car distance driven by inhabitants
        for (Id<Person> inhabitant : inhabitants) {
            Optional<Double> totalCarDistance = getTotalCarDistance(inhabitant, scenario.getPopulation());
            if (totalCarDistance.isPresent()) {
                inhabitantsCarDistanceStats.addValue(totalCarDistance.get() / 1000.0); //convert to km
            }
        }

        //now we calculate the total car distance driven by non-inhabitants who have destination activities in the area
        for (Id<Person> agentId : agentsWithDestinationActs) {
            Optional<Double> totalCarDistance = getTotalCarDistance(agentId, scenario.getPopulation());
            if (totalCarDistance.isPresent()) {
                destinationAgentsCarDistanceStats.addValue(totalCarDistance.get() / 1000.0); //convert to km
            }
        }

        writeTilesCSVs(inhabitantsCarDistanceStats, destinationAgentsCarDistanceStats);

        return 0;
    }

    private void writeGeometryFile() {
        List<SimpleFeature> list = shp.readFeatures();
        Path path = output.getPath("energy_consumption_area.gpkg");
        if(path.toFile().exists()){
            log.warn("The output file " + path + " already exists and will be overwritten.");
            path.toFile().delete();
        }
        GeoFileWriter.writeGeometries(list, output.getPath("energy_consumption_area.gpkg").toString());
    }

    private void writeTilesCSVs(DescriptiveStatistics inhabitantsCarDistanceStats, DescriptiveStatistics destinationAgentsCarDistanceStats) {
        double factor = sampleOptions.getUpscaleFactor();

        // Total stats
        DecimalFormat df = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.US));
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("distance_stats_residents.csv").toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("Total driven car distance by residents  [km]:", df.format(inhabitantsCarDistanceStats.getSum() * factor));
            printer.printRecord("Mean driven car distance by resident car drivers [km]:", df.format(inhabitantsCarDistanceStats.getMean())); //no scale factor, because this a mean!
            printer.printRecord("Number of resident car drivers [1]:", df.format(inhabitantsCarDistanceStats.getN() * factor));
        } catch (IOException ex) {
            log.error(ex);
        }
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("distance_stats_non_residents.csv").toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("Total driven car distance by non-residents with 1+ activities in shape [km]:", df.format(destinationAgentsCarDistanceStats.getSum() * factor));
            printer.printRecord("Mean driven car distance by non-resident car drivers with 1+ activities in shape [km]:", df.format(destinationAgentsCarDistanceStats.getMean()));
            printer.printRecord("Number of non-resident  car drivers [1]:", df.format(destinationAgentsCarDistanceStats.getN() * factor));
        } catch (IOException ex) {
            log.error(ex);
        }
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("energy_consumption_residents.csv").toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("Assumed energy consumption [kWh/100km]:", df.format(this.averageConsumptionInKWhPer100km));
            printer.printRecord("Total energy consumption by resident car drivers  [kWh]:", df.format(inhabitantsCarDistanceStats.getSum()  * factor / 100.0 * this.averageConsumptionInKWhPer100km));
            printer.printRecord("Mean energy consumption by resident car drivers [kWh]:", df.format(inhabitantsCarDistanceStats.getMean()  * factor / 100.0 * this.averageConsumptionInKWhPer100km));
        } catch (IOException ex) {
            log.error(ex);
        }
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("energy_consumption_non_residents.csv").toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("Assumed energy consumption [kWh/100km]:", df.format(this.averageConsumptionInKWhPer100km));
            printer.printRecord("Total energy consumption by resident car drivers  [kWh]:", df.format(destinationAgentsCarDistanceStats.getSum()  * factor / 100.0 * this.averageConsumptionInKWhPer100km));
            printer.printRecord("Mean energy consumption by resident car drivers [kWh]:", df.format(destinationAgentsCarDistanceStats.getMean()  * factor / 100.0 * this.averageConsumptionInKWhPer100km));
        } catch (IOException ex) {
            log.error(ex);
        }
    }

    private static @NotNull Optional<Double> getTotalCarDistance(Id<Person> agentId, Population population) {
        Optional<Double> totalCarDistance = TripStructureUtils.getLegs(population.getPersons().get(agentId).getSelectedPlan()).stream()
                .filter(leg -> leg.getMode().equals(TransportMode.car))
                .filter(leg -> leg.getRoute() != null)
                .map(leg -> leg.getRoute().getDistance())
                .reduce(Double::sum);
        return totalCarDistance;
    }

}
