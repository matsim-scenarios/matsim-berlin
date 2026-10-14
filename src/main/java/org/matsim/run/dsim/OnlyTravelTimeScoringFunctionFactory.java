package org.matsim.run.dsim;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

import java.util.List;

/**
 * Scores a plan by the negative elapsed travel time of its trips in seconds, from the departure of the first leg to the arrival of the last
 * leg of each trip, including waiting between the legs. Activities, money, stuck agents and score events are not scored.
 * <p>
 * This is the same as {@code OnlyTravelTimeDependentScoring} of the parallel QSim in rust, which is used by {@code berlin_iterations_v7} in
 * parallel-qsim-berlin. Trips with a leg without departure or travel time are skipped, the rust implementation fails on them.
 */
public final class OnlyTravelTimeScoringFunctionFactory implements ScoringFunctionFactory {

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new TravelTimeTripScoring());
		return sumScoringFunction;
	}

	private static final class TravelTimeTripScoring implements SumScoringFunction.TripScoring {

		private double score;

		@Override
		public void handleTrip(TripStructureUtils.Trip trip) {
			List<Leg> legs = trip.getLegsOnly();
			if (legs.isEmpty())
				return;

			Leg first = legs.getFirst();
			Leg last = legs.getLast();
			if (first.getDepartureTime().isUndefined() || last.getDepartureTime().isUndefined() || last.getTravelTime().isUndefined())
				return;

			double arrival = last.getDepartureTime().seconds() + last.getTravelTime().seconds();
			score -= arrival - first.getDepartureTime().seconds();
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}
	}
}
