package org.matsim.run;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;


/*	Übung mit dem Ziel die Erstellung von services, VehicleTypes, Carriers etc.
	 * zu üben
	 */
public class uebung01 {

	public static void main(String[] args) {
		
//Create carrier with services and shipments

		Carriers anbieter1 = new Carriers() ;
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("Fahrer1", Carrier.class));
//Service erstellen
		CarrierService service1 = CarrierService.Builder.newInstance(Id.create("Service1", CarrierService.class),Id.createLinkId("i(1,4)"))
				.setName("Tonne1")
				.setServiceDuration(0.2)
				.setCapacityDemand(10)
				.build();
		CarrierService service2 = CarrierService.Builder.newInstance(Id.create("Service2", CarrierService.class),Id.createLinkId("i(6,9)"))
				.setName("Tonne2")
				.setServiceDuration(0.2)
				.setCapacityDemand(15)
				.build();
		myCarrier.getServices().add(service1);
		myCarrier.getServices().add(service2);
		//	myCarrier.getServices().add(createMatsimService("Service1", "i(3,9)", 2));
		//	myCarrier.getServices().add(createMatsimService("Service2", "i(4,9)", 2));		
		
//FahrzeugTyp erstellen

		CarrierVehicleType carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("gridType", org.matsim.vehicles.VehicleType.class))

				.setCapacity(20)
				.setMaxVelocity(10)
				.setCostPerDistanceUnit(0.0001)
				.setCostPerTimeUnit(0.001)
				.setFixCost(130)
				.setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.015))
				.build();
// FahrzeugTyp zu FahrzeugTypen hinzufügen
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);
// Fahrzeug erstellen		
		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(10,2)"))
				.setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType.getId()).build();
// Dienstleister erstellen		
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addType(carrierVehType)
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);		
		myCarrier.setCarrierCapabilities(ccBuilder.build());
// Carriers hinzufügen
		anbieter1.addCarrier(myCarrier);
// Fahrzeugtypen den Anbietern zuordenen
		new CarrierVehicleTypeLoader(anbieter1).loadVehicleTypes(vehicleTypes);
		
// Netzwerk integrieren	und Kosten für jsprit 			

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("scenarios/equil/networkBeispiel.xml"); 
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network,vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();
		netBuilder.setTimeSliceWidth(1800);	
		
//Build jsprit, solve and route VRP for carrierService only -> need solution to convert Services to Shipments
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(myCarrier, network);
		vrpBuilder.setRoutingCost(netBasedCosts);
		VehicleRoutingProblem problem = vrpBuilder.build();
		
// get the algorithm out-of-the-box, search solution and get the best one.
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

//Routing bestPlan to Network
		CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(myCarrier, bestSolution);
		NetworkRouter.routePlan(carrierPlanServices,netBasedCosts);
		myCarrier.setSelectedPlan(carrierPlanServices);
		

				
		

		
//Ausgaben		
		
	//	System.out.println("Nutzerdaten: "+typLkw1.getUserData());
	//	System.out.println(typLkw1.getCapacityDimensions());
	//	System.out.println(typLkw1.toString());	
	}

}
