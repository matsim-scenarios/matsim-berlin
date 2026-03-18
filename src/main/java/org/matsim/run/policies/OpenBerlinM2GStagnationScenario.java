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
 * 2) price change in pt
 * 3) drt
 * 4) marginal utility of money
 * 5) bicycle speed
 * 6) sharing
 * 7) home office
 * 8) road capacity
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
//		2) price change in pt
//		no changes in config compared to base case
//		3) drt
//		no changes in config compared to base case
//		TODO: drt is more complicated because the EstimatorScenario inherits from BerlinDrtScenario, not OpenBerlinScenario
//		4) marginal utility of money
//		set marginal utility of money to 1.5: everything is/feels more expensive now (default 1.0)
		OpenBerlinBetaMoneyScenario.setBetaMoneyInConfig(config, BETA_MONEY);
//		5) bicycle speed
//		no changes in config compared to base case
//		6) sharing
//		no changes in config compared to base case
//		7) home office
//		no changes in config compared to base case
//		8) road capacity
//		no changes in config compared to base case
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
//		2) price change in pt
//		no changes in scenario compared to base case
//		3) drt
//		no changes in scenario compared to base case
//		4) marginal utility of money
//		no changes in scenario compared to base case
//		5) bicycle speed
//		no changes in scenario compared to base case
//		6) sharing
//		no changes in scenario compared to base case
//		7) home office
//		no changes in scenario compared to base case
//		8) road capacity
//		no changes in scenario compared to base case
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in scenario compared to base case
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

//		1) vehicle composition
//		TODO: do this here or completely in post-processing?
//		2) price change in pt
//		no changes in controller compared to base case
//		3) drt
//		no changes in controller compared to base case
//		4) marginal utility of money
//		no changes in controller compared to base case
//		5) bicycle speed
//		no changes in controller compared to base case
//		6) sharing
//		no changes in controller compared to base case
//		7) home office
//		no changes in controller compared to base case
//		8) road capacity
//		no changes in controller compared to base case
//		9) changes in maximum allowed speed for motorized vehicles
//		no changes in controller compared to base case
	}
}
