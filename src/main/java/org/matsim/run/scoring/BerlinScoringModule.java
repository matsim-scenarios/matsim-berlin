package org.matsim.run.scoring;

import jakarta.inject.Singleton;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.PseudoRandomScoringModule;

/**
 * Install scoring related components.
 */
public class BerlinScoringModule extends AbstractModule {

	@Override
	public void install() {

		bind(TransitRouteToMode.class).in(Singleton.class);

		bindScoringFunctionFactory().to(BerlinScoringFunctionFactory.class).in(Singleton.class);

		// Estimated situational error scale EC_S: sd of the per-(person, mode, trip situation)
		// normal error component, estimated directly at the plan level (plan_model.py,
		// error-components MXL; smoke-scale k=9/1pct value 2.213 +- 0.47, pending 10pct).
		// Replaces the earlier derived value pi^2/6/3.32 ~ 0.495, which (a) was a variance
		// passed as a sd and (b) is superseded: the scale is now an estimate, not a conversion.
		// The residual plan-level Gumbel of the estimation is supplied by the ChangeExpBeta
		// plan selector (scale 1 by construction), not by this term.
		install(new PseudoRandomScoringModule(TasteVariationsConfigParameterSet.VariationType.normal, 2.213453));
	}

}
