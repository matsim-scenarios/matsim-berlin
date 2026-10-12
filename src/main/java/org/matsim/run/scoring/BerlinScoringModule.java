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

		// scale 0 disables the pseudo-random trip error; the plan selector supplies the choice randomness
		install(new PseudoRandomScoringModule(TasteVariationsConfigParameterSet.VariationType.normal, 0));
	}

}
