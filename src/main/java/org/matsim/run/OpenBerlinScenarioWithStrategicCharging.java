package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.StrategicChargingScenarioConfigurator;
import org.matsim.contrib.ev.strategic.StrategicChargingScenarioConfigurator.Settings;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;

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

		// prepar the settings for strategic charging
		Settings settings = new Settings();
		settings.persons.subpopulations.add("person");

		// activate persons, add vehicles, add chargers
		configurator = new StrategicChargingScenarioConfigurator(settings);
		configurator.configureScenario(scenario);

		// update cost structures
		configurator.configureCosts(scenario.getConfig());
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
	}
}
