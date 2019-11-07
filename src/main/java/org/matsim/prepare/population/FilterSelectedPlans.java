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

package org.matsim.prepare.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class FilterSelectedPlans {
	
	private static final Logger log = Logger.getLogger(FilterSelectedPlans.class);
	
	private final static String inputPlans = "...";
	private final static String outputPlans = "...";
	
	public static void main(String[] args) {
		
		FilterSelectedPlans filter = new FilterSelectedPlans();
		filter.run(inputPlans, outputPlans);
	}
	
	public void run (final String inputPlans, final String outputPlans) {
		
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("GK4");
		config.plans().setInputFile(inputPlans);
		config.plans().setInputCRS("GK4");
		Scenario scInput = ScenarioUtils.loadScenario(config);
		
		Scenario scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		popOutput.getAttributes().putAttribute("coordinateReferenceSystem", "EPSG:31468");
		
		for (Person p : scInput.getPopulation().getPersons().values()){
			Person personNew = popOutput.getFactory().createPerson(p.getId());
			
			for (String attribute : p.getAttributes().getAsMap().keySet()) {
				personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
			}
			personNew.addPlan(p.getSelectedPlan());
									
			popOutput.addPerson(personNew);
		}
		
		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

