package org.matsim.run;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MosaikRunner {

    public static void main(String[] args) {

        var config = RunBerlinScenario.prepareConfig(args);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

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
}
