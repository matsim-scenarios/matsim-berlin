package org.matsim.analysis.autofrei;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.vehicles.*;

import static org.matsim.run.policies.autofrei.RunAutofreiPolicy.NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC;

public class HbefaVehiclePreparation {
	public static final String AVERAGE = "average";
	private static final Logger log = LogManager.getLogger(HbefaVehiclePreparation.class);

	public static void main(String[] args) {
		String file = "/Users/paulh/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-baseCaseCtdExtended/berlin-v6.4.output_vehicles.xml.zst";

		Vehicles vehiclesContainer = VehicleUtils.createVehiclesContainer();
		new MatsimVehicleReader.VehicleReader(vehiclesContainer).readFile(file);

		for (VehicleType type : vehiclesContainer.getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();

			if (engineInformation.getAttributes().isEmpty()) {
				switch (type.getId().toString()) {
					case TransportMode.car -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
					}
					case NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
					}
					case TransportMode.ride, TransportMode.bike -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
					}
					case "freight", TransportMode.truck -> {
						VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
					}
					default -> log.warn("Skipping unknown vehicle type: {}", type.getId());
				}
			}
			VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
			VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
			VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
		}

		VehicleUtils.writeVehicles(vehiclesContainer, file.replace("output_vehicles.xml.zst", "output_vehicles_hbefa.xml.zst"));
	}
}
