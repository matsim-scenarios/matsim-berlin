package org.matsim.run.scoring;

import com.google.inject.Key;
import jakarta.inject.Singleton;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

/**
 * Module to bind components needed for advanced scoring functionality configured by {@link AdvancedScoringConfigGroup}.
 */
public class AdvancedScoringModule extends AbstractModule {

	@Override
	public void install() {

		ConfigUtils.addOrGetModule(getConfig(), AdvancedScoringConfigGroup.class);

		bind(ScoringParametersForPerson.class).to(IndividualPersonScoringParameters.class).in(Singleton.class);

		addControlerListenerBinding().to(AdvancedScoringOutputWriter.class).in(Singleton.class);

		bindScoringFunctionFactory().to(AdvancedScoringFunctionFactory.class).in(Singleton.class);
	}
}
