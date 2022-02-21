/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.run.accessibility;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.*;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.run.RunBerlinScenario;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author dziemke
 */
public final class RunBerlinScenarioWithAccessibilities {

	private static final Logger log = Logger.getLogger(RunBerlinScenarioWithAccessibilities.class);

	// Accessibility configurations
	private static final boolean push2Geoserver = false; // Set true for run on server
	private static final boolean createQGisOutput = true; // Set false for run on server
    private static final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SCHOOL});
    private static final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY

	public static void main(String[] args) {
		for (String arg : args) {
			log.info( arg );
		}

        if ( args.length==0 ) {
            args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
        }
		
		Config config = prepareConfig( args ) ;
		Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
		Controler controler = prepareControler( scenario ) ;
		controler.run() ;

        if (createQGisOutput) {createQGisOutput(config);}
    }

    public static Config prepareConfig( String [] args, ConfigGroup... customModules) {
        ConfigGroup[] customModulesToAdd = new ConfigGroup[]{new AccessibilityConfigGroup()};
        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        Config config = RunBerlinScenario.prepareConfig(args, customModulesAll);

        config.controler().setOutputDirectory("../../shared-svn/projects/accessibility-berlin/output/v4/5000_8h05_school/");

        File opportunitiesFile = new File("../../shared-svn/projects/accessibility-berlin/osm/berlin/amenities/2018-05-30/facilities_classified.xml");
        config.facilities().setInputFile(opportunitiesFile.getAbsolutePath());


        ConfigUtils.setVspDefaults(config);

        AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
        acg.setTimeOfDay((8*60.+5.)*60.);
        acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
        acg.setShapeFileCellBasedAccessibility("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp");
        acg.setTileSize_m(5000);
        acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
        acg.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
        acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
        acg.setOutputCrs(config.global().getCoordinateSystem());

        config.controler().setLastIteration(0);

        return config ;
    }
	
	public static Controler prepareControler( Scenario scenario ) {
		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork()); // will be aggregated in downstream code!

		// Use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

		// Accessibility module
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			module.setCreateQGisOutput(createQGisOutput);
			controler.addOverridingModule(module);
		}

		return controler;
	}

    private static void createQGisOutput(Config config) {

//		final boolean includeDensityLayer = true;
        final Integer range = 9; // In the current implementation, this must always be 9
        final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
        final Double upperBound = 3.5;
//      final int populationThreshold = (50 / (1000/tileSize_m * 1000/tileSize_m)); // / by zero
        final int populationThreshold = 0;

        String osName = System.getProperty("os.name");
        String workingDirectory = config.controler().getOutputDirectory();
        for (String actType : activityTypes) {
            String actSpecificWorkingDirectory = workingDirectory + actType + "/";
            for (Modes4Accessibility mode : ((AccessibilityConfigGroup) config.getModules().get(AccessibilityConfigGroup.GROUP_NAME)).getIsComputingMode()) {
                VisualizationUtils.createQGisOutputRuleBasedStandardColorRange(actType, mode.toString(), envelope , workingDirectory,
                        config.global().getCoordinateSystem(), lowerBound, upperBound, range, populationThreshold);
//					VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
//							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, tileSize_m, populationThreshold);
                VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
            }
        }
    }
}