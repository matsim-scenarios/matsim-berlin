package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;

/**
 * Berlin scenario including the possibility to change the daily monetary constant for pt.
 * And thus the simulation of pt ticketing.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinPtPricingScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinPtPricingScenario.class);

	@CommandLine.Option(names = "--pt-daily-monetary-constant", description = "Defines to which value the daily monetary constant for pt is set. Default = -3.0", defaultValue = "-3.0")
	private double dailyMonetaryConstantPt;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		setDailyMonetaryConstantPtInConfig(config, dailyMonetaryConstantPt);
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
	 * dailyMonetaryConstantPt in [Eu/d].
	 */
	static void setDailyMonetaryConstantPtInConfig(Config config, double dailyMonetaryConstantPt) {
		config.scoring().getModes().get(TransportMode.pt).setDailyMonetaryConstant(dailyMonetaryConstantPt);
		if (dailyMonetaryConstantPt != -3.0) {
			log.info("Daily monetary constant for pt was set to {}. Default is -3.0. Make sure this is what you want.", dailyMonetaryConstantPt);
		}
	}
}
