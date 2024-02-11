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
	private final Map<String, ScoringParameters> cache = new ConcurrentHashMap<>();


	private final Scenario scenario;
	private final VspScoringConfigGroup vspScoring;
	private final ScoringConfigGroup scoring;

	@Inject
	public DetailedPersonScoringParameters(Scenario scenario) {
		this.scenario = scenario;
		this.vspScoring = ConfigUtils.addOrGetModule(scenario.getConfig(), VspScoringConfigGroup.class);
		this.scoring = scenario.getConfig().scoring();
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		String subpopulation = PopulationUtils.getSubpopulation(person);

		return this.cache.computeIfAbsent(subpopulation, k -> {

			ScoringConfigGroup.ScoringParameterSet scoringParameters = scoring.getScoringParameters(k);

			Map<String, ActivityUtilityParameters> activityParams = new TreeMap<>();
			for (ScoringConfigGroup.ActivityParams params : scoringParameters.getActivityParams()) {
				ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params);
				activityParams.put(params.getActivityType(), factory.build());
			}

			ScoringParameters.Builder builder = new ScoringParameters.Builder(scoring,
				scoringParameters, activityParams, scenario.getConfig().scenario());

			if ("person".equals(k)) {

				for (Map.Entry<String, VspScoringConfigGroup.ModeParams> e : vspScoring.getModeParams().entrySet()) {

					ModeUtilityParameters params = builder.getModeParameters(e.getKey());
					DistanceGroupModeUtilityParameters p = new DistanceGroupModeUtilityParameters(params,
						vspScoring.getDistGroups(), e.getValue());

					builder.setModeParameters(e.getKey(), p);
				}
			}

			return builder.build();
		});
	}
}
