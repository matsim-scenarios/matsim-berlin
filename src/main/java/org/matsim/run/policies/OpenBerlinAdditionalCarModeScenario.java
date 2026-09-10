package org.matsim.run.policies;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.*;

import static org.matsim.run.policies.MobilityToGridScenariosUtils.AVERAGE;
import static org.matsim.run.policies.MobilityToGridScenariosUtils.RICH;

/**
 * Berlin scenario including the possibility to add another car mode with altered cost.
 * The mode may be used to simulate an alternative car mode (e.g. different drive train tech) which is more expensive than "usual car".
 * All necessary configs will be made in this class.
 */
public class OpenBerlinAdditionalCarModeScenario extends OpenBerlinScenario {
	public static final String CAR_EXPENSIVE = "carExpensive";

	private static final Logger log = LogManager.getLogger(OpenBerlinAdditionalCarModeScenario.class);
	private static final String SMC_RICH = "RichSubtourModeChoice";
	private static List<String> richSmcAvailableModes;
	private static List<String> richSmcChainBasedModes;
	private static SubtourModeChoice richSubtourModeChoice;

	private static final SplittableRandom splittableRandom = new SplittableRandom(15);

//	@CommandLine.Option(names = "--rich-agents-percentage", description = "Percentage of agents to be tagged as rich. Read as 'X% richest agents'. Value between 0-1.", defaultValue = "0.1")
//	private static double pctForTagging;

	@CommandLine.Option(names = "--expensive-distance-cost", description = "Defines to which value the monetary distance rate for the new, more expensive car mode is set. " +
		"Default = -0.0003Eu/m, which is double the usual distance cost of car.", defaultValue = "-0.0003")
	private static double expensiveMonetaryDistanceRate;

	@CommandLine.Option(names = "--sigma", description = "Sigma value for uniform distribution of agent wise modal asc for carExpensive. " +
		"Distribution is: y = (randomDouble[0,1] - 0.5) * 2 * sigma + mean.", defaultValue = "3.0")
	private static double sigma;

