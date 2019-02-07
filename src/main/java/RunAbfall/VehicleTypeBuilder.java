package RunAbfall;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.EngineInformation.FuelType;

public class VehicleTypeBuilder {
	/**
	 * @return
	 */
	public static CarrierVehicleType createVehicleType() {
		return CarrierVehicleType.Builder
				.newInstance(Id.create("GargabeTruckTyp1", org.matsim.vehicles.VehicleType.class))

				.setCapacity(100).setMaxVelocity(10).setCostPerDistanceUnit(0.0001).setCostPerTimeUnit(0.001)
				.setFixCost(130).setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.015)).build();
	}

}
