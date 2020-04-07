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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.DrtSpeedUpModule;
import org.matsim.run.dynamicShutdown.DynamicShutdownControlerListenerImpl;
import org.matsim.run.dynamicShutdown.ModeChoiceCoverageControlerListener;
import org.matsim.run.dynamicShutdown.TerminateDynamically;
import org.matsim.run.RunBerlinScenario;

/**
* @author ikaddoura
*/

public class RunDrtOpenBerlinScenarioWithDrtSpeedUp {
	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenarioWithDrtSpeedUp.class);

	public static void main(String[] args) {
		for (String arg : args) {
			log.info( arg );
		}
//
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"}  ;
		}

		Config config = RunDrtOpenBerlinScenario.prepareConfig( args , new DrtSpeedUpConfigGroup() ) ;
		DrtSpeedUpModule.adjustConfig(config);

		//jr start
		//
//		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//		config.qsim().setNumberOfThreads(8);
//		config.global().setNumberOfThreads(8);
		config.controler().setLastIteration(1250);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setWriteSnapshotsInterval(0);
		config.transit().setUsingTransitInMobsim(false); //jr end



		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config ) ;
		for( Person person : scenario.getPopulation().getPersons().values() ){
			person.getPlans().removeIf( (plan) -> plan!=person.getSelectedPlan() ) ;
		}

		Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;

		controler.addOverridingModule(new DrtSpeedUpModule());

		// jr start
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().to(DynamicShutdownControlerListenerImpl.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TerminationCriterion.class).to(TerminateDynamically.class);
			}
		});

		// jr end

		controler.run() ;

		RunBerlinScenario.runAnalysis(controler);
	}

}

