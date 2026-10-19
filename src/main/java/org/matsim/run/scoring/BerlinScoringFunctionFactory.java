package org.matsim.run.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.functions.*;

/**
 * Plugs together the scoring function factory for the Berlin model.
 * <p>
 * The activity term scores each activity against its own typical duration, taken by the injected
 * {@link TypicalDurationCalculator} from the person's typicalDurations attribute rather than from the activity type
 * (see {@link org.matsim.run.OpenBerlinScenario#prepareConfig} and
 * {@link org.matsim.application.prepare.population.EncodeTypicalDurations}).
 */
public final class BerlinScoringFunctionFactory implements ScoringFunctionFactory {

	private final Config config;
	private final AnalysisMainModeIdentifier mmi;
	private final ScoringParametersForPerson params;
	private final TypicalDurationCalculator typicalDurationCalculator;
	private final TransitRouteToMode ptRouteToMode;
	private final PseudoRandomScorer pseudoRNG;


	@Inject
	public BerlinScoringFunctionFactory(Config config, AnalysisMainModeIdentifier mmi,
										ScoringParametersForPerson params,
										TypicalDurationCalculator typicalDurationCalculator,
										TransitRouteToMode ptRouteToMode,
										PseudoRandomScorer pseudoRNG) {
		this.config = config;
		this.mmi = mmi;
		this.params = params;
		this.typicalDurationCalculator = typicalDurationCalculator;
		this.ptRouteToMode = ptRouteToMode;
		this.pseudoRNG = pseudoRNG;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		final ScoringParameters parameters = params.getScoringParameters(person);

//		with the person, the core pairs each executed activity with the selected plan's main activity at the same
//		position, and the calculator takes the typical duration at that position from the person attribute.
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters, typicalDurationCalculator, person));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, config.transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new PseudoRandomTripScoring(person.getId(), mmi, pseudoRNG));
		sumScoringFunction.addScoringFunction(new TransitTripScoring(parameters, ptRouteToMode));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;

	}

}
