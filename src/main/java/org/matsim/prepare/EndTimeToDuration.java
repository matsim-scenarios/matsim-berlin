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
	name = "end-time-to-duration",
	description = "For short activities, remove the end time and encode the span as a maximum duration instead."
)
public class EndTimeToDuration implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(EndTimeToDuration.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = {"--end-time-to-duration"}, description = "If set (> 0), remove the end time and encode " +
		"it as a maximum duration for activities shorter than this value (seconds). Default 0 does nothing.", defaultValue = "0")
	private int endTimeToDuration = 0;

	public static void main(String[] args) {
		new EndTimeToDuration().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		population.getPersons().values().forEach(this::encodeShortActivitiesAsDuration);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Copied from {@code SplitActivityTypesDuration}'s {@code --end-time-to-duration} handling, kept as its own
	 * single-purpose step. Activities shorter than {@link #endTimeToDuration} are forced to have their initial
	 * duration, i.e. their end time is removed and encoded as a maximum duration.
	 * <p>
	 * !! --> One also needs to set "mutateDuration=false" for TimeAllocationMutator. <-- !!
	 */
	private void encodeShortActivitiesAsDuration(Person person) {
		for (Plan plan : person.getPlans()) {

			List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			for (Activity act : activities) {

				double duration;
				if (act.getMaximumDuration().isDefined()) {
					duration = act.getMaximumDuration().seconds();
				} else {
					duration = act.getEndTime().orElse(0) - act.getStartTime().orElse(0);
				}

				if (endTimeToDuration > 0 && duration <= endTimeToDuration && act.getEndTime().isDefined()) {
					act.setEndTimeUndefined();
					act.setMaximumDuration(duration);
				}
			}
		}
	}

}
