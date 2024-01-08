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

public class DetailedPersonScoringParameters implements ScoringParametersForPerson {

	/**
	 * Cache instances of {@link ActivityUtilityParameters} for each activity type.
	 * All params are the same for each person.
	 */
	private final Map<String, ActivityUtilityParameters> utilParams = new TreeMap<>();

	/**
	 * Cache instances of {@link ModeUtilityParameters} for each mode.
	 */
	private final Map<String, DistanceGroupModeUtilityParameters> modeParams = new TreeMap<>();

	@Inject
	private Scenario scenario;

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		ScoringConfigGroup scoring = scenario.getConfig().scoring();
		String subpopulation = PopulationUtils.getSubpopulation(person);

		ScoringConfigGroup.ScoringParameterSet scoringParameters = scoring.getScoringParameters(subpopulation);

		Map<String, ActivityUtilityParameters> personParams = new TreeMap<>();

		for (ScoringConfigGroup.ActivityParams params : scoringParameters.getActivityParams()) {
			ActivityUtilityParameters p = utilParams.computeIfAbsent(params.getActivityType(), k -> {
				ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params);
				return factory.build();
			});

			personParams.put(params.getActivityType(), p);
		}

		ScoringParameters.Builder builder = new ScoringParameters.Builder(scoring, scoringParameters, personParams,
			scenario.getConfig().scenario());

		// TODO: not configurable at the moment
		if (subpopulation.equals("person")) {

			VspScoringConfigGroup vspScoring = ConfigUtils.addOrGetModule(scenario.getConfig(), VspScoringConfigGroup.class);

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
