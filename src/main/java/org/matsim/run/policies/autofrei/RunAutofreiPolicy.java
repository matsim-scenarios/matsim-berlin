package org.matsim.run.policies.autofrei;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class RunAutofreiPolicy extends RunAutofreiBaseCaseCtdExtended {
	private static final Set<String> RESTRICTED_MODES = Set.of(TransportMode.car, TransportMode.ride);
	public static final String NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC = "commercial_goods_car";

	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiPolicy.class, args);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		Network network = scenario.getNetwork();
		AutofreiUtils.restrictInnerRingLinks(network, RESTRICTED_MODES);
		replaceLowestScorePlanByWalkPlan(scenario);
	}

	public static void replaceLowestScorePlanByWalkPlan(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if(!(person.getId().toString().startsWith("berlin_") || person.getId().toString().startsWith("bb_"))){
				// skip freight agents.
				continue;
			}

			// remove plan with lowest score from unselected plans
			Plan selectedPlan = person.getSelectedPlan();
			List<? extends Plan> unselectedPlans = person.getPlans().stream().filter(p -> p != selectedPlan).toList();
			Plan min = Collections.min(unselectedPlans, Comparator.comparing(Plan::getScore));
			person.removePlan(min);

			// create a new walk plan based on the selected plan
			Plan newPlan = PopulationUtils.createPlan(person);
			List<Activity> activities = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			for (int i = 0; i < activities.size()-1; i++) {
				newPlan.addActivity(activities.get(i));
				newPlan.addLeg(PopulationUtils.createLeg(TransportMode.walk));
			}
			newPlan.addActivity(activities.getLast());

			person.addPlan(newPlan);
		}
	}
}
