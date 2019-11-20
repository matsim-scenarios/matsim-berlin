/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 *
 * @author ikaddoura
 *
 */
public class CreateFreightAgents {

	private static final Logger log = Logger.getLogger(CreateFreightAgents.class);

	private int personCounter = 0;
	private final Map<String, SimpleFeature> features = new HashMap<>();
	private final Scenario scenario;
	private final Map<Id<Link>, Integer> linkId2dailyFreightTrafficVolumeToBerlin = new HashMap<>();
	private final Map<Id<Link>, Integer> linkId2dailyFreightTrafficVolumeFromBerlin = new HashMap<>();

	private final String freightActivityType = "freight";
	
	public static void main(String [] args) throws IOException, ParseException {

		final String networkFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/network/network_300.xml.gz";
		final String inputFileSHP = "../../shared-svn/studies/ihab/berlin/shapeFiles/berlin_area/Berlin.shp";
		
		final String outputFilePopulation = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/freight/freight-agents-berlin4.1_sampleSize0.1.xml.gz";
		final double sampleSize = 0.1;

		CreateFreightAgents popGenerator = new CreateFreightAgents(networkFile, inputFileSHP);
		popGenerator.run(sampleSize, outputFilePopulation);	
	}


	public CreateFreightAgents(String networkFile, String inputFileSHP) throws IOException {
		
		// 2064 B1 Mahlsdorf
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("44729"), (int) (1916/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("44732"), (int) (1916/2.));
		
		// 3625 B158 Ahrensfelde
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("37354"), (int) (1279/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("37355"), (int) (1279/2.));
		
		// 2065 B2 Malchow
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("87998"), (int) (1045/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("87997"), (int) (1045/2.));

		// 2011 A114 Buchholz
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("47076"), (int) (1762/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("31062"), (int) (1762/2.));

		// 2063 B96a Blankenfelde
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("27534"), (int) (401/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("27533"), (int) (401/2.));
		
		// 2010 A111 Heiligensee 2
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("100059"), (int) (4329/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("143046"), (int) (4329/2.));

		// 2062 B5 Staaken
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("13245"), (int) (1986/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("98267"), (int) (1986/2.));
		
		// 2061 B1 Nikolassee
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("144245"), (int) (805/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("144244"), (int) (805/2.));
		
		// 3615 A115 Drewitz
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("57921"), (int) (5135/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("130736"), (int) (5135/2.));

		// 2066 B101 Marienfelde
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("89980"), (int) (2409/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("119310"), (int) (2409/2.));

		// 3720 B96 Dahlewitz 2
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("119942"), (int) (1143/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("142698"), (int) (1143/2.));

		// 2013 A113 Schönefeld
		linkId2dailyFreightTrafficVolumeToBerlin.put(Id.createLinkId("87464"), (int) (5203/2.));
		linkId2dailyFreightTrafficVolumeFromBerlin.put(Id.createLinkId("105681"), (int) (5203/2.));

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		scenario = ScenarioUtils.loadScenario(config);
		
		log.info("Reading shp file...");

		SimpleFeatureSource fts = ShapeFileReader.readDataFile(inputFileSHP);		

		SimpleFeatureIterator it = fts.getFeatures().features();
		while (it.hasNext()) {
			SimpleFeature ft = it.next();
			features.put("berlin", ft);
		}
		it.close();

		log.info("Reading shp file... Done.");
		
	}


