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

package org.matsim.analysis;

import one.util.streamex.StreamEx;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME;

public class IncomeDependentPersonAnalysis {

	private static final String INPUT_POPULATION = "";

	public static void main(String[] args) {
		Population population = PopulationUtils.readPopulation(INPUT_POPULATION);
		printNrOfPersonsClassifiedByDRTLegsAndIncome(population);
	}

	private static void printNrOfPersonsClassifiedByDRTLegsAndIncome(Population population) {
		Map<Double, List<Integer>> nrOfDrtLegsByIncome = StreamEx.of(population.getPersons().values())
				.filter(p -> PopulationUtils.getSubpopulation(p).equals("person"))
				.mapToEntry(p -> PopulationUtils.getPersonAttribute(p, PERSONAL_INCOME_ATTRIBUTE_NAME) == null ?  -999.999 : (double) PopulationUtils.getPersonAttribute(p, PERSONAL_INCOME_ATTRIBUTE_NAME),
						p -> getNumberOfDrtLegs(p))
				.filterValues(v -> v > 0)
				.grouping(toList());

		int lowerBound = 2;
		int upperBound = 5;

		System.out.println("\t<2\t2\t3\t4\t5\t>5");

		nrOfDrtLegsByIncome.forEach((income, drtLegsList) -> System.out.println(income + printArray(getListClassified(drtLegsList, lowerBound, upperBound))) );
	}

	private static String printArray(int[] array){
		String s = "";
		for (int i : array) {
			s+= "\t" + i;
		}
		return s;
	}

	/**
	 * returns an array with that contains the classified list...
	 * i.e. how many elements in the list are
	 * @param l
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	private static int[] getListClassified(List<Integer> l, int lowerBound, int upperBound){
		int[] array = new int[upperBound - lowerBound + 3];
		Arrays.fill(array,0);
		l.forEach(nr -> {
			if (nr < lowerBound) array[0]++;
			else if (nr > upperBound) array[array.length-1]++;
			else array[array.length - 2 - upperBound + nr]++;
		});
		return array;
	}

	private static int getNumberOfDrtLegs(Person person){
		return (int) TripStructureUtils.getLegs(person.getSelectedPlan()).stream()
				.filter(l -> l.getMode().equals("drt"))
				.count();
	}

}


