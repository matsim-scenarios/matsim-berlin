package org.matsim.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import picocli.CommandLine;

import java.util.Set;

/**
 * Run configurable Berlin scenario experiments.
 */
@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION,
	mixinStandardHelpOptions = true, showDefaultValues = true)
public final class OpenBerlinScenarioExperiments extends OpenBerlinScenario {

	private static final double REASONABLE_PRICES_CAR_DAILY_MONETARY_CONSTANT = -8.48;
	private static final double REASONABLE_PRICES_CAR_MONETARY_DISTANCE_RATE = -0.000093;
	private static final double BUSINESS_COSTS_CAR_DAILY_MONETARY_CONSTANT = -12.32;
	private static final double BUSINESS_COSTS_CAR_MONETARY_DISTANCE_RATE = -0.000156;
	private static final double PT_DAILY_MONETARY_CONSTANT = -3.0;
	private static final double PERFORMING_UTILS_HR = 6.0;
	private static final double RIDE_ALPHA = 1.0;
	private static final double V72_INCOME_EXPONENT = 0.275502;

	// Alternative-specific constants from the v6.4 scoring configuration.
	private static final double CAR_CONSTANT = -0.487593225702316;
	private static final double RIDE_CONSTANT = -1.153972863285258;
	private static final double PT_CONSTANT = 0.43220302370275404;
	private static final double BIKE_CONSTANT = -0.8720995248245994;

	@CommandLine.Option(names = "--experiment", defaultValue = "moreReasonablePrices",
		description = "Experiment to apply: ${COMPLETION-CANDIDATES}.")
	private Experiment experiment = Experiment.moreReasonablePrices;

	@CommandLine.Option(names = "--income-exponent", defaultValue = "0.275502",
		description = "Exponent for income-dependent utility of money.")
	private double incomeExponent = V72_INCOME_EXPONENT;

	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinScenarioExperiments.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		Config preparedConfig = super.prepareConfig(config);

		switch (experiment) {
			case moreReasonablePrices -> applyMoreReasonablePrices(preparedConfig, incomeExponent);
			case businessCosts -> applyBusinessCosts(preparedConfig, incomeExponent);
		}

		return preparedConfig;
	}

	static void applyMoreReasonablePrices(Config config, double incomeExponent) {
		applyScoring(config, incomeExponent,
			REASONABLE_PRICES_CAR_DAILY_MONETARY_CONSTANT,
			REASONABLE_PRICES_CAR_MONETARY_DISTANCE_RATE);
	}

	static void applyBusinessCosts(Config config, double incomeExponent) {
		applyScoring(config, incomeExponent,
			BUSINESS_COSTS_CAR_DAILY_MONETARY_CONSTANT,
			BUSINESS_COSTS_CAR_MONETARY_DISTANCE_RATE);
	}

	private static void applyScoring(Config config, double incomeExponent,
			double carDailyMonetaryConstant, double carMonetaryDistanceRate) {
		if (!Double.isFinite(incomeExponent) || incomeExponent < 0.0) {
			throw new IllegalArgumentException("--income-exponent must be a finite, non-negative number.");
		}

		ScoringConfigGroup scoring = config.scoring();
		ScoringConfigGroup.ScoringParameterSet parameters = scoring.getScoringParameters(null);

		TasteVariationsConfigParameterSet tasteVariations = parameters.getOCreateTasteVariationsParams();
		tasteVariations.setIncomeExponent(incomeExponent);
		// Keep v7.2 income scaling, but do not load person-specific random mode constants.
		tasteVariations.setVariationsOf(Set.of());

		// Remove the v7.2 estimates before applying the v6.4 scoring parameters below.
		for (ScoringConfigGroup.ModeParams mode : parameters.getModes().values()) {
			mode.setConstant(0.0);
			mode.setDailyUtilityConstant(0.0);
			mode.setMarginalUtilityOfTraveling(0.0);
			mode.setMarginalUtilityOfDistance(0.0);
		}

		// Retain the v6.4 base value; MATSim's v7.2 person scoring applies the configured income exponent.
		scoring.setMarginalUtilityOfMoney(1.0);
		//use the normal default again
		scoring.setPerforming_utils_hr(PERFORMING_UTILS_HR);

		ScoringConfigGroup.ModeParams car = scoring.getOrCreateModeParams(TransportMode.car);
		car.setConstant(CAR_CONSTANT);
		car.setDailyMonetaryConstant(carDailyMonetaryConstant);
		car.setMonetaryDistanceRate(carMonetaryDistanceRate);
		// Both price experiments define no additional time-variable monetary car cost.
		car.setMarginalUtilityOfTraveling(0.0);

		ScoringConfigGroup.ModeParams pt = scoring.getOrCreateModeParams(TransportMode.pt);
		pt.setConstant(PT_CONSTANT);
		pt.setDailyMonetaryConstant(PT_DAILY_MONETARY_CONSTANT);
		pt.setMonetaryDistanceRate(0.0);

		scoring.getOrCreateModeParams(TransportMode.ride).setConstant(RIDE_CONSTANT);
		scoring.getOrCreateModeParams(TransportMode.bike).setConstant(BIKE_CONSTANT);

		// This must be applied after the car and performing parameters because the helper derives ride from them.
		RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(scoring, RIDE_ALPHA);
	}


	public enum Experiment {
		moreReasonablePrices,
		businessCosts
	}
}
