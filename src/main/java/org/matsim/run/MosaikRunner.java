package org.matsim.run;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.ReRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.vis.snapshotwriters.SnapshotWritersModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MosaikRunner {

    public static void main(String[] args) {

        var config = RunBerlinScenario.prepareConfig(args);
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

        /*
        // merge transit schedule and network

        // remove all references of the old network from the schedule
        for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
            facility.setLinkId(null);
        }

        // remove network routes from schedule
        scenario.getTransitSchedule().getTransitLines().values().parallelStream()
                .flatMap(transitLine -> transitLine.getRoutes().values().stream())
                .forEach(route -> route.setRoute(null));

        // remove all facilities from transit schedule which were added by a previous run of create pseudo network this is quite brittle and relies on the implementation of createpseudonetwork
        scenario.getTransitSchedule().getFacilities().values().stream()
                .filter(f -> f.getId().toString().contains("."))
                .collect(Collectors.toSet())
                .forEach(stop -> scenario.getTransitSchedule().removeStopFacility(stop));

        // add pseudo network for pt
        new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "pt_", 0.1, 100000.0).createNetwork();

         */

        var bbox = createBoundingBox();
        var network = scenario.getNetwork();
        // if links are within the bounding box replace simplified links with detailed geometries
        // first add detailed geometries for the original geometry information
        var linksToDelete = network.getLinks().values().parallelStream()
                .filter(link -> isCoveredBy(link, bbox))
                .peek(link -> {

                    // get the original geometry information from the link attributes
                    var originalGeometry = NetworkUtils.getOriginalGeometry(link);

                    // convert original link's attributes into map once
                    var attributes = link.getAttributes().getAsMap();

                    // for each node in the original geometry add one link
                    for (int i = 0; i < originalGeometry.size(); i++) {

                        network.addNode(originalGeometry.get(i));

                        var from = i == 0 ? link.getFromNode() : originalGeometry.get(i);
                        var to = i == originalGeometry.size() - 1 ? link.getToNode() : originalGeometry.get(i + 1);
                        var newLink = network.getFactory().createLink(Id.createLinkId(link.getId().toString() + "_" + i), from, to);

                        // copy all values of the original link
                        newLink.setAllowedModes(link.getAllowedModes());
                        newLink.setCapacity(link.getCapacity());
                        newLink.setFreespeed(link.getFreespeed());
                        newLink.setNumberOfLanes(link.getNumberOfLanes());

                        // copy all unstructured attributes
                        attributes.forEach((key, value) -> newLink.getAttributes().putAttribute(key, value));

                        network.addLink(newLink);
                    }
                })
                .collect(Collectors.toSet());

        // now delete the links simplified links
        for (var link : linksToDelete) {
            network.removeLink(link.getId());
        }

        // remove linkids from activities
        scenario.getPopulation().getPersons().values().parallelStream()
                .flatMap(person -> person.getPlans().stream())
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Activity)
                .map(element -> (Activity) element)
                .forEach(activity -> activity.setLinkId(null));

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
