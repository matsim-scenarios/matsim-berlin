package org.matsim.analysis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.charts.XYLineChart;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * In-loop point elasticity estimate, computed analytically from the agents' plan memories
 * every iteration ("route 1"): each agent's plan choice is treated as a scale-1 logit over
 * the scored plans in memory (exact under ChangeExpBeta; a diagnostic under BestScore).
 * The derivative of expected car/pt trips w.r.t. a multiplicative cost factor (at factor 1)
 * is then closed-form: per agent the covariance, under the plan-choice distribution, between
 * trips of the mode and the plan's money exposure to that factor. Summed over agents and
 * divided by expected trips this yields a point elasticity of the behavioral stage at the
 * current relaxation state (open loop: network conditions held fixed).
 *
 * Money exposures mirror the --car-cost-factor / --pt-cost-factor semantics:
 * car = fuel (car and ride legs) + car daily cost; pt = pt daily cost.
 *
 * Output: elasticity_stats.csv and an every-iteration elasticityEstimate.png in the
 * output directory, in the spirit of the core score statistics chart.
 */
public class PointElasticityStatsModule extends AbstractModule {

	@Override
	public void install() {
		addControlerListenerBinding().to(PointElasticityStatsListener.class).in(Singleton.class);
	}

	static class PointElasticityStatsListener implements IterationEndsListener {

		private static final Logger log = LogManager.getLogger(PointElasticityStatsListener.class);

		private final Population population;
		private final ScoringParametersForPerson scoringParams;

		private final List<Integer> iterations = new ArrayList<>();
		private final List<Double> eCar = new ArrayList<>();
		private final List<Double> ePt = new ArrayList<>();
		private final List<Double> eBikeSpeed = new ArrayList<>();
		private final List<Double> shareCar = new ArrayList<>();
		private final List<Double> sharePt = new ArrayList<>();
		private final List<Double> shareBike = new ArrayList<>();

		@Inject
		PointElasticityStatsListener(Population population, ScoringParametersForPerson scoringParams) {
			this.population = population;
			this.scoringParams = scoringParams;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {

			double totalTrips = 0;
			double carTrips = 0;
			double ptTrips = 0;
			double bikeTrips = 0;
			double carDeriv = 0;
			double ptDeriv = 0;
			double bikeSpeedDeriv = 0;

			for (Person person : population.getPersons().values()) {
				if (!"person".equals(PopulationUtils.getSubpopulation(person)))
					continue;

				ScoringParameters params;
				try {
					params = scoringParams.getScoringParameters(person);
				} catch (RuntimeException e) {
					continue;
				}
				double mUoM = params.marginalUtilityOfMoney;
				double mdrCar = monetaryDistanceRate(params, TransportMode.car);
				double mdrRide = monetaryDistanceRate(params, TransportMode.ride);
				double dmcCar = dailyMoneyConstant(params, TransportMode.car);
				double dmcPt = dailyMoneyConstant(params, TransportMode.pt);
				// effective bike time cost per second: mode mUTT minus foregone performing
				double bikeTimeCoeff = travelTimeCoeff(params, TransportMode.bike) - params.marginalUtilityOfPerforming_s;

				List<double[]> plans = new ArrayList<>(); // score, nCar, nPt, xCar, xPt
				for (Plan plan : person.getPlans()) {
					if (plan.getScore() == null || plan.getScore().isNaN())
						continue;

					int nCar = 0;
					int nPt = 0;
					int nBike = 0;
					int nAll = 0;
					double carMeters = 0;
					double rideMeters = 0;
					double bikeSeconds = 0;
					boolean carUsed = false;
					boolean ptUsed = false;

					for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
						nAll++;
						boolean hasCar = false;
						boolean hasPt = false;
						boolean hasBike = false;
						for (Leg leg : trip.getLegsOnly()) {
							switch (leg.getMode()) {
								case TransportMode.car -> {
									hasCar = true;
									carMeters += dist(leg);
								}
								case TransportMode.ride -> rideMeters += dist(leg);
								case TransportMode.pt -> hasPt = true;
								case TransportMode.bike -> {
									hasBike = true;
									bikeSeconds += travelSeconds(leg);
								}
								default -> { }
							}
						}
						if (hasCar) nCar++;
						if (hasPt) nPt++;
						if (hasBike) nBike++;
						carUsed |= hasCar;
						ptUsed |= hasPt;
					}

					// utility exposure to the respective cost factor (utils, typically negative)
					double xCar = mUoM * (mdrCar * carMeters + mdrRide * rideMeters + (carUsed ? dmcCar : 0));
					double xPt = mUoM * (ptUsed ? dmcPt : 0);
					// utility exposure to a bike SPEED factor f (hours scale 1/f): dU/df at f=1
					double xBikeSpeed = -bikeTimeCoeff * bikeSeconds;

					plans.add(new double[]{plan.getScore(), nCar, nPt, xCar, xPt, nAll, nBike, xBikeSpeed});
				}

				if (plans.isEmpty())
					continue;

				// scale-1 logit over the scored plan memory (numerically stabilized)
				double max = plans.stream().mapToDouble(p -> p[0]).max().orElse(0);
				double denom = plans.stream().mapToDouble(p -> Math.exp(p[0] - max)).sum();

				double expCar = 0;
				double expPt = 0;
				double expBike = 0;
				double expXCar = 0;
				double expXPt = 0;
				double expXBike = 0;
				double expCarXCar = 0;
				double expPtXPt = 0;
				double expBikeXBike = 0;
				double nTrips = 0;
				for (double[] p : plans) {
					double prob = Math.exp(p[0] - max) / denom;
					expCar += prob * p[1];
					expPt += prob * p[2];
					expBike += prob * p[6];
					expXCar += prob * p[3];
					expXPt += prob * p[4];
					expXBike += prob * p[7];
					expCarXCar += prob * p[1] * p[3];
					expPtXPt += prob * p[2] * p[4];
					expBikeXBike += prob * p[6] * p[7];
					nTrips += prob * p[5];
				}

				carTrips += expCar;
				ptTrips += expPt;
				bikeTrips += expBike;
				totalTrips += nTrips;
				// d E[trips_m] / d factor = cov(trips_m, exposure_m) under the plan-choice distribution
				carDeriv += expCarXCar - expCar * expXCar;
				ptDeriv += expPtXPt - expPt * expXPt;
				bikeSpeedDeriv += expBikeXBike - expBike * expXBike;
			}

			double elCar = carTrips > 0 ? carDeriv / carTrips : Double.NaN;
			double elPt = ptTrips > 0 ? ptDeriv / ptTrips : Double.NaN;
			double elBikeSpeed = bikeTrips > 0 ? bikeSpeedDeriv / bikeTrips : Double.NaN;

			iterations.add(event.getIteration());
			eCar.add(elCar);
			ePt.add(elPt);
			eBikeSpeed.add(elBikeSpeed);
			shareCar.add(totalTrips > 0 ? carTrips / totalTrips : Double.NaN);
			sharePt.add(totalTrips > 0 ? ptTrips / totalTrips : Double.NaN);
			shareBike.add(totalTrips > 0 ? bikeTrips / totalTrips : Double.NaN);

			log.info("In-loop point elasticity estimate: car {} pt {} bike(speed) {}", elCar, elPt, elBikeSpeed);

			writeCsv(event);
			writePng(event);
		}