	@CommandLine.Option(names = "--mean", description = "Mean value for uniform distribution of agent wise modal asc for carExpensive. " +
		"Distribution is: y = (randomDouble[0,1] - 0.5) * 2 * sigma + mean.", defaultValue = "0.0")
	private static double mean;

//	@CommandLine.Option(names = "--switch-car-trips-handling", description = "Decides whether for the initial demand, all car trips are changed to carExpensive trips or only those car trips" +
//		"of agents tagged as rich.")
//	static ManualModeSwitchForCarTripsHandling switchForCarTripsHandling = ManualModeSwitchForCarTripsHandling.SWITCH_ALL_CAR_TRIPS;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		configureAdditionalCarModeInConfig(config);

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		configureAdditionalCarModeInScenario(scenario);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);

		configureAdditionalCarModeInController(controler);
	}

	/**
	 * Make all necessary configs for the additional car mode in config.
	 */
	static void configureAdditionalCarModeInConfig(Config config) {
//		if (pctForTagging < 0 || pctForTagging > 1.) {
//			log.fatal("you defined --rich-agents-percentage as {}, but the value should be between 0-1. Aborting!", pctForTagging);
//			throw new IllegalStateException("");
//		}

//		add new car mode to qsim modes
		Collection<String> mainModes = new HashSet<>(config.qsim().getMainModes());
		mainModes.add(CAR_EXPENSIVE);
		config.qsim().setMainModes(mainModes);

//		add new car mode to routed modes
		Collection<String> routingModes = new HashSet<>(config.routing().getNetworkModes());
		routingModes.add(CAR_EXPENSIVE);
		config.routing().setNetworkModes(routingModes);

//		add scoring params for new car mode
		ScoringConfigGroup.ModeParams carParams = config.scoring().getModes().get(TransportMode.car);
		ScoringConfigGroup.ModeParams carExpensiveParams = config.scoring().getOrCreateModeParams(CAR_EXPENSIVE);

		if (expensiveMonetaryDistanceRate >= carParams.getMonetaryDistanceRate()) {
			log.fatal("You defined a monetary distance rate for the new expensive car mode of {}, " +
				"which is equal or cheaper than the monetary distance rate of 'normal' car ({}). Aborting!", expensiveMonetaryDistanceRate, carParams.getMonetaryDistanceRate());
			throw new IllegalStateException("");
		}

		carExpensiveParams.setConstant(carParams.getConstant());
		carExpensiveParams.setDailyMonetaryConstant(carParams.getDailyMonetaryConstant());
		carExpensiveParams.setMarginalUtilityOfTraveling(carParams.getMarginalUtilityOfTraveling());
		carExpensiveParams.setMonetaryDistanceRate(expensiveMonetaryDistanceRate);

//		add new car mode to smc chainBasedModes and available modes
		List<String> chainBasedModes = new ArrayList<>(List.of(config.subtourModeChoice().getChainBasedModes()));
		chainBasedModes.add(CAR_EXPENSIVE);
		config.subtourModeChoice().setChainBasedModes(chainBasedModes.toArray(new String[0]));

//		richSmcChainBasedModes = new ArrayList<>(chainBasedModes);
//		richSmcChainBasedModes.remove(TransportMode.car);

		List<String> smcModes = new ArrayList<>(List.of(config.subtourModeChoice().getModes()));
		smcModes.add(CAR_EXPENSIVE);
		config.subtourModeChoice().setModes(smcModes.toArray(new String[0]));

//		richSmcAvailableModes = new ArrayList<>(smcModes);
//		richSmcAvailableModes.remove(TransportMode.car);

//		add new car mode to analyzed modes for tt calculation
		Set<String> analyzedModes = new HashSet<>(config.travelTimeCalculator().getAnalyzedModes());
		analyzedModes.add(CAR_EXPENSIVE);
		config.travelTimeCalculator().setAnalyzedModes(analyzedModes);

//		first copy all, then adapt smc for subpop rich to verlin smc
//		Set<ReplanningConfigGroup.StrategySettings> copiedStrategies = new HashSet<>();
//		for (ReplanningConfigGroup.StrategySettings strategy : config.replanning().getStrategySettings()) {
//			if (strategy.getSubpopulation().equals("person")) {
////				copy strategy for subpop person to subpop rich except SMC
//				if (strategy.getStrategyName().equals("SubtourModeChoice")) {
//					continue;
//				}
//
//				ReplanningConfigGroup.StrategySettings copy = new ReplanningConfigGroup.StrategySettings();
//				copy.setStrategyName(strategy.getStrategyName());
//				copy.setSubpopulation(RICH);
//
////				set start weights for strategies as for subpop person
//				double weight = 0.;
//				if (strategy.getStrategyName().equals("ChangeExpBeta")) {
//					weight = 1.;
//				} else {
//					weight = 0.15;
//				}
//				copy.setWeight(weight);
//				copiedStrategies.add(copy);
//			}
//		}
//		copiedStrategies.forEach(s -> config.replanning().addStrategySettings(s));
//
////		add car expensive smc as strategy for subpop rich
//		ReplanningConfigGroup.StrategySettings richSmc = new ReplanningConfigGroup.StrategySettings();
//		richSmc.setStrategyName(SMC_RICH);
//		richSmc.setSubpopulation(RICH);
//		richSmc.setWeight(0.15);
//		config.replanning().addStrategySettings(richSmc);
	}

	/**
	 * add vehicle type for new car mode.
	 * add new mode to network.
	 * tag rich agents.
	 */
	static void configureAdditionalCarModeInScenario(Scenario scenario) {
		//		add vehicle type for car expensive
		EngineInformation carEngineInfo = scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.car, VehicleType.class)).getEngineInformation();
		VehicleType carExpensiveType = VehicleUtils.createVehicleType(Id.create(CAR_EXPENSIVE, VehicleType.class));
		carExpensiveType.setNetworkMode(CAR_EXPENSIVE);
		EngineInformation carExpensiveEngineInfo = carExpensiveType.getEngineInformation();

		VehicleUtils.setHbefaVehicleCategory(carExpensiveEngineInfo, HbefaVehicleCategory.PASSENGER_CAR.toString());
//		car expensive is assumed to be synthetic fuels, which exhaust the same pollutants as conventional ICE vehicles
		VehicleUtils.setHbefaTechnology(carExpensiveEngineInfo, "petrol (4S)");
		VehicleUtils.setHbefaSizeClass(carExpensiveEngineInfo, AVERAGE);
		VehicleUtils.setHbefaEmissionsConcept(carExpensiveEngineInfo, AVERAGE);

//		as car expensive is assumed to be synthetic fuels, we assume that "normal car" is bev
		VehicleUtils.setHbefaTechnology(carEngineInfo, MobilityToGridScenariosUtils.Hbefa41Technology.ELECTRICITY.toString().toLowerCase(Locale.ROOT));
		scenario.getVehicles().addVehicleType(carExpensiveType);

//		add carExpensive as allowed mode when car is allowed
		for (Link link : scenario.getNetwork().getLinks().values()) {
//			skip pt links
			if (link.getId().toString().startsWith("pt_")) {
				continue;
			}

			if (link.getAllowedModes().contains(TransportMode.car)) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add(CAR_EXPENSIVE);
				link.setAllowedModes(modes);
			}
		}

//		distribute carExpensive agent wise asc
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (PersonUtils.getModeConstants(person) != null &&
				!PersonUtils.getModeConstants(person).containsKey(CAR_EXPENSIVE)) {
				// linear
//					mean=0.0; sigma=3.0 as for bike; calculation same as in class AddPersonSpecificAscsStreamReading
				double carExpensiveModeConstant = (splittableRandom.nextDouble() - 0.5) * 2 * sigma + mean;
				Map<String, String> modeConstants = new HashMap<>(PersonUtils.getModeConstants(person));
				modeConstants.put(CAR_EXPENSIVE, String.valueOf(carExpensiveModeConstant));
				PersonUtils.setModeConstants(person, modeConstants);
			}
		}

