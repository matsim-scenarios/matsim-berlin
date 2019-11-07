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

package org.matsim.run.drt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.DrtSpeedUpModule;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

/**
* @author ikaddoura
*/

public class RunOpenBerlinWithDRTSpeedUp {
	private static final Logger log = Logger.getLogger(RunOpenBerlinWithDRTSpeedUp.class);

	public static void main(String[] args) {
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"test/input/berlin-drt-v5.5-1pct.config.xml"}  ;
		}
		
		Config config = RunDrtOpenBerlinScenario.prepareConfig( args , new DrtSpeedUpConfigGroup() ) ;
		DrtSpeedUpModule.adjustConfig(config);
		
		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
		Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				// the current one in open berlin needs to be overwritten
				bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifierWithDrtTeleportation.class);
			}
		});
		
		controler.addOverridingModule(new DrtSpeedUpModule());
		
		controler.run() ;
	}

}

