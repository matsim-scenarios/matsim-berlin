/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 *
 */
package org.matsim.run.drt.intermodalTripFareCompensator;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.drt.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup.CompensationCondition;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author vsp-gleich
 */
public class IntermodalTripFareCompensatorsTest {
	private static final Logger log = Logger.getLogger( IntermodalTripFareCompensatorsTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

    /**
     * Test method for {@link IntermodalTripFareCompensatorPerDay}.
     */
    @Test
    public void testIntermodalTripFareCompensatorPerDay() {

        Fixture fixture = new Fixture();
        Config config = fixture.config;
        Scenario scenario = fixture.scenario;

        IntermodalTripFareCompensatorConfigGroup compensatorConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorConfig.setCompensationCondition(CompensationCondition.PtModeUsedAnywhereInTheDay);
        compensatorConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorConfig.setPtModesAsString(TransportMode.pt);
        double compensationMoneyPerTrip = 1.0;
        compensatorConfig.setCompensationMoneyPerTrip(compensationMoneyPerTrip);
        double compensationScorePerTrip = 2.0;
        compensatorConfig.setCompensationScorePerTrip(compensationScorePerTrip);
        double compensationMoneyPerDay = 10.0;
        compensatorConfig.setCompensationMoneyPerDay(compensationMoneyPerDay);
        double compensationScorePerDay = 20.0;
        compensatorConfig.setCompensationScorePerDay(compensationScorePerDay);
        
        IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = new IntermodalTripFareCompensatorsConfigGroup();
        compensatorsConfig.addParameterSet(compensatorConfig);
        
        config.addModule(compensatorsConfig);
        
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(0);
        
        Controler controler = new Controler( scenario );
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        
        EventsManager events = controler.getEvents();
        PersonMoneySumAndScoreSumCalculator fareSummer = new PersonMoneySumAndScoreSumCalculator();
        events.addHandler(fareSummer);
        
        controler.run();
        
        Map<Id<Person>, Double> person2Fare = fareSummer.getPerson2Fare();
        Map<Id<Person>, Double> person2Score = fareSummer.getPerson2Score();
        
		Assert.assertNull("NoPtButDrt received compensation but should not", person2Fare.get(fixture.personIdNoPtButDrt));
        Assert.assertNull("NoPtButDrt received compensation but should not", person2Score.get(fixture.personIdNoPtButDrt));
		Assert.assertNull("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt));
        Assert.assertNull("PtNoDrt received compensation but should not", person2Score.get(fixture.personIdPtNoDrt));
		
		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", compensationMoneyPerDay + 1 * compensationMoneyPerTrip,
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", compensationScorePerDay + 1 * compensationScorePerTrip,
                person2Score.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", compensationMoneyPerDay + 1 * compensationMoneyPerTrip,
				person2Fare.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", compensationScorePerDay + 1 * compensationScorePerTrip,
                person2Score.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", compensationMoneyPerDay + 3 * compensationMoneyPerTrip,
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", compensationScorePerDay + 3 * compensationScorePerTrip,
                person2Score.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);

    }
    
    @Test
    public void testIntermodalTripFareCompensatorPerTrip() {

        Fixture fixture = new Fixture();
        Config config = fixture.config;
        Scenario scenario = fixture.scenario;

        IntermodalTripFareCompensatorConfigGroup compensatorConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorConfig.setCompensationCondition(CompensationCondition.PtModeUsedInSameTrip);
        compensatorConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorConfig.setPtModesAsString(TransportMode.pt);
        double compensationMoneyPerTrip = 1.0;
        compensatorConfig.setCompensationMoneyPerTrip(compensationMoneyPerTrip);
        double compensationScorePerTrip = 2.0;
        compensatorConfig.setCompensationScorePerTrip(compensationScorePerTrip);
        
        IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = new IntermodalTripFareCompensatorsConfigGroup();
        compensatorsConfig.addParameterSet(compensatorConfig);
        
        config.addModule(compensatorsConfig);
        
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(0);
        
        Controler controler = new Controler( scenario );
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        
        EventsManager events = controler.getEvents();
        PersonMoneySumAndScoreSumCalculator fareSummer = new PersonMoneySumAndScoreSumCalculator();
        events.addHandler(fareSummer);
        
        controler.run();
        
        Map<Id<Person>, Double> person2Fare = fareSummer.getPerson2Fare();
        Map<Id<Person>, Double> person2Score = fareSummer.getPerson2Score();
        
		Assert.assertNull("NoPtButDrt received compensation but should not", person2Fare.get(fixture.personIdNoPtButDrt));
        Assert.assertNull("NoPtButDrt received compensation but should not", person2Score.get(fixture.personIdNoPtButDrt));
		Assert.assertNull("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt));
        Assert.assertNull("PtNoDrt received compensation but should not", person2Score.get(fixture.personIdPtNoDrt));
		
		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", 1 * compensationMoneyPerTrip,
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", 1 * compensationScorePerTrip,
                person2Score.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("Pt1DrtDifferentTrips received compensation but should not", person2Fare.get(fixture.personIdPt1DrtDifferentTrips));
        Assert.assertNull("Pt1DrtDifferentTrips received compensation but should not", person2Score.get(fixture.personIdPt1DrtDifferentTrips));
		
		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", 3 * compensationMoneyPerTrip,
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", 3 * compensationScorePerTrip,
                person2Score.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
    }
    
    @Test
    public void testIntermodalTripFareCompensatorPerDayAndPerTripAndReset() {

        Fixture fixture = new Fixture();
        Config config = fixture.config;
        Scenario scenario = fixture.scenario;
        
        IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = new IntermodalTripFareCompensatorsConfigGroup();
        
        IntermodalTripFareCompensatorConfigGroup compensatorPerDayConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorPerDayConfig.setCompensationCondition(CompensationCondition.PtModeUsedAnywhereInTheDay);
        compensatorPerDayConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorPerDayConfig.setPtModesAsString(TransportMode.pt);
        double compensationMoneyPerTripAnywhereInTheDay = 111.0;
        compensatorPerDayConfig.setCompensationMoneyPerTrip(compensationMoneyPerTripAnywhereInTheDay);
        double compensationScorePerTripAnywhereInTheDay = 222.0;
        compensatorPerDayConfig.setCompensationScorePerTrip(compensationScorePerTripAnywhereInTheDay);
        double compensationMoneyPerDayAnywhereInTheDay = 1111.0;
        compensatorPerDayConfig.setCompensationMoneyPerDay(compensationMoneyPerDayAnywhereInTheDay);
        double compensationScorePerDayAnywhereInTheDay = 2222.0;
        compensatorPerDayConfig.setCompensationScorePerDay(compensationScorePerDayAnywhereInTheDay);
        compensatorsConfig.addParameterSet(compensatorPerDayConfig);

        IntermodalTripFareCompensatorConfigGroup compensatorPerTripConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorPerTripConfig.setCompensationCondition(CompensationCondition.PtModeUsedInSameTrip);
        compensatorPerTripConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorPerTripConfig.setPtModesAsString(TransportMode.pt);
        double compensationMoneyPerTripSameTrip = 1.0;
        compensatorPerTripConfig.setCompensationMoneyPerTrip(compensationMoneyPerTripSameTrip);
        double compensationScorePerTripSameTrip = 2.0;
        compensatorPerTripConfig.setCompensationScorePerTrip(compensationScorePerTripSameTrip);
        compensatorsConfig.addParameterSet(compensatorPerTripConfig);
        
        config.addModule(compensatorsConfig);
        
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(1); // simulate 2 iterations to check that both compensators reset between iterations
        
        Controler controler = new Controler( scenario );
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        
        EventsManager events = controler.getEvents();
        PersonMoneySumAndScoreSumCalculator fareSummer = new PersonMoneySumAndScoreSumCalculator();
        events.addHandler(fareSummer);
        
        controler.run();
        
        Map<Id<Person>, Double> person2Fare = fareSummer.getPerson2Fare();
        Map<Id<Person>, Double> person2Score = fareSummer.getPerson2Score();
        
        // The result should be the same no matter whether we run 1 or 2 iterations (replanning switched off). So check after 2nd iteration.
		Assert.assertNull("NoPtButDrt received compensation but should not",
				person2Fare.get(fixture.personIdNoPtButDrt));
        Assert.assertNull("NoPtButDrt received compensation but should not",
                person2Score.get(fixture.personIdNoPtButDrt));
		Assert.assertNull("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt));
        Assert.assertNull("PtNoDrt received compensation but should not", person2Score.get(fixture.personIdPtNoDrt));

		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation",
				compensationMoneyPerDayAnywhereInTheDay + 1 * (compensationMoneyPerTripAnywhereInTheDay + compensationMoneyPerTripSameTrip),
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt1DrtSameTrip received wrong compensation",
                compensationScorePerDayAnywhereInTheDay + 1 * (compensationScorePerTripAnywhereInTheDay + compensationScorePerTripSameTrip),
                person2Score.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);

		Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", compensationMoneyPerDayAnywhereInTheDay + 1 * compensationMoneyPerTripAnywhereInTheDay,
				person2Fare.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", compensationScorePerDayAnywhereInTheDay + 1 * compensationScorePerTripAnywhereInTheDay,
                person2Score.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);

		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation",
				compensationMoneyPerDayAnywhereInTheDay + 3 * (compensationMoneyPerTripAnywhereInTheDay + compensationMoneyPerTripSameTrip),
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation",
                compensationScorePerDayAnywhereInTheDay + 3 * (compensationScorePerTripAnywhereInTheDay + compensationScorePerTripSameTrip),
                person2Score.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
    }
    
    private static class PersonMoneySumAndScoreSumCalculator implements PersonMoneyEventHandler, PersonScoreEventHandler {
        Map<Id<Person>, Double> person2Fare = new HashMap<>();
        Map<Id<Person>, Double> person2Score = new HashMap<>();
    	
        @Override
        public void handleEvent(PersonMoneyEvent event) {
        	if (!person2Fare.containsKey(event.getPersonId())) {
        		person2Fare.put(event.getPersonId(), event.getAmount());
        	} else {
        		person2Fare.put(event.getPersonId(), person2Fare.get(event.getPersonId()) + event.getAmount());
        	}
        }

        @Override
        public void handleEvent(PersonScoreEvent event) {
            if (!person2Score.containsKey(event.getPersonId())) {
                person2Score.put(event.getPersonId(), event.getAmount());
            } else {
                person2Score.put(event.getPersonId(), person2Score.get(event.getPersonId()) + event.getAmount());
            }
        }

        @Override
        public void reset(int iteration) {
        	person2Fare.clear();
            person2Score.clear();
        }
        
        private Map<Id<Person>, Double> getPerson2Fare() {
        	return person2Fare;
        }
        private Map<Id<Person>, Double> getPerson2Score() {
            return person2Score;
        }
    }
}
