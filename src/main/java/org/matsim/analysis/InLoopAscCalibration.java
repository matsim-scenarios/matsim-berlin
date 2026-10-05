package org.matsim.analysis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.charts.XYLineChart;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-loop ASC calibration: the in-run sibling of the outer-loop python ASCCalibrator
 * (matsim-python-tools), sharing scaffolding with {@link PointElasticityStatsModule}.
 *
 * Every iteration, expected mode shares are computed analytically from the agents' plan
 * memories (scale-1 logit over scored plans, as in the elasticity monitor), together with
 * the local sensitivity d ln(share) / d asc from the same covariance machinery. Per-mode
 * additive constants ("offsets", applied per trip through a scoring component) are then
 * updated with a Newton-conditioned version of the classic logit update
 * (ln target - ln share, relative to the fixed mode walk).
 *
 * The calibration deliberately ignores the annealing schedule and iteration budget: it
 * decides convergence itself (share errors within tolerance over a window of iterations)
 * and then COMMITS -- offsets are frozen for the rest of the run, so the run's tail
 * relaxes under the final constants. Post-commit drift beyond tolerance is logged loudly.
 *
 * Shares are measured over persons with ids starting with "berlin" (mirroring the python
 * calibration's person filter); the offsets apply to everyone's scoring, as in the outer
 * loop. Known approximation, shared with Cadyts-style in-loop schemes: plans in memory
 * carry the offsets of the iteration they were scored in; the vintage mixing vanishes as
 * updates decay and is second-order for the gains used here. Best paired with the
 * ChangeExpBeta selector, which re-executes (and thereby re-scores) memory plans.
 *
 * Outputs: asc_calibration_stats.csv, ascOffsets.png, ascShareErrors.png (updated every
 * iteration), asc_offsets_final.txt on commit (config-ready constants).
 */
public final class InLoopAscCalibration extends AbstractModule {

	/**
	 * Modes in main-mode hierarchy order (ascending priority, mirroring
	 * DefaultAnalysisMainModeIdentifier); walk is the fixed reference mode.
	 */
	static final List<String> MODES = List.of("walk", "bike", "ride", "car", "pt");
	private static final int FIXED_MODE = 0; // walk

	/** SrV 2018 mode share targets for persons living in Berlin (as in calibrate.py). */
	private static final Map<String, Double> DEFAULT_TARGETS = Map.of(
		"walk", 0.296769, "bike", 0.177878, "pt", 0.265073, "car", 0.200673, "ride", 0.059607);

	private static final double GAIN = 0.3;
	private static final double MAX_STEP = 0.2;
	private static final double MIN_SENSITIVITY = 0.25;
	private static final double TOLERANCE = 0.005; // absolute share points
	// commit additionally requires the offsets themselves to be stationary: largest applied
	// step below this, over the whole window (guards against committing while the calibrator
	// is still actively cancelling relaxation drift, e.g. after a calibrated warm start)
	private static final double OFFSET_STATIONARY = 0.02;
	// third, independent commit witness: fraction of plan objects in the counted persons'
	// memories that are new since the previous iteration. Error and step quietness are
	// correlated (both read the current drift rate) and can go quiet together during a
	// mid-annealing lull; churn observes the forcing itself. Measured, not read from the
	// annealing config, so the calibration stays schedule-agnostic.
	private static final double CHURN_TOLERANCE = 0.02;
	private static final int WINDOW = 25;
	private static final int MIN_ITERATIONS = 30;
	// commits are provisional: if the share error exceeds 2x tolerance for this many
	// consecutive iterations after a commit, the calibrator re-arms and resumes updating.
	// The commit that survives to the end of the run is the one that counts.
	private static final int REARM_WINDOW = 5;

	private final boolean active;

	public InLoopAscCalibration(boolean active) {
		this.active = active;
	}

	@Override
	public void install() {
		bind(Offsets.class).in(Singleton.class);
		if (active) {
			addControlerListenerBinding().to(Listener.class).in(Singleton.class);
		}
	}

	static int mainModeIndex(List<Leg> legs) {
		int idx = 0;
		for (Leg leg : legs) {
			int i = MODES.indexOf(leg.getMode());
			if (i > idx)
				idx = i;
		}
		return idx;
	}

	/**
	 * Mutable per-mode score offsets. Bound always (inert zeros unless the calibration
	 * listener is active), so the scoring factories can depend on it unconditionally.
	 */
	public static final class Offsets {
		private volatile double[] values = new double[MODES.size()];

		public double get(int modeIndex) {
			return values[modeIndex];
		}

		double[] snapshot() {
			return values.clone();
		}

		void update(double[] newValues) {
			this.values = newValues;
		}
	}

	/**
	 * Scoring component applying the current offsets, once per trip by main mode.
	 * Uses the trip-scoring granularity so it is consistent with how constants are
	 * applied and how shares are counted.
	 */
	public static final class OffsetTripScoring implements SumScoringFunction.TripScoring {

		private final Offsets offsets;
		private double score = 0;

		public OffsetTripScoring(Offsets offsets) {
			this.offsets = offsets;
		}

		@Override
		public void handleTrip(TripStructureUtils.Trip trip) {
			score += offsets.get(mainModeIndex(trip.getLegsOnly()));
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void explainScore(StringBuilder out) {
			out.append("ascCalibrationOffset_util=").append(score);
		}
	}

	static final class Listener implements IterationEndsListener {

		private static final Logger log = LogManager.getLogger(InLoopAscCalibration.class);

		private final Population population;
		private final Offsets offsets;
		private final Config config;

		private final List<Integer> iterations = new ArrayList<>();
		private final List<double[]> offsetHistory = new ArrayList<>();
		private final List<double[]> shareErrorHistory = new ArrayList<>();
		private final List<Double> churnHistory = new ArrayList<>();

		private int withinToleranceStreak = 0;
		private boolean committed = false;
		private int committedAt = -1;
		private int overToleranceStreak = 0;
		private int rearmCount = 0;
		private final List<Boolean> committedHistory = new ArrayList<>();
		private java.util.Set<Plan> previousPlans =
			java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

		@Inject
		Listener(Population population, Offsets offsets, Config config) {
			this.population = population;
			this.offsets = offsets;
			this.config = config;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {

			int n = MODES.size();
			double[] expTrips = new double[n];
			// diagonal and total-covariance accumulators for d E[trips_m] / d asc_m
			double[] covMm = new double[n];
			double[] covTotM = new double[n];

			java.util.Set<Plan> currentPlans =
				java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			long newPlans = 0;

			for (Person person : population.getPersons().values()) {
				if (!"person".equals(PopulationUtils.getSubpopulation(person)))
					continue;
				if (!person.getId().toString().startsWith("berlin"))
					continue;

				for (Plan plan : person.getPlans()) {
					currentPlans.add(plan);
					if (!previousPlans.contains(plan))
						newPlans++;
				}

				List<double[]> plans = new ArrayList<>(); // score, n_0..n_{k-1}, n_total
				for (Plan plan : person.getPlans()) {
					if (plan.getScore() == null || plan.getScore().isNaN())
						continue;
					double[] row = new double[n + 2];
					row[0] = plan.getScore();
					for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
						row[1 + mainModeIndex(trip.getLegsOnly())]++;
						row[n + 1]++;
					}
					plans.add(row);
				}
				if (plans.isEmpty())
					continue;

				double max = plans.stream().mapToDouble(p -> p[0]).max().orElse(0);
				double denom = plans.stream().mapToDouble(p -> Math.exp(p[0] - max)).sum();

				double[] eM = new double[n];
				double[] eMm = new double[n];
				double[] eTotM = new double[n];
				double eTot = 0;
				for (double[] p : plans) {
					double prob = Math.exp(p[0] - max) / denom;
					eTot += prob * p[n + 1];
					for (int m = 0; m < n; m++) {
						eM[m] += prob * p[1 + m];
						eMm[m] += prob * p[1 + m] * p[1 + m];
						eTotM[m] += prob * p[n + 1] * p[1 + m];
					}
				}
				for (int m = 0; m < n; m++) {
					expTrips[m] += eM[m];
					covMm[m] += eMm[m] - eM[m] * eM[m];
					covTotM[m] += eTotM[m] - eTot * eM[m];
				}
			}

			double churn = currentPlans.isEmpty() ? 1.0 : (double) newPlans / currentPlans.size();
			boolean firstIteration = previousPlans.isEmpty();
			previousPlans = currentPlans;

			double total = 0;
			for (double t : expTrips)
				total += t;
			if (total <= 0)
				return;

			double[] shares = new double[n];
			double[] shareError = new double[n];
			double maxAbsError = 0;
			for (int m = 0; m < n; m++) {
				shares[m] = expTrips[m] / total;
				shareError[m] = shares[m] - DEFAULT_TARGETS.get(MODES.get(m));
				maxAbsError = Math.max(maxAbsError, Math.abs(shareError[m]));
			}

			// Newton-conditioned logit update, unless committed
			if (!committed) {
				double refError = Math.log(DEFAULT_TARGETS.get(MODES.get(FIXED_MODE))) - Math.log(shares[FIXED_MODE]);
				double[] next = offsets.snapshot();
				double maxAbsStep = 0;
				for (int m = 0; m < n; m++) {
					if (m == FIXED_MODE)
						continue;
					double err = Math.log(DEFAULT_TARGETS.get(MODES.get(m))) - Math.log(shares[m]) - refError;
					// d ln share_m / d asc_m from the measured covariances
					double sensitivity = covMm[m] / Math.max(expTrips[m], 1e-9) - covTotM[m] / total;
					sensitivity = Math.max(sensitivity, MIN_SENSITIVITY);
					double step = GAIN * err / sensitivity;
					step = Math.max(-MAX_STEP, Math.min(MAX_STEP, step));
					next[m] += step;
					maxAbsStep = Math.max(maxAbsStep, Math.abs(step));
				}
				offsets.update(next);

				boolean settled = maxAbsError < TOLERANCE && maxAbsStep < OFFSET_STATIONARY
					&& !firstIteration && churn < CHURN_TOLERANCE;
				withinToleranceStreak = settled ? withinToleranceStreak + 1 : 0;
				if (withinToleranceStreak >= WINDOW && event.getIteration() >= MIN_ITERATIONS) {
					committed = true;
					committedAt = event.getIteration();
					overToleranceStreak = 0;
					log.info("In-loop ASC calibration COMMITTED at iteration {} (churn {}): offsets frozen.",
						committedAt, churn);
					writeFinal(event);
				}
			} else {
				overToleranceStreak = maxAbsError > 2 * TOLERANCE ? overToleranceStreak + 1 : 0;
				if (overToleranceStreak >= REARM_WINDOW) {
					committed = false;
					rearmCount++;
					withinToleranceStreak = 0;
					overToleranceStreak = 0;
					log.warn("In-loop ASC calibration RE-ARMED at iteration {} (re-arm #{}): share error escaped "
						+ "after commit at {}; resuming updates.", event.getIteration(), rearmCount, committedAt);
				}
			}

			iterations.add(event.getIteration());
			offsetHistory.add(offsets.snapshot());
			shareErrorHistory.add(shareError);
			committedHistory.add(committed);
			churnHistory.add(churn);

			writeCsv(event);
			writePngs(event);
		}

		private void writeCsv(IterationEndsEvent event) {
			Path out = Path.of(event.getServices().getControllerIO().getOutputFilename("asc_calibration_stats.csv"));
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
				writer.print("iteration,committed,churn");
				for (String m : MODES)
					writer.printf(",offset_%s", m);
				for (String m : MODES)
					writer.printf(",share_error_%s", m);
				writer.println();
				for (int i = 0; i < iterations.size(); i++) {
					writer.printf("%d,%b,%f", iterations.get(i), committedHistory.get(i), churnHistory.get(i));
					for (double v : offsetHistory.get(i))
						writer.printf(",%f", v);
					for (double v : shareErrorHistory.get(i))
						writer.printf(",%f", v);
					writer.println();
				}
			} catch (IOException e) {
				log.warn("Could not write asc calibration stats", e);
			}
		}

		private void writePngs(IterationEndsEvent event) {
			if (iterations.size() < 2)
				return;
			double[] x = iterations.stream().mapToDouble(Integer::doubleValue).toArray();

			XYLineChart offsetsChart = new XYLineChart("In-loop ASC calibration: offsets"
				+ (committed ? " (committed at " + committedAt + ")" : ""), "iteration", "offset [utils]");
			XYLineChart errorChart = new XYLineChart("In-loop ASC calibration: share errors", "iteration", "share - target");
			for (int m = 0; m < MODES.size(); m++) {
				final int mm = m;
				offsetsChart.addSeries(MODES.get(m), x, offsetHistory.stream().mapToDouble(o -> o[mm]).toArray());
				errorChart.addSeries(MODES.get(m), x, shareErrorHistory.stream().mapToDouble(o -> o[mm]).toArray());
			}
			offsetsChart.saveAsPng(event.getServices().getControllerIO().getOutputFilename("ascOffsets.png"), 800, 600);
			errorChart.saveAsPng(event.getServices().getControllerIO().getOutputFilename("ascShareErrors.png"), 800, 600);
		}

		private void writeFinal(IterationEndsEvent event) {
			Path out = Path.of(event.getServices().getControllerIO().getOutputFilename("asc_offsets_final.txt"));
			double[] finalOffsets = offsets.snapshot();
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
				writer.printf("# In-loop ASC calibration, committed at iteration %d%n", committedAt);
				writer.println("# mode, offset, config constant (current) + offset = suggested constant");
				for (int m = 0; m < MODES.size(); m++) {
					double base = config.scoring().getOrCreateModeParams(MODES.get(m)).getConstant();
					writer.printf("%s, %f, %f%n", MODES.get(m), finalOffsets[m], base + finalOffsets[m]);
				}
			} catch (IOException e) {
				log.warn("Could not write final asc offsets", e);
			}
		}
	}
}
