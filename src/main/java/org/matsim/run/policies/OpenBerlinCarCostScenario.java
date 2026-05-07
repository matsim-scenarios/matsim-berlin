package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;

/**
 * Berlin scenario including the possibility to change car cost parameters.
 * We can change car fix cost (per day) or car distance based cost.
 * The changes in car cost also change the cost params of ride, because they are dependent on the car params.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinCarCostScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinCarCostScenario.class);

	@CommandLine.Option(names = "--car-fix-cost", description = "Defines to which value the daily monetary constant for mode car is set. Default = -5.0Eu/d", defaultValue = "-5.0")
	private double carFixCost;

	@CommandLine.Option(names = "--car-distance-cost", description = "Defines to which value the monetary distance rate for mode car is set. Default = -0.000149Eu/m", defaultValue = "-0.000149")
	private double carDistanceCost;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		setCarCostInConfig(config, carFixCost, carDistanceCost);
		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}

	/**
	 * daily mon constant in Eu/d.
	 * monetary distance rate in Eu/m.
	 */
	static void setCarCostInConfig(Config config, double carFixCost, double carDistanceCost) {
		if (carFixCost != -5.0) {
			config.scoring().getModes().get(TransportMode.car).setDailyMonetaryConstant(carFixCost);
			log.info("Daily monetary constant for car was set to {}. Default is -5.0. Make sure this is what you want.", carFixCost);
		}
		if (carDistanceCost != -0.000149) {
			config.scoring().getModes().get(TransportMode.car).setMonetaryDistanceRate(carDistanceCost);
//			alpha = 1.0 is the value from the base run class OpenBerlinScenario
			RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);
			log.info("Monetary distance rate for car was set to {}. Default is -0.000149. Make sure this is what you want.", carFixCost);
			log.info("Please note that this also changes the monetary distance rate for mode ride because it is dependent on the monetary distance rate for car.");
			log.info("Thus, the monetary distance rate for ride was set to {}", config.scoring().getModes().get(TransportMode.ride).getMonetaryDistanceRate());
		}
	}
}
