package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.common.conventions.vsp.SnzActivities;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
	name = "split-wrap-around-activities",
	description = "Disable wrap-around scoring by splitting the first and last act of the day into _morning and _evening act types."
)
public class SplitWrapAroundActivities implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SplitWrapAroundActivities.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	public static void main(String[] args) {
		new SplitWrapAroundActivities().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		population.getPersons().values().forEach(this::changeWrapAroundActsIntoMorningAndEveningActs);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Disable wrap-around scoring of the first and last act of the day by giving them different subtypes
	 * "_morning" and "_evening", so that no wrap-around scoring will be performed. Only rewrites the
	 * activity types; it neither parses nor writes any activity duration tags.
	 */
	private void changeWrapAroundActsIntoMorningAndEveningActs(Person person) {
//		ignore freight / commercial traffic agents and stay home agents
		if (!"person".equals(person.getAttributes().getAttribute("subpopulation")) ||
			person.getSelectedPlan().getPlanElements().size() == 1) {
			return;
		}

		for (Plan plan : person.getPlans()) {
			Activity first = (Activity) plan.getPlanElements().getFirst();
			Activity last = (Activity) plan.getPlanElements().getLast();

			String typeFirst = first.getType();
			String typeLast = last.getType();

			if (!typeFirst.equals(typeLast)) {
//				if first and last act do not have the same type, we will not change anything.
				continue;
			}

			first.setType(SnzActivities.createMorningActivityType(typeFirst));
			last.setType(SnzActivities.createEveningActivityType(typeLast));
		}
	}

}
