/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.run.drt;


import org.apache.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Map;

import static org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;


/**
 * @author ikaddoura, zmeng
 *
 */
@FixMethodOrder(MethodSorters.JVM)

/**
 *
 * Tests an old scenario based on v5.2 of the OpenBerlinScenario with drt in the hundekopf area.
 * A setup similar to AVOEV, but different from Berlkoenig
 *
 */
public class RunBerlinDrtHundekopfScenarioTest {
	private static final Logger log = Logger.getLogger( RunBerlinDrtHundekopfScenarioTest.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	// there are two potential sav users in the test input plans
	@Test
	public final void testDrtBerlinScenarioA() {

		try {

			String configFileName ;
			String drtServiceAreaShapeFile;

			configFileName = "test/input/berlin-drtA-v5.2-1pct.config.xml";
			drtServiceAreaShapeFile = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";

			Config config = RunDrtOpenBerlinScenario.prepareConfig(new String[]{configFileName} ) ;

			AvoevConfigGroup avoevConfig = ConfigUtils.addOrGetModule( config, AvoevConfigGroup.class );
			avoevConfig.setDrtServiceAreaShapeFileName( drtServiceAreaShapeFile );

			config.controler().setWriteEventsInterval(1);

			config.plans().setInputFile("berlin-v5.2-1pct.plans_test-agents.xml");
			config.controler().setLastIteration(40);

//			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			// not needed when using utils.getOutputDirectory() (which removes the directory)

			config.controler().setOutputDirectory( utils.getOutputDirectory() + "run_output/" );

			config.planCalcScore().getModes().get("drt").setConstant(99999); // to make the drt mode very attractive

			config.plansCalcRoute().setInsertingAccessEgressWalk(true);

			// faster without simulated pt; might cause a runtime exception in case the pt mode is chosen (use teleported pt?)
			config.transit().setUseTransit(false);
			ModeRoutingParams pars = new ModeRoutingParams(TransportMode.pt);
			pars.setTeleportedModeSpeed(4.166);
			config.plansCalcRoute().addModeRoutingParams(pars);

			// to have some mode changes in the first iterations
			for (StrategySettings settings : config.strategy().getStrategySettings()) {
				if (settings.getStrategyName().equals( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice )) {
					settings.setWeight(999999.);
				}
			}

			config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );

			// ---

			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario( config );
			//			new NetworkWriter(scenario.getNetwork()).write(utils.getOutputDirectory() + "drt-berlin-v5.network.xml.gz");
			//			new PopulationWriter(scenario.getPopulation()).write(utils.getOutputDirectory() + "drt-berlin-v5.2-1pct.plans.xml.gz");

			// ---

			Controler controler = RunDrtOpenBerlinScenario.prepareControler( scenario ) ;

			final DrtUsersEventHandler drtUsersEventHandler = new DrtUsersEventHandler();
			controler.addOverridingModule( new AbstractModule(){
				@Override public void install(){
					this.addEventHandlerBinding().toInstance( drtUsersEventHandler ); ;
				}
			} ) ;

			controler.run() ;

			//			String outputConfigFile ="test/output/org/matsim/run/RunBerlinAvoevScenarioTest/testDrtBerlinScenarioA/run_output/berlin-drtA-v5.2-1pct.output_config.xml";
			//			String testEventsFile =utils.getOutputDirectory() + "run_output/ITERS/it.30/berlin-drtA-v5.2-1pct.30.events.xml.gz";
			//			String networkFile =utils.getOutputDirectory() + "run_output/berlin-drtA-v5.2-1pct.output_network.xml.gz";
			//			String outputPlansFile = "berlin-drtA-v5.2-1pct.output_plans.xml.gz";
			//			String outputEventsFile = utils.getOutputDirectory() + "run_output/berlin-drtA-v5.2-1pct.output_events.xml.gz";

			//			EventsManager eventsManager = EventsUtils.createEventsManager();
			//			EventsManager eventsManager2 = EventsUtils.createEventsManager();

			//			Network network = NetworkUtils.createNetwork();
			//			new MatsimNetworkReader(network).readFile(networkFile);

			//			DrtUsersEventHandler drtUsersEventHandler2 = new DrtUsersEventHandler(network);

			//			eventsManager.addHandler(drtUsersEventHandler);
			//			eventsManager2.addHandler(drtUsersEventHandler2);

			//			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			//			MatsimEventsReader reader2 = new MatsimEventsReader(eventsManager2);

			//			reader.readFile(outputEventsFile);
			//			reader2.readFile(testEventsFile);

			//			Config outputConfig = ConfigUtils.loadConfig(outputConfigFile);
			//			outputConfig.plans().setInputFile(outputPlansFile);

			//			Scenario outputScenario = ScenarioUtils.loadScenario(outputConfig);
			//			new PopulationReader(outputScenario);
			//			Population population = outputScenario.getPopulation();

			double score = 0;

			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan selectedPlan = person.getSelectedPlan();
				List<PlanElement> planElements = selectedPlan.getPlanElements();
				for (PlanElement pe : planElements) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if(leg.getMode().equals("drt")) {
							score += selectedPlan.getScore();
							break;
						}
					}
				}
			}

			//			double delta = 0.1;

			// Because of changes in the MATSim core, in particular some fixes related to the teleportation speed of car access_walk and car egress_walk legs, the scores have changed
			// TODO: ZM: Please update the score comparisons below. ihab April'19
			// NOTE: the getScoreStats() is available directly from control(l)er. kai, jul'19

			Map<ScoreItem, Map<Integer, Double>> history = controler.getScoreStats().getScoreHistory();;

			log.info("score=" + score ) ;
			log.info( "scoreAv00=" + history.get( ScoreItem.average).get(0) ) ;
			log.info( "scoreAv40=" + history.get(ScoreItem.average).get(40) ) ;
			log.info("scoreEx00=" + history.get(ScoreItem.executed).get(0) ) ;
			log.info( "scoreEx40=" + history.get(ScoreItem.executed).get(40));

			// test whether the scores of drt_users remain constant.
			//			Assert.assertEquals(500152.62011033605, score, delta);

			// test whether the scores of it.0 and 40 remain constant.
			//			Assert.assertEquals(101.11934394711798, berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average).get(0),delta);
			//			Assert.assertEquals(70097.03911093851,  berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average).get(40),delta);
			//
			//			Assert.assertEquals(101.11934394711798, berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.executed).get(0),delta);
			//			Assert.assertEquals(125082.81243046453,  berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.executed).get(40),delta);
			//
			//			Assert.assertEquals(101.11934394711798, berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.best).get(0),delta);
			//			Assert.assertEquals(125103.26225240833,  berlin.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.best).get(40),delta);

			// TODO: ZM: Please also fix the following tests. ihab April'19

			// test whether the number of drt_users in output_Events remain constant.
			//			Assert.assertEquals(5, drtUsersEventHandler.getCntOfDrtTrips(),MatsimTestUtils.EPSILON);
			//			Assert.assertEquals(2, drtUsersEventHandler.getCntOfDrtUsers(),MatsimTestUtils.EPSILON);
			//			Assert.assertEquals(0, drtUsersEventHandler.getCntOfDrtStuck(),MatsimTestUtils.EPSILON);

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

}
