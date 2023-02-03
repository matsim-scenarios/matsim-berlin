package org.matsim.synthetic;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "assign-commuters", description = "Assign commuting destination and distance")
public class AssignCommuters implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Option(names = "--input")
	private Path input;

	@CommandLine.Option(names = "--output")
	private Path output;

	@CommandLine.Option(names = "--sample", description = "Sample size of the population", defaultValue = "0.25")
	private double sample;

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {

		// TODO: all within zone until commute data is ready
		if (PersonUtils.isEmployed(person)) {
			person.getAttributes().putAttribute(Attributes.COMMUTE, person.getAttributes().getAttribute(Attributes.GEM));
		}

		// TODO: commuting distances
	}
}
