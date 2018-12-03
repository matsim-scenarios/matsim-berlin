package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.CarrierService;

import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;

public class Code_Beispiele {

// Fahrzeug erzeugen
	
	/*	
		Builder fahrzeugTyp1 = VehicleTypeImpl.Builder.newInstance("Müllwagen1");
		fahrzeugTyp1.setUserData("Nutzerdaten1");
		fahrzeugTyp1.setFixedCost(2000).setCostPerDistance(20).setCostPerTransportTime(10);
		fahrzeugTyp1.setCostPerServiceTime(40).setMaxVelocity(13.89).setCostPerWaitingTime(5);
		fahrzeugTyp1.addCapacityDimension(1, 200);
		VehicleType typLkw1 = fahrzeugTyp1.build();		
			
 
		Carriers carriers = new Carriers();
		Id<Carrier> carrierID = Id.create("Fahrer1", Carrier.class);
		Carrier myCarrier = CarrierImpl.newInstance(carrierID);
*/
	
// Nachfrage erzeugen	
				
	/*	Builder service = CarrierService.Builder.newInstance(1,1);		
		myCarrier.getServices().add(service);
	*/ 
	
	
}
