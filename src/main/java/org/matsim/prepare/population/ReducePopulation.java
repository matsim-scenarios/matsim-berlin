/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author gleich
 *
 */
public class ReducePopulation {
	
	private final static Logger log = Logger.getLogger(ReducePopulation.class);

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("DHDN_GK4");
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");
		config.plans().setInputFile("/home/gregor/Downloads/berlin-drt-v5.5-1pct_drt-119.output_plans.xml.gz");
		double factor = 0.1;
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		reducePopulation(scenario, factor);
		
		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation());
		popWriter.write("/home/gregor/Downloads/berlin-drt-v5.5-1pct_drt-119_0.1.output_plans.xml.gz");
	}
	
	static void reducePopulation( Scenario scenario, double factor ) {
		log.info("Reducing population by a factor of " + factor);
		// reduce to some sample in order to increase turnaround during debugging:
		Random random = new Random(4711) ;
		Iterator<?> it = scenario.getPopulation().getPersons().entrySet().iterator() ;
		while( it.hasNext() ) {
			it.next() ;
			if ( random.nextDouble() < (1-factor) ) {
				it.remove();
			}
		}
	}

}
