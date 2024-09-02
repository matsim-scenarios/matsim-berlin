package org.matsim.prepare.choices;


import org.jetbrains.annotations.Nullable;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Uses the subtour mutator to generate plans.
 */
public class SubtourPlanGenerator implements CandidateGenerator {

	private final int k;
	private final SubtourModeChoice modeChoice;
	private final PlanAlgorithm algo;
	private final TopKChoicesGenerator gen;

	public SubtourPlanGenerator(int k, TopKChoicesGenerator generator, PermissibleModesCalculator permissibleModesCalculator, Config config) {
		this.k = k;
		this.modeChoice = new SubtourModeChoice(config.global(), config.subtourModeChoice(), permissibleModesCalculator);
		this.algo = modeChoice.getPlanAlgoInstance();
		this.gen = generator;
	}

	@Override
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> set, @Nullable boolean[] booleans) {

		List<String[]> result = new ArrayList<>();
		result.add(planModel.getCurrentModes());

		for (int i = 0; i < k; i++) {
			Plan plan = planModel.getPlan();
			algo.run(plan);

			PlanModel updated = PlanModel.newInstance(plan);
			result.add(updated.getCurrentModesMutable());
		}

		return gen.generatePredefined(planModel, result)
			.stream().distinct().toList();
	}
}
