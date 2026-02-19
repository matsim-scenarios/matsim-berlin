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
 * Berlin scenario including the possibility to change beta money and thus the perception of monetary prices.
 * E.g. for pt or car.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinBetaMoneyScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinBetaMoneyScenario.class);

	@CommandLine.Option(names = "--beta-money", description = "Defines to which value the general marginal ut. of money is set. Default = 1.0", defaultValue = "1.0")
	private double betaMoney;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		setBetaMoneyInConfig(config, betaMoney);
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
	 * betaMoney in [util/Eu].
	 */
	static void setBetaMoneyInConfig(Config config, double betaMoney) {
		config.scoring().setMarginalUtilityOfMoney(betaMoney);
		if (betaMoney != 1.0) {
			log.info("Marginal utility of money was set to {}. Default is 1.0. Make sure this is what you want.", betaMoney);
		}
	}
}
