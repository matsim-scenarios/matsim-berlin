package org.matsim.run.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

/**
 * The stock Charypar-Nagel scoring, but with the activity term scored against each activity's own typical duration
 * (see {@link TypicalDurations}). Used by the calibration, which otherwise runs on the core default factory and would
 * score every activity against the untagged type's fallback duration once the duration-binned types are gone.
 * <p>
 * Exists because {@code CharyparNagelScoringFunctionFactory} is final and takes no
 * {@link org.matsim.core.scoring.functions.TypicalDurationCalculator}. Deliberately not
 * {@link BerlinScoringFunctionFactory}: the calibration does not want the pseudo-random, transit-trip and score-event
 * terms of the scenario's scoring.
 */
public final class CalibrationScoringFunctionFactory implements ScoringFunctionFactory {

	private final Config config;
	private final ScoringParametersForPerson params;
	private final Network network;

	@Inject
	CalibrationScoringFunctionFactory(Config config, ScoringParametersForPerson params, Network network) {
		this.config = config;
		this.params = params;
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		final ScoringParameters parameters = params.getScoringParameters(person);

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters, TypicalDurations.forPerson(config, person), person));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, config.transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		return sumScoringFunction;
	}
}