	private void run(double sampleSize, String outputFilePopulation) throws ParseException, IOException {

		Random rnd = new Random();

		for (Id<Link> linkId : linkId2dailyFreightTrafficVolumeToBerlin.keySet()) {
			createFreightBerlinZielverkehr(scenario, rnd, linkId, linkId2dailyFreightTrafficVolumeToBerlin.get(linkId) * sampleSize);
		}
		
		for (Id<Link> linkId : linkId2dailyFreightTrafficVolumeFromBerlin.keySet()) {
			createFreightBerlinQuellverkehr(scenario, rnd, linkId, linkId2dailyFreightTrafficVolumeFromBerlin.get(linkId) * sampleSize);
		}	

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputFilePopulation);
		log.info("population written to: " + outputFilePopulation);
		log.info("Population contains " + personCounter +" agents.");
	}

	private void createFreightBerlinQuellverkehr(Scenario scenario, Random rnd, Id<Link> linkId, double odSum) {
		Population population = scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();

		for (int i = 0; i <= odSum; i++) {
			Person pers = popFactory.createPerson(Id.create("freight_" + personCounter + "_berlin" + "-" + linkId.toString(), Person.class));			
			Plan plan = popFactory.createPlan();
						
			Point startP = getRandomPointInFeature(rnd, features.get("berlin"));
			if ( startP==null ) log.warn("Point is null.");
			Activity startActivity = popFactory.createActivityFromCoord(this.freightActivityType, MGC.point2Coord(startP) ) ;
			
			double startTime = calculateNormallyDistributedTime(12 * 3600, 6 * 3600.);  // approx. 2/3 between 6.00 and 18.00
			startActivity.setEndTime(startTime);
			plan.addActivity(startActivity);

			Leg leg1 = popFactory.createLeg("freight");
			plan.addLeg(leg1);

			Activity endActivity = popFactory.createActivityFromCoord(this.freightActivityType, scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord());
			plan.addActivity(endActivity);

			pers.addPlan(plan) ;
			population.addPerson(pers) ;
			
			scenario.getPopulation().getPersons().get(pers.getId()).getAttributes().putAttribute(scenario.getConfig().plans().getSubpopulationAttributeName(), "freight");
			
			personCounter++;
		}
	}


	private void createFreightBerlinZielverkehr(Scenario scenario, Random rnd, Id<Link> linkId, double odSum) {
		Population population = scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();

		for (int i = 0; i <= odSum; i++) {
			Person pers = popFactory.createPerson(Id.create("freight_" + personCounter + "_" + linkId.toString() + "-" + "berlin", Person.class));
			
			Plan plan = popFactory.createPlan();
						
			Activity startActivity = popFactory.createActivityFromCoord(this.freightActivityType, scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord());

			double startTime = calculateNormallyDistributedTime(10 * 3600., 4 * 3600.); // approx. 2/3 between 6.00 and 14.00
			startActivity.setEndTime(startTime);
			plan.addActivity(startActivity);

			Leg leg1 = popFactory.createLeg("freight");
			plan.addLeg(leg1);

			Point endPoint = getRandomPointInFeature(rnd, features.get("berlin"));
			if ( endPoint==null ) log.warn("Point is null.");
			
			Activity endActivity = popFactory.createActivityFromCoord(this.freightActivityType, MGC.point2Coord(endPoint) ) ;
			plan.addActivity(endActivity);

			pers.addPlan(plan) ;
			population.addPerson(pers) ;
			
			scenario.getPopulation().getPersons().get(pers.getId()).getAttributes().putAttribute(scenario.getConfig().plans().getSubpopulationAttributeName(), "freight");

			personCounter++;
		}
	}

	private double calculateRandomlyDistributedValue(Random rnd, double i, double abweichung){
		double rnd1 = rnd.nextDouble();
		double rnd2 = rnd.nextDouble();
		
		double vorzeichen = 0;
		if (rnd1<=0.5){
			vorzeichen = -1.0;
		}
		else {
			vorzeichen = 1.0;
		}
		double endTimeInSec = (i + (rnd2 * abweichung * vorzeichen));
		return endTimeInSec;
	}
	
	private double calculateNormallyDistributedTime(double mean, double stdDev) {
		Random random = new Random();
		boolean leaveLoop = false;
		double endTimeInSec = Double.MIN_VALUE;
		
		while(leaveLoop == false) {
			double normal = random.nextGaussian();
			endTimeInSec = mean + stdDev * normal;
			
			if (endTimeInSec >= 0. && endTimeInSec <= 24. * 3600.) {
				leaveLoop = true;
			}
		}
		
		if (endTimeInSec < 0. || endTimeInSec > 24. * 3600) {
			throw new RuntimeException("Shouldn't happen. Aborting...");
		}
		return endTimeInSec;
	}
	
	private static Point getRandomPointInFeature(Random rnd, SimpleFeature ft) {

		// TODO: account for intra-zonal land use areas, e.g. CORINE Landuse data

		if ( ft!=null ) {

			Point p = null;
			double x, y;
			do {
				x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
				y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
				p = MGC.xy2Point(x, y);
			} while ( !((Geometry) ft.getDefaultGeometry()).contains(p));
			return p;

		} else {
			return null ;
		}


	}
	
}
