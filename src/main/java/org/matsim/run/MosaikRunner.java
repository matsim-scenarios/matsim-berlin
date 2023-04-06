package org.matsim.run;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.ReRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.vis.snapshotwriters.SnapshotWritersModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TransferQueue;
import java.util.stream.Collectors;

public class MosaikRunner {

    public static void main(String[] args) {

        var config = RunBerlinScenario.prepareConfig(args);
        config.global().setCoordinateSystem("EPSG:25833");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.strategy().clearStrategySettings();

        // allow only re-route and plan selection
        var reRouteFreight = new StrategyConfigGroup.StrategySettings()
                .setStrategyName("ReRoute")
                .setWeight(0.5)
                .setSubpopulation("freight");

        var reRoutePersons = new StrategyConfigGroup.StrategySettings()
                .setStrategyName("ReRoute")
                .setWeight(0.5)
                .setSubpopulation("person");

        var changePlansPersons =  new StrategyConfigGroup.StrategySettings()
                .setStrategyName("ChangeExpBeta")
                .setWeight(0.5)
                .setSubpopulation("person");

        var changePlansFreight =  new StrategyConfigGroup.StrategySettings()
                .setStrategyName("ChangeExpBeta")
                .setWeight(0.5)
                .setSubpopulation("freight");

        config.strategy().addStrategySettings(reRouteFreight);
        config.strategy().addStrategySettings(reRoutePersons);
        config.strategy().addStrategySettings(changePlansPersons);
        config.strategy().addStrategySettings(changePlansFreight);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        var scenario = RunBerlinScenario.prepareScenario(config);

        // remove linkids from activities
        scenario.getPopulation().getPersons().values().parallelStream()
                .flatMap(person -> person.getPlans().stream())
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Activity)
                .map(element -> (Activity) element)
                .forEach(activity -> {
                    activity.setLinkId(null);
                });

        // replace trips in plans with single empty legs which only have a main mode
        for (var person : scenario.getPopulation().getPersons().values()) {

            // copy plans
            List<Plan> plans =  new ArrayList<>(person.getPlans());

            // remove old reference
            person.getPlans().clear();
            person.setSelectedPlan(null);

            for (var plan : plans) {

                var trips = TripStructureUtils.getTrips(plan);
                var newPlan = scenario.getPopulation().getFactory().createPlan();
                newPlan.addActivity(PopulationUtils.getFirstActivity(plan));

                for (var trip : trips) {

                    var mainMode = TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trip.getTripElements());
                    var leg = scenario.getPopulation().getFactory().createLeg(mainMode);
                    newPlan.addLeg(leg);
                    newPlan.addActivity(trip.getDestinationActivity());
                }

                person.addPlan(newPlan);
            }
        }

        // remove linkids from facilities
        // facilities are basically immutable. Create new facilities without link references
        var factory = new ActivityFacilitiesFactoryImpl();
        var facilities = scenario.getActivityFacilities().getFacilities().values().parallelStream()
                .map(facility -> factory.createActivityFacility(facility.getId(), facility.getCoord()))
                .collect(Collectors.toSet());

        // throw out old facilities
        scenario.getActivityFacilities().getFacilities().clear();

        // add new facilities to scenario
        for (var facility : facilities) {
            scenario.getActivityFacilities().addActivityFacility(facility);
        }

        var controler = RunBerlinScenario.prepareControler(scenario);
        controler.run();
    }

    private static PreparedGeometry createBoundingBox() {

        var geometry = new GeometryFactory().createPolygon(
                new Coordinate[]{
                        new Coordinate(385030.5, 5818413.0), new Coordinate(385030.5, 5820459),
                        new Coordinate(387076.5, 5820459), new Coordinate(385030.5, 5820459),
                        new Coordinate(385030.5, 5818413.0)
                }
        );
        return new PreparedGeometryFactory().create(geometry);
    }

    private static boolean isCoveredBy(Link link, PreparedGeometry geometry) {
        return geometry.covers(MGC.coord2Point(link.getFromNode().getCoord()))
                && geometry.covers(MGC.coord2Point(link.getToNode().getCoord()));
    }
}
