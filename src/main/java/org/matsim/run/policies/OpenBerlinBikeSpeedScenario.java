package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;

import javax.annotation.Nullable;

/**
 * Berlin scenario including the possibility to change the speed of bicycles.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinBikeSpeedScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinBikeSpeedScenario.class);

	@CommandLine.Option(names = "--max-bike-speed", description = "Defines to which value in km/h the maximum velocity of bikes is set. Default = 10.728 km/h", defaultValue = "10.728")
	private double maxBikeSpeedKmH;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		assertNoTeleportedBikeParamsInConfig(config, maxBikeSpeedKmH);

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		setMaxBikeSpeedInScenario(scenario, maxBikeSpeedKmH);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}

	/**
	 * this method makes sure that the provided max. bike speed is valid +
	 * there are no teleported mode params for bike (bike is routed on the network).
	 */
	static void assertNoTeleportedBikeParamsInConfig(Config config, double maxBikeSpeedKmH) {
		if (maxBikeSpeedKmH <= 0.0) {
			log.fatal("You tried to set the maximum bike speed to {}. It is invalid. Aborting!", maxBikeSpeedKmH);
			throw new IllegalStateException("");
		}

//		bike is teleported, but routed on the network in berlin 6.4
//		this means that there are no teleported mode params for bike. bike speed is set via vehicleType or freespeed
		if (config.routing().getTeleportedModeParams().get(TransportMode.bike) != null) {
			log.fatal("Found teleported mode params for {}." +
				" In matsim-berlin v6.4 {} is routed on the network, so there should not be teleported mode params for this mode! Aborting!", TransportMode.bike, TransportMode.bike);
			throw new IllegalStateException("");
		}
	}

	/**
	 * set max. bike speed to the given value in the vehicle type "bike".
	 */
	static void setMaxBikeSpeedInScenario(Scenario scenario, double maxBikeSpeedKmH) {
		//		original bike speed of berlin 6.4 is 2.98 m/s = 10.728 km/h
		if (maxBikeSpeedKmH != 10.728) {
			scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.bike, VehicleType.class)).setMaximumVelocity(maxBikeSpeedKmH / 3.6);
			log.info("Maximum velocity for {} was set to {} km/h. Default is 10.728 km/h. Make sure this is what you want.", TransportMode.bike, maxBikeSpeedKmH);
		}
	}
}
