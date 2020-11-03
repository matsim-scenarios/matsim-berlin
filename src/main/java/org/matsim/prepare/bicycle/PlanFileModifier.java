package org.matsim.prepare.bicycle;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanFileModifier {

    public static void main(String[] args) {
        String inputPlansFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans_uncalibrated.xml.gz";
        String outputPlansFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans-no-bicycle-routes.xml.gz";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(inputPlansFile);

        // Delete routes for bicycling agents
        scenario.getPopulation().getPersons().values().parallelStream()
                .flatMap(person -> person.getPlans().stream())
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(element -> element instanceof Leg)
                .map(element -> (Leg) element)
                .filter(leg -> leg.getMode().equals("bicycle"))
                .forEach(leg -> leg.setRoute(null));

        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
        populationWriter.write(outputPlansFile);
    }
}