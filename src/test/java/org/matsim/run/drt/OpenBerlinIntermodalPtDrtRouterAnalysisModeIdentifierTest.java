/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.run.drt;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author gleich
 *
 */
public class OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifierTest {
	private static final Logger log = Logger.getLogger( OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifierTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void testSingleModeTrips() {
		log.info("Running test0...");
		
		OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.pt, mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.car));
			Assert.assertEquals("Wrong mode!", TransportMode.car, mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.drt));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.drt, mainModeIdentifier.identifyMainMode(planElements));
		}
		
		log.info("Running test0... Done.");
	}
	
	@Test
	public final void testIntermodalPtDrtTrip() {
		log.info("Running testIntermodalPtDrtTrip...");
		
		OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.drt));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", 
					OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS, 
					mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg("drt2"));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!",
					OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS, 
					mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.drt));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg("drt2"));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", 
					OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS, 
					mainModeIdentifier.identifyMainMode(planElements));
		}
		
		log.info("Running testIntermodalPtDrtTrip... Done.");
	}
	
	@Test
	public final void testIntermodalPtDrtTripWithWalk() {
		log.info("Running testIntermodalPtDrtTrip...");
		
		OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.drt));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.walk));
			Assert.assertEquals("Wrong mode!", OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS, 
					mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg("drt2"));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.walk));
			Assert.assertEquals("Wrong mode!", OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS, 
					mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.drt));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
			planElements.add(factory.createLeg(TransportMode.walk));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg("drt2"));
			planElements.add(factory.createActivityFromLinkId("drt2 interaction", null));
			planElements.add(factory.createLeg(TransportMode.walk));
			Assert.assertEquals("Wrong mode!", OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS, 
					mainModeIdentifier.identifyMainMode(planElements));
		}
		
		log.info("Running testIntermodalPtDrtTrip... Done.");
	}

	@Test(expected = RuntimeException.class)
	public void testRuntimeExceptionTripWithoutMainMode() {
		log.info("Running testRuntimeExceptionTripWithoutMainMode...");
		
		OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		List<PlanElement> planElements = new ArrayList<>();
		planElements.add(factory.createLeg(TransportMode.non_network_walk));
		planElements.add(factory.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, null));
		planElements.add(factory.createLeg(TransportMode.non_network_walk));
		// should throw an exception, because main mode cannot be identified
		mainModeIdentifier.identifyMainMode(planElements);
		log.info("Running testRuntimeExceptionTripWithoutMainMode... Done.");
	}
	
	@Test(expected = RuntimeException.class)
	public void testRuntimeExceptionOnlyNonNetworkWalk() {
		log.info("Running testRuntimeExceptionOnlyNonNetworkWalk...");
		
		OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		List<PlanElement> planElements = new ArrayList<>();
		planElements.add(factory.createLeg(TransportMode.non_network_walk));
		// should throw an exception, because non_network_walk is not a main mode
		mainModeIdentifier.identifyMainMode(planElements);
	}

}
