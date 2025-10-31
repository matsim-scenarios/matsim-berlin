package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.StrategicChargingScenarioConfigurator;
import org.matsim.contrib.ev.strategic.StrategicChargingScenarioConfigurator.Settings;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class OpenBerlinScenarioWithStrategicCharging extends OpenBerlinScenario {
	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinScenarioWithStrategicCharging.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);

		// set up general base configuration for strategic charging
		StrategicChargingUtils.configure(config);

		// configure standalone strategic charging (without mode choice, etc.) for the
		// "person" subpopulation
		StrategicChargingUtils.configureStandaloneReplanning(config, "person");

		// avoid aborting because not ChangeExpBeta is defined
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		// set up anaylsis
		configureAnalysis(config);

		return config;
	}

	private StrategicChargingScenarioConfigurator configurator;

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		// caveat for the vehicles when running the Berlin scenario
		verifyVehicles(scenario);

		// prepare the settings for strategic charging
		Settings settings = new Settings();
		settings.persons.subpopulations.add("person");

		// activate persons, add vehicles, add chargers
		configurator = new StrategicChargingScenarioConfigurator(settings);
		configurator.configureScenario(scenario);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		// install the relevant modules
		StrategicChargingUtils.configureController(controler);

		// feed back infrastructure into the controller
		configurator.applyInfrastructure(controler);
	}

	private void configureAnalysis(Config config) {
		// track score every 10 iterations
		StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config);
		sevcConfig.setScoreTrackingInterval(10);

		// some output for visualization and analysis
		EvConfigGroup evConfig = EvConfigGroup.get(config);
		evConfig.setWriteVehicleTrajectoriesInterval(100);
		evConfig.setWriteZonalEnergyDemandInterval(1);

		SquareGridZoneSystemParams gridParams = new SquareGridZoneSystemParams();
		gridParams.setCellSize(1000.0);
		evConfig.addParameterSet(gridParams);
	}

	private void verifyVehicles(Scenario scenario) {
		scenario.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		/*
		 * The problem here is that the Berlin scenario has an incomplete vehicle file
		 * and that the vehicles source is set to `modeVehicleTypesFromVehiclesData`.
		 * Even if we add specific vehicles to the persons, those vehicles will be
		 * overriden by automatically generated vehicle-type-specific vehicles. This is
		 * why we start the Berlin sceanrio WITH A VEHICLES FILE THAT IS AN OUTPUT OF A
		 * PREVIOUS SIMULATION of that scenario. This way, we have all the individual
		 * vehicles and override some of them.
		 */

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (PopulationUtils.getSubpopulation(person).equals("person")) {
				Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, "car");

				if (vehicleId == null) {
					throw new IllegalStateException("See verifyVehicles in the run script.");
				}
			}
		}
	}
}
