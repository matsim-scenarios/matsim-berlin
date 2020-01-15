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
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author vsp-gleich
 */
public class IntermodalTripFareCompensatorPerDayTest {
	private static final Logger log = Logger.getLogger( IntermodalTripFareCompensatorPerDayTest.class ) ;
	
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
        compensatorConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorConfig.setPtModesAsString(TransportMode.pt);
        double compensationPerTrip = 1.0;
        compensatorConfig.setCompensationPerTrip(compensationPerTrip);
        
        config.addModule(compensatorConfig);

        double endOfDay = 30 * 3600.0;
        config.qsim().setEndTime(endOfDay);
        
        // prepare dummy simulation to trigger an AfterMobsimEvent
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setLastIteration(0);
//        Scenario scenario = ScenarioUtils.createScenario(config);

//        
//        Module module = new AbstractModule() {
//            @Override
//            public void install() {
//                install( new NewControlerModule() );
//                install( new ControlerDefaultCoreListenersModule() );
//                install( new ControlerDefaultsModule() );
//                install( new ScenarioByInstanceModule( scenario ));
//            }
//        };
//        com.google.inject.Injector injector = Injector.createInjector( config, module );
//        EventsManager events = injector.getInstance(EventsManager.class);
//        
        Controler controler = new Controler( scenario );
        EventsManager events = controler.getEvents();
        
        IntermodalTripFareCompensatorPerDay tfh = new IntermodalTripFareCompensatorPerDay(compensatorConfig, events, config.qsim());
        events.addHandler(tfh);
        controler.addControlerListener(tfh);
        
        Map<Id<Person>, Double> person2Fare = new HashMap<>();
        events.addHandler(new PersonMoneyEventHandler() {
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
            }
        });
        
        controler.run();
        
//        Id<Person> personIdNoPtButDrt = Id.createPersonId("NoPtButDrt");
//        Id<Person> personIdPtNoDrt = Id.createPersonId("PtNoDrt");
//        Id<Person> personIdPt1Drt = Id.createPersonId("Pt1Drt");
//        Id<Person> personIdPt3Drt = Id.createPersonId("Pt3Drt");
//
//        // test trip with drt mode but not intermodal
//        events.processEvent(new PersonDepartureEvent(0.0, personIdNoPtButDrt, Id.createLinkId("12"), TransportMode.drt));
//        
//        // test intermodal trip without drt mode (only unrelated other mode)
//        events.processEvent(new PersonDepartureEvent(12.0, personIdPtNoDrt, Id.createLinkId("23"), TransportMode.car));
//        events.processEvent(new PersonDepartureEvent(13.0, personIdPtNoDrt, Id.createLinkId("34"), TransportMode.pt));
//        
//        // test intermodal trip with pt and drt mode
//        events.processEvent(new PersonDepartureEvent(22.0, personIdPt1Drt, Id.createLinkId("23"), TransportMode.drt));
//        events.processEvent(new PersonDepartureEvent(23.0, personIdPt1Drt, Id.createLinkId("34"), TransportMode.pt));
//        events.processEvent(new PersonDepartureEvent(24.0, personIdPt1Drt, Id.createLinkId("23"), TransportMode.car));
//        
//        // test intermodal trips with pt and multiple drt legs
//        events.processEvent(new PersonDepartureEvent(32.0, personIdPt3Drt, Id.createLinkId("23"), TransportMode.drt));
//        events.processEvent(new PersonDepartureEvent(33.0, personIdPt3Drt, Id.createLinkId("34"), TransportMode.pt));
//        events.processEvent(new PersonDepartureEvent(34.0, personIdPt3Drt, Id.createLinkId("23"), TransportMode.drt));
//		// end trip
////        events.processEvent(new ActivityStartEvent(35.0, personIdPt3Drt, Id.createLinkId("23"), Id.create("dummy", ActivityFacility.class), "blub"));
//        
//        // drt on other monomodal drt trip (without pt)
//        events.processEvent(new PersonDepartureEvent(36.0, personIdPt3Drt, Id.createLinkId("23"), TransportMode.drt));
        
        // trigger compensation payment
//        controler.run();
//        events.processEvent(new AfterMobsimEvent(null, 0));        
        
//        injector.getInstance(ControlerListenerManagerImpl.class).fireControlerAfterMobsimEvent(0);
//        log.error(person2Fare.get(personIdPt3Drt));
        
        // compensation paid third time (second trip)
        
		Assert.assertTrue("NoPtButDrt received compensation but should not", person2Fare.get(fixture.personIdNoPtButDrt) == null);
		Assert.assertTrue("PtNoDrt received compensation but should not", person2Fare.get(fixture.personIdPtNoDrt) == null);
		
		Assert.assertEquals("Pt1DrtSameTrip received wrong compensation", 1 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt1DrtSameTrip), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Pt1DrtDifferentTrips received wrong compensation", 1 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt1DrtDifferentTrips), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Pt3DrtIn2IntermodalTrips received wrong compensation", 3 * compensationPerTrip,
				person2Fare.get(fixture.personIdPt3DrtIn2IntermodalTrips), MatsimTestUtils.EPSILON);

    }


}
