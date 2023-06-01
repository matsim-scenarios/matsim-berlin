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
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.PersonSpecificScoringAttributesSetter;
import org.matsim.utils.math.RandomFromDistribution;

import java.util.*;

public class AssignPersonModeConstants {

    private static final Logger log = LogManager.getLogger(AssignPersonModeConstants.class);

    public static void main(String[] args) {
        String inputPopulationFile = "~/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";
//        String mode = TransportMode.drt;
//        String mode = TransportMode.car;
        String mode = "bicycle";

        double mean = 0.0;
        double sigma = 100.0;
//		String outputPopulationFile = "berlin-v5.5-10pct_drtAscLogNormal_mean_" +
//        String outputPopulationFile = "berlin-v5.5-10pct_drtAscUniform_mean_" +
//                mean + "_sigma_" + sigma + ".plans.xml.gz";
        String outputPopulationFile = "berlin-v5.5-10pct_" + mode + "_asc2ExtremeValues_mean_" +
                mean + "_sigma_" + sigma + ".plans.xml.gz";

        Population population = PopulationUtils.readPopulation(inputPopulationFile);

//		SplittableRandom splittableRandom = new SplittableRandom(1234);
//		PersonSpecificScoringAttributesSetter.setLogNormalModeConstant((Collection<Person>) population.getPersons().values(),
//				mode, mean, sigma, splittableRandom);

        Random random = new Random(1234);
        population.getPersons().values().forEach((person) -> {
            Map<String, String> modeConstants = PersonUtils.getModeConstants(person);
            if (modeConstants == null) {
                modeConstants = new HashMap<>();
            }

//            modeConstants.put(mode, Double.toString((random.nextDouble() - 0.5) * 2 * sigma + mean));
            modeConstants.put(mode, Double.toString((random.nextDouble() < 0.5 ? sigma : -sigma) + mean));

            PersonUtils.setModeConstants(person, modeConstants);
        });


        PopulationUtils.writePopulation(population, outputPopulationFile);
    }

}

