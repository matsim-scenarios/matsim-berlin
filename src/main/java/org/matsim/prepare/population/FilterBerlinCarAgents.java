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

import com.sun.jdi.connect.Transport;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;
import java.util.stream.Collectors;

public class FilterBerlinCarAgents {

	private static Logger log = Logger.getLogger(FilterBerlinCarAgents.class);

	private static final String POPULATION_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-10pct.output_plans.xml.gz";
	private static final String OUTPUT_POPULATION = "D:/berlin-v5.5-10pct.plans.berlinCarUsersOnly.xml.gz";

	public static void main(String[] args) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(POPULATION_FILE);

		int nrBefore = scenario.getPopulation().getPersons().size();

		List<Id<Person>> personsToDelete = scenario.getPopulation().getPersons().values().stream()
				.filter(person -> ! (isBerlinPerson(person) && isCarUser(person)))
				.map(person -> person.getId())
				.collect(Collectors.toList());

		personsToDelete.forEach(personId -> scenario.getPopulation().removePerson(personId));

		new PopulationWriter(scenario.getPopulation()).write(OUTPUT_POPULATION);

		log.info("nr of agents before reduction = " + nrBefore);
		log.info("nr of agents after reduction = " + scenario.getPopulation().getPersons().size());

		log.info("finished...");
	}

	private static boolean isBerlinPerson(Person person){
		return PopulationUtils.getSubpopulation(person).equals("person") &&
				! (person.getId().toString().contains("freight") || PopulationUtils.getPersonAttribute(person, "home-activity-zone").equals("brandenburg") );
	}


	private static boolean isCarUser(Person person){
		return TripStructureUtils.getLegs(person.getSelectedPlan()).stream()
				.filter(leg -> TripStructureUtils.getRoutingMode(leg).equals(TransportMode.car))
				.findAny()
				.isPresent();
	}


}
