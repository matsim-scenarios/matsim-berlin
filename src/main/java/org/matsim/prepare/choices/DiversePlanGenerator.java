package org.matsim.prepare.choices;

import org.jetbrains.annotations.Nullable;
import org.matsim.core.population.PersonUtils;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.*;

/**
 * Generator to create candidates with different modes.
 */
public class DiversePlanGenerator implements CandidateGenerator {

	private final int topK;
	private final TopKChoicesGenerator gen;

	DiversePlanGenerator(int topK, TopKChoicesGenerator generator) {
		this.topK = topK;
		this.gen = generator;
	}

	@Override
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask) {

		List<String[]> chosen = new ArrayList<>();
		chosen.add(planModel.getCurrentModes());

		// Chosen candidate from data
		PlanCandidate existing = gen.generatePredefined(planModel, chosen).get(0);

		List<PlanCandidate> candidates = new ArrayList<>();
		boolean carUser = PersonUtils.canUseCar(planModel.getPerson());

		HashSet<String> modes = new HashSet<>(consideredModes);
		modes.remove(carUser ? "ride": "car");


		for (String mode : modes) {
			List<PlanCandidate> tmp = gen.generate(planModel, Set.of(mode), mask);
			if (!tmp.isEmpty())
				candidates.add(tmp.get(0));
		}

		Collections.sort(candidates);
		candidates.add(0, existing);

		return candidates.stream().distinct().limit(topK).toList();
	}
}
