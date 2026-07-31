package org.matsim.run;

import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.contrib.parking.parkingsearchparameterization.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.kernel.ConstantKernelDistance;
import org.matsim.core.network.kernel.DefaultKernelFunction;
import org.matsim.core.network.kernel.KernelDistance;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.*;
import org.matsim.run.policies.PlanBasedParkingCapacityInitializerBerlin;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.contrib.parking.parkingsearchparameterization.ParkingUtils.LINK_ON_STREET_SPOTS;

public class TestParkingChessboard {

	private static final Logger log = LogManager.getLogger(TestParkingChessboard.class);
	/**
	 * Explicit parking search inserts this fixed parking activity into every car trip. The proxy
	 * models only occupancy-dependent search delay, so this constant is removed when comparing
	 * the two approaches directly. It is also written into the explicit-search config below.
	 */
	private static final double EXPLICIT_PARKING_INTERACTION_TIME = 60;
	/**
	 * The performance scenario contains four adjacent chessboards. Multiplying the original
	 * 100-person population by 80 yields about 2,000 agents per board, while 20 spaces per link
	 * preserves the demand-to-supply ratio of the earlier single-board benchmark.
	 */
	private static final int PERFORMANCE_ITERATIONS = 100;
	private static final int PERFORMANCE_POPULATION_MULTIPLIER = 80;
	private static final int PERFORMANCE_PARKING_SPOTS_PER_LINK = 20;
	private static final double PERFORMANCE_QSIM_END_TIME = 12 * 3600;
	private static final List<ParkingSearchStrategy> PERFORMANCE_STRATEGIES = List.of(
		ParkingSearchStrategy.Random,
		ParkingSearchStrategy.Benenson,
		ParkingSearchStrategy.DistanceMemory
	);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testParking() {
		//ParkingProxyConfigGroup parkingProxyConfigGroup = new ParkingProxyConfigGroup();

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"));

		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setOutputDirectory("./parkingChessboardOutput5ParkingSpotsPerLink");
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.scoring().setWriteExperiencedPlans(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		//double sampleSize = scenario.getConfig().qsim().getFlowCapFactor();

		for (Link link: scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 5);
			}
		}

/*		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				if (count < 15) {
					link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, (int) 5);
					count++;
				} else {
					break;
				}
			}
		} */
		Controler controler = new Controler(scenario);

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding("ParkingOccupancyOberserver").to(ParkingOccupancyObserver.class);
				addMobsimScopeEventHandlerBinding().to(ParkingOccupancyObserver.class);
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
				addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(ParkingOccupancyObserver.class).in(Singleton.class);
					bind(ParkingCapacityInitializer.class).to(PlanBasedParkingCapacityInitializerBerlin.class);
					bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
					bind(KernelDistance.class).toInstance(new ConstantKernelDistance(500));
					// use parameters from Belloche Paper https://pdf.sciencedirectassets.com/308315/1-s2.0-S2352146515X00032/1-s2.0-S2352146515000526/main.pdf?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEKn%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCICjqKOg8BiZaX434gG0V6S68pKD3x%2BfiiAj529bEHVzEAiEAize6AxQ2S06wRldce%2BX2Pn7tRssYhfhLdYHCcfOm3CMqsgUIEhAFGgwwNTkwMDM1NDY4NjUiDJTkFL2yfN9d7Een5SqPBdoyLYsf38Quz5jeOoOlee6BoBYPWd3pdF5Ft4OFuEtY2vBT8D1mNvwjiIc5o9EMbil%2Bn4J4ubPZLgahREJtFLKvTN%2FSYWaQlaNAr7g%2FFqyOMkNkZ4zRmuTFZHuhUCDkBsFzM7%2BP%2FeB509RzVnNtEFcXnSGmM5CU2QLXxaSpydoaYjNJa7GHot4wTWpAQSPNms98xmQw9SOcZpEEJKs5KbRpzjNzE%2Fw8oOckV4%2FcfXmCNboUbUVp7R%2Bt58iQakgTgumFGS2Mi1sJsVoRu0fc6EnGLsigMrrjftN8xAL7ravb0qhYY%2F1ohHd0LzzFmoJK32pNJonSai6zT97ttRM8PCH7hqoInHlVoRkm7asM8BUdDltEKQwxqG7i37IURCJj5ppO1EDK8YUoiJjgZUDFsl25g0zMGCD4UHpTiFnpl2JkrqHi0QN6du7OwhWUvu7mNUbrAkOleA79iT5FxS6x%2FMIYv2c63rGYxDy5uR5VZcZOS%2FQf5xH9yi5dnjgEMFdFO8%2FfiCyHNUP5a%2B6TQxJvoWcIP9dxZR0CeYQh4hguWeUiiKrFE4Z751wjgqMEEh6%2BktHL6Oh4wcnPm%2BRLjerz1AeQ7lqRyEJ6Jeef3qxbLPZpAum6AYFeKRwQeS6RDDfIgfNgrJVShSskjLUM9rxsgMMVZSreXSRzabXuYIdS%2BnV6sECS%2BahjG2XYX5VXSU%2BDAK8RKqHW323obs4iCBfWx6hMHPuBm57dWfSS829PiOEg7N1Y7u7IhN2nw99t9bwLiSKz6DJO%2FvQg5yeNtO61tYtEj5qxiHu8Sl4Myo3k%2Ba%2FgltmdpjddFAP7Gj%2BWDNc8G1KCN9nFKpNcMQp8%2Fc1evd0irMNgPjIeDi8fZQRpntowzt%2BJvwY6sQF3KQaY7OKk8oCrJwBhqBCXka4LjU%2BmSSEtBfnCSGyP2rGdPMG%2BEa5A4UoXaOve97jMdluCz8qxtStBCcFIGtOlVkduNXiYL%2BEK9Mfsrsh7by6oPxl%2BYjEhxfB%2BcTta4%2BODDQlxAFYuTh9TMyzFkSsrpug2HAxx5wOs02Nyr3i3N%2B1EyurdvJS7wxnn5yUhaglO%2FTrYeIlRXygDrQeOKrcvPBaIHVLm%2Bo7YyQWHH%2BtFFoI%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20250325T091640Z&X-Amz-SignedHeaders=host&X-Amz-Expires=300&X-Amz-Credential=ASIAQ3PHCVTYQYKBTS5L%2F20250325%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=c2f30fe9ebfee1301f9171ec0be610f8f6264eef89de1593c609a77eb2eff192&hash=35e4e916f56003c6740345396f4f9903fabcef13107cfa7a35d4b431a1044238&host=68042c943591013ac2b2430a89b270f6af2c76d8dfd086a07176afe7c76c2c61&pii=S2352146515000526&tid=spdf-2ba191d3-1250-4cf4-afd7-42eeada590bd&sid=9d2025d3623a93452e5b8c37c012e71dc9d7gxrqb&type=client&tsoh=d3d3LnNjaWVuY2VkaXJlY3QuY29t&rh=d3d3LnNjaWVuY2VkaXJlY3QuY29t&ua=1e035650550554095a5d0d&rr=925d4f92bb04e531&cc=de
					//f(occ, K) = alpha * exp(-beta * (occ / K))
					bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(0.11, -8.586));
					addControlerListenerBinding().to(ParkingOccupancyObserver.class);
					addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
				}
			});
		controler.run();
	}

	@Test
	void explicitParkingSearch() {
		//Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"));
		Config config = ConfigUtils.loadConfig("parkingsearch/config.xml", new ParkingSearchConfigGroup());
		System.out.println(config.getContext());
		config.controller().setLastIteration(10);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setOutputDirectory("./withExplicitParkingSearch");
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.scoring().setWriteExperiencedPlans(true);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setEndTime(50 * 3600);

		ScoringConfigGroup.ActivityParams parkingParams = new ScoringConfigGroup.ActivityParams();
		parkingParams.setScoringThisActivityAtAll(false);
		parkingParams.setActivityType("parking");
		parkingParams.setTypicalDuration(3600.0);
		config.scoring().addActivityParams(parkingParams);

		//ParkingSearchConfigGroup parkingSearchConfigGroup = new ParkingSearchConfigGroup();
		//parkingSearchConfigGroup.setParkingSearchStrategy(ParkingSearchStrategy.Benenson);
		//config.addModule(parkingSearchConfigGroup);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		addParkingFacilities(scenario, 5);

		/*Person personToKeep = null;
		for (Person person: scenario.getPopulation().getPersons().values()) {
			if(person.getId().equals(Id.createPersonId("1"))) {
				personToKeep = person;
			}
		}

		scenario.getPopulation().getPersons().clear();
		PopulationUtils.resetRoutes(personToKeep.getSelectedPlan());
		PopulationUtils.checkRouteModeAndReset(scenario.getPopulation(), scenario.getNetwork());
		scenario.getPopulation().addPerson(personToKeep); */

		Controller controller = ControllerUtils.createController(scenario);
		SetupParking.installParkingModules(controller);


		controller.run();
	}

	@Test
	void parkingProxyProducesSimilarTravelTimesAsExplicitParkingSearch() {
		// Run every explicit routing strategy to expose how its behavioral assumptions affect
		// travel time. NearestParkingSpot is the like-for-like proxy benchmark; the other strategies
		// explicitly cruise through the network and are therefore expected to produce different values.
		Map<ParkingSearchStrategy, TripTravelTimeResult> explicitResults =
			new EnumMap<>(ParkingSearchStrategy.class);
		for (ParkingSearchStrategy strategy : ParkingSearchStrategy.values()) {
			explicitResults.put(strategy, runExplicitParkingSearch(
				utils.getOutputDirectory() + "/" + strategy.name(), strategy, 5));
		}
		TripTravelTimeResult proxy = runParkingProxy(
			utils.getOutputDirectory() + "/proxy", 5);

		explicitResults.forEach((strategy, result) ->
			log.info("Mean trip travel time with explicit {} parking search: {} s",
				strategy, result.meanTravelTime()));
		log.info("Mean trip travel time with parking proxy: {} s", proxy.meanTravelTime());

		explicitResults.forEach((strategy, result) -> {
			assertEquals(result.completedLegs(), proxy.completedLegs(),
				"Explicit " + strategy + " search and the proxy should complete the same number of trips.");
			assertTrue(Double.isFinite(result.meanTravelTime()) && result.meanTravelTime() > 0,
				"Explicit " + strategy + " search should produce a valid mean trip travel time.");
		});

		TripTravelTimeResult nearestParkingSpot = explicitResults.get(ParkingSearchStrategy.NearestParkingSpot);
		// Door-to-door explicit time includes the fixed parking activity. Subtract it before
		// comparing with the proxy, which represents parking search only as an added car delay.
		double comparableExplicitTravelTime =
			nearestParkingSpot.meanTravelTime() - EXPLICIT_PARKING_INTERACTION_TIME;
		assertEquals(comparableExplicitTravelTime, proxy.meanTravelTime(),
			comparableExplicitTravelTime * 0.05,
			"The parking proxy should reproduce nearest-spot travel time within 5% after removing "
				+ "the explicit module's fixed parking interaction.");
	}

	@Test
	void parkingScarcityIncreasesTravelTimes() {
		Map<ParkingSearchStrategy, TripTravelTimeResult> explicitWithFiveSpots =
			new EnumMap<>(ParkingSearchStrategy.class);
		Map<ParkingSearchStrategy, TripTravelTimeResult> explicitWithOneSpot =
			new EnumMap<>(ParkingSearchStrategy.class);
		for (ParkingSearchStrategy strategy : ParkingSearchStrategy.values()) {
			explicitWithFiveSpots.put(strategy, runExplicitParkingSearch(
				utils.getOutputDirectory() + "/five-spots/" + strategy.name(), strategy, 5));
			explicitWithOneSpot.put(strategy, runExplicitParkingSearch(
				utils.getOutputDirectory() + "/one-spot/" + strategy.name(), strategy, 1));
		}
		TripTravelTimeResult proxyWithFiveSpots = runParkingProxy(
			utils.getOutputDirectory() + "/proxy-five-spots", 5);
		TripTravelTimeResult proxyWithOneSpot = runParkingProxy(
			utils.getOutputDirectory() + "/proxy-one-spot", 1);

		for (ParkingSearchStrategy strategy : ParkingSearchStrategy.values()) {
			TripTravelTimeResult fiveSpots = explicitWithFiveSpots.get(strategy);
			TripTravelTimeResult oneSpot = explicitWithOneSpot.get(strategy);
			log.info(
				"Explicit {} with five/one parking spots: {}/{} s ({} / {} completed)",
				strategy, fiveSpots.meanTravelTime(), oneSpot.meanTravelTime(),
				fiveSpots.completedLegs(), oneSpot.completedLegs());

			assertTrue(Double.isFinite(fiveSpots.meanTravelTime()) && fiveSpots.meanTravelTime() > 0,
				"Explicit " + strategy + " should produce a valid baseline travel time.");
			assertTrue(Double.isFinite(oneSpot.meanTravelTime()) && oneSpot.meanTravelTime() > 0,
				"Explicit " + strategy + " should produce a valid scarcity travel time.");
			assertTrue(oneSpot.meanTravelTime() >= fiveSpots.meanTravelTime(),
				"Scarcity should not reduce mean travel time for explicit " + strategy + " search.");
			assertTrue(oneSpot.completedLegs() <= fiveSpots.completedLegs(),
				"Scarcity should not increase completed trips for explicit " + strategy + " search.");
		}

		log.info(
			"Parking proxy with five/one parking spots: {}/{} s ({} / {} completed)",
			proxyWithFiveSpots.meanTravelTime(), proxyWithOneSpot.meanTravelTime(),
			proxyWithFiveSpots.completedLegs(), proxyWithOneSpot.completedLegs());

		//TODO understand why this is the case
		// Benenson is the explicit strategy expected to react clearly to occupied facilities by
		// cruising. Other strategies are still reported above, but their different behavioral
		// assumptions may make them insensitive to this particular capacity reduction.
		assertTrue(
			explicitWithOneSpot.get(ParkingSearchStrategy.Benenson).meanTravelTime()
				> explicitWithFiveSpots.get(ParkingSearchStrategy.Benenson).meanTravelTime(),
			"Benenson parking search should take longer when parking is scarce.");
		assertTrue(proxyWithOneSpot.meanTravelTime() > proxyWithFiveSpots.meanTravelTime(),
			"The parking proxy should produce longer travel times when parking is scarce.");
		assertEquals(proxyWithFiveSpots.completedLegs(), proxyWithOneSpot.completedLegs(),
			"The parking proxy should complete the same number of trips at both supply levels.");
	}

	/**
	 * Coarse in-process runtime comparison rather than a microbenchmark. The test only verifies
	 * that timing was recorded because JVM warm-up, garbage collection, execution order, and the
	 * continuing MATSim random stream can all influence the absolute measurements.
	 */
	@Test
	void compareParkingSearchComputationalPerformanceOverOneHundredIterations() {

		int lastIteration = PERFORMANCE_ITERATIONS;

		// NearestParkingSpot is excluded because its dynamic route cannot be restarted in a later
		// controller iteration. Every strategy below therefore receives the same 100-iteration setup.
		for (ParkingSearchStrategy strategy : PERFORMANCE_STRATEGIES) {
			SimulationRunResult result = runExplicitParkingSearch(
				utils.getOutputDirectory() + "/performance-explicit-" + strategy.name(),
				strategy, PERFORMANCE_PARKING_SPOTS_PER_LINK, lastIteration,
				PERFORMANCE_POPULATION_MULTIPLIER, PERFORMANCE_QSIM_END_TIME);
			log.info("QSim runtime for explicit {} over {} iterations: {} ms total, {} ms/iteration",
				strategy, PERFORMANCE_ITERATIONS, result.qsimRuntimeMillis(),
				result.qsimRuntimeMillis() / (double) PERFORMANCE_ITERATIONS);
			assertTrue(result.qsimRuntimeMillis() > 0,
				"Explicit " + strategy + " must produce a positive QSim runtime.");
		}

		SimulationRunResult proxy = runParkingProxy(
			utils.getOutputDirectory() + "/performance-proxy",
			PERFORMANCE_PARKING_SPOTS_PER_LINK, lastIteration,
			PERFORMANCE_POPULATION_MULTIPLIER, PERFORMANCE_QSIM_END_TIME);

		log.info("QSim runtime for parking proxy over {} iterations: {} ms total, {} ms/iteration",
			PERFORMANCE_ITERATIONS, proxy.qsimRuntimeMillis(),
			proxy.qsimRuntimeMillis() / (double) PERFORMANCE_ITERATIONS);
		assertTrue(proxy.qsimRuntimeMillis() > 0,
			"The parking proxy must produce a positive QSim runtime.");
	}

	@Test
	void noParking() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"));
		config.controller().setOutputDirectory("./chessboardNoParkingOutput");
		//config.global().setRandomSeed(randomSeed);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.scoring().setWriteExperiencedPlans(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();

	}

	private TripTravelTimeResult runExplicitParkingSearch(
		String outputDirectory,
		ParkingSearchStrategy parkingSearchStrategy,
		double parkingSpotsPerLink) {
		return runExplicitParkingSearch(
			outputDirectory, parkingSearchStrategy, parkingSpotsPerLink, 0, 1, 50 * 3600).travelTimes();
	}

	private SimulationRunResult runExplicitParkingSearch(
		String outputDirectory,
		ParkingSearchStrategy parkingSearchStrategy,
		double parkingSpotsPerLink,
		int lastIteration,
		int populationMultiplier,
		double qsimEndTime) {
		Config config = createComparisonConfig(
			outputDirectory, parkingSearchStrategy, lastIteration, qsimEndTime);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareBenchmarkScenario(scenario, populationMultiplier);
		addParkingFacilities(scenario, parkingSpotsPerLink);
		Controller controller = ControllerUtils.createController(scenario);
		QsimRuntimeCollector qsimRuntimes = new QsimRuntimeCollector();
		//add car travel time comparison
		TripTravelTimeHandler travelTimes = new TripTravelTimeHandler();
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(travelTimes);
				addControlerListenerBinding().toInstance(qsimRuntimes);
			}
		});
		//from MATSim libs
		SetupParking.installParkingModules(controller);

		controller.run();

		return new SimulationRunResult(travelTimes.result(), qsimRuntimes.totalRuntimeMillis());
	}

	private TripTravelTimeResult runParkingProxy(String outputDirectory, int parkingSpotsPerLink) {
		return runParkingProxy(outputDirectory, parkingSpotsPerLink, 0, 1, 50 * 3600).travelTimes();
	}

	private SimulationRunResult runParkingProxy(
		String outputDirectory,
		int parkingSpotsPerLink,
		int lastIteration,
		int populationMultiplier,
		double qsimEndTime) {
		Config config = createComparisonConfig(
			outputDirectory, ParkingSearchStrategy.NearestParkingSpot, lastIteration, qsimEndTime);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareBenchmarkScenario(scenario, populationMultiplier);
		QsimRuntimeCollector qsimRuntimes = new QsimRuntimeCollector();

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, parkingSpotsPerLink);
			}
		}

		Controler controller = new Controler(scenario);
		TripTravelTimeHandler travelTimes = new TripTravelTimeHandler();
		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding("ParkingOccupancyObserver").to(ParkingOccupancyObserver.class);
				addMobsimScopeEventHandlerBinding().to(ParkingOccupancyObserver.class);
				addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
				bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
				addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
			}
		});
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(travelTimes);
				addControlerListenerBinding().toInstance(qsimRuntimes);
				bind(ParkingOccupancyObserver.class).in(Singleton.class);
				bind(ParkingCapacityInitializer.class).to(PlanBasedParkingCapacityInitializerBerlin.class);
				bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
				bind(KernelDistance.class).toInstance(new ConstantKernelDistance(500));
				bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(0.11, -8.586));
				addControlerListenerBinding().to(ParkingOccupancyObserver.class);
				addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
			}
		});
		controller.run();

		return new SimulationRunResult(travelTimes.result(), qsimRuntimes.totalRuntimeMillis());
	}

	private void prepareBenchmarkScenario(Scenario scenario, int populationMultiplier) {
		// Only the performance benchmark requests a larger population. This keeps the smaller
		// behavioral comparisons on the original parking-search example scenario.
		if (populationMultiplier < 1) {
			throw new IllegalArgumentException("Population multiplier must be at least one.");
		}
		if (populationMultiplier == 1) {
			return;
		}

		List<NetworkTile> tiles = expandNetworkToFourTiles(scenario.getNetwork());
		replicatePopulation(scenario, populationMultiplier);
		distributePopulationAcrossTiles(scenario.getPopulation(), tiles);
	}

	private List<NetworkTile> expandNetworkToFourTiles(Network network) {
		// Take snapshots because the copies are added to the same network while iterating.
		List<Node> originalNodes = new ArrayList<>(network.getNodes().values());
		List<Link> originalLinks = new ArrayList<>(network.getLinks().values());

		double[] bounds = NetworkUtils.getBoundingBox(originalNodes);
		double horizontalOffset = bounds[2] - bounds[0];
		double verticalOffset = bounds[3] - bounds[1];

		// Tile 0 is the original. The copies form a 2x2 board: right, below, and below-right.
		List<NetworkTile> tiles = List.of(
			new NetworkTile(0, 0, 0),
			new NetworkTile(1, horizontalOffset, 0),
			new NetworkTile(2, 0, -verticalOffset),
			new NetworkTile(3, horizontalOffset, -verticalOffset)
		);

		// Adjacent copies share their boundary coordinates. Reusing those nodes joins the four
		// boards into one routable network without gaps or artificial connector links.
		Map<Coord, Node> nodesByCoordinate = new HashMap<>();
		for (Node node : originalNodes) {
			nodesByCoordinate.put(node.getCoord(), node);
		}

		for (NetworkTile tile : tiles.subList(1, tiles.size())) {
			Map<Id<Node>, Node> copiedNodes = new HashMap<>();
			for (Node original : originalNodes) {
				Coord shiftedCoord = tile.shift(original.getCoord());
				Node copy = nodesByCoordinate.get(shiftedCoord);
				if (copy == null) {
					copy = network.getFactory().createNode(tile.nodeId(original.getId()), shiftedCoord);
					original.getAttributes().getAsMap().forEach(copy.getAttributes()::putAttribute);
					network.addNode(copy);
					nodesByCoordinate.put(shiftedCoord, copy);
				}
				copiedNodes.put(original.getId(), copy);
			}

			for (Link original : originalLinks) {
				copyLink(network, original, tile, copiedNodes);
			}
		}

		log.info("Expanded chessboard network from {} to {} links and from {} to {} nodes",
			originalLinks.size(), network.getLinks().size(),
			originalNodes.size(), network.getNodes().size());
		return tiles;
	}

	private void copyLink(
		Network network, Link original, NetworkTile tile, Map<Id<Node>, Node> copiedNodes) {
		Link copy = network.getFactory().createLink(
			tile.linkId(original.getId()),
			copiedNodes.get(original.getFromNode().getId()),
			copiedNodes.get(original.getToNode().getId()));

		// Preserve every network property that can affect QSim runtime or routing. Only the link ID
		// and its endpoints differ between tiles.
		copy.setAllowedModes(new HashSet<>(original.getAllowedModes()));
		copy.setCapacity(original.getCapacity());
		copy.setFreespeed(original.getFreespeed());
		copy.setLength(original.getLength());
		copy.setNumberOfLanes(original.getNumberOfLanes());
		original.getAttributes().getAsMap().forEach(copy.getAttributes()::putAttribute);
		network.addLink(copy);
	}

	private void distributePopulationAcrossTiles(Population population, List<NetworkTile> tiles) {
		Random random = MatsimRandom.getLocalInstance();
		int[] personsPerTile = new int[tiles.size()];
		// MATSim's random generator deliberately remains in its current state. Consequently, each
		// approach gets an independent but approximately even spatial population distribution.
		for (Person person : population.getPersons().values()) {
			NetworkTile tile = tiles.get(random.nextInt(tiles.size()));
			personsPerTile[tile.index()]++;

			for (Plan plan : person.getPlans()) {
				movePlanToTile(plan, tile);
			}
		}

		log.info("Distributed {} agents across the four chessboards: {}", population.getPersons().size(),
			java.util.Arrays.toString(personsPerTile));
	}

	private void movePlanToTile(Plan plan, NetworkTile tile) {
		for (var element : plan.getPlanElements()) {
			if (element instanceof Activity activity) {
				if (activity.getCoord() != null) {
					activity.setCoord(tile.shift(activity.getCoord()));
				}
				if (activity.getLinkId() != null) {
					activity.setLinkId(tile.linkId(activity.getLinkId()));
				}
			}
		}

		// Routes copied with the plans still contain link IDs from the original tile. Clearing them
		// lets MATSim route each plan through the tile containing its shifted activities.
		PopulationUtils.resetRoutes(plan);
	}

	private void replicatePopulation(Scenario scenario, int multiplier) {
		Population population = scenario.getPopulation();
		// Keep a snapshot of the 100 source persons. Otherwise newly created copies would themselves
		// be copied again while iterating over the growing population.
		List<Person> originalPersons = new ArrayList<>(population.getPersons().values());
		for (int copy = 1; copy < multiplier; copy++) {
			for (Person original : originalPersons) {
				Person clone = population.getFactory().createPerson(
					Id.createPersonId(original.getId() + "_copy_" + copy));
				original.getAttributes().getAsMap().forEach(clone.getAttributes()::putAttribute);

				for (Plan originalPlan : original.getPlans()) {
					Plan clonedPlan = population.getFactory().createPlan();
					PopulationUtils.copyFromTo(originalPlan, clonedPlan);
					clone.addPlan(clonedPlan);
					if (originalPlan == original.getSelectedPlan()) {
						clone.setSelectedPlan(clonedPlan);
					}
				}
				population.addPerson(clone);
			}
		}

		log.info("Replicated population by factor {}: {} agents", multiplier, population.getPersons().size());
	}

	private record NetworkTile(int index, double deltaX, double deltaY) {
		private Coord shift(Coord coord) {
			return new Coord(coord.getX() + deltaX, coord.getY() + deltaY);
		}

		private Id<Node> nodeId(Id<Node> originalId) {
			return Id.createNodeId(originalId + "_tile_" + index);
		}

		private Id<Link> linkId(Id<Link> originalId) {
			return index == 0 ? originalId : Id.createLinkId(originalId + "_tile_" + index);
		}
	}

	private Config createComparisonConfig(
		String outputDirectory,
		ParkingSearchStrategy parkingSearchStrategy,
		int lastIteration,
		double qsimEndTime) {
		Config config = ConfigUtils.loadConfig("parkingsearch/config.xml", new ParkingSearchConfigGroup());
		ParkingSearchConfigGroup parkingSearchConfig =
			ConfigUtils.addOrGetModule(config, ParkingSearchConfigGroup.class);
		parkingSearchConfig.setParkingSearchStrategy(parkingSearchStrategy);
		// Without this flag, a vehicle may park on a link after its facility is full. Enforcing
		// facility-only parking makes the configured capacities meaningful in the scarcity test.
		parkingSearchConfig.setCanParkOnlyAtFacilities(true);
		parkingSearchConfig.setParkDuration(EXPLICIT_PARKING_INTERACTION_TIME);
		config.controller().setLastIteration(lastIteration);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setOutputDirectory(outputDirectory);
		config.controller().setCreateGraphs(false);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.scoring().setWriteExperiencedPlans(true);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setEndTime(qsimEndTime);
		ScoringConfigGroup.ActivityParams parkingParams = new ScoringConfigGroup.ActivityParams("parking");
		parkingParams.setScoringThisActivityAtAll(false);
		parkingParams.setTypicalDuration(3600.0);
		config.scoring().addActivityParams(parkingParams);
		return config;
	}

	private void addParkingFacilities(Scenario scenario, double capacity) {
		// The example config already contains four curbside facilities. Replace them so that the
		// explicit search receives exactly the same supply as the proxy: one capacity value per car link.
		ActivityFacilities facilities = scenario.getActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		facilities.getFacilities().clear();

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				Id<ActivityFacility> facilityId = Id.create(link.getId() + "_parking", ActivityFacility.class);
				ActivityFacility facility = factory.createActivityFacility(facilityId, link.getId());
				facility.setCoord(link.getCoord());

				ActivityOption parking = factory.createActivityOption("parking");
				parking.setCapacity(capacity);
				facility.addActivityOption(parking);
				facilities.addActivityFacility(facility);
			}
		}
	}

	/**
	 * Measures complete trips between main activities instead of only the car leg. Explicit
	 * parking search inserts parking and "car interaction" stage activities, and may add walking
	 * or cruising outside the original car leg. Ignoring those stage activities as trip boundaries
	 * ensures that all parking-related time remains part of the measured door-to-door trip.
	 */
	private static final class TripTravelTimeHandler
		implements ActivityEndEventHandler, ActivityStartEventHandler {

		private final Map<Id<Person>, Double> departures = new HashMap<>();
		private double totalTravelTime;
		private int arrivals;

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (isMainActivity(event.getActType())) {
				departures.put(event.getPersonId(), event.getTime());
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (!isMainActivity(event.getActType())) {
				return;
			}
			Double departure = departures.remove(event.getPersonId());
			if (departure != null) {
				totalTravelTime += event.getTime() - departure;
				arrivals++;
			}
		}

		private boolean isMainActivity(String activityType) {
			// "parking" is not registered as a standard MATSim stage activity in this setup.
			return !"parking".equals(activityType) && !TripStructureUtils.isStageActivityType(activityType);
		}

		TripTravelTimeResult result() {
			assertTrue(arrivals > 0, "The scenario must contain completed trips.");
			return new TripTravelTimeResult(totalTravelTime / arrivals, arrivals);
		}
	}

	/** Measures only time spent in QSim, excluding controller setup, scoring and output writing. */
	private static final class QsimRuntimeCollector
		implements BeforeMobsimListener, AfterMobsimListener {

		private long startNanos;
		private long totalRuntimeNanos;

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			startNanos = System.nanoTime();
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			totalRuntimeNanos += System.nanoTime() - startNanos;
		}

		long totalRuntimeMillis() {
			return totalRuntimeNanos / 1_000_000;
		}
	}

	private record TripTravelTimeResult(double meanTravelTime, int completedLegs) {
	}

	private record SimulationRunResult(TripTravelTimeResult travelTimes, long qsimRuntimeMillis) {
	}

}
