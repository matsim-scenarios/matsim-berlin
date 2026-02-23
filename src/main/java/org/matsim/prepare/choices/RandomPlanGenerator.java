package org.matsim.prepare.choices;

import org.jetbrains.annotations.Nullable;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.ModeEstimate;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.*;

/**
 * Generates random candidates.
 */
public class RandomPlanGenerator implements ChoiceGenerator {

	private final int topK;
	private final TopKChoicesGenerator gen;
	private final SplittableRandom rnd = new SplittableRandom(0);

	public RandomPlanGenerator(int topK, TopKChoicesGenerator generator) {
		this.topK = topK;
		this.gen = generator;
	}

	@Override
	public List<PlanCandidate> generate(Plan plan, PlanModel planModel, @Nullable Set<String> consideredModes) {

		List<String[]> chosen = new ArrayList<>();
		chosen.add(planModel.getCurrentModes());

		// Chosen candidate from data
		PlanCandidate existing = gen.generatePredefined(planModel, chosen).get(0);

		// This changes the internal state to randomize the estimates
		// random selection is biased because of mass conservation
		// due to that, this class should not be used
		for (Map.Entry<String, List<ModeEstimate>> entry : planModel.getEstimates().entrySet()) {
			for (ModeEstimate est : entry.getValue()) {
				double[] utils = est.getLegEstimates();
				if (utils != null)
					for (int i = 0; i < utils.length; i++) {
						utils[i] = -rnd.nextDouble();
					}
			}
		}

		List<PlanCandidate> result = new ArrayList<>();
		result.add(existing);
		result.addAll(gen.generate(planModel, consideredModes, null));

		return result.stream().distinct().limit(topK).toList();
	}

}
