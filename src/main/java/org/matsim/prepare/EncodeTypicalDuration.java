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

@CommandLine.Command(
	name = "encode-typical-duration",
	description = "Encode each activity's typical duration as a \"typicalDuration\" attribute on the activity."
)
public class EncodeTypicalDuration implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(EncodeTypicalDuration.class);

	private static final double SECONDS_PER_DAY = 24 * 3600;

	/**
	 * Attribute key under which the typical duration (in seconds) is stored on each activity. Named after
	 * {@link org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams#TYPICAL_DURATION}.
	 */
	public static final String TYPICAL_DURATION = "typicalDuration";

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--simulation-period-in-days", description = "Effective end of day for the last (non-wrap-around) activity, as a multiple of 24h. " +
		"Corresponds to config.scenario().getSimulationPeriodInDays(); pass the same value the scenario uses.")
	private double simulationPeriodInDays = 1.0;

	@CommandLine.Option(names = "--min-typical-duration", description = "If set (> 0), clamp each activity's typical " +
		"duration to at least this many seconds, and prolong too-short middle activities to at least this actual " +
		"duration. Very small typical durations make the Charypar-Nagel zero-utility duration underflow and the " +
		"performing utility explode; prolonging the actual duration keeps the executed activity consistent with its " +
		"(clamped) typical duration, since short duration-based activities are never time-mutated.", defaultValue = "0")
	private double minTypicalDuration = 0;

	public static void main(String[] args) {
		new EncodeTypicalDuration().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		population.getPersons().values().forEach(this::encodeTypicalDuration);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Encode the typical duration of every activity as the {@link #TYPICAL_DURATION} attribute on the activity.
	 * This replaces the previous activity-type tagging of the typical duration.
	 * <p>
	 * The first and last activity of a plan need special treatment because their start resp. end is not observed
	 * within the simulation period:
	 * <ul>
	 *   <li>If the first and last activity have the same type, they are wrap-around activities (the usual
	 *   criterion, which is switched off elsewhere by rewriting the types). Both are given the same typical
	 *   duration, namely the combined wrapped-around duration (morning part of the first activity plus the
	 *   evening part of the last activity up to midnight), as per the previous logic.</li>
	 *   <li>Otherwise the last activity is treated as an ordinary end-of-day activity: its typical duration runs
	 *   from its start to the effective end of the simulation period ({@code simulationPeriodInDays * 24h}) rather
	 *   than to a fixed 24h as the old code assumed.</li>
	 * </ul>
	 */
	void encodeTypicalDuration(Person person) {
		for (Plan plan : person.getPlans()) {

			List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			// Stay-home agents (single activity) have no observed duration; nothing to encode.
			if (activities.size() < 2) {
				continue;
			}

			double[] starts = startTimes(plan, activities);

			// Middle activities are fully observed: their typical duration is simply their own duration.
			for (int i = 1; i < activities.size() - 1; i++) {
				Activity act = activities.get(i);
				prolongToMinimumDuration(act, starts[i]);
				setTypicalDuration(act, ownDuration(act, starts[i]));
			}

			Activity first = activities.get(0);
			Activity last = activities.get(activities.size() - 1);
			double lastStart = starts[activities.size() - 1];

			if (first.getType().equals(last.getType())) {
				// wrap-around: first and last are one activity wrapping around midnight.
				double wrapped = ownDuration(first, 0) + (SECONDS_PER_DAY - lastStart);
				setTypicalDuration(first, wrapped);
				setTypicalDuration(last, wrapped);
			} else {
				// no wrap-around: the last activity runs from its start to the effective end of the simulation period.
				setTypicalDuration(first, ownDuration(first, 0));
				setTypicalDuration(last, simulationPeriodInDays * SECONDS_PER_DAY - lastStart);
			}
		}
	}

	/**
	 * Start time of every activity: the recorded start time where present, otherwise reconstructed by walking the chain
	 * (previous end plus leg travel times). Populations that never went through a simulation carry no start times at
	 * all; assuming 0 for those (as {@code getStartTime().orElse(0)} used to) silently turned every typical duration
	 * into the activity's end time.
	 */
	private static double[] startTimes(Plan plan, List<Activity> activities) {
		double[] starts = new double[activities.size()];
		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
		double clock = 0;
		for (int i = 0; i < activities.size(); i++) {
			Activity act = activities.get(i);
			if (i > 0) {
				double travel = 0;
				if (i - 1 < trips.size()) {
					for (var leg : trips.get(i - 1).getLegsOnly()) {
						travel += leg.getTravelTime().orElse(0);
					}
				}
				clock += travel;
			}
			starts[i] = act.getStartTime().orElse(clock);
			clock = starts[i];
			if (act.getEndTime().isDefined()) {
				clock = act.getEndTime().seconds();
			} else if (act.getMaximumDuration().isDefined()) {
				clock += act.getMaximumDuration().seconds();
			}
		}
		return starts;
	}

	/**
	 * Duration of an activity as observed in the plan: end time minus start time if the end time is defined,
	 * otherwise the maximum duration.
	 */
	private static double ownDuration(Activity act, double start) {
		if (act.getEndTime().isDefined()) {
			return act.getEndTime().seconds() - start;
		} else {
			return act.getMaximumDuration().seconds();
		}
	}

	private void setTypicalDuration(Activity act, double typicalDuration) {
		if (minTypicalDuration > 0) {
			typicalDuration = Math.max(typicalDuration, minTypicalDuration);
		}
		act.getAttributes().putAttribute(TYPICAL_DURATION, typicalDuration);
	}

	/**
	 * When a minimum typical duration is configured, prolong a too-short (middle) activity so its actual, executed
	 * duration is at least the minimum, encoded as a maximum duration (the end time is removed). Clamping only the
	 * typical duration is not enough: {@link EndTimeToDuration} turns short activities into duration-based ones, which
	 * the TimeAllocationMutator does not mutate, so a short activity would otherwise stay short forever -- performed
	 * far below its (clamped) typical duration, which is exactly the degenerate scoring input we want to avoid.
	 */
	private void prolongToMinimumDuration(Activity act, double start) {
		if (minTypicalDuration > 0 && ownDuration(act, start) < minTypicalDuration) {
			act.setEndTimeUndefined();
			act.setMaximumDuration(minTypicalDuration);
		}
	}

}
