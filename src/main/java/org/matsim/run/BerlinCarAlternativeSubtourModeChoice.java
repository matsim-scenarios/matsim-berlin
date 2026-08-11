package org.matsim.run;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * SMC for differentiation between agents which may use carExpensive only vs agents which may choose between carExpensive and "normal" car.
 */
public class BerlinCarAlternativeSubtourModeChoice implements Provider<PlanStrategy> {
	public static final String STRATEGY_NAME = "BerlinCarAlternativeSubTourModeChoice";

	@Inject
	private GlobalConfigGroup globalConfigGroup;
	@Inject
	private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	@Inject
	private PermissibleModesCalculator permissibleModesCalculator;

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());

		builder.addStrategyModule(new org.matsim.core.replanning.modules.SubtourModeChoice(globalConfigGroup, subtourModeChoiceConfigGroup, permissibleModesCalculator));

		builder.addStrategyModule(new AbstractMultithreadedModule(globalConfigGroup) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return new BerlinCarAlternativePlanAlgorithm(subtourModeChoiceConfigGroup);
			}
		});

		return null;
	}
}
