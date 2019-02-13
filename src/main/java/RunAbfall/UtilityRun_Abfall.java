package RunAbfall;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.EngineInformation.FuelType;

public class UtilityRun_Abfall {

	/**
	 * @param 
	 * 
	 * @return
	 */
	public static CarrierVehicleType createVehicleType(String vehicleTypeId, int capacity, double maxVelocity,
										double costPerDistanceUnit, double costPerTimeUnit, double fixCosts, FuelType engineInformation, double literPerMeter) {
		return CarrierVehicleType.Builder
				.newInstance(Id.create(vehicleTypeId, VehicleType.class))
				.setCapacity(capacity).setMaxVelocity(maxVelocity).setCostPerDistanceUnit(costPerDistanceUnit).setCostPerTimeUnit(costPerTimeUnit)
				.setFixCost(fixCosts).setEngineInformation(new EngineInformationImpl(engineInformation, literPerMeter)).build();
	
	}

	/**
	 * Method for creating a new Garbage truck
	 * 
	 * @param
	 * 
	 * @return
	 */
	public static CarrierVehicle createCarrierVehicle(String VehicleId, String linkDepot, double earliestStartingTime,
			double latestFinishingTime, CarrierVehicleType carrierVehType) {

		return CarrierVehicle.Builder.newInstance(Id.create(VehicleId, Vehicle.class), Id.createLinkId(linkDepot))
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime)
				.setTypeId(carrierVehType.getId()).build();
	}

}
