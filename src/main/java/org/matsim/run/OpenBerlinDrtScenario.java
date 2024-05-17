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

package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.legacy.run.BerlinExperimentalConfigGroup;
import org.matsim.legacy.run.drt.BerlinShpUtils;
import org.matsim.legacy.run.drt.OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier;
import org.matsim.legacy.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.legacy.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extend the {@link OpenBerlinScenario} by DRT functionality. <br>
 * By default, a config is loaded where a drt mode is operated in all of Berlin with 10,000 vehicles. <br>
 * Alternatively, you can provide another drt-only config using the {@code --drt-config} comand line option. <br>
 * This run script then configures drt to be perceived just like pt and to be fully tariff-integrated into pt. <br>
 */
public class OpenBerlinDrtScenario extends OpenBerlinScenario{

	//TODO: write tests

	private static final Logger log = LogManager.getLogger(OpenBerlinDrtScenario.class);

	@CommandLine.Option(names = "--drt-config",
		defaultValue = "input/v6.1/berlin-v6.1.drt-config.xml",
		description = "Path to drt (only) config. Should contain only additional stuff to base config. Otherwise overrides.")
	private String drtConfig;

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinDrtScenario.class, args);
	}

	@Override
	protected List<ConfigGroup> getCustomModules() {
		List<ConfigGroup> customModules = super.getCustomModules();
		customModules.addAll(Lists.newArrayList(
			new BerlinExperimentalConfigGroup(),
			new DvrpConfigGroup(),
			new MultiModeDrtConfigGroup(),
			new SwissRailRaptorConfigGroup(),
			new IntermodalTripFareCompensatorsConfigGroup(),
			new PtIntermodalRoutingModesConfigGroup()));
		return customModules;
	}

	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);

		ConfigUtils.loadConfig(config, drtConfig);

		//modify output directory and runId
		config.controller().setOutputDirectory(config.controller().getOutputDirectory() + "-drt");
		config.controller().setRunId(config.controller().getRunId() + "-drt");

		//drt only works with the following sim start time interpretation
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

		MultiModeDrtConfigGroup multiMideDrtCfg = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiMideDrtCfg, config.scoring(), config.routing());

		Set<String> modes = new HashSet<>();

		ScoringConfigGroup.ModeParams ptParams = config.scoring().getModes().get(TransportMode.pt);
		IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);

		for (DrtConfigGroup drtCfg : multiMideDrtCfg.getModalElements()) {
			modes.add(drtCfg.getMode());

			//copy all scoring params from pt
			ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(drtCfg.getMode());
			modeParams.setConstant(ptParams.getConstant());
			modeParams.setMarginalUtilityOfDistance(ptParams.getMarginalUtilityOfDistance());
			modeParams.setMarginalUtilityOfTraveling(ptParams.getMarginalUtilityOfTraveling());
			modeParams.setDailyUtilityConstant(ptParams.getDailyUtilityConstant());

			//assume that the drt is fully integrated in pt, i.e. fare integration
			modeParams.setMonetaryDistanceRate(ptParams.getMonetaryDistanceRate());
			modeParams.setDailyMonetaryConstant(ptParams.getDailyMonetaryConstant());
			config.scoring().addModeParams(modeParams);
		}

		//assume that (all) the drt is fully integrated in pt, i.e. fare integration
		IntermodalTripFareCompensatorConfigGroup drtCompensationCfg = new IntermodalTripFareCompensatorConfigGroup();
		drtCompensationCfg.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedAnywhereInTheDay);
		drtCompensationCfg.setCompensationMoneyPerDay(ptParams.getDailyMonetaryConstant());
		drtCompensationCfg.setNonPtModes(ImmutableSet
			.<String>builder()
			.addAll(modes)
			.build());
		compensatorsConfig.addParameterSet(drtCompensationCfg);

		//include drt in mode-choice and add mode params.
		//by using a Set, it should be assured that they aren't included twice.
		modes.addAll(Arrays.asList(config.subtourModeChoice().getModes()));
		config.subtourModeChoice().setModes(modes.toArray(String[]::new));

		//Here (or when extending this class), you can configure the dvrp and the drt config groups.
		//Of course you can configure on the xml (config) level, alternatively.
		//for example you can configure prices and compensations, service area etc.,
		//whether dvrp modes should operate on a mode-specific sub-network or on the entire car-network,
		//how the time-constraints for the dispatch should be parameterized etc.

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		//if the input plans contain DrtRoutes, this will cause problems later in the DrtRouteFactory
		//to avoid this, the DrtRouteFactory would have to get set before loading the scenario, just like in Open Berlin v5.x
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		//if the drt mode is configured as a dvrp network mode and if it has a service area
		//the drt mode is added to the links in the service area with a buffer of +2000 meter (per default or otherwise configured in BerlinExperimentalConfigGroup)
		//and transit stops 200m around the service area are tagged to be be served by the corresponding drt.
		prepareNetworkAndTransitScheduleForDrt(scenario);

		//Here (or when extending this class), you can mutate the scenario (e.g. population, network, ...)
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		// drt + dvrp modules
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.class);
				bind(MainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);

			}
		});

		// yyyy there is fareSModule (with S) in config. ?!?!  kai, jul'19
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
		controler.addOverridingModule(new PtIntermodalRoutingModesModule());
	}

	/**
	 * This code is copied from matsim-berlin v5.x {@code RunDrtOpenBerlinScenario.prepareScenario()} and sub-methods.
	 * @param scenario network and transit schedule are mutated as side effects.
	 */
	private static void prepareNetworkAndTransitScheduleForDrt(Scenario scenario) {
		BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(scenario.getConfig(), BerlinExperimentalConfigGroup.class);
		DvrpConfigGroup dvrpConfigGroup = DvrpConfigGroup.get(scenario.getConfig());

		for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(scenario.getConfig()).getModalElements()) {
			String drtServiceAreaShapeFile = drtCfg.drtServiceAreaShapeFile;
			if (drtServiceAreaShapeFile != null && !drtServiceAreaShapeFile.equals("") && !drtServiceAreaShapeFile.equals("null")) {

				if (dvrpConfigGroup.networkModes.contains(drtCfg.getMode())){
					// Michal says restricting drt to a drt network roughly the size of the service area helps to speed up.
					// This is even more true since drt started to route on a freespeed TT matrix (Nov '20).
					// A buffer of 10km to the service area Berlin includes the A10 on some useful stretches outside Berlin.
					if (berlinCfg.getTagDrtLinksBufferAroundServiceAreaShp() >= 0.0) {
						//TODO: inline/move method ?
						RunDrtOpenBerlinScenario.addDRTmode(scenario, drtCfg.getMode(), drtServiceAreaShapeFile, berlinCfg.getTagDrtLinksBufferAroundServiceAreaShp());
					}
				}

				tagTransitStopsInServiceArea(scenario.getTransitSchedule(),
					"drtStopFilter", "station_S/U/RE/RB_drtServiceArea",
					drtServiceAreaShapeFile,
					"stopFilter", "station_S/U/RE/RB",
					// some S+U stations are located slightly outside the shp File, e.g. U7 Neukoelln, U8
					// Hermannstr., so allow buffer around the shape.
					// This does not mean that a drt vehicle can pick the passenger up outside the service area,
					// rather the passenger has to walk the last few meters from the drt drop off to the station.
					200.0);
			}
		}
	}

	private static void tagTransitStopsInServiceArea(TransitSchedule transitSchedule,
													 String newAttributeName, String newAttributeValue,
													 String drtServiceAreaShapeFile,
													 String oldFilterAttribute, String oldFilterValue,
													 double bufferAroundServiceArea) {
		log.info("Tagging pt stops marked for intermodal access/egress in the service area.");
		BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
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
