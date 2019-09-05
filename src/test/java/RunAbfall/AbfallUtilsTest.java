/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package RunAbfall;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

/**
 * @author nagel
 *
 */
public class AbfallUtilsTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	
	@Before
	public final void setUp() {
	
	}

	@Test
	public final void testcreateCarrierMao() {
		HashMap<String, Carrier> carrierMap = AbfallUtils.createCarrier();
		Assert.assertEquals(3, carrierMap.size());
		Assert.assertTrue(carrierMap.containsKey("Nordring"));
		//...
	}
	

	@Test
	public final void testcreateGarbageTruck() {
		AbfallUtils.carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("truckType", VehicleType.class)).build();
		CarrierVehicle truck = AbfallUtils.createGarbageTruck("testTruck", "12345", 3600., 7200.);
		Assert.assertEquals("testTruck", truck.getVehicleId().toString());
		Assert.assertEquals("12345", truck.getLocation().toString());
		Assert.assertEquals(3600., truck.getEarliestStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(7200., truck.getLatestEndTime(), MatsimTestUtils.EPSILON);
		
	}

}
