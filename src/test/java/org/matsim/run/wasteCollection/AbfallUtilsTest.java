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
package org.matsim.run.wasteCollection;

import java.util.Collection;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author nagel
 *
 */
public class AbfallUtilsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public final void setUp() {

	}

	@Test
	public final void testCreateCarrierMap() {
		String vehicleTypesFileLocation = "scenarios/berlin-v5.5-10pct/input/wasteCollection/vehicleTypes.xml";
		String inputCarriersWithDieselVehicle = "scenarios/berlin-v5.5-10pct/input/wasteCollection/carriers_diesel_vehicle.xml";
		String inputCarriersWithMediumBatteryVehicle = "scenarios/berlin-v5.5-10pct/input/wasteCollection/carriers_medium_EV.xml";
		String inputCarriersWithSmallBatteryVehicle = "scenarios/berlin-v5.5-10pct/input/wasteCollection/carriers_small_EV.xml";
		Config config = ConfigUtils.createConfig();
		config = AbfallUtils.prepareConfig(config, 0, vehicleTypesFileLocation, inputCarriersWithDieselVehicle);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		// creates carrier
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		HashMap<String, Carrier> carrierMap = AbfallUtils.createCarrier(carriers);
		Assert.assertEquals(4, carrierMap.size());
		Assert.assertTrue(carrierMap.containsKey("Nordring"));
		Assert.assertTrue(carrierMap.containsKey("MalmoeerStr"));
		Assert.assertTrue(carrierMap.containsKey("Gradestrasse"));
		Assert.assertTrue(carrierMap.containsKey("Forckenbeck"));
		for (Carrier singleCarrier : carrierMap.values()) {
			Assert.assertNotNull(singleCarrier);
		}
		
		config = AbfallUtils.prepareConfig(config, 0, vehicleTypesFileLocation, inputCarriersWithMediumBatteryVehicle);
		scenario = ScenarioUtils.loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		// creates carrier
		carriers = FreightUtils.addOrGetCarriers(scenario);
		carrierMap = AbfallUtils.createCarrier(carriers);
		Assert.assertEquals(4, carrierMap.size());
		Assert.assertTrue(carrierMap.containsKey("Nordring"));
		Assert.assertTrue(carrierMap.containsKey("MalmoeerStr"));
		Assert.assertTrue(carrierMap.containsKey("Gradestrasse"));
		Assert.assertTrue(carrierMap.containsKey("Forckenbeck"));
		for (Carrier singleCarrier : carrierMap.values()) {
			Assert.assertNotNull(singleCarrier);
		}
		config = AbfallUtils.prepareConfig(config, 0, vehicleTypesFileLocation, inputCarriersWithSmallBatteryVehicle);
		scenario = ScenarioUtils.loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		// creates carrier
		carriers = FreightUtils.addOrGetCarriers(scenario);
		carrierMap = AbfallUtils.createCarrier(carriers);
		Assert.assertEquals(4, carrierMap.size());
		Assert.assertTrue(carrierMap.containsKey("Nordring"));
		Assert.assertTrue(carrierMap.containsKey("MalmoeerStr"));
		Assert.assertTrue(carrierMap.containsKey("Gradestrasse"));
		Assert.assertTrue(carrierMap.containsKey("Forckenbeck"));
		for (Carrier singleCarrier : carrierMap.values()) {
			Assert.assertNotNull(singleCarrier);
		}
	}

	@Test
	public final void testCreateMapWithLinksInDistricts() {

	}

	@Test
	public final void testCreateDumpMap() {
		HashMap<String, Id<Link>> garbageDumps = AbfallUtils.createDumpMap();
		Assert.assertEquals(5, garbageDumps.size());
		Assert.assertTrue(garbageDumps.get("Ruhleben").toString().equals("142010"));
		Assert.assertTrue(garbageDumps.get("Gradestr").toString().equals("71781"));
		Assert.assertTrue(garbageDumps.get("Pankow").toString().equals("145812"));
		Assert.assertTrue(garbageDumps.get("ReinickenD").toString().equals("59055"));
		Assert.assertTrue(garbageDumps.get("GruenauerStr").toString().equals("97944"));
		for (Id<Link> link : garbageDumps.values()) {
			Assert.assertNotNull(link);
		}

	}

	@Test
	public final void testShapeFile() {
		final String berlinDistrictsWithGarbageInformations = "scenarios/berlin-v5.5-10pct/input/wasteCollection/garbageInput/districtsWithGarbageInformations.shp";
		Collection<SimpleFeature> districtsWithGarbage = ShapeFileReader
				.getAllFeatures(berlinDistrictsWithGarbageInformations);
		for (SimpleFeature districtInformation : districtsWithGarbage) {
			Assert.assertNotNull(districtInformation.getAttribute("Depot"));
			Assert.assertNotNull(districtInformation.getAttribute("Ortsteil"));
			Assert.assertTrue(((double) districtInformation.getAttribute("MO")
					+ (double) districtInformation.getAttribute("DI") + (double) districtInformation.getAttribute("MI")
					+ (double) districtInformation.getAttribute("DO")
					+ (double) districtInformation.getAttribute("FR")) > 0);

			HashMap<String, Id<Link>> garbageDumps = AbfallUtils.createDumpMap();
			if ((double) districtInformation.getAttribute("MO") > 0)
				Assert.assertTrue(garbageDumps.containsKey(districtInformation.getAttribute("Mo-Ent").toString()));
			if ((double) districtInformation.getAttribute("DI") > 0)
				Assert.assertTrue(garbageDumps.containsKey(districtInformation.getAttribute("Di-Ent").toString()));
			if ((double) districtInformation.getAttribute("MI") > 0)
				Assert.assertTrue(garbageDumps.containsKey(districtInformation.getAttribute("Mi-Ent").toString()));
			if ((double) districtInformation.getAttribute("DO") > 0)
				Assert.assertTrue(garbageDumps.containsKey(districtInformation.getAttribute("Do-Ent").toString()));
			if ((double) districtInformation.getAttribute("FR") > 0)
				Assert.assertTrue(garbageDumps.containsKey(districtInformation.getAttribute("Fr-Ent").toString()));
			Assert.assertNotNull(districtInformation.getDefaultGeometry());

		}
		Assert.assertEquals(96, districtsWithGarbage.size());
	}
}
