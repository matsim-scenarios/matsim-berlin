/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.prepare.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.util.Random;

public class AssignIncome {

	private static final Logger log = Logger.getLogger(AssignIncome.class);

	public static void assignIncomeToPersonSubpopulationAccordingToGermanyAverage(Population population){
		// https://de.wikipedia.org/wiki/Einkommensverteilung_in_Deutschland
		// besser https://www.destatis.de/DE/Themen/Gesellschaft-Umwelt/Einkommen-Konsum-Lebensbedingungen/Einkommen-Einnahmen-Ausgaben/Publikationen/Downloads-Einkommen/einkommensverteilung-2152606139004.pdf?__blob=publicationFile
		// Anteil der Personen (%) an allen Personen 10 20 30 40 50 60 70 80 90 100
		// Nettoäquivalenzeinkommen(€) 826 1.142 1.399 1.630 1.847 2.070 2.332 2.659 3.156 4.329

		log.info("start assigning income to persons according to german average");

		final Random rnd = MatsimRandom.getLocalInstance();

		population.getPersons().values().stream()
				.filter(person ->  {
					String subpopulation = PopulationUtils.getSubpopulation(person);
					return subpopulation != null && subpopulation.equals("person"); //only assign income to person subpopulation (not to freight etc.)
				})
				//don't overwrite income attribute (input plans may have income attributes already)
				.filter(person -> person.getAttributes().getAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME) == null)
				.forEach(person -> {
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
						person.getAttributes().putAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME, income);
				});

		log.info("finished");
	}

}

