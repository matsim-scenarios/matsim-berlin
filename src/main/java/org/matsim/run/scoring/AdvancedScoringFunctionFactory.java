package org.matsim.run.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

/**
 * Same as {@link org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory} but with {@link PiecewiseLinearlLegScoring}.
 */
public class AdvancedScoringFunctionFactory implements ScoringFunctionFactory {

	@Inject
	private Config config;

	@Inject
	private ScoringParametersForPerson params;

	@Inject
	private Network network;

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		final ScoringParameters parameters = params.getScoringParameters(person);

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters));
		// replaced original leg scoring
		sumScoringFunction.addScoringFunction(new PiecewiseLinearlLegScoring(parameters, this.network, config.transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;
	}

}
