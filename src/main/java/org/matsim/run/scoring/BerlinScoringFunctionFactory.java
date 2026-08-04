package org.matsim.run.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.functions.*;

/**
 * Plugs together the scoring function factory for the Berlin model.
 * <p>
 * The activity term scores each activity against its own typical duration, taken from the
 * {@value org.matsim.prepare.EncodeTypicalDuration#TYPICAL_DURATION} attribute rather than from the activity type (see
 * {@link org.matsim.prepare.EncodeTypicalDuration}). Person activities must carry one, enforced by
 * {@link RequiredTypicalDurationCalculator}; freight and commercial subpopulations fall back to the config values.
 */
public final class BerlinScoringFunctionFactory implements ScoringFunctionFactory {

	private final Config config;
	private final AnalysisMainModeIdentifier mmi;
	private final Network network;
	private final ScoringParametersForPerson params;
	private final TransitRouteToMode ptRouteToMode;
	private final PseudoRandomScorer pseudoRNG;


	@Inject
	public BerlinScoringFunctionFactory(Config config, AnalysisMainModeIdentifier mmi, Network network,
										ScoringParametersForPerson params,
										TransitRouteToMode ptRouteToMode,
										PseudoRandomScorer pseudoRNG) {
		this.config = config;
		this.mmi = mmi;
		this.network = network;
		this.params = params;
		this.ptRouteToMode = ptRouteToMode;
		this.pseudoRNG = pseudoRNG;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		final ScoringParameters parameters = params.getScoringParameters(person);

//		the activities the events machinery hands to the scoring carry no attributes, so the calculator is given the
//		person and reads them off the selected plan's main activities.
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters, TypicalDurations.forPerson(config, person), person));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, config.transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new PseudoRandomTripScoring(person.getId(), mmi, pseudoRNG));
		sumScoringFunction.addScoringFunction(new TransitTripScoring(parameters, ptRouteToMode));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;

	}

}
