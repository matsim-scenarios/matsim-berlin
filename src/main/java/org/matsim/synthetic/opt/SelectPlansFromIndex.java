package org.matsim.synthetic.opt;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(name = "select-plans-idx", description = "Select plan index as specified from input.")
public class SelectPlansFromIndex implements MATSimAppCommand {

	@CommandLine.Option(names = "--input", description = "Path to input plans.", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Desired output plans.", required = true)
	private Path output;

	@CommandLine.Option(names = "--csv", description = "Path to input plans (Usually experienced plans).", required = true)
	private Path csv;

	@CommandLine.Mixin
	private CsvOptions csvOpt;

	public static void main(String[] args) {
		new SelectPlansFromIndex().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());
		Object2IntMap<Id<Person>> idx = new Object2IntOpenHashMap<>();
		try (CSVParser parser = csvOpt.createParser(csv)) {
			for (CSVRecord record : parser) {
				idx.put(Id.createPersonId(record.get("id")), Integer.parseInt(record.get("idx")));
			}
		}

		Set<Id<Person>> toRemove = new HashSet<>();

		for (Person person : population.getPersons().values()) {

			List<? extends Plan> plans = person.getPlans();
			Set<Plan> removePlans = new HashSet<>();

			// will be 0 if no value is present
			int planIndex = idx.getInt(person.getId());
			if (planIndex == -1) {
				toRemove.add(person.getId());
				continue;
			}

			for (int i = 0; i < plans.size(); i++) {
				if (i == planIndex) {
					person.setSelectedPlan(plans.get(i));
				} else
					removePlans.add(plans.get(i));
			}
			removePlans.forEach(person::removePlan);
		}

		toRemove.forEach(population::removePerson);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
