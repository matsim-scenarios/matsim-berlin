package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

/**
 * Berlin scenario including the possibility to change the handling of bike (routed on network, in qsim, teleported...).
 * All necessary configs will be made in this class.
 */
public class OpenBerlinBikeNetworkScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinBikeNetworkScenario.class);

	@CommandLine.Option(names = "--bike-handling", description = "Defines how transport mode bike is simulated in the berlin scenario.", required = true)
	private BikeHandling bikeHandling = BikeHandling.ROUTED_ON_NETWORK_NOT_IN_QSIM;
	@CommandLine.Option(names = "--bike-pce", description = "PCE (passenger car equivalents) for bike, if simulated in qsim. Default seems to be 0.2.")
	private double bikePce = 0.2;

	/**
	 * make all necessary config changes for different simulation scenarios of bike.
	 */
	public static void configChangesForBikeNetworkScenario(Config config, BikeHandling bikeHandling) {
		if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_NOT_IN_QSIM) {
//			default
		} else if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_IN_QSIM) {
			QSimConfigGroup qSimConfigGroup = ConfigUtils.addOrGetModule(config, QSimConfigGroup.class);

//			add bike to qsim main modes =: to congested modes
			Set<String> mainModes = new HashSet<>(qSimConfigGroup.getMainModes());
			mainModes.add(TransportMode.bike);
			qSimConfigGroup.setMainModes(mainModes);
			log.info("Added bike as a qsim main mode. Hence, it will be simulated as congested mode.");

		} else if (bikeHandling == BikeHandling.TELEPORTED) {
			RoutingConfigGroup routingConfigGroup = ConfigUtils.addOrGetModule(config, RoutingConfigGroup.class);

//			remove bike as routed (on network) mode
			Set<String> networkModes = new HashSet<>(routingConfigGroup.getNetworkModes());
			networkModes.remove(TransportMode.bike);
			routingConfigGroup.setNetworkModes(networkModes);
			log.info("Removed bike as network mode. Bike is not routed on the network.");

//			add teleported mode params for bike
			RoutingConfigGroup.TeleportedModeParams bikeParams = new RoutingConfigGroup.TeleportedModeParams(TransportMode.bike);
			bikeParams.setBeelineDistanceFactor(1.3);
//			according to v6.4 vehicle types file the reported bike speed in SrV is 10.29km/h
			double bikeTeleportedSpeed = BigDecimal
				.valueOf(10.29 / 3.6)
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();
			bikeParams.setTeleportedModeSpeed(bikeTeleportedSpeed);
			routingConfigGroup.addTeleportedModeParams(bikeParams);
			log.info("Added teleported mode params for bike with teleportedModeSpeed {}.", bikeTeleportedSpeed);
		}
	}

	/**
	 * make all necessary scenario changes for different simulation scenarios of bike.
	 */
	public static void scenarioChangesForBikeNetworkScenario(Scenario scenario, BikeHandling bikeHandling, double bikePce) {
		if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_NOT_IN_QSIM) {
//			default
		} else if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_IN_QSIM) {
//			set pce if different to default
			if (bikePce != 0.2) {
				scenario.getVehicles()
					.getVehicleTypes()
					.get(Id.create(TransportMode.bike, VehicleType.class))
					.setPcuEquivalents(bikePce);
				log.info("PCE (passenger car equivalents) for bike was set to {}. Default is {}.", bikePce, 0.2);
			}
		} else if (bikeHandling == BikeHandling.TELEPORTED) {
//			remove bike veh type
			scenario.getVehicles().removeVehicleType(Id.create(TransportMode.bike, VehicleType.class));
			log.info("Removed vehicle type for bike.");
		}
	}

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		configChangesForBikeNetworkScenario(config, bikeHandling);
		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		scenarioChangesForBikeNetworkScenario(scenario, bikeHandling, bikePce);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}

	/**
	 * Helper Enum to configure how bikes are simulated.
	 */
	public enum BikeHandling {ROUTED_ON_NETWORK_NOT_IN_QSIM, ROUTED_ON_NETWORK_IN_QSIM, TELEPORTED}
}
