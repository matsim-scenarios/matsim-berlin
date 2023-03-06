package org.matsim.synthetic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
		name = "merge-plans",
		description = "Merge plans of the same person into one population."
)
public class MergePlans implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(MergePlans.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to input populations")
	private List<Path> inputs;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private List<Path> output;

	public static void main(String[] args) {
		new MergePlans().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(inputs.get(0).toString());

		// TODO: only merge selected plans

		for (int i = 1; i < inputs.size(); i++) {

			String filename = inputs.get(i).toString();
			log.info("Reading {}", filename);

			Population pop = PopulationUtils.readPopulation(filename);

			for (Person p : pop.getPersons().values()) {

				Person destPerson = population.getPersons().get(p.getId());
				if (destPerson == null) {
					log.warn("Person {} not present in all populations.", p.getId());
					continue;
				}

				for (Plan plan : p.getPlans()) {
					destPerson.addPlan(plan);
				}
			}
		}

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
