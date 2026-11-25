package org.matsim.run;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.OpenBerlinBikeNetworkScenario;
import org.matsim.run.policies.OpenBerlinBikeNetworkScenario.*;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idea of this test:
 * I want to prove that bicycle tts are not updated when bicycle is not added to analyzed modes in TravelTimeCalculator ConfigGroup.
 * So, first, cyclist is routed on empty link, cars somewhere else.
 * After that, cars are routed onto the link used by the cyclist.
 * If there is no update on the tt of the cyclist without, but with bike in analyzed included in analyzedModes, we have our prove.
 */
class BicycleOnNetworkTestEquil {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@TempDir
	private Path p;

	private String inputPopulationPath;
	private String inputNetworkPath;
	private Set<BikeConfig> configurations;

	private final Id<Link> bikeStartLinkId = Id.createLinkId("6");
	private final Id<Link> bikeEndLinkId = Id.createLinkId("15");
	private final Id<Link> carStartLinkId = Id.createLinkId("5");
	private final Id<Link> carEndLinkId = Id.createLinkId("14");
	private final Id<Link> generalStartLinkId = Id.createLinkId("1");
	private final Id<Link> generalEndLinkId = Id.createLinkId("20");

	@BeforeEach
	void setUp() {
		// Initialize inputPath after p is injected
		inputPopulationPath = p.resolve("test-population.xml.gz").toString();
		inputNetworkPath = p.resolve("test-network.xml.gz").toString();

		configurations = Set.of(
			new BikeConfig(
				BikeSpeedHandling.BICYCLE_LINK_SPEED_CALCULATOR,
				BikeTravelTimeHandling.BICYCLE_TRAVEL_TIME,
				BikeTravelDisutilityHandling.ONLY_TIME_DEPENDENT_DISUTILITY
			),
			new BikeConfig(
				BikeSpeedHandling.BICYCLE_LINK_SPEED_CALCULATOR,
				BikeTravelTimeHandling.BICYCLE_TRAVEL_TIME,
				BikeTravelDisutilityHandling.OTHER
			),
			new BikeConfig(
				BikeSpeedHandling.BICYCLE_LINK_SPEED_CALCULATOR,
				BikeTravelTimeHandling.NO_BICYCLE_TRAVEL_TIME,
				BikeTravelDisutilityHandling.ONLY_TIME_DEPENDENT_DISUTILITY
			),
			new BikeConfig(
				BikeSpeedHandling.BICYCLE_LINK_SPEED_CALCULATOR,
				BikeTravelTimeHandling.NO_BICYCLE_TRAVEL_TIME,
				BikeTravelDisutilityHandling.OTHER
			),
// BicycleTravelTime needs BicycleLinkSpeedCalculator
//			new BikeConfig(
//				BikeSpeedHandling.NO_BICYCLE_LINK_SPEED_CALCULATOR,
//				BikeTravelTimeHandling.BICYCLE_TRAVEL_TIME,
//				BikeTravelDisutilityHandling.ONLY_TIME_DEPENDENT_DISUTILITY
//			),
//			new BikeConfig(
//				BikeSpeedHandling.NO_BICYCLE_LINK_SPEED_CALCULATOR,
//				BikeTravelTimeHandling.BICYCLE_TRAVEL_TIME,
//				BikeTravelDisutilityHandling.OTHER
//			),
			new BikeConfig(
				BikeSpeedHandling.NO_BICYCLE_LINK_SPEED_CALCULATOR,
				BikeTravelTimeHandling.NO_BICYCLE_TRAVEL_TIME,
				BikeTravelDisutilityHandling.ONLY_TIME_DEPENDENT_DISUTILITY
			),
			new BikeConfig(
				BikeSpeedHandling.NO_BICYCLE_LINK_SPEED_CALCULATOR,
				BikeTravelTimeHandling.NO_BICYCLE_TRAVEL_TIME,
				BikeTravelDisutilityHandling.OTHER
			)
		);
	}

//	@Test
//	void testWithoutBikeInTravelTimeCalculatorBikeOnly() {
////		Set<Double> pceValues = Set.of(0.0, 0.01, 0.1, 0.2, 0.3);
//		Set<Double> pceValues = Set.of(0.3);
//
//		for (Double pceValue : pceValues) {
//
////			URL context = ExamplesUtils.getTestScenarioURL("equil");
////			Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));
//
//
//			String yamlPath = getYamlPathFromCluster(pceValue);
//
//			Config config = ConfigUtils.loadConfig("./input/v6.4/berlin-v6.4.config.xml");
//
//			createTestPopulation1Cyclist(config);
//
//			int code = MATSimApplication.execute(OpenBerlinBikeNetworkScenario.class, config,
//				"--3pct",
//				"--output", utils.getOutputDirectory() + "/output-bike-pce-" + pceValue,
//				"--iterations", "5",
//				"--config:plans.inputPlansFile", inputPopulationPath,
//				"--config:qsim.numberOfThreads", "2",
//				"--config:global.numberOfThreads", "2",
//				"--config:simwrapper.defaultDashboards", "disabled",
//				"--bike-handling", "ROUTED_ON_NETWORK_IN_QSIM",
//				"--bike-pce", pceValue.toString(),
//				"--yaml", yamlPath
//			);
//
//			assertThat(code).isZero();
//		}
//	}

