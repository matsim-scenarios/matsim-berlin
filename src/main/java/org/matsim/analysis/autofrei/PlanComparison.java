package org.matsim.analysis.autofrei;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

public class PlanComparison {
	public static void main(String[] args) {
		String baseFolder = "/Users/paulh/Downloads/baseCaseCtdExtended/";
		String policyFolder = "/Users/paulh/Downloads/deparking-p1_max1000/";

		Population base = PopulationUtils.readPopulation(baseFolder + "berlin-v6.4.output_plans.xml.zst");
		Population policy = PopulationUtils.readPopulation(policyFolder + "berlin-v6.4.output_plans.xml.zst");

		filter(base);
		filter(policy);

		PopulationUtils.writePopulation(base, baseFolder + "berlin-v6.4.output_plans_filtered.xml.gz");
		PopulationUtils.writePopulation(policy, policyFolder + "berlin-v6.4.output_plans_filtered.xml.gz");
	}

	private static void filter(Population population) {
		population.getPersons().entrySet()
			.removeIf(p -> !(p.getKey().equals(Id.createPersonId("berlin_93fdff6a")) || p.getKey().equals(Id.createPersonId("berlin_8a4ab87d")) ));
	}
}
