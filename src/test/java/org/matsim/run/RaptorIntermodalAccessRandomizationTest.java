/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.run.drt.SimpleTripChainAnalysisModeIdentifier;
import org.matsim.run.singleTripStrategies.RandomSingleTripReRoute;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RaptorIntermodalAccessRandomizationTest {

    public @Rule
    MatsimTestUtils testUtils = new MatsimTestUtils();
//    ExamplesUtils examplesUtils = new ExamplesUtils();

    private static final String WEIRD_WALK = "weirdWalk";
    private static final Logger log = Logger.getLogger( RaptorIntermodalAccessRandomizationTest.class ) ;
    Id<Person> agent1Id = Id.createPersonId("weirdWalkTravelTimeDifferentFromRoutedAgent");
    Id<Person> agent2Id = Id.createPersonId("weirdWalkTravelTimeSameAsRoutedAgent");

    @Test
    public void accessModeWithDifferentTravelTimeThanRoutedTest() {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "multimodalnetwork.xml").toString());
        config.transit().setTransitScheduleFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "transitschedule.xml").toString());
        config.transit().setVehiclesFile(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "transitVehicles.xml").toString());

        config.transit().setUseTransit(true);

        config.controler().setWritePlansInterval(1);
        config.controler().setWriteEventsInterval(1);
        config.controler().setWriteTripsInterval(1);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setLastIteration(50);
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());
        config.plans().setHandlingOfPlansWithoutRoutingMode(PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);

        PlansCalcRouteConfigGroup.ModeRoutingParams bikeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams();
        bikeRoutingParams.setMode(TransportMode.bike);
        bikeRoutingParams.setTeleportedModeSpeed(1.0);
        bikeRoutingParams.setBeelineDistanceFactor(1.0);
        config.plansCalcRoute().addModeRoutingParams(bikeRoutingParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams normalWalkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams();
        normalWalkRoutingParams.setMode(TransportMode.walk);
        normalWalkRoutingParams.setTeleportedModeSpeed(1.0);
        normalWalkRoutingParams.setBeelineDistanceFactor(1.0);
        config.plansCalcRoute().addModeRoutingParams(normalWalkRoutingParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams weirdWalkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams();
        weirdWalkRoutingParams.setMode(WEIRD_WALK);
        weirdWalkRoutingParams.setTeleportedModeSpeed(2.0);
        weirdWalkRoutingParams.setBeelineDistanceFactor(1.0);
        config.plansCalcRoute().addModeRoutingParams(weirdWalkRoutingParams);

        config.planCalcScore().setPerforming_utils_hr(-3.6); // -> 1s = 1/100 util, 1m/s -> 1m = 1/100 util

        PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeParams.setTypicalDuration(7*3600.0);
        config.planCalcScore().addActivityParams(homeParams);

        PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workParams.setTypicalDuration(2*3600.0);
        config.planCalcScore().addActivityParams(workParams);

        PlanCalcScoreConfigGroup.ModeParams bikeScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike);
        bikeScoreParams.setConstant(-100);
        config.planCalcScore().addModeParams(bikeScoreParams);

        PlanCalcScoreConfigGroup.ModeParams ptScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.pt);
        config.planCalcScore().addModeParams(ptScoreParams);

        PlanCalcScoreConfigGroup.ModeParams normalWalkScoreParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.walk);
        config.planCalcScore().addModeParams(normalWalkScoreParams);

        PlanCalcScoreConfigGroup.ModeParams weirdWalkScoreParams = new PlanCalcScoreConfigGroup.ModeParams(WEIRD_WALK);
        weirdWalkScoreParams.setConstant(-0.4);
        config.planCalcScore().addModeParams(weirdWalkScoreParams);

        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        StrategyConfigGroup.StrategySettings stratSetsChangeExpBeta = new StrategyConfigGroup.StrategySettings();
        stratSetsChangeExpBeta.setStrategyName("ChangeExpBeta");
        stratSetsChangeExpBeta.setWeight(0.5);
        config.strategy().addStrategySettings(stratSetsChangeExpBeta);

        StrategyConfigGroup.StrategySettings stratSetsSingleTripReRoute = new StrategyConfigGroup.StrategySettings();
        stratSetsSingleTripReRoute.setStrategyName("RandomSingleTripReRoute");
        stratSetsSingleTripReRoute.setWeight(0.3);
        config.strategy().addStrategySettings(stratSetsSingleTripReRoute);

        StrategyConfigGroup.StrategySettings stratSetsModeChoice = new StrategyConfigGroup.StrategySettings();
        stratSetsModeChoice.setStrategyName("SubtourModeChoice");
        stratSetsModeChoice.setWeight(0.2);
        config.strategy().addStrategySettings(stratSetsModeChoice);

        config.subtourModeChoice().setModes(new String[]{TransportMode.pt, TransportMode.bike});
        config.subtourModeChoice().setChainBasedModes(new String[]{}); // avoid complexity

        SwissRailRaptorConfigGroup raptorConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
        raptorConfig.setUseIntermodalAccessEgress(true);

        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccessParams = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        walkAccessParams.setInitialSearchRadius(10000);
        walkAccessParams.setSearchExtensionRadius(1000);
        walkAccessParams.setMaxRadius(10000);
        walkAccessParams.setMode(TransportMode.walk);
        raptorConfig.addIntermodalAccessEgress(walkAccessParams);

        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet weirdWalkAccessParams = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        weirdWalkAccessParams.setInitialSearchRadius(10000);
        weirdWalkAccessParams.setSearchExtensionRadius(1000);
        weirdWalkAccessParams.setMaxRadius(10000);
        weirdWalkAccessParams.setMode(WEIRD_WALK);
        raptorConfig.addIntermodalAccessEgress(weirdWalkAccessParams);

        BerlinExperimentalConfigGroup berlinConfig = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
        BerlinExperimentalConfigGroup.IntermodalAccessEgressModeUtilityRandomization weirdWalkRandomizationConfig = new BerlinExperimentalConfigGroup.IntermodalAccessEgressModeUtilityRandomization();
        weirdWalkRandomizationConfig.setAccessEgressMode(WEIRD_WALK);
        weirdWalkRandomizationConfig.setAdditiveRandomizationWidth(100);
        berlinConfig.addIntermodalAccessEgressModeUtilityRandomization(weirdWalkRandomizationConfig);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        //create test agents
        PopulationFactory pf = scenario.getPopulation().getFactory();
        ActivityFacilitiesFactory activityFacilitiesFactory = scenario.getActivityFacilities().getFactory();

        Id<ActivityFacility> homeFacilityId = Id.create("home", ActivityFacility.class);
        ActivityFacility homeFacility = activityFacilitiesFactory.createActivityFacility(homeFacilityId, CoordUtils.createCoord(50,1050), Id.createLinkId("1211")); // 1000m left of transit stop 1
        scenario.getActivityFacilities().addActivityFacility(homeFacility);

        Id<ActivityFacility> workFacilityId = Id.create("work", ActivityFacility.class);
        ActivityFacility shopFacility = activityFacilitiesFactory.createActivityFacility(workFacilityId, CoordUtils.createCoord(4450,1050), Id.createLinkId("4142")); // 500m right of transit stop 3
        scenario.getActivityFacilities().addActivityFacility(shopFacility);

        Person agent1 = pf.createPerson(agent1Id);
        scenario.getPopulation().addPerson(agent1);
        createAndAddPlan(pf, agent1, homeFacilityId, workFacilityId);

        Person agent2 = pf.createPerson(agent2Id);
        scenario.getPopulation().addPerson(agent2);
        createAndAddPlan(pf, agent2, homeFacilityId, workFacilityId);

        Map<Id<Person>, Map<Integer, Double>> person2AccessTripNr2AddedTravelTime = new HashMap<>();
        Map<Integer, Double> agent1TravelTimeChanges = new HashMap<>();
        agent1TravelTimeChanges.put(0, 1000.0);
        agent1TravelTimeChanges.put(1, -250.0);
        person2AccessTripNr2AddedTravelTime.put(agent1Id, agent1TravelTimeChanges);

        Controler controler = new Controler(scenario);
        final TravelTimeInMobsimModifier travelTimeModifier = new TravelTimeInMobsimModifier(person2AccessTripNr2AddedTravelTime);

        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                install( new SwissRailRaptorModule() );
            }
        } );

        controler.addOverridingModule( new AbstractModule(){
                @Override public void install() {
                    addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
                    bind(RaptorIntermodalAccessEgress.class).to(BerlinRaptorIntermodalAccessEgress.class);
                    this.addControlerListenerBinding().toInstance( travelTimeModifier );
                    bind(AnalysisMainModeIdentifier.class).to(SimpleTripChainAnalysisModeIdentifier.class);
                }
            });

        controler.run();

        /*
         * pt access/egress utilities:
         * access 1000m to stop 1:
         * walk: 1000m/1m/s = 1000s -> -1 utils
         * weirdWalk: 1000m/2m/s = 500s -> -0.5 utils -0.4 ASC utils = -0.9
         * --> router should prefer weirdWalk,
         * but for agent1 we add 1000s travel time -> weirdWalk = -0.9 - 1.0 utils = -1.9 -> worse.
         * Agent2 unchanged -0.9
         */
        Assert.assertEquals(TransportMode.walk, ((Leg) agent1.getSelectedPlan().getPlanElements().get(1)).getMode());
        Assert.assertEquals(WEIRD_WALK, ((Leg) agent2.getSelectedPlan().getPlanElements().get(1)).getMode());

        /*
         * pt access/egress utilities:
         * return journey:
         * access 500m to stop 3:
         * walk: 500m/1m/s = 500s -> -0.5 utils
         * weirdWalk: 500m/2m/s = 250s -> -0.25 utils -0.4 ASC utils = -0.65
         * --> router should prefer walk,
         * but for agent1 we deduct 250s travel time -> weirdWalk = -0.65 + 0.25 utils = -0.4 -> better.
         * Agent2 unchanged -0.65
         */
        Assert.assertEquals(WEIRD_WALK, ((Leg) agent1.getSelectedPlan().getPlanElements().get(7)).getMode());
        Assert.assertEquals(TransportMode.walk, ((Leg) agent2.getSelectedPlan().getPlanElements().get(7)).getMode());
    }

    private void createAndAddPlan(PopulationFactory pf, Person agent, Id<ActivityFacility> homeFacilityId, Id<ActivityFacility> workFacilityId) {
        Plan plan = pf.createPlan();
        agent.addPlan(plan);

        Activity home = pf.createActivityFromActivityFacilityId("home", homeFacilityId);
        home.setEndTime(7. * 3600.);
        plan.addActivity(home);

        Leg leg1 = pf.createLeg(TransportMode.pt);
        plan.addLeg(leg1);

        Activity work = pf.createActivityFromActivityFacilityId("work", workFacilityId);
        work.setEndTime(17. * 3600. - 60); // with 0 s weirdWalk time should manage to catch the 17:00:00 train (planned 250s, so would have missed)
        plan.addActivity(work);

        Leg leg2 = pf.createLeg(TransportMode.pt);
        plan.addLeg(leg2);

        Activity home2 = pf.createActivityFromActivityFacilityId("home", homeFacilityId);
        home2.setEndTime(Double.POSITIVE_INFINITY);
        plan.addActivity(home2);
    }

    private class TravelTimeInMobsimModifier implements BeforeMobsimListener, AfterMobsimListener {
        private final Map<Id<Person>, Map<Integer, Double>> person2AccessTripNr2AddedTravelTime;

        TravelTimeInMobsimModifier (Map<Id<Person>, Map<Integer, Double>> person2LegNr2AddedTravelTime) {
            this.person2AccessTripNr2AddedTravelTime = person2LegNr2AddedTravelTime;
        }

        // change travel time for mobsim
        @Override
        public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) {
            Population pop = beforeMobsimEvent.getServices().getScenario().getPopulation();

            // log output selected mode chains for debugging
            String outLog = "modeChoice iteration " + beforeMobsimEvent.getIteration() + ": ";
            for (Person person: pop.getPersons().values()) {
                Plan plan = person.getSelectedPlan();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (int i = 0; i < trips.size(); i++) {
                    outLog += "Agent: " + person.getId() + " tripNr " + i + " modes: " + trips.get(i).getLegsOnly().stream().map(leg -> leg.getMode() + "-").collect(Collectors.joining()) + "\t";
                }
            }
            log.warn(outLog);

            // keep consistent with asserts in accessModeWithDifferentTravelTimeThanRoutedTest
            if (TransportMode.walk.equals(((Leg) pop.getPersons().get(agent1Id).getSelectedPlan().getPlanElements().get(1)).getMode()) && WEIRD_WALK.equals(((Leg) pop.getPersons().get(agent2Id).getSelectedPlan().getPlanElements().get(1)).getMode()) &&
                    WEIRD_WALK.equals(((Leg) pop.getPersons().get(agent1Id).getSelectedPlan().getPlanElements().get(7)).getMode()) && TransportMode.walk.equals(((Leg) pop.getPersons().get(agent2Id).getSelectedPlan().getPlanElements().get(7)).getMode())) {
                log.error("iteration " + beforeMobsimEvent.getIteration() + " correct modes assigned");
            }

            for (Map.Entry<Id<Person>, Map<Integer, Double>> personId2TripChanges: person2AccessTripNr2AddedTravelTime.entrySet()) {
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(pop.getPersons().get(personId2TripChanges.getKey()).getSelectedPlan());
                for (Map.Entry<Integer, Double> tripNr2Addition: personId2TripChanges.getValue().entrySet()) {
                    Leg firstLeg = trips.get(tripNr2Addition.getKey()).getLegsOnly().get(0);
                    if (firstLeg.getMode().equals(WEIRD_WALK)) {
                        firstLeg.setTravelTime(firstLeg.getTravelTime().seconds() + tripNr2Addition.getValue());
                        firstLeg.getRoute().setTravelTime(firstLeg.getRoute().getTravelTime().seconds() + tripNr2Addition.getValue());
                    }
                }
            }
        }

        // change travel time back after mobsim
        @Override
        public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
            Population pop = afterMobsimEvent.getServices().getScenario().getPopulation();
            for (Map.Entry<Id<Person>, Map<Integer, Double>> personId2TripChanges: person2AccessTripNr2AddedTravelTime.entrySet()) {
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(pop.getPersons().get(personId2TripChanges.getKey()).getSelectedPlan());
                for (Map.Entry<Integer, Double> tripNr2Addition: personId2TripChanges.getValue().entrySet()) {
                    Leg firstLeg = trips.get(tripNr2Addition.getKey()).getLegsOnly().get(0);
                    if (firstLeg.getMode().equals(WEIRD_WALK)) {
                        firstLeg.setTravelTime(firstLeg.getTravelTime().seconds() - tripNr2Addition.getValue());
                        firstLeg.getRoute().setTravelTime(firstLeg.getRoute().getTravelTime().seconds() - tripNr2Addition.getValue());
                    }
                }
            }
        }
    }
}
