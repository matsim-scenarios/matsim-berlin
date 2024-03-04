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
 * Makes some assumptions about what modes should be included, or left out.
 * See defined types for the mixture of available modes.
 */
public class DiversePlanCandidateGenerator implements CandidateGenerator {

	private final int topK;
	private final TopKChoicesGenerator gen;

	private List<Set<String>> carTypes = List.of(
		Set.of("car", "walk"),
		Set.of("car", "bike", "walk", "pt"),
		Set.of("ride", "bike", "walk"),
		Set.of("walk", "bike"),
		Set.of("walk", "pt")
	);

	private List<Set<String>> rideTypes = List.of(
		Set.of("ride", "walk"),
		Set.of("ride", "bike", "walk", "pt"),
		Set.of("walk", "bike"),
		Set.of("walk", "pt")
	);

	DiversePlanCandidateGenerator(int topK, TopKChoicesGenerator generator) {
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

		List<Set<String>> types = carUser ? carTypes : rideTypes;

		for (Set<String> modes : types) {
			List<PlanCandidate> tmp = gen.generate(planModel, modes, mask);

			// Use two candidates from every option
			if (tmp.size() <= 3)
				candidates.addAll(tmp);
			else
				candidates.addAll(tmp.subList(0, 3));
		}

		Collections.sort(candidates);
		candidates.add(0, existing);

		return candidates.stream().distinct().limit(topK).toList();
	}
}
