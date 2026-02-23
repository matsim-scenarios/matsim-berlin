package org.matsim.run.policies.autofrei;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.population.CleanPopulation;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AutofreiUtils {
	private static final Logger log = LogManager.getLogger(AutofreiUtils.class);

	/**
	 * Adds a new mode to the config based on the car mode. The new mode is added to the network modes, main modes and analyzed modes. The scoring parameters are copied from the car mode.
	 */
	static void addNewModeBasedOnCarToLinks(Config newConfig, String newMode) {
		Collection<String> networkModes = new HashSet<>(newConfig.routing().getNetworkModes());
		networkModes.add(newMode);
		newConfig.routing().setNetworkModes(networkModes);

		Collection<String> mainModes = new HashSet<>(newConfig.qsim().getMainModes());
		mainModes.add(newMode);
		newConfig.qsim().setMainModes(mainModes);

		// don't add to subtour mode choice, as small scale commerical traffic doesn't to mode choice.

		Set<String> analyzedModes = new HashSet<>(newConfig.travelTimeCalculator().getAnalyzedModes());
		analyzedModes.add(newMode);
		newConfig.travelTimeCalculator().setAnalyzedModes(analyzedModes);

		ScoringConfigGroup.ModeParams carParams = newConfig.scoring().getModes().get(TransportMode.car);
		ScoringConfigGroup.ModeParams commercialCarParams = getCommercialCarParams(carParams, newMode);
		newConfig.scoring().addModeParams(commercialCarParams);

		ScoringConfigGroup.ActivityParams activityParams = new ScoringConfigGroup.ActivityParams();
		activityParams.setActivityType(newMode + " interaction");
		activityParams.setScoringThisActivityAtAll(false);
	}

	private static ScoringConfigGroup.ModeParams getCommercialCarParams(ScoringConfigGroup.ModeParams carParams, String newMode) {
		ScoringConfigGroup.ModeParams commercialCarParams = new ScoringConfigGroup.ModeParams(newMode);
		commercialCarParams.setConstant(carParams.getConstant());
		commercialCarParams.setDailyMonetaryConstant(carParams.getDailyMonetaryConstant());
		commercialCarParams.setDailyUtilityConstant(carParams.getDailyUtilityConstant());
		commercialCarParams.setMarginalUtilityOfDistance(carParams.getMarginalUtilityOfDistance());
		commercialCarParams.setMarginalUtilityOfTraveling(carParams.getMarginalUtilityOfTraveling());
		commercialCarParams.setMonetaryDistanceRate(carParams.getMonetaryDistanceRate());
		return commercialCarParams;
	}

	/**
	 * Adds a new vehicle type to the scenario based on the car vehicle type. The new vehicle type has the same attributes as the car vehicle type, but with a different id and mode.
	 */
	static void addNewVehicleTypeBasedOnCar(Scenario scenario, String newMode) {
		VehicleType carType = scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.car, VehicleType.class));
		VehicleType commercialCarType = VehicleUtils.createVehicleType(Id.create(newMode, VehicleType.class), newMode);
		commercialCarType.setLength(carType.getLength());
		commercialCarType.setWidth(commercialCarType.getWidth());
		commercialCarType.setPcuEquivalents(commercialCarType.getPcuEquivalents());
		VehicleUtils.setAccessTime(commercialCarType, VehicleUtils.getAccessTime(carType));
		VehicleUtils.setEgressTime(commercialCarType, VehicleUtils.getEgressTime(carType));
		VehicleUtils.setDoorOperationMode(commercialCarType, VehicleUtils.getDoorOperationMode(carType));

		scenario.getVehicles().addVehicleType(commercialCarType);
	}

	static void changeNetworkModeOfCommercialVehicleTypes(Scenario scenario, String newMode, Set<String> commercialVehicleTypes) {
		for (VehicleType vehicleType : scenario.getVehicles().getVehicleTypes().values()) {
			if (commercialVehicleTypes.contains(vehicleType.getId().toString())) {
				vehicleType.setNetworkMode(newMode);
			}
		}
	}

	static void cleanPopulation(Population population) {
		population.getPersons().values().stream()
			.flatMap(p -> p.getPlans().stream())
			.flatMap(p -> p.getPlanElements().stream())
			.forEach(p -> {
				removeActivityLocation(p);
				CleanPopulation.removeRouteFromLeg(p);
			});
	}

	private static void removeActivityLocation(PlanElement el) {
		if (el instanceof Activity act) {
			act.setLinkId(null);

			// there are agents with no coordinate. Only if the coordinate is set, we remove the facilityId the preserve the activity location.
//			if (act.getCoord() != null) {
//				act.setFacilityId(null);
//			}
		}
	}

	static void replaceCarTripsByNewMode(Scenario scenario, String newMode, Set<String> subpopulations) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String subpopulation = (String) person.getAttributes().getAttribute("subpopulation");

			// skip, if person not in provided subpopulation list. If the list is empty, all agents are included.
			if (!subpopulations.contains(subpopulation)) {
				continue;
			}

			Map<String, Id<VehicleType>> vehicleTypes = VehicleUtils.getVehicleTypes(person);
			Id<VehicleType> vehicleTypeId = vehicleTypes.get(TransportMode.car);
			if (vehicleTypeId != null) {
				// in this case, the person has a car
				vehicleTypes.put(newMode, vehicleTypeId);
			}


			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity activity) {
						String type = activity.getType();

						if (type.equals("car interaction")) {
							activity.setType(newMode + " interaction");
						}
					} else if (planElement instanceof Leg leg) {
						if (leg.getMode().equals(TransportMode.car)) {
							// set modes correctly for the former car legs
							leg.setMode(newMode);
							leg.setRoutingMode(newMode);
						}

						if (leg.getRoutingMode() != null && leg.getRoutingMode().equals(TransportMode.car)) {
							// set modes correctly for the walk legs
							leg.setRoutingMode(newMode);
						}
					}
				}
			}
		}
	}

	static void addNewModeBasedOnCarToLinks(Network network, String newMode) {
		network.getLinks().values().stream()
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			.forEach(link -> NetworkUtils.addAllowedMode(link, newMode));
	}

	static void restrictInnerRingLinks(Network network, Set<String> restrictedModes) {
		File shapefile = new File("input/v6.4/umweltzone/Umweltzone_Berlin.shp");
		ShpOptions shpOptions = new ShpOptions(shapefile.getAbsolutePath(), "EPSG:25833", null);
		ShpOptions.Index index = shpOptions.createIndex("_");

		MathTransform transform;
		try {
			CoordinateReferenceSystem sourceCRS = org.geotools.referencing.CRS.decode("EPSG:25832");
			CoordinateReferenceSystem targetCRS = org.geotools.referencing.CRS.decode("EPSG:25833");
			transform = org.geotools.referencing.CRS.findMathTransform(sourceCRS, targetCRS, true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}

		Set<Id<Link>> links = new HashSet<>();
		Corrections corrections = manualCorrections();
		for (Link link : network.getLinks().values()) {
			// Check if in Hundekopf area
			if (notContainsNode(link.getFromNode(), index, transform) && notContainsNode(link.getToNode(), index, transform)) {
				continue;
			}

			// Check if street name in manual add corrections. In this case no restriction.
			String name = (String) link.getAttributes().getAttribute("name");
			if (corrections.addStreet().contains(name)) {
				continue;
			}

			// Check if link id in manual add corrections. In this case no restriction.
			if (corrections.addLink().contains(link.getId())) {
				continue;
			}

			boolean restrict = false;

			// Check highway type
			if (!NetworkUtils.getHighwayType(link).equals("primary")) {
				restrict = true;
			}

			// Check if street name in manual remove corrections
			if (corrections.removeStreet().contains(name)) {
				restrict = true;
			}

			// Check if link id in manual remove corrections
			if (corrections.removeLinks().contains(link.getId())) {
				restrict = true;
			}

			// apply restrictions
			if (restrict) {
				links.add(link.getId());
				for (String restrictedMode : restrictedModes) {
					NetworkUtils.removeAllowedMode(link, restrictedMode);
				}
			}
		}
		log.info("Restricted {} links for car traffic.", links.size());

		MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
		multimodalNetworkCleaner.run(restrictedModes);
	}

	private static boolean notContainsNode(Node node, ShpOptions.Index index, MathTransform transform) {
		Coordinate point = MGC.coord2Coordinate(node.getCoord());
		Coordinate transformedCoordinate = null;
		try {
			transformedCoordinate = JTS.transform(point, null, transform);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}
		boolean ret = !index.contains(MGC.coordinate2Coord(transformedCoordinate));
		return ret;
	}

	private static Corrections manualCorrections() {
		HashSet<Id<Link>> linksRemove = new HashSet<>();
		HashSet<Id<Link>> linksAdd = new HashSet<>();
		HashSet<String> remove = new HashSet<>();
		HashSet<String> add = new HashSet<>();

		// Martin-Luther-Straße
		remove.add("Hofjägerallee");
		remove.add("Klingelhöferstraße");
		remove.add("Lützowplatz");
		remove.add("Herkulesbrücke");
		remove.add("Schillstraße");
		remove.add("An der Urania");
		remove.add("Martin-Luther-Straße");

		//Dominicusstraße
		linksRemove.add(Id.createLinkId("28497757#0"));
		linksRemove.add(Id.createLinkId("397052774"));
		linksRemove.add(Id.createLinkId("172655509#0"));
		linksRemove.add(Id.createLinkId("172655510#0"));
		linksRemove.add(Id.createLinkId("28497759#0"));
		linksRemove.add(Id.createLinkId("4525780#0"));
		linksRemove.add(Id.createLinkId("28497755#0"));
		linksRemove.add(Id.createLinkId("529086046"));

		//ADD (!) Tiergarten Spreetunnel
		add.add("Tunnel Tiergarten Spreebogen");
		linksAdd.add(Id.createLinkId("4401925#0"));
		linksAdd.add(Id.createLinkId("139099142#0"));

		//Landsberger Allee
		remove.add("Landsberger Allee");
		remove.add("Platz der Vereinten Nationen");
		remove.add("Mollstraße");

		//Karl-Liebknecht-Straße
		linksRemove.add(Id.createLinkId("1105526112#0"));
		linksRemove.add(Id.createLinkId("318282850#0"));
		linksRemove.add(Id.createLinkId("24914141#5"));
		linksRemove.add(Id.createLinkId("1101404262#0"));
		linksRemove.add(Id.createLinkId("112128418#0"));
		linksRemove.add(Id.createLinkId("1101404262#0"));
		linksRemove.add(Id.createLinkId("1020548406"));

		//Prenzlauer Allee
		remove.add("Prenzlauer Allee");

		return new Corrections(linksRemove, linksAdd, remove, add);
	}

	public record Corrections(Set<Id<Link>> removeLinks, Set<Id<Link>> addLink, Set<String> removeStreet, Set<String> addStreet) {
	}
}
