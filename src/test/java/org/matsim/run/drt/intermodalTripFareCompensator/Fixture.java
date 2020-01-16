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

package org.matsim.run.drt.intermodalTripFareCompensator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

class Fixture {
	
    Id<Person> personIdNoPtButDrt = Id.createPersonId("NoPtButDrt");
    Id<Person> personIdPtNoDrt = Id.createPersonId("PtNoDrt");
    Id<Person> personIdPt1DrtSameTrip = Id.createPersonId("Pt1DrtSameTrip");
    Id<Person> personIdPt1DrtDifferentTrips = Id.createPersonId("Pt1DrtDifferentTrips");
    Id<Person> personIdPt3DrtIn2IntermodalTrips = Id.createPersonId("Pt3DrtIn2IntermodalTrips");
    
    Config config;
    Scenario scenario;
    
    Fixture() {
    	buildConfig();
    	this.scenario = ScenarioUtils.createScenario(config);
    	buildAndAddNetwork(scenario);
    	buildAndAddAgents(scenario);
    }
	
	private void buildConfig() {
		this.config = ConfigUtils.createConfig();
		config.qsim().setEndTime(10 * 3600.0);
		
		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(DefaultSelector.KeepLastSelected);
		stratSets.setWeight(1.0);
		config.strategy().addStrategySettings(stratSets);
		
		ActivityParams actParamsHome = new ActivityParams("home");
		actParamsHome.setTypicalDuration(1.0 * 3600);
		config.planCalcScore().addActivityParams(actParamsHome);
		
		ActivityParams actParamsWork = new ActivityParams("work");
		actParamsWork.setTypicalDuration(1.0 * 3600);
		config.planCalcScore().addActivityParams(actParamsWork);
		
		ActivityParams actParamsPtInteraction = new ActivityParams("pt interaction");
		actParamsPtInteraction.setTypicalDuration(0.0);
		actParamsPtInteraction.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(actParamsPtInteraction);
		
		ActivityParams actParamsDrtInteraction = new ActivityParams("drt interaction");
		actParamsDrtInteraction.setTypicalDuration(0.0);
		actParamsDrtInteraction.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(actParamsDrtInteraction);
		
		// install (teleported dummy) modes
		ModeRoutingParams walkModeRoutingParams = new ModeRoutingParams(TransportMode.walk);
		walkModeRoutingParams.setBeelineDistanceFactor(1.0);
		walkModeRoutingParams.setTeleportedModeSpeed(5.0);
		config.plansCalcRoute().addModeRoutingParams(walkModeRoutingParams);
		
		ModeRoutingParams ptModeRoutingParams = new ModeRoutingParams(TransportMode.pt);
		ptModeRoutingParams.setBeelineDistanceFactor(1.0);
		ptModeRoutingParams.setTeleportedModeSpeed(5.0);
		config.plansCalcRoute().addModeRoutingParams(ptModeRoutingParams);
		
		ModeRoutingParams drtModeRoutingParams = new ModeRoutingParams(TransportMode.drt);
		drtModeRoutingParams.setBeelineDistanceFactor(1.0);
		drtModeRoutingParams.setTeleportedModeSpeed(5.0);
		config.plansCalcRoute().addModeRoutingParams(drtModeRoutingParams);
		
		ModeParams walkModeParams = new ModeParams(TransportMode.walk);
		config.planCalcScore().addModeParams(walkModeParams);
		
		ModeParams ptModeParams = new ModeParams(TransportMode.pt);
		config.planCalcScore().addModeParams(ptModeParams);
		
		ModeParams drtModeParams = new ModeParams(TransportMode.drt);
		config.planCalcScore().addModeParams(drtModeParams);
	}

