package org.matsim.prepare.opt;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(name = "extract-plans-idx", description = "Extract plan index from population.")
public class ExtractPlanIndexFromType implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Option(names = "--input", description = "Path to input plans.", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Desired output plans.", required = true)
	private Path output;

	private final Object2IntMap<String> mapping = new Object2IntOpenHashMap<>();

	@Override
	public Integer call() throws Exception {

		StreamingPopulationReader reader = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));

		reader.addAlgorithm(this);

		reader.readFile(input.toString());

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {
			csv.printRecord("id", "idx");
			for (Object2IntMap.Entry<String> e : mapping.object2IntEntrySet()) {
				csv.printRecord(e.getKey(), e.getIntValue());
			}
		}

		return 0;
	}

	@Override
	public void run(Person person) {
		String type = person.getSelectedPlan().getType();
		mapping.put(person.getId().toString(), Integer.parseInt(type));
	}
}
