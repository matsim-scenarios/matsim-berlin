package org.matsim.prepare.choices;

import org.jetbrains.annotations.Nullable;
import org.matsim.core.population.PersonUtils;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generator to create candidates with different modes.
 * Makes some assumptions about what modes should be included, or left out.
 * See defined types for the mixture of available modes.
 */
public class DiversePlanCandidateGenerator implements CandidateGenerator {

	private final int topK;
	private final TopKChoicesGenerator gen;

	private List<Set<String>> carTypes = List.of(
		Set.of("car", "bike", "walk", "pt"),
		Set.of("bike", "walk", "pt"),
		Set.of("walk", "bike"),
		Set.of("walk", "pt")
	);

	private List<Set<String>> rideTypes = List.of(
		Set.of("ride", "bike", "walk", "pt"),
		Set.of("bike", "walk", "pt"),
		Set.of("walk", "bike"),
		Set.of("walk", "pt")
	);

	DiversePlanCandidateGenerator(int topK, TopKChoicesGenerator generator) {
		this.topK = topK;
		this.gen = generator;
	}

	@Override
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask) {

		if (mask != null) {
			throw new UnsupportedOperationException("Masking is not supported for this generator");
		}

		List<String[]> chosen = new ArrayList<>();
		chosen.add(planModel.getCurrentModes());

		// Chosen candidate from data
		PlanCandidate existing = gen.generatePredefined(planModel, chosen).get(0);

		Set<PlanCandidate> candidates = new LinkedHashSet<>();
		candidates.add(existing);

		boolean carUser = PersonUtils.canUseCar(planModel.getPerson());

		List<Set<String>> types = carUser ? carTypes : rideTypes;

		for (Set<String> modes : types) {
			List<PlanCandidate> tmp = gen.generate(planModel, modes, null);

			// Use two candidates from every option
			if (tmp.size() <= 2)
				candidates.addAll(tmp);
			else
				candidates.addAll(tmp.subList(0, 2));
		}

		return candidates.stream().limit(topK).toList();
	}
}
