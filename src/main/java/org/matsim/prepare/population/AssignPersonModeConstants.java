/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.PersonSpecificScoringAttributesSetter;

import java.util.Collection;
import java.util.SplittableRandom;

public class AssignPersonModeConstants {

	private static final Logger log = LogManager.getLogger(AssignPersonModeConstants.class);

	public static void main (String[] args) {
		String inputPopulationFile = "~/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";
		String mode = TransportMode.drt;
		double logNormalMean = -0.1;
		double logNormalSigma = 0.1;
		String outputPopulationFile = "berlin-v5.5-10pct_drtAscLogNormal_mean_" +
				logNormalMean + "_sigma_" + logNormalSigma + ".plans.xml.gz";

		Population population = PopulationUtils.readPopulation(inputPopulationFile);

		SplittableRandom splittableRandom = new SplittableRandom(1234);
		PersonSpecificScoringAttributesSetter.setLogNormalModeConstant((Collection<Person>) population.getPersons().values(),
				mode, logNormalMean, logNormalSigma, splittableRandom);

		PopulationUtils.writePopulation(population, outputPopulationFile);
	}

}

