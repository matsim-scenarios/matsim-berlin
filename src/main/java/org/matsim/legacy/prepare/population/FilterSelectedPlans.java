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
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class FilterSelectedPlans {

	private static final Logger log = LogManager.getLogger(FilterSelectedPlans.class);

	private final static String inputPlans = "D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/base_b18_pt03_c1_mgn0.6.output_plans.xml.gz";//"...";
	private final static String outputPlans = "D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";//"...";
	private final static boolean deleteRoutes = false;

	public static void main(String[] args) {

		FilterSelectedPlans filter = new FilterSelectedPlans();
		filter.run(inputPlans, outputPlans, deleteRoutes);
	}

	public void run (final String inputPlans, final String outputPlans, final boolean deleteRoutes) {

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
		config.plans().setInputFile(inputPlans);
		config.plans().setInputCRS("EPSG:31468");
		Scenario scInput = ScenarioUtils.loadScenario(config);

		Scenario scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		popOutput.getAttributes().putAttribute("coordinateReferenceSystem", "EPSG:31468");

		TripsToLegsAlgorithm trips2Legs = new TripsToLegsAlgorithm(TripStructureUtils.getRoutingModeIdentifier());

		for (Person p : scInput.getPopulation().getPersons().values()){
			Person personNew = popOutput.getFactory().createPerson(p.getId());

			for (String attribute : p.getAttributes().getAsMap().keySet()) {
				personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
			}
			Plan selectedPlan = p.getSelectedPlan();
			if (deleteRoutes) {
				trips2Legs.run(selectedPlan);
			}
			personNew.addPlan(selectedPlan);

			popOutput.addPerson(personNew);
		}

		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

