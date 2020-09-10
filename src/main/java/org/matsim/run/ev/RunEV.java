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

package org.matsim.run.ev;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBerlinScenario;

class RunEV {

	public static void main(String[] args) {
		if(args.length == 0){
			args = new String[1];
			args[0] = "scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-1pct.config-ev-test.xml";
		}
		Config config = RunBerlinScenario.prepareConfig(args, new EvConfigGroup());
//		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
//		config.qsim().setUsePersonIdForMissingVehicleId(false);

		//TODO
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.none);

		config.planCalcScore().addActivityParams(
				new PlanCalcScoreConfigGroup.ActivityParams(
						PlanCalcScoreConfigGroup.createStageActivityType(TransportMode.car + VehicleChargingHandler.CHARGING_IDENTIFIER))
				.setScoringThisActivityAtAll(false));

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		Scenario scenario = RunBerlinScenario.prepareScenario(config);
		Controler controler = RunBerlinScenario.prepareControler(scenario);

		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding(TransportMode.car).toProvider(new MyEvNetworkRoutingProvider(TransportMode.car));
				installQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						bind(VehicleChargingHandler.class).asEagerSingleton();
						addMobsimScopeEventHandlerBinding().to(VehicleChargingHandler.class);
					}
				});
			}
		});
		controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));

		controler.run();

	}
}