//		RoutingModeMainModeIdentifier mainModeIdentifier = new RoutingModeMainModeIdentifier();

//		tag highest X% income agents
//		List<? extends Person> sorted = scenario.getPopulation().getPersons().values().stream()
//			.filter(p -> p.getAttributes().getAttribute("subpopulation").equals("person"))
//			.sorted(Comparator.comparingDouble(PersonUtils::getIncome).reversed())
//			.toList();
//
//		int count = (int) Math.ceil(sorted.size() * pctForTagging);
//
//		List<? extends Person> financiallyFortunate = sorted.subList(0, count);
//
//		for (Person rich : financiallyFortunate) {
//			scenario.getPopulation().getPersons()
//				.get(rich.getId()).getAttributes().putAttribute("subpopulation", RICH);
//		}
//
//		Set<String> consideredSubpopulationsForManualModeSwitch = new HashSet<>();
//		consideredSubpopulationsForManualModeSwitch.add("person");
//		consideredSubpopulationsForManualModeSwitch.add(RICH);
//
//		if (switchForCarTripsHandling == ManualModeSwitchForCarTripsHandling.SWITCH_ONLY_CAR_TRIPS_OF_RICH_AGENTS) {
//			consideredSubpopulationsForManualModeSwitch.remove("person");
//		}
//
//		//		switch all car legs to new car mode
//		for (Person person : scenario.getPopulation().getPersons().values()) {
//			if (!consideredSubpopulationsForManualModeSwitch.contains(person.getAttributes().getAttribute("subpopulation").toString())) {
//				continue;
//			}
//
//			for (Plan p : person.getPlans()) {
//				List<PlanElement> planElements = p.getPlanElements();
//				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(p);
//
//				for (TripStructureUtils.Trip trip : trips) {
//					List<PlanElement> fullTrip =
//						planElements.subList(
//							planElements.indexOf( trip.getOriginActivity() ) + 1,
//							planElements.indexOf( trip.getDestinationActivity() ));
//					String mode = mainModeIdentifier.identifyMainMode(fullTrip);
//
//					if (mode.equals(TransportMode.car)) {
//						fullTrip.clear();
//						Leg leg = PopulationUtils.createLeg(CAR_EXPENSIVE);
//						TripStructureUtils.setRoutingMode(leg, CAR_EXPENSIVE);
//						fullTrip.add(leg);
//						if (fullTrip.size() != 1) throw new RuntimeException(fullTrip.toString());
//					}
//				}
//			}
//		}
	}

	/**
	 * Add separate carExpensive SMC without normal car and bind.
	 */
	static void configureAdditionalCarModeInController(Controler controler) {
		//		add smc without "normal" car mode to be used for subpopulation "rich"
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
////				define new SMC strategy
//				this.addPlanStrategyBinding(SMC_RICH).toProvider(new Provider<PlanStrategy>() {
//					@Override
//					public PlanStrategy get() {
//						PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
//
//						Config newConfig = new Config();
//						SubtourModeChoiceConfigGroup richSmcCfg = new SubtourModeChoiceConfigGroup();
//						richSmcCfg.setBehavior(controler.getConfig().subtourModeChoice().getBehavior());
//						richSmcCfg.setChainBasedModes(richSmcChainBasedModes.toArray(new String[0]));
//						richSmcCfg.setConsiderCarAvailability(controler.getConfig().subtourModeChoice().considerCarAvailability());
//						richSmcCfg.setCoordDistance(controler.getConfig().subtourModeChoice().getCoordDistance());
//						richSmcCfg.setModes(richSmcAvailableModes.toArray(new String[0]));
//						richSmcCfg.setProbaForRandomSingleTripMode(controler.getConfig().subtourModeChoice().getProbaForRandomSingleTripMode());
//						newConfig.addModule(richSmcCfg);
//
////						the config here must not be the controler.getConfig() because then car is taken back into the mode choice set
//						richSubtourModeChoice = new SubtourModeChoice(controler.getConfig().global(), richSmcCfg,
//							new PermissibleModesCalculatorImpl(newConfig));
//						builder.addStrategyModule(richSubtourModeChoice);
//
//						builder.addStrategyModule(new AbstractMultithreadedModule(controler.getConfig().global()) {
//							@Override
//							public PlanAlgorithm getPlanAlgoInstance() {
//								return richSubtourModeChoice.getPlanAlgoInstance();
//							}
//						});
//
//						return builder.build();
//					}
//				});
//				// Normally this is bound with the default subtour mode choice, because we use our own variant this is bound again here
//				bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorImpl.class);
//			}
//		});

//		travel time binding for carExpensive
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(CAR_EXPENSIVE).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(CAR_EXPENSIVE).to(carTravelDisutilityFactoryKey());
			}
		});
	}

	private enum ManualModeSwitchForCarTripsHandling {SWITCH_ALL_CAR_TRIPS, SWITCH_ONLY_CAR_TRIPS_OF_RICH_AGENTS}
}
