package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;

/**
 * Berlin scenario including the possibility to change the handling of bike (on network, in qsim, teleported...).
 * All necessary configs will be made in this class.
 */
public class OpenBerlinBikeNetworkScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinBikeNetworkScenario.class);

	@CommandLine.Option(names = "--bike-handling", description = "Defines how transport mode bike is simulated in the berlin scenario.", required = true)
	private BikeHandling bikeHandling = BikeHandling.ROUTED_ON_NETWORK_NOT_IN_QSIM;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_NOT_IN_QSIM) {
//			default
		} else if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_IN_QSIM) {
//			TODO: add bike to qsim mainModes. What else? Have a look at dresden/lausitz for that.

		} else if (bikeHandling == BikeHandling.TELEPORTED) {
//			TODO: remove bike as routed mode. Add teleportation params for bike??

		}


		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_NOT_IN_QSIM) {
//			default
		} else if (bikeHandling == BikeHandling.ROUTED_ON_NETWORK_IN_QSIM) {
//			TODO: tag network modes with bike?. What else? Have a look at dresden/lausitz for that.
//			TODO: implement different PCE values here? Or in config?

		} else if (bikeHandling == BikeHandling.TELEPORTED) {
//			TODO: remove bike as routed mode. Add teleportation params for bike??

		}
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}

	/**
	 * Helper Enum to configure how bikes are simulated.
	 */
	private enum BikeHandling {ROUTED_ON_NETWORK_NOT_IN_QSIM, ROUTED_ON_NETWORK_IN_QSIM, TELEPORTED}
}
