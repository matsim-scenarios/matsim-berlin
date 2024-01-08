package org.matsim.run.scoring;

import jakarta.inject.Singleton;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

/**
 * Module to bind components needed for VSP scoring.
 */
public class VspScoringModule extends AbstractModule {

	@Override
	public void install() {

		ConfigUtils.addOrGetModule(getConfig(), VspScoringConfigGroup.class);

		bind(ScoringParametersForPerson.class).to(DetailedPersonScoringParameters.class).in(Singleton.class);

		bindScoringFunctionFactory().to(VspScoringFunctionFactory.class).in(Singleton.class);
	}
}
