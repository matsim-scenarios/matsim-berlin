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

package org.matsim.prepare.drt;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.List;

public class DeleteDRTRoutesFromPopulation {


	public static void main(String[] args) {

		String inputPopulation = "D:/svn/shared-svn/projects/pave/matsim-input-files/drt-test-demand/pave109.output_plans.xml.gz";
		Population population = PopulationUtils.readPopulation(inputPopulation);


		population.getPersons().values().parallelStream()
				.forEach(person -> {
					List<Plan> plansToDelete = new ArrayList<>();
					person.getPlans().forEach(plan -> {
								if (plan.equals(person.getSelectedPlan())){
									TripStructureUtils.getLegs(plan)
											.forEach(leg -> {
												if(leg.getRoute() instanceof GenericRouteImpl && leg.getMode().equals("drt")){
													leg.setRoute(null);
												}
											});
								} else plansToDelete.add(plan);
							}
					);
					plansToDelete.forEach(plan -> person.removePlan(plan));
				});
		PopulationUtils.writePopulation(population, "D:/svn/shared-svn/projects/pave/matsim-input-files/drt-test-demand/pave109.output_plans_selected-only-woDrtRoutes.xml.gz");
	}
}
