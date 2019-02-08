package RunAbfall;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
	 * TODO: Werte mit übergeben (Name, depot, TW, vehType)
	 * 
	 * @param 
	 * 
	 * @return
	 */
	public static CarrierVehicle createCarrierVehicle(String VehicleName, String depot, double startTime, double endTime, CarrierVehicleType carrierVehType) {
		
	//	final String depot = "i(1,0)";
	//	final double startTime = 21600;
	//	final double endTime = 54000;
		
		return CarrierVehicle.Builder
				.newInstance(Id.create(VehicleName, Vehicle.class), Id.createLinkId(depot))
				.setEarliestStart(startTime).setLatestEnd(endTime).setTypeId(carrierVehType.getId()).build();
	}
	
}
