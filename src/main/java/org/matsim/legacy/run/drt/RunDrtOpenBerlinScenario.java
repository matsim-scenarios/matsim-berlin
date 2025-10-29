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

package org.matsim.legacy.run.drt;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.legacy.run.BerlinExperimentalConfigGroup;
import org.matsim.legacy.run.RunBerlinScenario;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

/**
 * This class starts a simulation run with DRT.
 *
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- Initial plans only modified such that persons receive a specific income.
 *
 * @author ikaddoura
 */

public final class RunDrtOpenBerlinScenario {

	private static final Logger log = LogManager.getLogger(RunDrtOpenBerlinScenario.class);

	private static final String DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE = "drtStopFilter";
	private static final String DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE = "station_S/U/RE/RB_drtServiceArea";

	public static void main(String[] args) throws CommandLine.ConfigurationException {

		for (String arg : args) {
			log.info( arg );
		}

		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"}  ;
		}

		Config config = prepareConfig( args ) ;
		Scenario scenario = prepareScenario( config ) ;
		Controler controler = prepareControler( scenario ) ;
		controler.run() ;
	}

	public static Controler prepareControler( Scenario scenario ) {

		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

		// drt + dvrp module
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);

				//use income-dependent marginal utility of money for scoring
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);
			}
		});

		// yyyy there is fareSModule (with S) in config. ?!?!  kai, jul'19
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
		controler.addOverridingModule(new PtIntermodalRoutingModesModule());

		return controler;
	}

	public static Scenario prepareScenario( Config config ) {

		Scenario scenario = RunBerlinScenario.prepareScenario( config );
		BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);

		for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {

			String drtServiceAreaShapeFile = drtCfg.getDrtServiceAreaShapeFile();
			if (drtServiceAreaShapeFile != null && !drtServiceAreaShapeFile.equals("") && !drtServiceAreaShapeFile.equals("null")) {

				// Michal says restricting drt to a drt network roughly the size of the service area helps to speed up.
				// This is even more true since drt started to route on a freespeed TT matrix (Nov '20).
				// A buffer of 10km to the service area Berlin includes the A10 on some useful stretches outside Berlin.
				if(berlinCfg.getTagDrtLinksBufferAroundServiceAreaShp() >= 0.0) {
					addDRTmode(scenario, drtCfg.getMode(), drtServiceAreaShapeFile, berlinCfg.getTagDrtLinksBufferAroundServiceAreaShp());
				}

				tagTransitStopsInServiceArea(scenario.getTransitSchedule(),
						DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE, DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE,
						drtServiceAreaShapeFile,
						"stopFilter", "station_S/U/RE/RB",
						// some S+U stations are located slightly outside the shp File, e.g. U7 Neukoelln, U8
						// Hermannstr., so allow buffer around the shape.
						// This does not mean that a drt vehicle can pick the passenger up outside the service area,
						// rather the passenger has to walk the last few meters from the drt drop off to the station.
						200.0); // TODO: Use constant in RunGTFS2MATSimOpenBerlin and here? Or better some kind of set available pt modes?
			}
		}

		return scenario;
	}

	public enum AdditionalInformation { none, acceptUnknownParamsBerlinConfig }

	public static Config prepareConfig( AdditionalInformation additionalInformation, String [] args, ConfigGroup... customModules) {
		ConfigGroup[] customModulesToAdd = new ConfigGroup[] { new DvrpConfigGroup(), new MultiModeDrtConfigGroup(),
				new SwissRailRaptorConfigGroup(), new IntermodalTripFareCompensatorsConfigGroup(),
				new PtIntermodalRoutingModesConfigGroup() };
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

		Config config = RunBerlinScenario.prepareConfig( additionalInformation, args, customModulesAll ) ;

		DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.scoring(), config.routing());

		return config ;
	}
	public static Config prepareConfig( String [] args, ConfigGroup... customModules) {
		return prepareConfig( AdditionalInformation.none, args, customModules ) ;
	}

	public static void addDRTmode(Scenario scenario, String drtNetworkMode, String drtServiceAreaShapeFile, double buffer) {

		log.info("Adjusting network...");

		BerlinShpUtils shpUtils = new BerlinShpUtils( drtServiceAreaShapeFile );

		int counter = 0;
		int counterInside = 0;
		int counterOutside = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				log.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(TransportMode.car)) {
				if (shpUtils.isCoordInDrtServiceAreaWithBuffer(link.getFromNode().getCoord(), buffer)
						|| shpUtils.isCoordInDrtServiceAreaWithBuffer(link.getToNode().getCoord(), buffer)) {
					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

					allowedModes.add(drtNetworkMode);

					link.setAllowedModes(allowedModes);
					counterInside++;
				} else {
					counterOutside++;
				}

			}
		}

		log.info("Total links: {}", counter);
		log.info("Total links inside service area: {}", counterInside);
		log.info("Total links outside service area: {}", counterOutside);

		Set<String> modes = new HashSet<>();
		modes.add(drtNetworkMode);
		new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
	}

	private static void tagTransitStopsInServiceArea(TransitSchedule transitSchedule,
			String newAttributeName, String newAttributeValue,
			String drtServiceAreaShapeFile,
			String oldFilterAttribute, String oldFilterValue,
			double bufferAroundServiceArea) {
		log.info("Tagging pt stops marked for intermodal access/egress in the service area.");
		BerlinShpUtils shpUtils = new BerlinShpUtils( drtServiceAreaShapeFile );
		for (TransitStopFacility stop: transitSchedule.getFacilities().values()) {
			if (stop.getAttributes().getAttribute(oldFilterAttribute) != null) {
				if (stop.getAttributes().getAttribute(oldFilterAttribute).equals(oldFilterValue)) {
					if (shpUtils.isCoordInDrtServiceAreaWithBuffer(stop.getCoord(), bufferAroundServiceArea)) {
						stop.getAttributes().putAttribute(newAttributeName, newAttributeValue);
					}
				}
			}
		}
	}

}

