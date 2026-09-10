package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.*;

import static org.matsim.run.policies.MobilityToGridScenariosUtils.AVERAGE;

/**
 * Berlin scenario including the possibility to add another car mode with altered cost.
 * The mode may be used to simulate an alternative car mode (e.g. different drive train tech) which is more expensive than "usual car".
 * All necessary configs will be made in this class.
 */
public class OpenBerlinAdditionalCarModeScenario extends OpenBerlinScenario {
	public static final String CAR_EXPENSIVE = "carExpensive";

	private static final Logger log = LogManager.getLogger(OpenBerlinAdditionalCarModeScenario.class);

	private static final SplittableRandom splittableRandom = new SplittableRandom(15);

	@CommandLine.Option(names = "--expensive-distance-cost", description = "Defines to which value the monetary distance rate for the new, more expensive car mode is set. " +
		"Default = -0.0003Eu/m, which is double the usual distance cost of car.", defaultValue = "-0.0003")
	private static double expensiveMonetaryDistanceRate;

	@CommandLine.Option(names = "--sigma", description = "Sigma value for uniform distribution of agent wise modal asc for carExpensive. " +
		"Distribution is: y = (randomDouble[0,1] - 0.5) * 2 * sigma + mean.", defaultValue = "3.0")
	private static double sigma;

	@CommandLine.Option(names = "--mean", description = "Mean value for uniform distribution of agent wise modal asc for carExpensive. " +
		"Distribution is: y = (randomDouble[0,1] - 0.5) * 2 * sigma + mean.", defaultValue = "0.0")
	private static double mean;

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
	}

	/**
	 * Make all necessary configs for the additional car mode in config.
	 */
	static void configureAdditionalCarModeInConfig(Config config) {
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

		List<String> smcModes = new ArrayList<>(List.of(config.subtourModeChoice().getModes()));
		smcModes.add(CAR_EXPENSIVE);
		config.subtourModeChoice().setModes(smcModes.toArray(new String[0]));

//		add new car mode to analyzed modes for tt calculation
		Set<String> analyzedModes = new HashSet<>(config.travelTimeCalculator().getAnalyzedModes());
		analyzedModes.add(CAR_EXPENSIVE);
		config.travelTimeCalculator().setAnalyzedModes(analyzedModes);
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
	}
}
