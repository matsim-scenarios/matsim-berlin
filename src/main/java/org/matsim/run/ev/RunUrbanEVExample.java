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

import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.sun.xml.bind.v2.TODO;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.urbanEV.ActivityWhileChargingFinder;
import org.matsim.urbanEV.EVUtils;
import org.matsim.urbanEV.UrbanEVModule;
import org.matsim.urbanEV.UrbanVehicleChargingHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;

/*
 * this is an example of how to run MATSim with the UrbanEV module which inserts charging activities for all legs which use a EV.
 * By default, {@link org.matsim.urbanEV.MATSimVehicleWrappingEVSpecificationProvider} is used, which declares any vehicle as an EV
 * that has a vehicle type with HbefaTechnology set to 'electricity'.
 * At the beginning of each iteration, the consumption is estimated. Charging is planned to take place during the latest possible activity in the agent's plan
 * that fits certain criteria (ActivityType and minimum duration) and takes place before the estimated SOC drops below a defined threshold.
 */
public class RunUrbanEVExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-1pct.config-ev-test2.xml", new EvConfigGroup());

		prepareConfig(config);
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
		RunUrbanEVExample.prepareConfig(config);
		config.controler().setOutputDirectory("test/output/urbanEVBerlin");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		//RunUrbanEVExample.addVehicles(scenario);
		Controler controler = prepareControler(scenario);

		controler.run();
	}

	public static Controler prepareControler(Scenario scenario) {

		Controler controler = new Controler(scenario);
		//plug in UrbanEVModule
		//TODO Test activities while charging
		controler.addOverridingModule(new UrbanEVModule());
		//register EV qsim components
		controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));
		return controler;
	}


	public static Scenario addVehicles (Scenario scenario){
		Map<String, VehicleType> mode2VehicleType = new HashMap<>();
		VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();
		VehicleType[] vehicleTypes = new VehicleType[50];
		//VehicleType carVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		for (int i = 0; i < 50; i++) {
			int min = 30;
			int max = 80;

			Random rand = new Random();
			int initialEnergyInKWh = rand.nextInt((max - min) + 30 + min);


			vehicleTypes[i] = vehiclesFactory.createVehicleType(Id.create("EV"+ i, VehicleType.class));
			VehicleUtils.setHbefaTechnology(vehicleTypes[i].getEngineInformation(), "electricity");
			VehicleUtils.setEnergyCapacity(vehicleTypes[i].getEngineInformation(), 200);
			EVUtils.setInitialEnergy(vehicleTypes[i].getEngineInformation(), initialEnergyInKWh);
			EVUtils.setChargerTypes(vehicleTypes[i].getEngineInformation(), Arrays.asList("a", "b", "default"));

			scenario.getVehicles().addVehicleType(vehicleTypes[i]);
			mode2VehicleType.put("EV"+i, vehicleTypes[i]);




		}
		//scenario.getVehicles().getVehicleTypes().remove(scenario.getVehicles().getVehicleTypes().get("car"));
		/*VehicleType carVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 200);
		EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), 100);
		EVUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));*/



		//scenario.getVehicles().addVehicleType(carVehicleType100);
		//mode2VehicleType.put(TransportMode.car, scenario.getVehicles().getVehicleTypes().get("car"));


			for (Person person : scenario.getPopulation().getPersons().values()) {
				Map<String,Id<Vehicle>> mode2VehicleId = new HashMap<>();

				for (String mode : mode2VehicleType.keySet()) {
					int min = 60;
					int max = 160;
					Random rand = new Random();
					int initialEnergyInKWh = rand.nextInt((max - min) + 60 + min);

					Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, mode);
					Vehicle vehicle = vehiclesFactory.createVehicle(vehicleId, mode2VehicleType.get(mode));

					scenario.getVehicles().addVehicle(vehicle);
					mode2VehicleId.put(mode, vehicleId);
					VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2VehicleId);

				}

			}




		return scenario;
		
	}

	public static void prepareConfig(Config config) {
		//TODO actually, should also work with all AccessEgressTypes but we have to check (write JUnit test)
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

		//register charging interaction activities for car
		config.planCalcScore().addActivityParams(
				new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.car + UrbanVehicleChargingHandler.PLUGOUT_INTERACTION)
						.setScoringThisActivityAtAll(false));
		config.planCalcScore().addActivityParams(
				new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.car + UrbanVehicleChargingHandler.PLUGIN_INTERACTION)
						.setScoringThisActivityAtAll(false));
	}
	private static void createAndRegisterVehicles(Scenario scenario, Map<String, VehicleType> mode2VehicleType){
		VehiclesFactory vFactory = scenario.getVehicles().getFactory();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			Map<String,Id<Vehicle>> mode2VehicleId = new HashMap<>();
			for (String mode : mode2VehicleType.keySet()) {
				Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, mode);
				Vehicle vehicle = vFactory.createVehicle(vehicleId, mode2VehicleType.get(mode));
				scenario.getVehicles().addVehicle(vehicle);
				mode2VehicleId.put(mode, vehicleId);
			}
			VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2VehicleId);//probably unnecessary
		}
	}

}
