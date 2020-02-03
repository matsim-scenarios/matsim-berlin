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
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
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
        double compensationPerTrip = 1.0;
        compensatorConfig.setCompensationPerTrip(compensationPerTrip);
        double compensationPerDay = 10.0;
        compensatorConfig.setCompensationPerDay(compensationPerDay);
        
        IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = new IntermodalTripFareCompensatorsConfigGroup();
        compensatorsConfig.addParameterSet(compensatorConfig);
        
        config.addModule(compensatorsConfig);
        
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(0);
        
        Controler controler = new Controler( scenario );
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        
        EventsManager events = controler.getEvents();
        FareSumCalculator fareSummer = new FareSumCalculator();
        events.addHandler(fareSummer);
        
        controler.run();
        
        Map<Id<Person>, Double> person2Fare = fareSummer.getPerson2Fare();
        
		Assert.assertNull("NoPtButDrt received compensation but should not", person2Fare.get(fixture.personIdNoPtButDrt));
		Assert.assertNull("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt));
		
		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", compensationPerDay + 1 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", compensationPerDay + 1 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", compensationPerDay + 3 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);

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
        double compensationPerTrip = 1.0;
        compensatorConfig.setCompensationPerTrip(compensationPerTrip);
        
        IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = new IntermodalTripFareCompensatorsConfigGroup();
        compensatorsConfig.addParameterSet(compensatorConfig);
        
        config.addModule(compensatorsConfig);
        
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(0);
        
        Controler controler = new Controler( scenario );
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        
        EventsManager events = controler.getEvents();
        FareSumCalculator fareSummer = new FareSumCalculator();
        events.addHandler(fareSummer);
        
        controler.run();
        
        Map<Id<Person>, Double> person2Fare = fareSummer.getPerson2Fare();
        
		Assert.assertNull("NoPtButDrt received compensation but should not", person2Fare.get(fixture.personIdNoPtButDrt));
		Assert.assertNull("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt));
		
		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", 1 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
		
		Assert.assertNull("Pt1DrtDifferentTrips received compensation but should not", person2Fare.get(fixture.personIdPt1DrtDifferentTrips));
		
		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", 3 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
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
        double compensationPerTripAnywhereInTheDay = 111.0;
        compensatorPerDayConfig.setCompensationPerTrip(compensationPerTripAnywhereInTheDay);
        double compensationPerDayAnywhereInTheDay = 1111.0;
        compensatorPerDayConfig.setCompensationPerDay(compensationPerDayAnywhereInTheDay);
        compensatorsConfig.addParameterSet(compensatorPerDayConfig);

        IntermodalTripFareCompensatorConfigGroup compensatorPerTripConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorPerTripConfig.setCompensationCondition(CompensationCondition.PtModeUsedInSameTrip);
        compensatorPerTripConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorPerTripConfig.setPtModesAsString(TransportMode.pt);
        double compensationPerTripSameTrip = 1.0;
        compensatorPerTripConfig.setCompensationPerTrip(compensationPerTripSameTrip);
        compensatorsConfig.addParameterSet(compensatorPerTripConfig);
        
        config.addModule(compensatorsConfig);
        
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(1); // simulate 2 iterations to check that both compensators reset between iterations
        
        Controler controler = new Controler( scenario );
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        
        EventsManager events = controler.getEvents();
        FareSumCalculator fareSummer = new FareSumCalculator();
        events.addHandler(fareSummer);
        
        controler.run();
        
        Map<Id<Person>, Double> person2Fare = fareSummer.getPerson2Fare();
        
        // The result should be the same no matter whether we run 1 or 2 iterations (replanning switched off). So check after 2nd iteration.
		Assert.assertNull("NoPtButDrt received compensation but should not",
				person2Fare.get(fixture.personIdNoPtButDrt));
		Assert.assertNull("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt));

		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation",
				compensationPerDayAnywhereInTheDay + 1 * (compensationPerTripAnywhereInTheDay + compensationPerTripSameTrip),
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);

		Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", compensationPerDayAnywhereInTheDay + 1 * compensationPerTripAnywhereInTheDay,
				person2Fare.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);

		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation",
				compensationPerDayAnywhereInTheDay + 3 * (compensationPerTripAnywhereInTheDay + compensationPerTripSameTrip),
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);
    }
    
    private static class FareSumCalculator implements PersonMoneyEventHandler {
        Map<Id<Person>, Double> person2Fare = new HashMap<>();
    	
        @Override
        public void handleEvent(PersonMoneyEvent event) {
        	if (!person2Fare.containsKey(event.getPersonId())) {
        		person2Fare.put(event.getPersonId(), event.getAmount());
        	} else {
        		person2Fare.put(event.getPersonId(), person2Fare.get(event.getPersonId()) + event.getAmount());
        	}
        }

        @Override
        public void reset(int iteration) {
        	person2Fare.clear();
        }
        
        private Map<Id<Person>, Double> getPerson2Fare() {
        	return person2Fare;
        }
    }
}
