package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.options.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "energy-consumption", description = "Roughly estimates electric energy consumption assuming 100% electrification of cars within an area.")
@CommandSpec(produces = {"energy_consumption_stats.csv"})
public class EnergyConsumptionAnalysis implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(EnergyConsumptionAnalysis.class);

    @CommandLine.Mixin
    private final InputOptions input = InputOptions.ofCommand(NoiseAnalysis.class);
    @CommandLine.Mixin
    private final OutputOptions output = OutputOptions.ofCommand(NoiseAnalysis.class);

    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();

//    @CommandLine.Mixin
//    private final ConfigOptions co = new ConfigOptions();

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

        //load _executed_ plans from output
        String executedPlansPath = ApplicationUtils.matchInput("executed", input.getRunDirectory()).toString();
        Population population = PopulationUtils.readPopulation(executedPlansPath);

        Set<Id<Person>> inhabitants = new HashSet();
        Set<Id<Person>> agentsWithDestinationActs = new HashSet();

        if (shp.isDefined()) {

            List<SimpleFeature> features = shp.readFeatures();


            population.getPersons().values().stream().
                    filter(person -> PopulationUtils.getSubpopulation(person).equals("person"))
                    .forEach(person -> {

                        //as long as executed plans have no person attributes, we need to reverse-engineer the home location in getHomeLocation()
                        HashSet<Activity> activitiesInShape =
                                TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream()
                                .filter(act -> shp.getGeometry().contains(MGC.coord2Point(act.getCoord())))
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
        } else {
            log.warn("No shape file defined. All 'person' agents in the executed plans will be treated as 'inhabitants'.");
            inhabitants.addAll(population.getPersons().values().stream()
                    .filter(person -> PopulationUtils.getSubpopulation(person).equals("person"))
                    .map(Person::getId)
                    .collect(Collectors.toCollection(HashSet::new)));
        }

        DescriptiveStatistics inhabitantsCarDistanceStats = new DescriptiveStatistics();
        DescriptiveStatistics destinationAgentsCarDistanceStats = new DescriptiveStatistics();

        //now we calculate the total car distance driven by inhabitants
        for (Id<Person> inhabitant : inhabitants) {
            Optional<Double> totalCarDistance = getTotalCarDistance(inhabitant, population);
            if (totalCarDistance.isPresent()) {
                inhabitantsCarDistanceStats.addValue(totalCarDistance.get() / 1000.0); //convert to km
            }
        }

        //now we calculate the total car distance driven by non-inhabitants who have destination activities in the area
        for (Id<Person> agentId : agentsWithDestinationActs) {
            Optional<Double> totalCarDistance = getTotalCarDistance(agentId, population);
            if (totalCarDistance.isPresent()) {
                destinationAgentsCarDistanceStats.addValue(totalCarDistance.get() /1000.0); //convert to km
            }
        }

        writeTilesCSV(inhabitantsCarDistanceStats, destinationAgentsCarDistanceStats);

        return 0;
    }

    private void writeTilesCSV(DescriptiveStatistics inhabitantsCarDistanceStats, DescriptiveStatistics destinationAgentsCarDistanceStats) {
        double factor = sampleOptions.getUpscaleFactor();

        // Total stats
        DecimalFormat df = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.US));
        try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("energy_consumption_stats.csv").toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("Total driven car distance by residents  [km]:", df.format(inhabitantsCarDistanceStats.getSum() * factor));
            printer.printRecord("Mean driven car distance by residents [km]:", df.format(inhabitantsCarDistanceStats.getMean() * factor));
            printer.printRecord("Total driven car distance by non-residents with 1+ activities in shape [km]:", df.format(destinationAgentsCarDistanceStats.getSum() * factor));
            printer.printRecord("Mean driven car distance by non-residents with 1+ activities in shape [km]:", df.format(destinationAgentsCarDistanceStats.getMean() * factor));
            printer.printRecord("Assumed energy consumption [kWh/100km]:", df.format(this.averageConsumptionInKWhPer100km));
            printer.printRecord("Total energy consumption by inhabitants  [m]:", df.format(inhabitantsCarDistanceStats.getSum()  * factor / 100.0 * this.averageConsumptionInKWhPer100km));
            printer.printRecord("Mean energy consumption by inhabitants [m]:", df.format(inhabitantsCarDistanceStats.getMean()  * factor/ 100.0 * this.averageConsumptionInKWhPer100km));
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
