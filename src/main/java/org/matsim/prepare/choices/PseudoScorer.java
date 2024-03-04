package org.matsim.prepare.choices;

import com.google.inject.Injector;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * Replicates the event sequence of a trip to perform scoring without simulation.
 */
final class PseudoScorer {

	private final EventsManager eventsManager;
	private final ScoringFunctionsForPopulation scoring;

	private final TimeInterpretation timeInterpretation;

	/**
	 * Buffer resulting score outputs.
	 */
	private final StringBuilder result = new StringBuilder();

	PseudoScorer(Injector injector, Population population) {
		this.eventsManager = EventsUtils.createEventsManager();
		this.scoring = new ScoringFunctionsForPopulation(
			eventsManager,
			new EventsToActivities(),
			new EventsToLegs(injector.getInstance(Scenario.class)),
			population,
			injector.getInstance(ScoringFunctionFactory.class)
		);
		this.timeInterpretation = TimeInterpretation.create(injector.getInstance(Config.class));
	}

	public Object2DoubleMap<String> score(Plan plan) {

		scoring.reset(0);
		scoring.init(plan.getPerson());

		createEvents(plan);

		scoring.finishScoringFunctions();

		ScoringFunction scoring = this.scoring.getScoringFunctionForAgent(plan.getPerson().getId());

		result.setLength(0);
		scoring.explainScore(result);

		Object2DoubleOpenHashMap<String> scores = new Object2DoubleOpenHashMap<>();

		String[] split = result.toString().split(ScoringFunction.SCORE_DELIMITER);
		for (String s : split) {
			String[] kv = s.split("=");
			if (kv.length > 1) {
				scores.put(kv[0], Double.parseDouble(kv[1]));
			}
		}

		scores.put("score", scoring.getScore());

		return scores;
	}

	private void createEvents(Plan plan) {

		TimeTracker tt = new TimeTracker(timeInterpretation);

		Activity currentAct = null;
		for (PlanElement el : plan.getPlanElements()) {

			if (el instanceof Activity act) {

				// The first activity does not have a start event
				if (currentAct != null) {
					eventsManager.processEvent(new ActivityStartEvent(tt.getTime().seconds(),
						plan.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType(), act.getCoord()
					));
				}

				tt.addElement(act);

				// The last activity does not have an end time
				if (tt.getTime().isDefined()) {
					eventsManager.processEvent(new ActivityEndEvent(tt.getTime().seconds(),
						plan.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType(), act.getCoord()
					));
				}

				currentAct = act;
			} else
				tt.addElement(el);

		}
	}
}
