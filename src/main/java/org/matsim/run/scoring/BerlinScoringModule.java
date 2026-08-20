package org.matsim.run.scoring;

import jakarta.inject.Singleton;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.PseudoRandomScoringModule;

/**
 * Install scoring related components.
 */
public class BerlinScoringModule extends AbstractModule {

	/**
	 * Published value: pi^2/6/3.32 ~ 0.495 (variance of gumbel / mean number of trips).
	 * Note this passes a variance where a sd is expected; kept as-is for reproducibility
	 * of the published model.
	 */
	public static final double PSEUDO_RANDOM_SCALE_PUBLISHED = 0.495;

	/**
	 * Re-estimated value: sd of the per-(person, mode, trip situation) normal error component,
	 * estimated directly at the plan level (plan_model.py, error-components MXL; smoke-scale
	 * k=9/1pct value 2.213 +- 0.47, pending 10pct). An estimate, not a conversion; the residual
	 * plan-level Gumbel of the estimation is supplied by the ChangeExpBeta plan selector.
	 */
	public static final double PSEUDO_RANDOM_SCALE_REESTIMATED = 2.213453;

	private final double pseudoRandomTripScale;

	public BerlinScoringModule() {
		this(PSEUDO_RANDOM_SCALE_PUBLISHED);
	}

	public BerlinScoringModule(double pseudoRandomTripScale) {
		this.pseudoRandomTripScale = pseudoRandomTripScale;
	}

	@Override
	public void install() {

		bind(TransitRouteToMode.class).in(Singleton.class);

		bindScoringFunctionFactory().to(BerlinScoringFunctionFactory.class).in(Singleton.class);

		install(new PseudoRandomScoringModule(TasteVariationsConfigParameterSet.VariationType.normal, pseudoRandomTripScale));
	}

}
