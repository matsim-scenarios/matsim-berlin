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

//		bind(TransitRouteToMode.class).in(Singleton.class);

		bindScoringFunctionFactory().to(BerlinScoringFunctionFactory.class).in(Singleton.class);

//		// scale = pi ^2 / 6 / 3.32 ~ 0.495 (variance of gumbel / mean number of trips)
//		install(new PseudoRandomScoringModule(TasteVariationsConfigParameterSet.VariationType.normal, 0.495));
	}

}
