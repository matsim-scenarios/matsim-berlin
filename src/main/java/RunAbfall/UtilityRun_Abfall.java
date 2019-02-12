package RunAbfall;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.EngineInformation.FuelType;

public class UtilityRun_Abfall {

	public static CarrierVehicleType createVehicleType() {
		return CarrierVehicleType.Builder
				.newInstance(Id.create("GargabeTruckTyp1", org.matsim.vehicles.VehicleType.class))

				.setCapacity(100).setMaxVelocity(10).setCostPerDistanceUnit(0.0001).setCostPerTimeUnit(0.001)
				.setFixCost(130).setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.015)).build();
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
