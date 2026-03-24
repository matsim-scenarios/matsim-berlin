package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MultiModals;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModes;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.mobsim.qsim.PreplanningEngineQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * Utils class for plugging together different policies in the same scenario.
 */
public final class MobilityToGridScenariosUtils {
	private static final Logger log = LogManager.getLogger(MobilityToGridScenariosUtils.class);

	private static final String AVERAGE = "average";

	private MobilityToGridScenariosUtils() {}

	public static void addEngineInformationToVehicleTypes(Scenario scenario, String carFuelType) {
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();

//				only set engine information if none are present
			if (engineInformation.getAttributes().isEmpty()) {
				switch (type.getId().toString()) {
//						all other vehicle types (which are not listed here) already have engine information assigned in the input vehicle types file
//						berlin-v6.4.vehicleTypes.xml in same dir as config.
//						for hbefa 4.1 (which we are using here) diesel, petrol etc. is saved as "EmissionConcept" whereas it HBEFA 4.2 it is saved as technology???
					case TransportMode.car -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
//							based on Kraftfahrzeugbestand germany 1.1.2025 ~60% petrol and 28% diesel, so we take petrol here.
//							source: https://www.kba.de/DE/Presse/Pressemitteilungen/Fahrzeugbestand/2025/pm10_fz_bestand_pm_komplett.html
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, carFuelType);
					}
					case TransportMode.ride -> {
//							ignore ride, the mode is routed on network, but then teleported
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case TransportMode.bike -> {
//							ignore bikes
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
					}
					case "freight", TransportMode.truck -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
						VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
						VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
						VehicleUtils.setHbefaEmissionsConcept(engineInformation, "diesel");
					}
					default -> throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
				}
			}
		}
//			ignore all pt veh types
		scenario.getTransitVehicles()
			.getVehicleTypes()
			.values().forEach(type -> VehicleUtils.setHbefaVehicleCategory(type.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString()));
	}

	/**
	 * configure qsim components for drt and sharing at the same time.
	 * If done separately, simulation fails.
	 */
	public static QSimComponentsConfigurator drtAndSharingQSimComponentsConfigurator(SharingConfigGroup sharingConfig, MultiModeDrtConfigGroup multiModeDrtConfigGroup) {
		return components -> {
			components.addNamedComponent(DynActivityEngine.COMPONENT_NAME);
			components.addNamedComponent(PreplanningEngineQSimModule.COMPONENT_NAME);

			//activate additional named components
//			additionalNamedComponents.forEach(components::addNamedComponent);

			List<String> dvrpModes = multiModeDrtConfigGroup.modes().toList();

			//activate all DvrpMode components
			MultiModals.requireAllModesUnique(dvrpModes);
			for (String m : dvrpModes) {
				components.addComponent(DvrpModes.mode(m));
			}

			for (SharingServiceConfigGroup serviceConfig : sharingConfig.getServices()) {
				components.addComponent(SharingModes.mode(SharingUtils.getServiceMode(serviceConfig)));
			}
		};
	}

	/**
	 * Enum for setting HBEFA 4.1 emission concept = fuel type for a vehicle type.
	 */
	public enum Hbefa41EmissionConcept {PETROL_4S, DIESEL, ELECTRICITY}
}
