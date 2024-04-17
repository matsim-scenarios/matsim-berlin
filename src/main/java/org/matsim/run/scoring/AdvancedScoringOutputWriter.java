package org.matsim.run.scoring;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * This class writes person specific information from {@link IndividualPersonScoringParameters} to the output.
 */
public class AdvancedScoringOutputWriter implements IterationEndsListener {


	@Inject
	private ScoringParametersForPerson scoring;

	private boolean outputWritten = false;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		if (outputWritten)
			return;

		if (!(scoring instanceof IndividualPersonScoringParameters params))
			return;

		OutputDirectoryHierarchy io = event.getServices().getControlerIO();

		String output = io.getOutputFilename("person_util_variations.csv");

		// Write scoring information for each person
		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(output), CSVFormat.DEFAULT)) {

			csv.print("person");
			csv.printRecord(params.header);

			for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {

				Object2DoubleMap<String> values = params.info.get(person.getId());
				if (values == null) {
					continue;
				}

				csv.print(person.getId());
				for (String s : params.header) {
					csv.print(values.getDouble(s));
				}
				csv.println();
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		params.header.clear();
		params.info.clear();

		outputWritten = true;
	}
}