	@Test
	void testWithoutBikeInTravelTimeCalculatorBikeWithCars() {
		for (BikeConfig bikeConfig : this.configurations) {
			//		Set<Double> pceValues = Set.of(0.0, 0.01, 0.1, 0.2, 0.3);
			Set<Double> pceValues = Set.of(0.3);

			for (Double pceValue : pceValues) {
				String yamlPath = getYamlPathFromCluster(pceValue);

				Config config = ConfigUtils.loadConfig("./input/v6.4/berlin-v6.4.config.xml");
				config.controller().setWriteEventsInterval(1);
				config.controller().setWritePlansInterval(1);

//			modify equil network and set it as input network
				modifyTestNetwork();
				config.network().setInputFile(inputNetworkPath);
				config.facilities().setInputFile(null);
				config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile);

				config.replanning().addStrategySettings(
					new ReplanningConfigGroup.StrategySettings()
						.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
						.setWeight(1.0)
						.setSubpopulation("cyclists")
				);
				config.replanning().setMaxAgentPlanMemorySize(1);

				config.transit().setUseTransit(false);

				createTestPopulationWith20Cars(config);

				int code = MATSimApplication.execute(OpenBerlinBikeNetworkScenario.class, config,
					"--3pct",
					"--output", utils.getOutputDirectory() + "/without-bike-in-analyzed-modes/output-bike-pce-" + pceValue + "-" + bikeConfig.speedHandling + "-" + bikeConfig.disutilityHandling + "-" + bikeConfig.travelTimeHandling(),
					"--iterations", "5",
					"--config:plans.inputPlansFile", inputPopulationPath,
					"--config:simwrapper.defaultDashboards", "disabled",
					"--bike-handling", "ROUTED_ON_NETWORK_IN_QSIM",
					"--bike-pce", pceValue.toString(),
					"--bike-speed-handling", bikeConfig.speedHandling.toString(),
					"--bike-travel-disutility-handling", bikeConfig.disutilityHandling.toString(),
					"--bike-travel-time-handling", bikeConfig.travelTimeHandling.toString(),
					"--yaml", yamlPath
				);
				assertThat(code).isZero();
			}
		}
	}

