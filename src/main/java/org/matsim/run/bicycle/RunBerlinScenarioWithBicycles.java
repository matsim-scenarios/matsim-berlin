/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.run.bicycle;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.Bicycles;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.run.RunBerlinScenario;

/**
 * @author dziemke
 */
public final class RunBerlinScenarioWithBicycles {

	private static final Logger log = Logger.getLogger(RunBerlinScenarioWithBicycles.class);

	public static void main(String[] args) {
		for (String arg : args) {
			log.info( arg );
		}

		if ( args.length==0 ) {
			throw new IllegalArgumentException("Config file needs to be provided.");
		}
		
		Config config = prepareConfig( args ) ;
		Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
		Controler controler = prepareControler( scenario ) ;
		controler.run() ;
	}
	
	public static Controler prepareControler( Scenario scenario ) {
		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
		Bicycles.addAsOverridingModule(controler);

		return controler;
	}

	public static Config prepareConfig( String [] args, ConfigGroup... customModules) {
		BicycleConfigGroup bicycleConfigGroup = new BicycleConfigGroup();
		bicycleConfigGroup.setBicycleMode("bicycle");
		ConfigGroup[] customModulesToAdd = new ConfigGroup[]{bicycleConfigGroup};
		ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

		int counter = 0;
		for (ConfigGroup customModule : customModules) {
			customModulesAll[counter] = customModule;
			counter++;
		}

		for (ConfigGroup customModule : customModulesToAdd) {
			customModulesAll[counter] = customModule;
			counter++;
		}

		Config config = RunBerlinScenario.prepareConfig( args, customModulesAll ) ;
		config.plansCalcRoute().removeModeRoutingParams("bicycle");

		//
		config.controler().setLastIteration(0);
		//

		return config ;
	}
}