		private void writeCsv(IterationEndsEvent event) {
			Path out = Path.of(event.getServices().getControllerIO().getOutputFilename("elasticity_stats.csv"));
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
				writer.println("iteration,elasticity_car,elasticity_pt,elasticity_bike_speed,share_car,share_pt,share_bike");
				for (int i = 0; i < iterations.size(); i++) {
					writer.printf("%d,%f,%f,%f,%f,%f,%f%n", iterations.get(i), eCar.get(i), ePt.get(i), eBikeSpeed.get(i),
						shareCar.get(i), sharePt.get(i), shareBike.get(i));
				}
			} catch (IOException e) {
				log.warn("Could not write elasticity stats", e);
			}
		}

		private void writePng(IterationEndsEvent event) {
			if (iterations.size() < 2)
				return;
			XYLineChart chart = new XYLineChart("In-loop point elasticity estimate (open loop)", "iteration",
				"elasticity (car/pt wrt cost factor, bike wrt speed factor)");
			chart.addSeries("car (cost)", toArray(iterations), toArray(eCar, iterations.size()));
			chart.addSeries("pt (cost)", toArray(iterations), toArray(ePt, iterations.size()));
			chart.addSeries("bike (speed)", toArray(iterations), toArray(eBikeSpeed, iterations.size()));
			chart.saveAsPng(event.getServices().getControllerIO().getOutputFilename("elasticityEstimate.png"), 800, 600);
		}

		/**
		 * Travel time of a leg as the plan knows it. The leg's own travel time is not reliable for this:
		 * with plans.removingUnnecessaryPlanAttributes (the VSP default, set in all run configs)
		 * {@link org.matsim.core.population.VspPlansCleaner} clears it on every leg before each mobsim, so
		 * only plans that have not been executed yet still carry it. The route keeps its travel time.
		 */
		private static double travelSeconds(Leg leg) {
			if (leg.getTravelTime().isDefined())
				return leg.getTravelTime().seconds();
			if (leg.getRoute() != null && leg.getRoute().getTravelTime().isDefined())
				return leg.getRoute().getTravelTime().seconds();
			return 0;
		}

		private static double dist(Leg leg) {
			if (leg.getRoute() == null || Double.isNaN(leg.getRoute().getDistance()))
				return 0;
			return leg.getRoute().getDistance();
		}

		private static double travelTimeCoeff(ScoringParameters params, String mode) {
			ModeUtilityParameters p = params.modeParams.get(mode);
			return p == null ? 0 : p.marginalUtilityOfTraveling_s;
		}

		private static double monetaryDistanceRate(ScoringParameters params, String mode) {
			ModeUtilityParameters p = params.modeParams.get(mode);
			return p == null ? 0 : p.monetaryDistanceCostRate;
		}

		private static double dailyMoneyConstant(ScoringParameters params, String mode) {
			ModeUtilityParameters p = params.modeParams.get(mode);
			return p == null ? 0 : p.dailyMoneyConstant;
		}

		private static double[] toArray(List<Integer> values) {
			return values.stream().mapToDouble(Integer::doubleValue).toArray();
		}

		private static double[] toArray(List<Double> values, int n) {
			double[] result = new double[n];
			for (int i = 0; i < n; i++) {
				Double v = values.get(i);
				result[i] = v == null || v.isNaN() ? 0 : v;
			}
			return result;
		}
	}
}