	private void buildAndAddNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000, 0));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(2000, 0));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(3000, 0));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(4000, 0));
		Node node6 = network.getFactory().createNode(Id.create("6", Node.class), new Coord(5000, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);
		// forward links
		Link link12 = network.getFactory().createLink(Id.create("12", Link.class), node1, node2);
		Link link23 = network.getFactory().createLink(Id.create("23", Link.class), node2, node3);
		Link link34 = network.getFactory().createLink(Id.create("34", Link.class), node3, node4);
		Link link45 = network.getFactory().createLink(Id.create("45", Link.class), node4, node5);
		Link link56 = network.getFactory().createLink(Id.create("56", Link.class), node5, node6);
		network.addLink(link12);
		network.addLink(link23);
		network.addLink(link34);
		network.addLink(link45);
		network.addLink(link56);
		// back links
		Link link21 = network.getFactory().createLink(Id.create("21", Link.class), node2, node1);
		Link link32 = network.getFactory().createLink(Id.create("32", Link.class), node3, node2);
		Link link43 = network.getFactory().createLink(Id.create("43", Link.class), node4, node3);
		Link link54 = network.getFactory().createLink(Id.create("54", Link.class), node5, node4);
		Link link65 = network.getFactory().createLink(Id.create("65", Link.class), node6, node5);
		network.addLink(link21);
		network.addLink(link32);
		network.addLink(link43);
		network.addLink(link54);
		network.addLink(link65);
	}
	
	private void buildAndAddAgents(Scenario scenario) {
		Population population = scenario.getPopulation();
		
		{
			Person personNoPtButDrt = population.getFactory().createPerson(personIdNoPtButDrt);
			population.addPerson(personNoPtButDrt);
			Plan planNoPtButDrt = PopulationUtils.createPlan(personNoPtButDrt);
			personNoPtButDrt.addPlan(planNoPtButDrt);
			
			Activity act1 = PopulationUtils.createAndAddActivityFromLinkId(planNoPtButDrt, "home", Id.create("12", Link.class));
			act1.setEndTime(0.0);
			Leg leg1 = createAddAndRouteLeg(planNoPtButDrt, TransportMode.walk, Id.create("12", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg1, TransportMode.drt);
			Activity actStage1 = PopulationUtils.createAndAddActivityFromLinkId(planNoPtButDrt, "drt interaction", Id.create("23", Link.class));
			actStage1.setMaximumDuration(0);
			Leg leg2 = createAddAndRouteLeg(planNoPtButDrt, TransportMode.drt, Id.create("23", Link.class), Id.create("34", Link.class));
			TripStructureUtils.setRoutingMode(leg2, TransportMode.drt);
			Activity actStage2 = PopulationUtils.createAndAddActivityFromLinkId(planNoPtButDrt, "drt interaction", Id.create("34", Link.class));
			actStage2.setMaximumDuration(0);
			Leg leg3 = createAddAndRouteLeg(planNoPtButDrt, TransportMode.walk, Id.create("34", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg3, TransportMode.drt);
			Activity act2 = PopulationUtils.createAndAddActivityFromLinkId(planNoPtButDrt, "work", Id.create("23", Link.class));
			act2.setEndTime(1 * 3600.0);
			Leg leg4 = createAddAndRouteLeg(planNoPtButDrt, TransportMode.walk, Id.create("23", Link.class), Id.create("12", Link.class));
			TripStructureUtils.setRoutingMode(leg4, TransportMode.walk);
			Activity act3 = PopulationUtils.createAndAddActivityFromLinkId(planNoPtButDrt, "home", Id.create("12", Link.class));
			act3.setEndTime(Double.POSITIVE_INFINITY);
		}
		
		{
			Person personPtNoDrt = population.getFactory().createPerson(personIdPtNoDrt);
			population.addPerson(personPtNoDrt);
			Plan planPtNoDrt = PopulationUtils.createPlan(personPtNoDrt);
			personPtNoDrt.addPlan(planPtNoDrt);
			
			Activity act1 = PopulationUtils.createAndAddActivityFromLinkId(planPtNoDrt, "home", Id.create("12", Link.class));
			act1.setEndTime(0.0);
			Leg leg1 = createAddAndRouteLeg(planPtNoDrt, TransportMode.walk, Id.create("12", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg1, TransportMode.pt);
			Activity actStage1 = PopulationUtils.createAndAddActivityFromLinkId(planPtNoDrt, "pt interaction", Id.create("23", Link.class));
			actStage1.setMaximumDuration(0);
			Leg leg2 = createAddAndRouteLeg(planPtNoDrt, TransportMode.pt, Id.create("23", Link.class), Id.create("34", Link.class));
			TripStructureUtils.setRoutingMode(leg2, TransportMode.pt);
			Activity actStage2 = PopulationUtils.createAndAddActivityFromLinkId(planPtNoDrt, "pt interaction", Id.create("34", Link.class));
			actStage2.setMaximumDuration(0);
			Leg leg3 = createAddAndRouteLeg(planPtNoDrt, TransportMode.walk, Id.create("34", Link.class), Id.create("45", Link.class));
			TripStructureUtils.setRoutingMode(leg3, TransportMode.pt);
			Activity act2 = PopulationUtils.createAndAddActivityFromLinkId(planPtNoDrt, "work", Id.create("45", Link.class));
			act2.setEndTime(1 * 3600.0);
			Leg leg4 = createAddAndRouteLeg(planPtNoDrt, TransportMode.walk, Id.create("45", Link.class), Id.create("12", Link.class));
			TripStructureUtils.setRoutingMode(leg4, TransportMode.walk);
			Activity act3 = PopulationUtils.createAndAddActivityFromLinkId(planPtNoDrt, "home", Id.create("12", Link.class));
			act3.setEndTime(Double.POSITIVE_INFINITY);
		}

		{
			Person personPt1DrtSameTrip = population.getFactory().createPerson(personIdPt1DrtSameTrip);
			population.addPerson(personPt1DrtSameTrip);
			Plan planPt1DrtSameTrip = PopulationUtils.createPlan(personPt1DrtSameTrip);
			personPt1DrtSameTrip.addPlan(planPt1DrtSameTrip);
			
			Activity act1 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "home", Id.create("12", Link.class));
			act1.setEndTime(0.0);
			Leg leg1 = createAddAndRouteLeg(planPt1DrtSameTrip, TransportMode.walk, Id.create("12", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg1, TransportMode.pt);
			Activity actStage1 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "pt interaction", Id.create("23", Link.class));
			actStage1.setMaximumDuration(0);
			Leg leg2 = createAddAndRouteLeg(planPt1DrtSameTrip, TransportMode.pt, Id.create("23", Link.class), Id.create("34", Link.class));
			TripStructureUtils.setRoutingMode(leg2, TransportMode.pt);
			Activity actStage2 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "pt interaction", Id.create("34", Link.class));
			actStage2.setMaximumDuration(0);
			Leg leg3 = createAddAndRouteLeg(planPt1DrtSameTrip, TransportMode.walk, Id.create("34", Link.class), Id.create("45", Link.class));
			TripStructureUtils.setRoutingMode(leg3, TransportMode.pt);
			Activity actStage3 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "pt interaction", Id.create("45", Link.class));
			actStage3.setMaximumDuration(0);
			Leg leg4 = createAddAndRouteLeg(planPt1DrtSameTrip, TransportMode.drt, Id.create("45", Link.class), Id.create("56", Link.class));
			TripStructureUtils.setRoutingMode(leg4, TransportMode.pt);
			Activity actStage4 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "pt interaction", Id.create("56", Link.class));
			actStage4.setMaximumDuration(0);
			Leg leg5 = createAddAndRouteLeg(planPt1DrtSameTrip, TransportMode.walk, Id.create("56", Link.class), Id.create("56", Link.class));
			TripStructureUtils.setRoutingMode(leg5, TransportMode.pt);
			Activity act2 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "work", Id.create("56", Link.class));
			act2.setEndTime(1 * 3600.0);
			Leg leg6 = createAddAndRouteLeg(planPt1DrtSameTrip, TransportMode.walk, Id.create("56", Link.class), Id.create("12", Link.class));
			TripStructureUtils.setRoutingMode(leg6, TransportMode.pt);
			Activity act3 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtSameTrip, "home", Id.create("12", Link.class));
			act3.setEndTime(Double.POSITIVE_INFINITY);
		}
		
		{
			Person personPt1DrtDifferentTrips = population.getFactory().createPerson(personIdPt1DrtDifferentTrips);
			population.addPerson(personPt1DrtDifferentTrips);
			Plan planPt1DrtDifferentTrips = PopulationUtils.createPlan(personPt1DrtDifferentTrips);
			personPt1DrtDifferentTrips.addPlan(planPt1DrtDifferentTrips);
			
			Activity act1 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "home", Id.create("12", Link.class));
			act1.setEndTime(0.0);
			Leg leg1 = createAddAndRouteLeg(planPt1DrtDifferentTrips, TransportMode.walk, Id.create("12", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg1, TransportMode.pt);
			Activity actStage1 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "pt interaction", Id.create("23", Link.class));
			actStage1.setMaximumDuration(0);
			Leg leg2 = createAddAndRouteLeg(planPt1DrtDifferentTrips, TransportMode.pt, Id.create("23", Link.class), Id.create("34", Link.class));
			TripStructureUtils.setRoutingMode(leg2, TransportMode.pt);
			Activity actStage2 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "pt interaction", Id.create("34", Link.class));
			actStage2.setMaximumDuration(0);
			Leg leg3 = createAddAndRouteLeg(planPt1DrtDifferentTrips, TransportMode.walk, Id.create("34", Link.class), Id.create("56", Link.class));
			TripStructureUtils.setRoutingMode(leg3, TransportMode.pt);
			Activity act2 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "work", Id.create("56", Link.class));
			act2.setEndTime(1 * 3600.0);
			Leg leg4 = createAddAndRouteLeg(planPt1DrtDifferentTrips, TransportMode.walk, Id.create("56", Link.class), Id.create("54", Link.class));
			TripStructureUtils.setRoutingMode(leg4, TransportMode.drt);
			Activity actStage3 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "drt interaction", Id.create("54", Link.class));
			actStage3.setMaximumDuration(0);
			Leg leg5 = createAddAndRouteLeg(planPt1DrtDifferentTrips, TransportMode.drt, Id.create("54", Link.class), Id.create("32", Link.class));
			TripStructureUtils.setRoutingMode(leg5, TransportMode.drt);
			Activity actStage4 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "drt interaction", Id.create("32", Link.class));
			actStage4.setMaximumDuration(0);
			Leg leg6 = createAddAndRouteLeg(planPt1DrtDifferentTrips, TransportMode.walk, Id.create("32", Link.class), Id.create("12", Link.class));
			TripStructureUtils.setRoutingMode(leg6, TransportMode.drt);
			Activity act3 = PopulationUtils.createAndAddActivityFromLinkId(planPt1DrtDifferentTrips, "home", Id.create("12", Link.class));
			act3.setEndTime(Double.POSITIVE_INFINITY);
		}
		
		{
			Person personPt3DrtIn2IntermodalTrips = population.getFactory().createPerson(personIdPt3DrtIn2IntermodalTrips);
			population.addPerson(personPt3DrtIn2IntermodalTrips);
			Plan planPt3DrtIn2IntermodalTrips = PopulationUtils.createPlan(personPt3DrtIn2IntermodalTrips);
			personPt3DrtIn2IntermodalTrips.addPlan(planPt3DrtIn2IntermodalTrips);
			
			Activity act1 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "home", Id.create("12", Link.class));
			act1.setEndTime(0.0);
			Leg leg1 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("12", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg1, TransportMode.pt);
			Activity actStage1 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("23", Link.class));
			actStage1.setMaximumDuration(0);
			Leg leg2 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.drt, Id.create("23", Link.class), Id.create("34", Link.class));
			TripStructureUtils.setRoutingMode(leg2, TransportMode.pt);
			Activity actStage2 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("34", Link.class));
			actStage2.setMaximumDuration(0);
			Leg leg3 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("34", Link.class), Id.create("45", Link.class));
			TripStructureUtils.setRoutingMode(leg3, TransportMode.pt);
			Activity actStage3 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("45", Link.class));
			actStage3.setMaximumDuration(0);
			Leg leg4 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.pt, Id.create("45", Link.class), Id.create("56", Link.class));
			TripStructureUtils.setRoutingMode(leg4, TransportMode.pt);
			Activity actStage4 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("56", Link.class));
			actStage4.setMaximumDuration(0);
			Leg leg5 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("56", Link.class), Id.create("65", Link.class));
			TripStructureUtils.setRoutingMode(leg5, TransportMode.pt);
			Activity actStage5 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("65", Link.class));
			actStage5.setMaximumDuration(0);
			Leg leg6 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.pt, Id.create("65", Link.class), Id.create("54", Link.class));
			TripStructureUtils.setRoutingMode(leg6, TransportMode.pt);
			Activity actStage6 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("54", Link.class));
			actStage6.setMaximumDuration(0);
			Leg leg7 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("54", Link.class), Id.create("43", Link.class));
			TripStructureUtils.setRoutingMode(leg7, TransportMode.pt);
			Activity actStage7 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("43", Link.class));
			actStage7.setMaximumDuration(0);
			Leg leg8 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.drt, Id.create("43", Link.class), Id.create("32", Link.class));
			TripStructureUtils.setRoutingMode(leg8, TransportMode.pt);
			Activity actStage8 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("32", Link.class));
			actStage8.setMaximumDuration(0);
			Leg leg9 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("32", Link.class), Id.create("21", Link.class));
			TripStructureUtils.setRoutingMode(leg9, TransportMode.pt);
			Activity act2 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "work", Id.create("21", Link.class));
			act2.setEndTime(1 * 3600.0);
			Leg leg10 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("21", Link.class), Id.create("12", Link.class));
			TripStructureUtils.setRoutingMode(leg10, TransportMode.pt);
			Activity actStage9 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("12", Link.class));
			actStage9.setMaximumDuration(0);
			Leg leg11 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.drt, Id.create("12", Link.class), Id.create("23", Link.class));
			TripStructureUtils.setRoutingMode(leg11, TransportMode.pt);
			Activity actStage10 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("23", Link.class));
			actStage10.setMaximumDuration(0);
			Leg leg12 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("23", Link.class), Id.create("34", Link.class));
			TripStructureUtils.setRoutingMode(leg12, TransportMode.pt);
			Activity actStage11 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("34", Link.class));
			actStage11.setMaximumDuration(0);
			Leg leg13 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.pt, Id.create("34", Link.class), Id.create("43", Link.class));
			TripStructureUtils.setRoutingMode(leg13, TransportMode.pt);
			Activity actStage12 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "pt interaction", Id.create("43", Link.class));
			actStage12.setMaximumDuration(0);
			Leg leg14 = createAddAndRouteLeg(planPt3DrtIn2IntermodalTrips, TransportMode.walk, Id.create("43", Link.class), Id.create("12", Link.class));
			TripStructureUtils.setRoutingMode(leg14, TransportMode.pt);
			Activity act3 = PopulationUtils.createAndAddActivityFromLinkId(planPt3DrtIn2IntermodalTrips, "home", Id.create("12", Link.class));
			act3.setEndTime(Double.POSITIVE_INFINITY);
		}
	}
	
	private Leg createAddAndRouteLeg(Plan plan, String mode, Id<Link> startLinkId, Id<Link> endLinkId) {
		Leg leg = PopulationUtils.createAndAddLeg(plan, mode);
		Route route = new GenericRouteImpl(startLinkId, endLinkId);
		route.setTravelTime(123.0);
		route.setDistance(456.0);
		leg.setRoute(route);
		return leg;
	}

}