//	@Test
//	void testWithBikeInTravelTimeCalculatorBikeOnly() {
////		Set<Double> pceValues = Set.of(0.0, 0.01, 0.1, 0.2, 0.3);
//		Set<Double> pceValues = Set.of(0.3);
//
//		for (Double pceValue : pceValues) {
//			String yamlPath = getYamlPathFromCluster(pceValue);
//
//			Config config = ConfigUtils.loadConfig("./input/v6.4/berlin-v6.4.config.xml");
//
//			Set<String> analyzedModes = new HashSet<>(config.travelTimeCalculator().getAnalyzedModes());
//			analyzedModes.add(TransportMode.bike);
//			config.travelTimeCalculator().setAnalyzedModes(analyzedModes);
//
//			createTestPopulation1Cyclist(config);
//
//			int code = MATSimApplication.execute(OpenBerlinBikeNetworkScenario.class, config,
//				"--3pct",
//				"--output", utils.getOutputDirectory() + "/output-bike-pce-" + pceValue,
//				"--iterations", "5",
//				"--config:plans.inputPlansFile", inputPopulationPath,
//				"--config:qsim.numberOfThreads", "2",
//				"--config:global.numberOfThreads", "2",
//				"--config:simwrapper.defaultDashboards", "disabled",
//				"--bike-handling", "ROUTED_ON_NETWORK_IN_QSIM",
//				"--bike-pce", pceValue.toString(),
//				"--yaml", yamlPath
//			);
//
//			assertThat(code).isZero();
//		}
//	}

	@Test
	void testWithBikeInTravelTimeCalculatorBikeWithCars() {
		for (BikeConfig bikeConfig : this.configurations) {
			//		Set<Double> pceValues = Set.of(0.0, 0.01, 0.1, 0.2, 0.3);
			Set<Double> pceValues = Set.of(0.3);

			for (Double pceValue : pceValues) {
				String yamlPath = getYamlPathFromCluster(pceValue);

				Config config = ConfigUtils.loadConfig("./input/v6.4/berlin-v6.4.config.xml");
				config.controller().setWriteEventsInterval(1);
				config.controller().setWritePlansInterval(1);

				Set<String> analyzedModes = new HashSet<>(config.travelTimeCalculator().getAnalyzedModes());
				analyzedModes.add(TransportMode.bike);
				config.travelTimeCalculator().setAnalyzedModes(analyzedModes);

//			modify equil network and set it as input network
				modifyTestNetwork();
				config.network().setInputFile(inputNetworkPath);
				config.facilities().setInputFile(null);
				config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile);

				config.replanning().addStrategySettings(
					new ReplanningConfigGroup.StrategySettings()
						.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
						.setWeight(1.0)
						.setSubpopulation("cyclists")
				);
				config.replanning().setMaxAgentPlanMemorySize(1);

				config.transit().setUseTransit(false);

				createTestPopulationWith20Cars(config);

				int code = MATSimApplication.execute(OpenBerlinBikeNetworkScenario.class, config,
					"--3pct",
					"--output", utils.getOutputDirectory() + "/with-bike-in-analyzed-modes/output-bike-pce-" + pceValue + "-" + bikeConfig.speedHandling + "-" + bikeConfig.disutilityHandling + "-" + bikeConfig.travelTimeHandling(),
					"--iterations", "5",
					"--config:plans.inputPlansFile", inputPopulationPath,
					"--config:simwrapper.defaultDashboards", "disabled",
					"--bike-handling", "ROUTED_ON_NETWORK_IN_QSIM",
					"--bike-pce", pceValue.toString(),
					"--bike-speed-handling", bikeConfig.speedHandling.toString(),
					"--bike-travel-disutility-handling", bikeConfig.disutilityHandling.toString(),
					"--bike-travel-time-handling", bikeConfig.travelTimeHandling.toString(),
					"--yaml", yamlPath
				);
				assertThat(code).isZero();
			}
		}
	}

	private void modifyTestNetwork() {
		Network network = NetworkUtils.readNetwork(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml").toString());

//			remove car from 1 connection to ensure that only bike is traversing it
//			add car to 2 connections, so we ensure that they a) start on the empty one and b) then reroute to one with cars
		for (Link l : network.getLinks().values()) {
			Set<String> modes = new HashSet<>();
			modes.addAll(l.getAllowedModes());

			if (Set.of(bikeStartLinkId, bikeEndLinkId).contains(l.getId())) {
//				modes.remove(TransportMode.car);
				modes.add(TransportMode.bike);
			}

			if (Set.of(carStartLinkId, carEndLinkId).contains(l.getId()) ||
				Set.of(generalEndLinkId, Id.createLinkId("21"), Id.createLinkId("22"), Id.createLinkId("23"), generalStartLinkId).contains(l.getId())) {
				modes.add(TransportMode.bike);
			}
			l.setAllowedModes(modes);
		}
		NetworkUtils.writeNetwork(network, inputNetworkPath);
	}

	private String getYamlPathFromCluster(double pceValue) {
		String yamlPath;

		int pce = (int) Math.round(pceValue * 100);

		switch (pce) {
			case 0 -> // 0.0
				yamlPath = "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/calib-pce-0.0/params/run4.yaml";
			case 1 -> // 0.01
				yamlPath = "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/calib-pce-0.01/params/run9.yaml";
			case 10 -> // 0.1
				yamlPath = "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/calib-pce-0.1/params/run7.yaml";
			case 20 -> // 0.2
				yamlPath = "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/calib-pce-0.2/params/run7.yaml";
			case 30 -> // 0.3
				yamlPath = "//sshfs.r/meinhardt@cluster-a.math.tu-berlin.de/net/ils/meinhardt/berlin-v6.4-bike-network-paper/bike-in-qsim/calib-pce-0.3/params/run8.yaml";
			default -> // unexpected value
				throw new IllegalArgumentException("Invalid pce: " + pceValue);
		}

		return yamlPath;
	}

	private void createTestPopulationWith20Cars(Config config) {
		Population population = PopulationUtils.createPopulation(config);
		PopulationFactory fac = population.getFactory();

//		home begin and work end of "split" in equil network
//		this is the fromNode from carStartLinkId and bikeStartLinkId; I did not want to load the network in here
		Coord homeCoord = new Coord(-20000.,0.);
		//		this is the toNode from carEndLinkId and bikeEndLinkId; I did not want to load the network in here
//		Coord workCoord = new Coord(0.,0.);

		for (int i = 0; i <= 20; i++) {
			Person carUser = fac.createPerson(Id.createPersonId("car_" + i));
			carUser.getAttributes().putAttribute("home_x", homeCoord.getX());
			carUser.getAttributes().putAttribute("home_y", homeCoord.getY());
			Plan plan = PopulationUtils.createPlan(carUser);

//			home at Wilhelmstr / Leipziger Str
			Activity home = fac.createActivityFromLinkId("home_2400", generalStartLinkId);
			home.setEndTime(3 * 3600);
//			Activity home2 = fac.createActivityFromCoord("home_2400", homeCoord);
//			home2.setEndTime(19 * 3600);
//			work at Wilhemstr 41; 2 links away from home coord
			Activity work = fac.createActivityFromLinkId("work_2400", generalEndLinkId);
			work.setEndTime(10 * 3600 + 25 * 60);

			Leg carLeg = fac.createLeg(TransportMode.car);
			carLeg.setRoute(RouteUtils.createNetworkRoute(List.of(generalStartLinkId, bikeStartLinkId, bikeEndLinkId, generalEndLinkId)));

			plan.addActivity(home);
			plan.addLeg(carLeg);
			plan.addActivity(work);
//			plan.addLeg(carLeg);
//			plan.addActivity(home2);

			carUser.addPlan(plan);
			PersonUtils.setIncome(carUser, 1000.);
			PersonUtils.setAge(carUser, 30);
			carUser.getAttributes().putAttribute("subpopulation", "person");
			population.addPerson(carUser);
		}

//		create 1 cyclist which should get stuck in traffic
		Person cyclist = fac.createPerson(Id.createPersonId("bike_" + 1));
		cyclist.getAttributes().putAttribute("home_x", homeCoord.getX());
		cyclist.getAttributes().putAttribute("home_y", homeCoord.getY());
		Plan plan = PopulationUtils.createPlan(cyclist);

//			home at Wilhelmstr / Leipziger Str
		Activity home = fac.createActivityFromCoord("home_2400", homeCoord);
		home.setEndTime(3 * 3600);
//		Activity home2 = fac.createActivityFromCoord("home_2400", homeCoord);
//		home2.setEndTime(19 * 3600);
//			work at Wilhemstr 41; 2 links away from home coord
		Activity work = fac.createActivityFromLinkId("work_2400", generalEndLinkId);
		work.setEndTime(10 * 3600 + 25 * 60);

		Leg bikeLeg = fac.createLeg(TransportMode.bike);
		bikeLeg.setRoute(RouteUtils.createNetworkRoute(List.of(generalStartLinkId, bikeStartLinkId, bikeEndLinkId, generalEndLinkId)));

		plan.addActivity(home);
		plan.addLeg(bikeLeg);
		plan.addActivity(work);
//		plan.addLeg(bikeLeg);
//		plan.addActivity(home2);

		cyclist.addPlan(plan);
		PersonUtils.setIncome(cyclist, 1000.);
		PersonUtils.setAge(cyclist, 30);
		cyclist.getAttributes().putAttribute("subpopulation", "cyclists");
		population.addPerson(cyclist);

		new PopulationWriter(population).write(this.inputPopulationPath);
	}

//	private void createTestPopulation1Cyclist(Config config) {
//		Population population = PopulationUtils.createPopulation(config);
//		PopulationFactory fac = population.getFactory();
//
//		Id.createLinkId("1");
//
//		Coord homeCoord = new Coord(797473.30,5826811.31);
////		workCoord taken from facility fbe48a5
//		Coord workCoord = new Coord(797620.5625,5826466.5);
//
////		for (int i = 0; i <= 20; i++) {
////			Person carUser = fac.createPerson(Id.createPersonId("car_" + i));
////			carUser.getAttributes().putAttribute("home_x", homeCoord.getX());
////			carUser.getAttributes().putAttribute("home_y", homeCoord.getY());
////			Plan plan = PopulationUtils.createPlan(carUser);
////
//////			home at Wilhelmstr / Leipziger Str
////			Activity home = fac.createActivityFromCoord("home_2400", homeCoord);
////			home.setEndTime(8 * 3600);
////			Activity home2 = fac.createActivityFromCoord("home_2400", homeCoord);
////			home2.setEndTime(19 * 3600);
//////			work at Wilhemstr 41; 2 links away from home coord
////			Activity work = fac.createActivityFromCoord("work_2400", workCoord);
////			work.setEndTime(17 * 3600 + 25 * 60);
////
////			Leg carLeg = fac.createLeg(TransportMode.car);
////
////			plan.addActivity(home);
////			plan.addLeg(carLeg);
////			plan.addActivity(work);
////			plan.addLeg(carLeg);
////			plan.addActivity(home2);
////
////			carUser.addPlan(plan);
////			PersonUtils.setIncome(carUser, 1000.);
////			PersonUtils.setAge(carUser, 30);
////			carUser.getAttributes().putAttribute("subpopulation", "person");
////			population.addPerson(carUser);
////		}
//
////		create 1 cyclist which should get stuck in traffic
//		Person cyclist = fac.createPerson(Id.createPersonId("bike_" + 1));
//		cyclist.getAttributes().putAttribute("home_x", homeCoord.getX());
//		cyclist.getAttributes().putAttribute("home_y", homeCoord.getY());
//		Plan plan = PopulationUtils.createPlan(cyclist);
//
////			home at Wilhelmstr / Leipziger Str
//		Activity home = fac.createActivityFromCoord("home_2400", homeCoord);
//		home.setEndTime(8 * 3600);
//		Activity home2 = fac.createActivityFromCoord("home_2400", homeCoord);
//		home2.setEndTime(19 * 3600);
////			work at Wilhemstr 41; 2 links away from home coord
//		Activity work = fac.createActivityFromCoord("work_2400", workCoord);
//		work.setEndTime(17 * 3600 + 25 * 60);
//
//		Leg bikeLeg = fac.createLeg(TransportMode.bike);
//
//		plan.addActivity(home);
//		plan.addLeg(bikeLeg);
//		plan.addActivity(work);
//		plan.addLeg(bikeLeg);
//		plan.addActivity(home2);
//
//		cyclist.addPlan(plan);
//		PersonUtils.setIncome(cyclist, 1000.);
//		PersonUtils.setAge(cyclist, 30);
//		cyclist.getAttributes().putAttribute("subpopulation", "cyclists");
//		population.addPerson(cyclist);
//
//		new PopulationWriter(population).write(this.inputPopulationPath);
//	}

	public record BikeConfig(
		BikeSpeedHandling speedHandling,
		BikeTravelTimeHandling travelTimeHandling,
		BikeTravelDisutilityHandling disutilityHandling
	) {}
}
