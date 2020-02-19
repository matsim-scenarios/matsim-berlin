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

package org.matsim.run.drt.ptRoutingModes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.run.drt.ptRoutingModes.PtIntermodalRoutingModesConfigGroup.PersonAttribute2ValuePair;
import org.matsim.run.drt.ptRoutingModes.PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet;
import org.matsim.testcases.MatsimTestUtils;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;

/**
 * 
 * @author gleich
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunDrtOpenBerlinScenarioWithPtRoutingModesTest {
		
	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenarioWithPtRoutingModesTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void testAFewAgentsOnly() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(0);
			config.strategy().clearStrategySettings();
			
			// Use RandomSingleTripReRoute, because in this branch only in RandomSingleTripReRoute drt is allowed as access/egress mode to pt
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName("RandomSingleTripReRoute");
			stratSets.setWeight(1.0);
			stratSets.setSubpopulation("person");
			config.strategy().addStrategySettings(stratSets);
			
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../../test/input/drt/drt-test-agents.xml");
			
			// jvm on build server has less cores than we set in the input config file and would complain about that
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			
			config.controler().setWritePlansInterval(1);
			
			// make pt more attractive to obtain less direct walks (routing mode pt) due to drt triangle walk being more attractive 
			config.planCalcScore().getScoringParameters("person").setMarginalUtlOfWaitingPt_utils_hr(5.0);
			
			// make drt more attractive to always obtain drt routes whenever possible
			config.planCalcScore().getScoringParameters("person").getOrCreateModeParams(TransportMode.drt).setConstant(100);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
				drtCfg.setDrtServiceAreaShapeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlkoenig-area/berlkoenig-area.shp");
			}
			
			String personAttributeDrt = "canUseDrt";
			String personAttributeDrtValue = "true";
			Id<Person> monomodalPtAgentId = Id.createPersonId("285614901pt");
			Id<Person> intermodalPtAgentId = Id.createPersonId("285614901pt_w_drt");
			
			SwissRailRaptorConfigGroup swissRaptorConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
			for (IntermodalAccessEgressParameterSet accessEgressParams: swissRaptorConfig.getIntermodalAccessEgressParameterSets()) {
				if (accessEgressParams.getMode().equals(TransportMode.drt)) {
					accessEgressParams.setPersonFilterAttribute(personAttributeDrt);
					accessEgressParams.setPersonFilterValue(personAttributeDrtValue);
				}
			}
			
			PtIntermodalRoutingModesConfigGroup ptRoutingModes = ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);
			PtIntermodalRoutingModeParameterSet routingModeParamSet = new PtIntermodalRoutingModeParameterSet();
			routingModeParamSet.setDelegateMode(TransportMode.pt);
			routingModeParamSet.setRoutingMode("pt_w_drt_allowed");
			PersonAttribute2ValuePair personAttributeValue = new PersonAttribute2ValuePair();
			personAttributeValue.setPersonFilterAttribute(personAttributeDrt);
			personAttributeValue.setPersonFilterValue(personAttributeDrtValue);
			routingModeParamSet.addPersonAttribute2ValuePair(personAttributeValue);
			ptRoutingModes.addPtIntermodalRoutingModeParameterSet(routingModeParamSet);
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			Person monomodalPtAgent = scenario.getPopulation().getPersons().get(monomodalPtAgentId);
			Person intermodalPtAgent = scenario.getPopulation().getFactory().createPerson(intermodalPtAgentId);
			Plan intermodalPtPlan = PopulationUtils.createPlan(intermodalPtAgent);
			PopulationUtils.copyFromTo(monomodalPtAgent.getSelectedPlan(), intermodalPtPlan);
			intermodalPtPlan.getPlanElements().stream().filter(pe -> pe instanceof Leg)
					.filter(leg -> ((Leg) leg).getMode().equals(TransportMode.pt))
					.forEach(leg -> ((Leg) leg).setMode("pt_w_drt_allowed"));
			intermodalPtAgent.addPlan(intermodalPtPlan);
			scenario.getPopulation().addPerson(intermodalPtAgent);
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;	
			
			Plan monomodalPtAgentPlan = scenario.getPopulation().getPersons().get(monomodalPtAgentId).getSelectedPlan();
			Plan intermodalPtAgentPlan = scenario.getPopulation().getPersons().get(intermodalPtAgentId).getSelectedPlan();
			
			int monomodalPtAgentPlanIntermodalTripCounter = countIntermodalPtAndDrtTrips(TripStructureUtils.getTrips(monomodalPtAgentPlan.getPlanElements()));
			int intermodalPtAgentPlanIntermodalTripCounter = countIntermodalPtAndDrtTrips(TripStructureUtils.getTrips(intermodalPtAgentPlan.getPlanElements()));
			
			Assert.assertEquals("monomodal pt agent has intermodal route (=drt for access or egress to pt)", 0, monomodalPtAgentPlanIntermodalTripCounter);
			Assert.assertTrue("intermodal pt agent has no intermodal route (=drt for access or egress to pt)", intermodalPtAgentPlanIntermodalTripCounter > 0);
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

	private int countIntermodalPtAndDrtTrips(List<Trip> trips) {
		int intermodalTripCounter = 0;
		for (Trip trip: trips) {
			Map<String, Integer> mode2NumberOfLegs = new HashMap<>();
			for (Leg leg: trip.getLegsOnly()) {
				if (!mode2NumberOfLegs.containsKey(leg.getMode())) {
					mode2NumberOfLegs.put(leg.getMode(), 1);
				} else {
					mode2NumberOfLegs.put(leg.getMode(), mode2NumberOfLegs.get(leg.getMode()) + 1);
				}
			}
			if (mode2NumberOfLegs.containsKey(TransportMode.drt) && mode2NumberOfLegs.containsKey(TransportMode.pt)) {
				intermodalTripCounter++;
			}
		}
		return intermodalTripCounter;
	}

}
