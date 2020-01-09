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

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author vsp-gleich
 */
public class IntermodalTripFareCompensatorTest {

    /**
     * Test method for {@link IntermodalTripFareCompensator}.
     */
    @Test
    public void testIntermodalTripFareCompensator() {

        Config config = ConfigUtils.createConfig();
        
        IntermodalTripFareCompensatorConfigGroup compensatorConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorConfig.setPtModesAsString(TransportMode.pt);
        double compensationPerTrip = 1.0;
        compensatorConfig.setCompensationPerTrip(compensationPerTrip);
        
        config.addModule(compensatorConfig);

        final MutableDouble fare = new MutableDouble(0);
        EventsManager events = EventsUtils.createEventsManager();
        IntermodalTripFareCompensator tfh = new IntermodalTripFareCompensator(compensatorConfig, events);
        events.addHandler(tfh);
        events.addHandler(new PersonMoneyEventHandler() {
            @Override
            public void handleEvent(PersonMoneyEvent event) {
                fare.add(event.getAmount());
            }

            @Override
            public void reset(int iteration) {
            }
        });
        Id<Person> personId = Id.createPersonId("p1");

        // test trip with drt mode but not intermodal
        events.processEvent(new PersonDepartureEvent(0.0, personId, Id.createLinkId("12"), TransportMode.drt));
        events.processEvent(new ActivityStartEvent(1.0, personId, Id.createLinkId("23"), Id.create("dummy", ActivityFacility.class), "work"));
        Assert.assertEquals("Compensation should be 0, but is not!", 0.0, fare.getValue(), MatsimTestUtils.EPSILON);
        
        // test intermodal trip without drt mode (only unrelated other mode)
        events.processEvent(new PersonDepartureEvent(2.0, personId, Id.createLinkId("23"), TransportMode.car));
        events.processEvent(new PersonDepartureEvent(3.0, personId, Id.createLinkId("34"), TransportMode.pt));
        
        // there should be no compensation so far
        Assert.assertEquals("Compensation should be 0, but is not!", 0.0, fare.getValue(), MatsimTestUtils.EPSILON);

        // test drt after pt leg
        events.processEvent(new PersonDepartureEvent(4.0, personId, Id.createLinkId("45"), TransportMode.drt));
        
        // compensation paid once
		Assert.assertEquals("After a pt and a drt leg compensation should be paid once, but is not", 1 * compensationPerTrip, fare.getValue(), MatsimTestUtils.EPSILON);
		
		// some distraction, nothing should change
        events.processEvent(new PersonDepartureEvent(4.0, personId, Id.createLinkId("45"), TransportMode.pt));
		
	    // compensation paid once
		Assert.assertEquals("After a pt and a drt leg compensation should be paid once, but is not", 1 * compensationPerTrip, fare.getValue(), MatsimTestUtils.EPSILON);

		
		// end trip
        events.processEvent(new ActivityStartEvent(5.0, personId, Id.createLinkId("23"), Id.create("dummy", ActivityFacility.class), "blub"));
        
        // test drt2 before pt with interaction activity in between
        events.processEvent(new PersonDepartureEvent(6.0, personId, Id.createLinkId("45"), "drt2"));
        events.processEvent(new ActivityStartEvent(7.0, personId, Id.createLinkId("56"), Id.create("dummy", ActivityFacility.class), "drt interaction"));
        events.processEvent(new PersonDepartureEvent(8.0, personId, Id.createLinkId("56"), TransportMode.pt));
        
        // compensation paid second time (second trip)
		Assert.assertEquals("After a drt2 and a pt leg compensation should be paid a 2nd time, but is not", 2 * compensationPerTrip, fare.getValue(), MatsimTestUtils.EPSILON);
		
		// some distraction, nothing should change
        events.processEvent(new PersonDepartureEvent(4.0, personId, Id.createLinkId("45"), TransportMode.pt));
		Assert.assertEquals("After a drt2 and a pt leg compensation should be paid a 2nd time, but is not", 2 * compensationPerTrip, fare.getValue(), MatsimTestUtils.EPSILON);
        
        events.processEvent(new PersonDepartureEvent(4.0, personId, Id.createLinkId("67"), TransportMode.drt));
        
        // compensation paid third time (second trip)
		Assert.assertEquals("After another drt leg compensation should be paid a 3nd time, but is not", 3 * compensationPerTrip, fare.getValue(), MatsimTestUtils.EPSILON);

    }


}
