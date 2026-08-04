package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Cleans the source demand and reschedules plans whose activities extend past the simulation day.
 * <p>
 * Two independent passes over every plan:
 * <ol>
 *   <li><b>Strip redundant maximum durations.</b> In the source data every activity that has an end time also
 *   carries a {@code max_dur} attribute that merely duplicates {@code end - start}. That attribute is noise, and it
 *   turns the activity into a duration-based one (which the {@code TimeAllocationMutator} does not mutate). We remove
 *   {@code max_dur} from any activity that has an end time, so activities stay end-time-based and mutable. (Genuinely
 *   duration-only activities -- none exist in the source -- keep their duration.)</li>
 *   <li><b>Reschedule over-long plans.</b> The source contains plans whose activities are scheduled far past the end
 *   of the simulation day (up to ~58h), which leaves the last (home) activity with a zero or negative performing
 *   window and produces degenerate scoring. Such a plan is remapped, order-preserving, so its last activity starts
 *   before the day end; see {@link #reschedule(Plan)}.</li>
 * </ol>
 * Intended to run early in the demand pipeline (before the wrap-around split and the typical-duration encoding), on
 * the person population only.
 */
@CommandLine.Command(
	name = "reschedule-late-plans",
	description = "Strip redundant max-duration attributes and reschedule (order-preserving) plans whose activities " +
		"extend past the simulation day, so the last activity keeps a positive window before the day end."
)
public class RescheduleLatePlans implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RescheduleLatePlans.class);

	private static final double SECONDS_PER_DAY = 24 * 3600;

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--simulation-period-in-days", description = "Length of the simulation day, as a " +
		"multiple of 24h; the last activity of a rescheduled plan is pulled before this. Pass the same value the " +
		"scenario uses (config.scenario().getSimulationPeriodInDays()).", defaultValue = "1.0")
	private double simulationPeriodInDays = 1.0;

	@CommandLine.Option(names = "--reserve", description = "Seconds reserved before the day end for the final trip " +
		"and a minimal last-activity window; the last departure of a rescheduled plan targets " +
		"simulationPeriodInDays*24h - reserve.", defaultValue = "3600")
	private double reserveSeconds = 3600;

	public static void main(String[] args) {
		new RescheduleLatePlans().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

		double target = simulationPeriodInDays * SECONDS_PER_DAY - reserveSeconds;
		if (target <= 0) {
			log.error("simulationPeriodInDays*24h - reserve = {}s is not positive; nothing to schedule into.", target);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		int strippedMaxDur = 0;
		int rescheduled = 0;
		int openedLast = 0;
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				strippedMaxDur += stripRedundantMaxDur(plan);
				openedLast += openLastActivity(plan);
				if (reschedule(plan)) {
					rescheduled++;
				}
			}
		}

		log.info("Stripped {} redundant max-duration attributes; opened {} last activities; rescheduled {} over-long plans (target last departure <= {}h).",
			strippedMaxDur, openedLast, rescheduled, target / 3600.);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Remove the maximum duration from every activity that also has an end time (there it is redundant and would
	 * otherwise keep the activity from being time-mutated). Returns the number of attributes removed.
	 */
	private static int stripRedundantMaxDur(Plan plan) {
		int removed = 0;
		for (var pe : plan.getPlanElements()) {
			if (pe instanceof Activity act && act.getEndTime().isDefined() && act.getMaximumDuration().isDefined()) {
				act.setMaximumDurationUndefined();
				removed++;
			}
		}
		return removed;
	}

	/**
	 * Normalize the last activity of the day to be open: the final home activity runs to the end of the simulation
	 * day, so it must not carry a planned end time. The source occasionally gives it a (stale) end time, which both
	 * misses the reschedule (which only inspects the departures before it) and makes the scoring treat it as a middle
	 * activity instead of the overnight last activity. Returns 1 if an end time was removed, else 0.
	 */
	private static int openLastActivity(Plan plan) {
		List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		if (activities.isEmpty()) {
			return 0;
		}
		Activity last = activities.get(activities.size() - 1);
		if (last.getEndTime().isUndefined()) {
			return 0;
		}
		last.setEndTimeUndefined();
		last.setMaximumDurationUndefined();
		return 1;
	}

	/**
	 * Reschedule a plan whose last departure is after {@code simulationPeriodInDays*24h - reserve}, so it fits within
	 * the day while keeping the activity order unchanged. Each departure (the end time of every activity but the last,
	 * which is open) is remapped by a continuous warp
	 * <pre>
	 *   t'_i = (1-beta) * (T/L) * t_i   +   beta * (i+1)/N * T,     beta = 1 - T/L
	 * </pre>
	 * where {@code T} is the target for the last departure, {@code L} the original last departure and {@code N} the
	 * number of departures. The first term proportionally squeezes the plan's own structure; the second spreads the
	 * activities evenly across the day. Both are increasing and {@code <= T}, so the result is increasing and fits.
	 * A plan that already fits ({@code L <= T}) is left untouched, and {@code beta} goes to zero as {@code L} goes to
	 * {@code T}, so the change is continuous and grows with the overshoot; the even-spread term keeps activities from
	 * clustering in the late part of the day. Returns whether the plan was rescheduled.
	 */
	private boolean reschedule(Plan plan) {
		List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		if (activities.size() < 2) {
			return false;
		}

		int n = activities.size() - 1; // number of departures; the last activity is open
		double[] t = new double[n];
		for (int i = 0; i < n; i++) {
			if (activities.get(i).getEndTime().isUndefined()) {
				// A non-last activity without an end time (duration-based) cannot be placed on the timeline here;
				// leave this plan untouched rather than guess.
				log.warn("Cannot reschedule plan of a person with a non-last activity without end time; leaving it unchanged.");
				return false;
			}
			t[i] = activities.get(i).getEndTime().seconds();
			if (i > 0 && t[i] < t[i - 1]) {
				// Activities out of chronological order (a rare source-data defect); do not warp such a plan.
				log.warn("Cannot reschedule a plan whose activity end times are not increasing; leaving it unchanged.");
				return false;
			}
		}

		double target = simulationPeriodInDays * SECONDS_PER_DAY - reserveSeconds;
		double last = t[n - 1];
		if (last <= target) {
			return false; // already fits: identity
		}

		double squeeze = target / last;
		double beta = 1.0 - squeeze;
		for (int i = 0; i < n; i++) {
			double proportional = squeeze * t[i];
			double evenlySpread = ((i + 1.0) / n) * target;
			double newEnd = (1.0 - beta) * proportional + beta * evenlySpread;
			Activity act = activities.get(i);
			act.setEndTime(newEnd);
			act.setStartTimeUndefined(); // the source start time is now inconsistent with the new schedule
		}
		// the open last activity keeps no end time; drop its now-inconsistent start time as well
		activities.get(n).setStartTimeUndefined();

		return true;
	}

}
