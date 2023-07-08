package org.matsim.run;

/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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


import org.locationtech.jts.algorithm.Distance;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.policies.DistanceBasedMoneyReward;
import org.matsim.policies.MobilityBudgetEventHandler;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunBerlinWithMobBudget {

    private static double dailyMobilityBudget;
    private static double distanceBasedMoneyReward;
    private static Map<Id<Person>, Double> personsEligibleForMobilityBudget = new HashMap<>();


    public static void main(String[] args) {

        if (args.length == 0) {
            args = new String[]{"",
                    "1.0",
                    "scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml",
            };
        }
        dailyMobilityBudget = Double.parseDouble(args[0]);
        distanceBasedMoneyReward = Double.parseDouble(args[1]);

        String[] configArgs = new String[args.length - 2];
        for (int i = 2; i <= args.length - 1; i++) {
            configArgs[i - 2] = args[i];
        }


        Config config = RunBerlinScenario.prepareConfig(configArgs);

        Scenario scenario = RunBerlinScenario.prepareScenario(config);

        for (Map.Entry<Id<Person>, Double> entry : getPersonsEligibleForMobilityBudget2FixedValue(scenario, dailyMobilityBudget).entrySet()) {
            Id<Person> person = entry.getKey();
            Double budget = entry.getValue();
            personsEligibleForMobilityBudget.put(person, budget);
        }
        MobilityBudgetEventHandler mobilityBudgetEventHandler = new MobilityBudgetEventHandler(personsEligibleForMobilityBudget);

        Controler controler = RunBerlinScenario.prepareControler(scenario);
        addMobilityBudgetHandler(controler, mobilityBudgetEventHandler);
        if (distanceBasedMoneyReward != 0) {
            DistanceBasedMoneyReward klimaTaler = new DistanceBasedMoneyReward(controler.getScenario().getConfig().plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk), controler.getScenario().getNetwork(), distanceBasedMoneyReward);
            addKlimaTaler(controler, klimaTaler);
        }

        controler.run();

    }


    static Map<Id<Person>, Double> getPersonsEligibleForMobilityBudget2FixedValue(Scenario scenario, Double value) {

        Map<Id<Person>, Double> persons2Budget = new HashMap<>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();
                //TripStructureUtil get Legs
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                    }
                }
                if (transportModeList.contains(TransportMode.car)) {
                    persons2Budget.put(personId, value);
                    Plan planNoCar = PopulationUtils.createPlan();
                    PopulationUtils.copyFromTo(plan, planNoCar);
                    List<TripStructureUtils.Trip> tripsNewPlan = TripStructureUtils.getTrips(planNoCar);
                    PopulationUtils.resetRoutes(planNoCar);
                    for (TripStructureUtils.Trip trip: tripsNewPlan) {
                        List<Leg> listLegs = trip.getLegsOnly();
                        for (Leg leg: listLegs) {
                            if (leg.getMode().equals(TransportMode.car)) {
                                leg.setMode(TransportMode.walk);
                            }
                        }
                    }
                    MainModeIdentifier OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();
                    new TripsToLegsAlgorithm(OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier).run(planNoCar);
                    person.addPlan(planNoCar);
                }
            }
        }
        return persons2Budget;
    }


    public static void addMobilityBudgetHandler(Controler controler, MobilityBudgetEventHandler mobilityBudgetEventHandler) {
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(mobilityBudgetEventHandler);
                addControlerListenerBinding().toInstance(mobilityBudgetEventHandler);
            }
        });
    }

    public static void addKlimaTaler(Controler controler, DistanceBasedMoneyReward klimaTaler) {
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(klimaTaler);
                addControlerListenerBinding().toInstance(klimaTaler);
                new PersonMoneyEventsAnalysisModule();
            }
        });
    }
}