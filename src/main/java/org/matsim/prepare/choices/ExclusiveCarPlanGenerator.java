package org.matsim.prepare.choices;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.PersonUtils;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generate two plans only, one with car and one without.
 * Also considers car availability and the current plan.
 */
public class ExclusiveCarPlanGenerator implements CandidateGenerator {

	private final TopKChoicesGenerator generator;

	public ExclusiveCarPlanGenerator(TopKChoicesGenerator generator) {
		this.generator = generator;
	}

	@Override
	public List<PlanCandidate> generate(PlanModel planModel, @Nullable Set<String> consideredModes, @Nullable boolean[] mask) {

		List<String[]> chosen = new ArrayList<>();
		chosen.add(planModel.getCurrentModes());

		// Chosen candidate from data
		PlanCandidate existing = generator.generatePredefined(planModel, chosen).get(0);

		List<PlanCandidate> candidates = new ArrayList<>();
		candidates.add(existing);

		Set<String> modes = new HashSet<>(consideredModes);
		modes.removeAll(Set.of(TransportMode.car, TransportMode.ride));

		boolean carUser = PersonUtils.canUseCar(planModel.getPerson());
		String carMode = carUser ? TransportMode.car : TransportMode.ride;

		PlanCandidate alternative;
		if (ArrayUtils.contains(planModel.getCurrentModes(), carMode)) {
			List<PlanCandidate> choices = generator.generate(planModel, modes, mask);

			if (choices.isEmpty())
				return null;

			alternative = choices.get(0);

		} else {
			modes.add(carMode);
			List<PlanCandidate> choices = generator.generate(planModel, modes, mask);

			if (choices.isEmpty())
				return null;

			alternative = choices.get(0);

			// The generated plan might not contain the car mode, remove other modes that might be better
			if (!ArrayUtils.contains(alternative.getModes(), carMode)) {
				modes.remove(TransportMode.pt);
				modes.remove(TransportMode.bike);

				choices = generator.generate(planModel, modes, mask);
				if (choices.isEmpty())
					return null;

				alternative = choices.get(0);
			}
		}

		candidates.add(alternative);

		return candidates.stream().distinct().limit(2).toList();
	}
}
