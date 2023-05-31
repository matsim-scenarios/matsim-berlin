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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class UseDurationInsteadOfEndTime {

	private static final Logger log = LogManager.getLogger(UseDurationInsteadOfEndTime.class);

	private final static String inputPlans = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/plans_500_10-1_10pct_clc_act-split.xml.gz";
	private final static String outputPlans = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/plans_500_10-1_10pct_clc_act-split_duration7200.xml.gz";
	private static final String[] attributes = {};

	private final double durationThreshold = 7200.;

	public static void main(String[] args) {

		UseDurationInsteadOfEndTime filter = new UseDurationInsteadOfEndTime();
		filter.run(inputPlans, outputPlans, attributes);
	}

	public void run (final String inputPlans, final String outputPlans, final String[] attributes) {

		log.info("Accounting for the following attributes:");
		for (String attribute : attributes) {
			log.info(attribute);
		}
		log.info("Other person attributes will not appear in the output plans file.");

		Scenario scOutput;

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scInput = ScenarioUtils.loadScenario(config);

		scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();

		for (Person p : scInput.getPopulation().getPersons().values()){
			PopulationFactory factory = popOutput.getFactory();
			Person personNew = factory.createPerson(p.getId());

			for (String attribute : attributes) {
				personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
			}

			popOutput.addPerson(personNew);

			for (Plan plan : p.getPlans()) {

				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {

						Activity act = (Activity) pE;

						if (act.getEndTime().seconds() > 0. && act.getEndTime().seconds() <= 30 * 3600.) {

							if (act.getAttributes().getAttribute("cemdapStopDuration_s") != null) {
								int cemdapDuration = (int) act.getAttributes().getAttribute("cemdapStopDuration_s");

								if (cemdapDuration <= durationThreshold) {
//									log.info("end time: " + act.getEndTime() + " --> " + Double.NEGATIVE_INFINITY);
									act.setEndTime(Double.NEGATIVE_INFINITY);

//									log.info("duration: " + act.getMaximumDuration() + " --> " + cemdapDuration);
									act.setMaximumDuration(cemdapDuration);

								}
							}

						} else {
							// don't do anything
						}
					}
				}


				personNew.addPlan(plan);
			}

		}

		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

