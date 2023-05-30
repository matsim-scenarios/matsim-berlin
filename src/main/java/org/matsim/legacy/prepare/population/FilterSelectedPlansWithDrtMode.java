/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.legacy.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class FilterSelectedPlansWithDrtMode {

	private static final Logger log = LogManager.getLogger(FilterSelectedPlansWithDrtMode.class);

	private final static String inputPlans = "...";
	private final static String outputPlans = "...";

	public static void main(String[] args) {

		FilterSelectedPlansWithDrtMode filter = new FilterSelectedPlansWithDrtMode();
		filter.run(inputPlans, outputPlans);
	}

	public void run (final String inputPlans, final String outputPlans) {

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
		config.plans().setInputFile(inputPlans);
		config.plans().setInputCRS("EPSG:31468");
		Scenario scInput = ScenarioUtils.loadScenario(config);

		Scenario scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		popOutput.getAttributes().putAttribute("coordinateReferenceSystem", "EPSG:31468");

		for (Person p : scInput.getPopulation().getPersons().values()){
			if (p.getSelectedPlan().getPlanElements().stream().
					filter(pe -> pe instanceof Leg).
					map(l -> ((Leg) l).getMode()).
					anyMatch(mode -> mode.equals(TransportMode.drt)))  {
				Person personNew = popOutput.getFactory().createPerson(p.getId());

				for (String attribute : p.getAttributes().getAsMap().keySet()) {
					personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
				}
				personNew.addPlan(p.getSelectedPlan());

				popOutput.addPerson(personNew);
			}
		}

		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

