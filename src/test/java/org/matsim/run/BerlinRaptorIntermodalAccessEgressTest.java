/* *********************************************************************** *
 * project: org.matsim.*												   *
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

import java.util.ArrayList;
import java.util.List;

import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress.RIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;

/**
 * @author vsp-gleich
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BerlinRaptorIntermodalAccessEgressTest {
	private static final Logger log = Logger.getLogger( BerlinRaptorIntermodalAccessEgressTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void testDrtAccess() {
		List<PlanElement> legs = new ArrayList<>();
		RaptorParameters params = null;

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population pop = scenario.getPopulation();
		PopulationFactory f = pop.getFactory();
		Person person = f.createPerson(Id.createPersonId("personSubpopulationNull"));

		// daily constants / rates are ignored, but set them anyway (to see whether they are used by error)
		PlanCalcScoreConfigGroup scoreCfg = config.planCalcScore();
		scoreCfg.setMarginalUtilityOfMoney(1.0);
		scoreCfg.setPerforming_utils_hr(0.00011 * 3600.0);
		ModeParams walkParams = scoreCfg.getOrCreateModeParams(TransportMode.walk);
		walkParams.setConstant(-1.2);
		walkParams.setDailyMonetaryConstant(-1.3);
		walkParams.setDailyUtilityConstant(-1.4);
		walkParams.setMarginalUtilityOfDistance(-0.00015);
		walkParams.setMarginalUtilityOfTraveling(-0.00016 * 3600.0);
		walkParams.setMonetaryDistanceRate(-0.00017);
		ModeParams drtParams = scoreCfg.getOrCreateModeParams(TransportMode.drt);
		drtParams.setConstant(-2.1);
		drtParams.setDailyMonetaryConstant(-2.2);
		drtParams.setDailyUtilityConstant(-2.3);
		drtParams.setMarginalUtilityOfDistance(-0.00024);
		drtParams.setMarginalUtilityOfTraveling(-0.00025 * 3600.0);
		drtParams.setMonetaryDistanceRate(-0.00026);
		
		DrtFaresConfigGroup drtFaresConfigGroup = ConfigUtils.addOrGetModule(config, DrtFaresConfigGroup.class);
		DrtFareConfigGroup drtFareConfigGroup = new DrtFareConfigGroup();
		drtFareConfigGroup.setMode(TransportMode.drt);
		drtFareConfigGroup.setBasefare(1.0);
		drtFareConfigGroup.setDailySubscriptionFee(10.0);
		drtFareConfigGroup.setMinFarePerTrip(2.0);
		drtFareConfigGroup.setDistanceFare_m(0.0002);
		drtFareConfigGroup.setTimeFare_h(0.0003 * 3600);
		drtFaresConfigGroup.addParameterSet(drtFareConfigGroup);
		
		BerlinRaptorIntermodalAccessEgress raptorIntermodalAccessEgress = new BerlinRaptorIntermodalAccessEgress(config);
		
		Leg walkLeg1 = PopulationUtils.createLeg(TransportMode.walk);
		walkLeg1.setDepartureTime(7*3600.0);
		walkLeg1.setTravelTime(100);
		Route walkRoute1 = new GenericRouteImpl(Id.createLinkId("dummy1"), Id.createLinkId("dummy2"));
		walkRoute1.setDistance(200.0);
		walkLeg1.setRoute(walkRoute1);
		legs.add(walkLeg1);
		
		Leg drtLeg = PopulationUtils.createLeg(TransportMode.drt);
		drtLeg.setDepartureTime(7*3600.0 + 100);
		drtLeg.setTravelTime(600); // current total 700
		Route drtRoute = new DrtRoute(Id.createLinkId("dummy2"), Id.createLinkId("dummy3"));
		drtRoute.setDistance(5000.0);
		drtLeg.setRoute(drtRoute);
		legs.add(drtLeg);
		
		Leg walkLeg2 = PopulationUtils.createLeg(TransportMode.walk);
		walkLeg2.setDepartureTime(7*3600.0 + 700);
		walkLeg2.setTravelTime(300); // current total 1000
		Route walkRoute2 = new GenericRouteImpl(Id.createLinkId("dummy3"), Id.createLinkId("dummy4"));
		walkRoute2.setDistance(400.0);
		walkLeg2.setRoute(walkRoute2);
		legs.add(walkLeg2);
		
		RIntermodalAccessEgress result = raptorIntermodalAccessEgress.calcIntermodalAccessEgress(legs, params, person, RaptorStopFinder.Direction.ACCESS );
		
		//Asserts
		Assert.assertEquals("Total travel time is wrong!", 1000.0, result.travelTime, MatsimTestUtils.EPSILON);
		
		/* 
		 * disutility: -1 * ( ASC + distance + time + monetary distance rate + fare)
		 * 
		 * walkLeg1: -1 * (-1.2 -0.00015*200 -(0.00016+0.00011)*100 -0.00017*200 -0 ) = 1.291
		 * drtLeg: -1 * (-2.1 -0.00024*5000 -(0.00025+0.00011)*600 -0.00026*5000 -max(2.0, 1+0.0002*5000+0.0003*600) ) = 6.996
		 * walkLeg2: -1 * (-1.2 -0.00015*400 -(0.00016+0.00011)*300 -0.00017*400 -0 ) = 1.409
		 */
		Assert.assertEquals("Total disutility is wrong!", 9.696, result.disutility, MatsimTestUtils.EPSILON);

		for (int i = 0; i < legs.size(); i++) {
			Assert.assertEquals("Input legs != output legs!", legs.get(i), result.routeParts.get(i));
		}
		Assert.assertEquals("Input legs != output legs!", legs.size(), result.routeParts.size());
	}
	
	@Test
	public final void testWalkAccess() {
		List<PlanElement> legs = new ArrayList<>();
		RaptorParameters params = null;
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population pop = scenario.getPopulation();
		PopulationFactory f = pop.getFactory();
		Person person = f.createPerson(Id.createPersonId("personSubpopulationNull"));

		// daily constants / rates are ignored, but set them anyway (to see whether they are used by error)
		PlanCalcScoreConfigGroup scoreCfg = config.planCalcScore();
		scoreCfg.setMarginalUtilityOfMoney(1.0);
		scoreCfg.setPerforming_utils_hr(0.00011 * 3600.0);
		ModeParams walkParams = scoreCfg.getOrCreateModeParams(TransportMode.walk);
		walkParams.setConstant(-1.2);
		walkParams.setDailyMonetaryConstant(-1.3);
		walkParams.setDailyUtilityConstant(-1.4);
		walkParams.setMarginalUtilityOfDistance(-0.00015);
		walkParams.setMarginalUtilityOfTraveling(-0.00016 * 3600.0);
		walkParams.setMonetaryDistanceRate(-0.00017);
	
		BerlinRaptorIntermodalAccessEgress raptorIntermodalAccessEgress = new BerlinRaptorIntermodalAccessEgress(config);
		
		Leg walkLeg1 = PopulationUtils.createLeg(TransportMode.walk);
		walkLeg1.setDepartureTime(7*3600.0);
		walkLeg1.setTravelTime(100);
		Route walkRoute1 = new GenericRouteImpl(Id.createLinkId("dummy1"), Id.createLinkId("dummy2"));
		walkRoute1.setDistance(200.0);
		walkLeg1.setRoute(walkRoute1);
		legs.add(walkLeg1);
		
		RIntermodalAccessEgress result = raptorIntermodalAccessEgress.calcIntermodalAccessEgress(legs, params, person, RaptorStopFinder.Direction.ACCESS );
		
		//Asserts
		Assert.assertEquals("Total travel time is wrong!", 100.0, result.travelTime, MatsimTestUtils.EPSILON);
		
		/* 
		 * disutility: -1 * ( ASC + distance + time + monetary distance rate + fare)
		 * 
		 * walkLeg1: -1 * (-1.2 -0.00015*200 -(0.00016+0.00011)*100 -0.00017*200 -0 ) = 1.291
		 */
		Assert.assertEquals("Total disutility is wrong!", 1.291, result.disutility, MatsimTestUtils.EPSILON);

		for (int i = 0; i < legs.size(); i++) {
			Assert.assertEquals("Input legs != output legs!", legs.get(i), result.routeParts.get(i));
		}
		Assert.assertEquals("Input legs != output legs!", legs.size(), result.routeParts.size());
	}
	
	@Test
	public final void testWalkAccessSubpopulation() {
		List<PlanElement> legs = new ArrayList<>();
		RaptorParameters params = null;
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population pop = scenario.getPopulation();
		PopulationFactory f = pop.getFactory();
		Person person = f.createPerson(Id.createPersonId("personSubpopulationDummy"));
		
		String subpopulationName = "dummySubpopulation";
		person.getAttributes().putAttribute("subpopulation", subpopulationName);

		Person personSubpopulationNull = f.createPerson(Id.createPersonId("personSubpopulationNull"));
		
		// daily constants / rates are ignored, but set them anyway (to see whether they are used by error)
		PlanCalcScoreConfigGroup.ScoringParameterSet scoreCfg = config.planCalcScore().getOrCreateScoringParameters(subpopulationName);
		scoreCfg.setMarginalUtilityOfMoney(1.0);
		scoreCfg.setPerforming_utils_hr(0.00011 * 3600.0);
		ModeParams walkParams = scoreCfg.getOrCreateModeParams(TransportMode.walk);
		walkParams.setConstant(-1.2);
		walkParams.setDailyMonetaryConstant(-1.3);
		walkParams.setDailyUtilityConstant(-1.4);
		walkParams.setMarginalUtilityOfDistance(-0.00015);
		walkParams.setMarginalUtilityOfTraveling(-0.00016 * 3600.0);
		walkParams.setMonetaryDistanceRate(-0.00017);
		
		// set other values for subpopulation null to check that they are not used by error
		PlanCalcScoreConfigGroup.ScoringParameterSet scoreCfgNullParams = config.planCalcScore().getOrCreateScoringParameters(null); // is this really necessary
		PlanCalcScoreConfigGroup scoreCfgNull = config.planCalcScore();
		scoreCfgNull.setMarginalUtilityOfMoney(1.0);
		scoreCfgNull.setPerforming_utils_hr(0.0002 * 3600.0);
		ModeParams walkParamsNull = scoreCfgNull.getOrCreateModeParams(TransportMode.walk);
		walkParamsNull.setConstant(-100);
		walkParamsNull.setMarginalUtilityOfTraveling(0.0);
	
		BerlinRaptorIntermodalAccessEgress raptorIntermodalAccessEgress = new BerlinRaptorIntermodalAccessEgress(config);
		
		Leg walkLeg1 = PopulationUtils.createLeg(TransportMode.walk);
		walkLeg1.setDepartureTime(7*3600.0);
		walkLeg1.setTravelTime(100);
		Route walkRoute1 = new GenericRouteImpl(Id.createLinkId("dummy1"), Id.createLinkId("dummy2"));
		walkRoute1.setDistance(200.0);
		walkLeg1.setRoute(walkRoute1);
		legs.add(walkLeg1);
		
		// Agent in dummy subpopulation
		RIntermodalAccessEgress result = raptorIntermodalAccessEgress.calcIntermodalAccessEgress(legs, params, person, RaptorStopFinder.Direction.ACCESS );
		
		//Asserts
		Assert.assertEquals("Total travel time is wrong!", 100.0, result.travelTime, MatsimTestUtils.EPSILON);
		
		/* 
		 * disutility: -1 * ( ASC + distance + time + monetary distance rate + fare)
		 * 
		 * walkLeg1: -1 * (-1.2 -0.00015*200 -(0.00016+0.00011)*100 -0.00017*200 -0 ) = 1.291
		 */
		Assert.assertEquals("Total disutility is wrong!", 1.291, result.disutility, MatsimTestUtils.EPSILON);

		for (int i = 0; i < legs.size(); i++) {
			Assert.assertEquals("Input legs != output legs!", legs.get(i), result.routeParts.get(i));
		}
		Assert.assertEquals("Input legs != output legs!", legs.size(), result.routeParts.size());
		
		// Agent in subpopulation null
		RIntermodalAccessEgress resultSubpopulationNull = raptorIntermodalAccessEgress.calcIntermodalAccessEgress(legs, params,
				personSubpopulationNull, RaptorStopFinder.Direction.ACCESS );
		
		//Asserts
		Assert.assertEquals("Total travel time is wrong!", 100.0, resultSubpopulationNull.travelTime, MatsimTestUtils.EPSILON);
		
		/* 
		 * disutility: -1 * ( ASC + distance + time + monetary distance rate + fare)
		 * 
		 * walkLeg1: -1 * (-100 -0*200 -(0.0002)*100 -0.0*200 -0 ) = 100.02
		 */
		Assert.assertEquals("Total disutility is wrong!", 100.02, resultSubpopulationNull.disutility, MatsimTestUtils.EPSILON);

		for (int i = 0; i < legs.size(); i++) {
			Assert.assertEquals("Input legs != output legs!", legs.get(i), resultSubpopulationNull.routeParts.get(i));
		}
		Assert.assertEquals("Input legs != output legs!", legs.size(), resultSubpopulationNull.routeParts.size());
	}
}
