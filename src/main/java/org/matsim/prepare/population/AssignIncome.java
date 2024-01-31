package org.matsim.prepare.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;

import java.util.SplittableRandom;

/**
 * Draw income from german wide distribution.
 */
public class AssignIncome implements PersonAlgorithm {

	private final SplittableRandom rnd = new SplittableRandom(1234);

	/**
	 * Assign income on whole population.
	 */
	public static void assignIncomeToPersons(Population population) {
		AssignIncome assignIncome = new AssignIncome();
		population.getPersons().values().forEach(assignIncome::run);
	}

	@Override
	public void run(Person person) {

		// Only handle persons
		if (!PopulationUtils.getSubpopulation(person).equals("person"))
			return;

		// The income is german wide and not related to other attributes
		// even though srv contains the (household) economic status, this can not be easily back calculated to the income
		person.getAttributes().removeAttribute(Attributes.ECONOMIC_STATUS);

		// https://de.wikipedia.org/wiki/Einkommensverteilung_in_Deutschland
		// besser https://www.destatis.de/DE/Themen/Gesellschaft-Umwelt/Einkommen-Konsum-Lebensbedingungen/Einkommen-Einnahmen-Ausgaben/Publikationen/Downloads-Einkommen/einkommensverteilung-2152606139004.pdf?__blob=publicationFile
		// Anteil der Personen (%) an allen Personen 10 20 30 40 50 60 70 80 90 100
		// Nettoäquivalenzeinkommen(€) 826 1.142 1.399 1.630 1.847 2.070 2.332 2.659 3.156 4.329

		double income = 0.;
		double rndDouble = rnd.nextDouble();

		if (rndDouble <= 0.1) income = 826.;
		else if (rndDouble > 0.1 && rndDouble <= 0.2) income = 1142.;
		else if (rndDouble > 0.2 && rndDouble <= 0.3) income = 1399.;
		else if (rndDouble > 0.3 && rndDouble <= 0.4) income = 1630.;
		else if (rndDouble > 0.4 && rndDouble <= 0.5) income = 1847.;
		else if (rndDouble > 0.5 && rndDouble <= 0.6) income = 2070.;
		else if (rndDouble > 0.6 && rndDouble <= 0.7) income = 2332.;
		else if (rndDouble > 0.7 && rndDouble <= 0.8) income = 2659.;
		else if (rndDouble > 0.8 && rndDouble <= 0.9) income = 3156.;
		else if (rndDouble > 0.9) income = 4329.;
		else {
			throw new RuntimeException("Aborting..." + rndDouble);
		}

		PersonUtils.setIncome(person, income);
	}

}
