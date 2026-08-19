package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
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

/**
 * Berlin scenario including the possibility to add eBike as available mode.
 * It is different to bike because it is faster (max. 25kmh) and has fix + distance based cost (similar to car).
 * All necessary configs will be made in this class.
 */
public class OpenBerlinEBikeScenario extends OpenBerlinScenario {
	public static final String E_BIKE = "eBike";

	private static final Logger log = LogManager.getLogger(OpenBerlinEBikeScenario.class);
	private static final SplittableRandom splittableRandom = new SplittableRandom(15);

//	TODO: decide on default
	@CommandLine.Option(names = "--ebike-distance-cost", description = "Defines to which value the monetary distance rate for ebike is set. " +
		"Default = -0.0003Eu/m. This is an approximation of energy cost needed to recharge the vehicle.", defaultValue = "-0.0003")
	private static double eBikeMonetaryDistanceRate;
	//	TODO: decide on default
	@CommandLine.Option(names = "--ebike-fix-cost", description = "Defines to which value the daily monetary constant for ebike is set. " +
		"Default = -0.0003Eu/m, which basically is purchase price / 7 year of usage / 250 days.", defaultValue = "-0.0003")
	private static double eBikeMonetaryConstant;
//	TODO: try out both and decide on default afterwards
	@CommandLine.Option(names = "--agent-wise-asc-handling", description = "Decides whether the agent wise asc for eBike is copied from bike or distributed individually.")
	static EBikeAgentWiseAscHandling eBikeAgentWiseAscHandling = EBikeAgentWiseAscHandling.FROM_BIKE;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		addEBikeInConfig(config);

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		addEBikeInScenario(scenario);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}

	/**
	 * Add eBike to all relevant config groups to route it on network + teleport it. (like bike).
	 */
	static void addEBikeInConfig(Config config) {
//		add eBike mode to routed modes
		Collection<String> routingModes = new HashSet<>(config.routing().getNetworkModes());
		routingModes.add(E_BIKE);
		config.routing().setNetworkModes(routingModes);

//		add scoring params for eBike mode
		ScoringConfigGroup.ModeParams bikeParams = config.scoring().getModes().get(TransportMode.bike);
		ScoringConfigGroup.ModeParams ebikeParams = config.scoring().getOrCreateModeParams(E_BIKE);

		if (eBikeMonetaryDistanceRate >= bikeParams.getMonetaryDistanceRate()) {
			log.fatal("You defined a monetary distance rate for the new expensive car mode of {}, " +
				"which is equal or cheaper than the monetary distance rate of 'normal' car ({}). Aborting!", eBikeMonetaryDistanceRate, bikeParams.getMonetaryDistanceRate());
			throw new IllegalStateException("");
		}

		ebikeParams.setConstant(bikeParams.getConstant());
		ebikeParams.setDailyMonetaryConstant(eBikeMonetaryConstant);
		ebikeParams.setMarginalUtilityOfTraveling(bikeParams.getMarginalUtilityOfTraveling());
		ebikeParams.setMonetaryDistanceRate(eBikeMonetaryDistanceRate);

//		add eBike to smc chainBasedModes and available modes
		List<String> chainBasedModes = new ArrayList<>(List.of(config.subtourModeChoice().getChainBasedModes()));
		chainBasedModes.add(E_BIKE);
		config.subtourModeChoice().setChainBasedModes(chainBasedModes.toArray(new String[0]));

		List<String> smcModes = new ArrayList<>(List.of(config.subtourModeChoice().getModes()));
		smcModes.add(E_BIKE);
		config.subtourModeChoice().setModes(smcModes.toArray(new String[0]));
	}

	/**
	 * add vehicle type for eBike.
	 * add new mode to network.
	 * add agent wise eBike ASC to population.
	 */
	static void addEBikeInScenario(Scenario scenario) {
//		add vehicle type for eBike
//		copy value from bike for eBike veh type
		VehicleType bikeType = scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.bike, VehicleType.class));
		EngineInformation bikeEngineInfo = bikeType.getEngineInformation();
		VehicleType eBikeType = VehicleUtils.createVehicleType(Id.create(E_BIKE, VehicleType.class));
		eBikeType.setNetworkMode(E_BIKE);
//		max speed for eBike is 25kmh
		eBikeType.setMaximumVelocity(25 / 3.6);
		eBikeType.setLength(bikeType.getLength());
		eBikeType.setWidth(bikeType.getWidth());
		eBikeType.setPcuEquivalents(bikeType.getPcuEquivalents());
		EngineInformation eBikeEngineInfo = eBikeType.getEngineInformation();

		VehicleUtils.setHbefaVehicleCategory(eBikeEngineInfo, VehicleUtils.getHbefaVehicleCategory(bikeEngineInfo));
//		car expensive is assumed to be synthetic fuels, which exhaust the same pollutants as conventional ICE vehicles
		VehicleUtils.setHbefaTechnology(eBikeEngineInfo, VehicleUtils.getHbefaTechnology(bikeEngineInfo));
		VehicleUtils.setHbefaSizeClass(eBikeEngineInfo, VehicleUtils.getHbefaSizeClass(bikeEngineInfo));
		VehicleUtils.setHbefaEmissionsConcept(eBikeEngineInfo, VehicleUtils.getHbefaEmissionsConcept(bikeEngineInfo));

		scenario.getVehicles().addVehicleType(eBikeType);

//		add eBike as allowed mode when bike is allowed
		for (Link link : scenario.getNetwork().getLinks().values()) {
//			skip pt links
			if (link.getId().toString().startsWith("pt_")) {
				continue;
			}

			if (link.getAllowedModes().contains(TransportMode.bike)) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add(E_BIKE);
				link.setAllowedModes(modes);
			}
		}

		if (eBikeAgentWiseAscHandling == EBikeAgentWiseAscHandling.FROM_BIKE) {
//			use bike personal asc
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (PersonUtils.getModeConstants(person) != null &&
					PersonUtils.getModeConstants(person).containsKey(TransportMode.bike)) {
//				assume that preference for bike is similar for eBike
					Map<String, String> modeConstants = new HashMap<>(PersonUtils.getModeConstants(person));
					modeConstants.put(E_BIKE, modeConstants.get(TransportMode.bike));
					PersonUtils.setModeConstants(person, modeConstants);
				}
			}
		} else if (eBikeAgentWiseAscHandling == EBikeAgentWiseAscHandling.SEPARATE_AGENT_MODAL_ASC) {
//			use separate values because if we use the bike values we just draw from bike and not from other modes?!
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (PersonUtils.getModeConstants(person) != null &&
					!PersonUtils.getModeConstants(person).containsKey(E_BIKE)) {
					// linear
//					mean=0.0; sigma=3.0 as for bike; calculation same as in class AddPersonSpecificAscsStreamReading
					double eBikeModeConstant = (splittableRandom.nextDouble() - 0.5) * 2 * 3.0 + 0.0;
					Map<String, String> modeConstants = new HashMap<>(PersonUtils.getModeConstants(person));
					modeConstants.put(E_BIKE, String.valueOf(eBikeModeConstant));
					PersonUtils.setModeConstants(person, modeConstants);
				}
			}
		}
	}

	private enum EBikeAgentWiseAscHandling {FROM_BIKE, SEPARATE_AGENT_MODAL_ASC}
}
