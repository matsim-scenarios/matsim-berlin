package org.matsim.prepare.choices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
		name = "assign-reference-population",
		description = "Assigns persons from reference data to a population."
)
public class AssignReferencePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AssignReferencePopulation.class);


	@CommandLine.Option(names = "--population", description = "Input population path.", required = true)
	private String populationPath;

	@CommandLine.Option(names = "--persons", description = "Input persons from survey data, in matsim-python-tools format.", required = true)
	private Path persons;

	@CommandLine.Option(names = "--trips", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path trips;

	@CommandLine.Option(names = "--facilities", description = "Shp file with facilities", required = true)
	private Path facilities;

	@CommandLine.Option(names = "--output", description = "Output population path.", required = true)
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new AssignReferencePopulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			log.error("No shapefile defined. Please specify a shapefile for the zones using the --shp option.");
			return 2;
		}

		if (!Files.exists(trips)) {
			log.error("Input trip file does not exist: {}", trips);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(populationPath);

		PlanBuilder builder = new PlanBuilder(shp, new ShpOptions(facilities, null, null), population.getFactory());

		builder.mergePlans(population, trips, persons);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
