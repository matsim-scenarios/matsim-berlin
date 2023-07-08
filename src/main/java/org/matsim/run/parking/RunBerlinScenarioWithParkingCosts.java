/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.run.parking;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;

import java.util.List;
import java.util.Set;

public class RunBerlinScenarioWithParkingCosts {

	private static double residentialParkingCosts;

	//https://www.berlin.de/ba-mitte/politik-und-verwaltung/aemter/ordnungsamt/parkraumbewirtschaftung/
	private static double nonResidentialParkingCostPerHour;
	private static final double MAX_DAILY_COST_FACTOR = 12;

	public static void main(String[] args) {

		if ( args.length==0 ) {
//			args = new String[] {"scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"}  ;
			args = new String[] {"" + (20.40d / (2*365)),
					"1.0",
					"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml",
					"--config:controler.lastIteration", "0"
			} ;
		}
		residentialParkingCosts = Double.parseDouble(args[0]);
		nonResidentialParkingCostPerHour = Double.parseDouble(args[1]);

		String[] configArgs = new String[args.length-2];
		for(int i = 2; i<=args.length-1; i++){
			configArgs[i-2] = args[i];
		}
		Config config = RunBerlinScenario.prepareConfig(configArgs, new ParkingCostConfigGroup());

		Scenario scenario = RunBerlinScenario.prepareScenario(config);

		attachParkingCostAttributesToLinks(scenario, residentialParkingCosts);

		Controler controler = RunBerlinScenario.prepareControler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new ParkingCostModule());
				install(new PersonMoneyEventsAnalysisModule());
			}
		});

		controler.run();

	}

	private static void attachParkingCostAttributesToLinks(Scenario scenario, double residentialParkingCosts){

		List<PreparedGeometry> berlinShape = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp"));
		ParkingCostConfigGroup parkingCfg = ConfigUtils.addOrGetModule(scenario.getConfig(), ParkingCostConfigGroup.class);

		for (Link link : scenario.getNetwork().getLinks().values()){
			Set<String> allowedModes = link.getAllowedModes();
			if(allowedModes.contains(parkingCfg.getMode()) && ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), berlinShape)){
				//attach residential parking attribute to all Berlin links
				link.getAttributes().putAttribute(parkingCfg.getResidentialParkingFeeAttributeName(), residentialParkingCosts);
				//duration-based parking costs. first hour is the same as all others
				link.getAttributes().putAttribute(parkingCfg.getFirstHourParkingCostLinkAttributeName(), nonResidentialParkingCostPerHour);
				link.getAttributes().putAttribute(parkingCfg.getExtraHourParkingCostLinkAttributeName(), nonResidentialParkingCostPerHour);
				//max costs per link per day
				link.getAttributes().putAttribute(parkingCfg.getMaxDailyParkingCostLinkAttributeName(), MAX_DAILY_COST_FACTOR * nonResidentialParkingCostPerHour);
			}
		}

	}


}
