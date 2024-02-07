package org.matsim.run.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class DetailedPersonScoringParameters implements ScoringParametersForPerson {

	/**
	 * Cache instances of {@link ActivityUtilityParameters} for each subpopulation.
	 */
	private final Map<String, Map<String, ActivityUtilityParameters>> activityParamsPerSubpopulation = new ConcurrentHashMap<>();

	/**
	 * Cache instances of {@link ModeUtilityParameters} for each mode.
	 */
	private final Map<String, DistanceGroupModeUtilityParameters> modeParams = new TreeMap<>();

	private final Scenario scenario;
	private final VspScoringConfigGroup vspScoring;

	@Inject
	public DetailedPersonScoringParameters(Scenario scenario) {
		this.scenario = scenario;
		this.vspScoring = ConfigUtils.addOrGetModule(scenario.getConfig(), VspScoringConfigGroup.class);
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		ScoringConfigGroup scoring = scenario.getConfig().scoring();
		String subpopulation = PopulationUtils.getSubpopulation(person);

		ScoringConfigGroup.ScoringParameterSet scoringParameters = scoring.getScoringParameters(subpopulation);

		Map<String, ActivityUtilityParameters> personParams = this.activityParamsPerSubpopulation.computeIfAbsent(subpopulation, k -> {
			Map<String, ActivityUtilityParameters> activityParams = new TreeMap<>();
			for (ScoringConfigGroup.ActivityParams params : scoringParameters.getActivityParams()) {
				ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params);
				activityParams.put(params.getActivityType(), factory.build());
			}
			return activityParams;
		});

		ScoringParameters.Builder builder = new ScoringParameters.Builder(scoring, scoringParameters, personParams,
			scenario.getConfig().scenario());

		// TODO: not configurable at the moment
		if ("person".equals(subpopulation)) {

			for (Map.Entry<String, VspScoringConfigGroup.ModeParams> e : vspScoring.getModeParams().entrySet()) {

				ModeUtilityParameters params = builder.getModeParameters(e.getKey());
				DistanceGroupModeUtilityParameters p = modeParams.computeIfAbsent(e.getKey(),
					k -> new DistanceGroupModeUtilityParameters(params, vspScoring.getDistGroups(), e.getValue()));

				builder.setModeParameters(e.getKey(), p);
			}
		}

		return builder.build();
	}
}
