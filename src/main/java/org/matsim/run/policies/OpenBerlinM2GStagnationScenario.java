package org.matsim.run.policies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.OpenBerlinScenario;

import javax.annotation.Nullable;

/**
 * This is the scenario class for the third M2G scenario: "stagnation" (Stagnation durch Resignation).
 * In this class, all changes regarding the chosen indicators are made.
 * The changes are called as methods from the according scenarios (e.g. OpenBerlinBetaMoneyScenario).
 * For the sake of readability and trying to prevent chaos the order of changes in each method of this class will be:
 * 1) vehicle composition
 * 2) drt
 * 3) marginal utility of money
 * 4) bicycle speed
 * 5) sharing
 * 6) home office
 * 7) road capacity
 * 8) (price change in pt)
 * 9) changes in maximum allowed speed for motorized vehicles
 * All necessary configurations will be made in this class.
 */
public class OpenBerlinM2GStagnationScenario extends OpenBerlinScenario {
	private static final double BETA_MONEY = 1.5;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) drt
//		no changes in config compared to base case
//		TODO: drt is more complicated because the EstimatorScenario inherits from BerlinDrtScenario, not OpenBerlinScenario
//		3) marginal utility of money
//		set marginal utility of money to 1.5: everything is/feels more expensive now (default 1.0)
		OpenBerlinBetaMoneyScenario.setBetaMoneyInConfig(config, BETA_MONEY);
//		4) bicycle speed
//		no changes in config compared to base case
//		5) sharing
//		no changes in config compared to base case
//		6) home office
//		no changes in config compared to base case
//		7) road capacity
//		no changes in config compared to base case
//		8) (price change in pt)
//		TODO: tbd
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in config compared to base case

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) drt
//		no changes in scenario compared to base case
//		TODO: add createScenario method for drt here (if needed)
//		3) marginal utility of money
//		no changes in scenario compared to base case
//		4) bicycle speed
//		no changes in scenario compared to base case
//		5) sharing
//		no changes in scenario compared to base case
//		6) home office
//		no changes in scenario compared to base case
//		7) road capacity
//		no changes in scenario compared to base case
//		8) (price change in pt)
//		TODO: tbd
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in scenario compared to base case
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) drt
//		no changes in controller compared to base case
//		3) marginal utility of money
//		no changes in controller compared to base case
//		4) bicycle speed
//		no changes in controller compared to base case
//		5) sharing
//		no changes in controller compared to base case
//		6) home office
//		no changes in controller compared to base case
//		7) road capacity
//		no changes in controller compared to base case
//		8) (price change in pt)
//		TODO: tbd
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in controller compared to base case
	}
}
