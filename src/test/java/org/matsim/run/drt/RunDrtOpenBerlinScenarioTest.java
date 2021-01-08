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

package org.matsim.run.drt;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.extensions.pt.PtExtensionsConfigGroup;
import org.matsim.run.BerlinExperimentalConfigGroup;
import org.matsim.extensions.pt.PtExtensionsConfigGroup.IntermodalAccessEgressModeUtilityRandomization;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup.CompensationCondition;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author gleich
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunDrtOpenBerlinScenarioTest {
		
	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenarioTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	// During debug some exceptions only occured at the replanning stage of the 3rd
	// iteration, so we need at least 3 iterations.
	// Have at least 0.1 pct of the population to have as many strange corner cases
	// as possible (because those tend to cause exceptions otherwise not found).
	@Test
	public final void eTest0_1pctUntilIteration3() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(2);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			// jvm on build server has less cores than we set in the input config file and would complain about that
			config.global().setNumberOfThreads(1);
			config.qsim().setNumberOfThreads(1);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
			}
			
			// Decrease population to 0.01% sample 
			BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
			berlinCfg.setPopulationDownsampleFactor(0.01);
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;			
			
			// TODO: test the scores in iteration 0 and 4
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	// During debug some exceptions only occured at the replanning stage of the 3rd
	// iteration, so we need at least 3 iterations.
	@Test
	public final void testAFewAgentsOnly() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"};
			
			Config config = RunDrtOpenBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(2);
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
			
			PtExtensionsConfigGroup ptExtensionsConfigGroup = ConfigUtils.addOrGetModule(config, PtExtensionsConfigGroup.class);
			
			IntermodalAccessEgressModeUtilityRandomization utilityRandomization = new IntermodalAccessEgressModeUtilityRandomization();
			utilityRandomization.setAccessEgressMode(TransportMode.drt);
			utilityRandomization.setAdditiveRandomizationWidth(20.);
			ptExtensionsConfigGroup.addIntermodalAccessEgressModeUtilityRandomization(utilityRandomization);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
				drtCfg.setDrtServiceAreaShapeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlkoenig-area/berlkoenig-area.shp");
				drtCfg.setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlkoenig-drt-v5.5/input/berlkoenig-drt-v5.5.drt-by-rndLocations-1000vehicles-4seats.xml.gz");
			}
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			FareEventChecker fareChecker = new FareEventChecker();
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(fareChecker);
				}
			});
			
			controler.run() ;	
			
			Plan intermodalPtAgentPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("285614901pt")).getSelectedPlan();
			
			int intermodalTripCounter = 0;
			int drtLegsInIntermodalTripsCounter = 0;
			
			List<Trip> trips = TripStructureUtils.getTrips(intermodalPtAgentPlan.getPlanElements());
			
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
					drtLegsInIntermodalTripsCounter = drtLegsInIntermodalTripsCounter + mode2NumberOfLegs.get(TransportMode.drt);
				}
			}
			Assert.assertTrue("pt agent has no intermodal route (=drt for access or egress to pt)", intermodalTripCounter > 0);
			
			// check drt-pt-intermodal trip fare compensator
			List<PersonMoneyEvent> moneyEventsIntermodalAgent = fareChecker.getEventsForPerson(Id.createPersonId("285614901pt"));
			IntermodalTripFareCompensatorsConfigGroup fareCompensators = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);
			double expectedCompensationAmountPerTrip = Double.NaN;
			for (IntermodalTripFareCompensatorConfigGroup fareCompensator : fareCompensators.getIntermodalTripFareCompensatorConfigGroups()) {
				if (fareCompensator.getDrtModes().contains(TransportMode.drt) && fareCompensator.getPtModes().contains(TransportMode.pt)) { 
					expectedCompensationAmountPerTrip = fareCompensator.getCompensationMoneyPerTrip();
				}
			}
			
			int compensatorMoneyEventsCounter = 0;
			for(PersonMoneyEvent event: moneyEventsIntermodalAgent) {
				if (Math.abs(event.getAmount() - expectedCompensationAmountPerTrip * drtLegsInIntermodalTripsCounter) < MatsimTestUtils.EPSILON) {
					// We do not know where the money event comes from, so these are money events *potentially* thrown by the intermodal trip fare compensator.
					compensatorMoneyEventsCounter++;
				}
			}
			
			Assert.assertTrue(
					"Number of potential intermodal trip fare compensator money events should be equal or higher than the number of intermodal trips."
							+ "drtLegsInIntermodalTripsCounter: " + drtLegsInIntermodalTripsCounter
							+ ", compensatorMoneyEventsCounter:" + compensatorMoneyEventsCounter,
					1 <= compensatorMoneyEventsCounter);
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

	@Test
	public final void testAFewAgentsOnlyWithHugeIntermodalTripFareCompensation() {
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
			config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(5);
			
			PtExtensionsConfigGroup ptExtensionsConfigGroup = ConfigUtils.addOrGetModule(config, PtExtensionsConfigGroup.class);
			
			IntermodalAccessEgressModeUtilityRandomization utilityRandomization = new IntermodalAccessEgressModeUtilityRandomization();
			utilityRandomization.setAccessEgressMode(TransportMode.drt);
			utilityRandomization.setAdditiveRandomizationWidth(20.);
			ptExtensionsConfigGroup.addIntermodalAccessEgressModeUtilityRandomization(utilityRandomization);
			
			for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
				drtCfg.setNumberOfThreads(1);
				drtCfg.setDrtServiceAreaShapeFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlkoenig-area/berlkoenig-area.shp");
				drtCfg.setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlkoenig-drt-v5.5/input/berlkoenig-drt-v5.5.drt-by-rndLocations-1000vehicles-4seats.xml.gz");
			}
			
			IntermodalTripFareCompensatorsConfigGroup compensatorsCfg = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);
			List<IntermodalTripFareCompensatorConfigGroup> remove = new ArrayList<>();
			for (IntermodalTripFareCompensatorConfigGroup previousCfg : compensatorsCfg.getIntermodalTripFareCompensatorConfigGroups()) {
				remove.add(previousCfg);
			}
			for (IntermodalTripFareCompensatorConfigGroup previousCfg : remove) {
				compensatorsCfg.removeParameterSet(previousCfg);
			}
			IntermodalTripFareCompensatorConfigGroup compensatorCfg = new IntermodalTripFareCompensatorConfigGroup();
			compensatorCfg.setCompensationCondition(CompensationCondition.PtModeUsedAnywhereInTheDay);
			compensatorCfg.setDrtModesAsString("drt");
			compensatorCfg.setPtModesAsString("pt");
			compensatorCfg.setCompensationMoneyPerTrip(10000);
			compensatorCfg.setCompensationScorePerDay(20000);
			compensatorsCfg.addParameterSet(compensatorCfg);
			
			config.transit().setUsingTransitInMobsim(false);
			
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
			
			FareEventChecker fareChecker = new FareEventChecker();
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(fareChecker);
				}
			});
			
			controler.run() ;	
			
			Plan intermodalPtAgentPlan = scenario.getPopulation().getPersons().get(Id.createPersonId("285614901pt")).getSelectedPlan();
			
			int intermodalTripCounter = 0;
			int drtLegsInIntermodalTripsCounter = 0;
			
			List<Trip> trips = TripStructureUtils.getTrips(intermodalPtAgentPlan.getPlanElements());
			
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
					drtLegsInIntermodalTripsCounter = drtLegsInIntermodalTripsCounter + mode2NumberOfLegs.get(TransportMode.drt);
				}
			}
			Assert.assertTrue("pt agent has no intermodal route (=drt for access or egress to pt)", intermodalTripCounter > 0);
			
			// check drt-pt-intermodal trip fare compensator
			List<PersonMoneyEvent> moneyEventsIntermodalAgent = fareChecker.getEventsForPerson(Id.createPersonId("285614901pt"));
			
			int hugeMoneyEventCounter = 0;
			for(PersonMoneyEvent event: moneyEventsIntermodalAgent) {
				if (event.getAmount() > 900) {
					hugeMoneyEventCounter++;
				}
			}

			Assert.assertEquals("Number of potential intermodal trip fare compensator money events should be equal to the number of persons who get a compensation.", 1, hugeMoneyEventCounter);
			Assert.assertTrue("Huge money events thrown at the end of the day should translate into a very large score!", 21000 < intermodalPtAgentPlan.getScore());

			List<String> agentIdHighScore = scenario.getPopulation().getPersons().values().stream().
					filter(person -> !person.getId().equals(Id.createPersonId("285614901pt"))).
					filter(person -> person.getSelectedPlan().getScore() > 1000.0).
					map(person -> person.getId() + " score: " + person.getSelectedPlan().getScore().toString()).collect(Collectors.toList());
			Assert.assertTrue("Other agents should not profit from compensations and should have normal scores, but the following had very high scores: " + agentIdHighScore,
					agentIdHighScore.isEmpty());

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

	private class FareEventChecker implements PersonMoneyEventHandler {
		private Map<Id<Person>, List<PersonMoneyEvent>> person2moneyEvents = new HashMap<>();

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			if (!person2moneyEvents.containsKey(event.getPersonId())) {
				person2moneyEvents.put(event.getPersonId(), new ArrayList<>());
			}
			person2moneyEvents.get(event.getPersonId()).add(event);
		}
		
		@Override
		public void reset(int iteration) {
			person2moneyEvents.clear();
		}

		List<PersonMoneyEvent> getEventsForPerson(Id<Person> personId) {
			return person2moneyEvents.get(personId);
		}
	}
}
