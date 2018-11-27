package org.matsim.run;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
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
		Carriers anbieter = new Carriers() ;
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));
		
//Service erstellen
		CarrierService service1 = CarrierService.Builder.newInstance(Id.create("Service1", CarrierService.class),Id.createLinkId(7))
				.setName("Tonne1")
				.setServiceDuration(300)
				.setCapacityDemand(10)
				.setServiceStartTimeWindow(TimeWindow.newInstance(21600,54000))
				.build();
		CarrierService service2 = CarrierService.Builder.newInstance(Id.create("Service2", CarrierService.class),Id.createLinkId(12))
				.setName("Tonne2")
				.setServiceDuration(300)
				.setCapacityDemand(15)
				.setServiceStartTimeWindow(TimeWindow.newInstance(21600,54000))
				.build();
		CarrierService service3 = CarrierService.Builder.newInstance(Id.create("Service3", CarrierService.class),Id.createLinkId(6))
				.setName("Tonne3")
				.setServiceDuration(150)
				.setCapacityDemand(8)
				.setServiceStartTimeWindow(TimeWindow.newInstance(21600,54000))
				.build();
		CarrierService service4 = CarrierService.Builder.newInstance(Id.create("Service4", CarrierService.class),Id.createLinkId(8))
				.setName("Tonne4")
				.setServiceDuration(300)
				.setCapacityDemand(12)
				.setServiceStartTimeWindow(TimeWindow.newInstance(21600,54000))
				.build();
		myCarrier.getServices().add(service1);
		myCarrier.getServices().add(service2);
		myCarrier.getServices().add(service3);
		myCarrier.getServices().add(service4);
		//	myCarrier.getServices().add(createMatsimService("Service1", "i(3,9)", 2));
		//	myCarrier.getServices().add(createMatsimService("Service2", "i(4,9)", 2));		
		
//FahrzeugTyp erstellen
		CarrierVehicleType carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("MüllwagenTyp1", org.matsim.vehicles.VehicleType.class))

				.setCapacity(50)
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
		CarrierVehicle carrierVehicle1 = CarrierVehicle.Builder.newInstance(Id.create("Müllwagen1", org.matsim.vehicles.Vehicle.class), Id.createLinkId(13))
				.setEarliestStart(21600.0).setLatestEnd(28800.0).setTypeId(carrierVehType.getId()).build();
		CarrierVehicle carrierVehicle2 = CarrierVehicle.Builder.newInstance(Id.create("Müllwagen2", org.matsim.vehicles.Vehicle.class), Id.createLinkId(13))
				.setEarliestStart(28800.0).setLatestEnd(36000.0).setTypeId(carrierVehType.getId()).build();
		
// Dienstleister erstellen		
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addType(carrierVehType)
				.addVehicle(carrierVehicle1).addVehicle(carrierVehicle2)
				.setFleetSize(FleetSize.FINITE);				//Flottengröße; Anzahl der Fahrzeug, die CarrierVehicle erzeugt wurden
		myCarrier.setCarrierCapabilities(ccBuilder.build());
// Carriers hinzufügen
		anbieter.addCarrier(myCarrier);
		
// Fahrzeugtypen den Anbietern zuordenen
		new CarrierVehicleTypeLoader(anbieter).loadVehicleTypes(vehicleTypes);
		
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
		
//Ausagbe in xml Datei
		new CarrierPlanXmlWriterV2(anbieter).write("output/Uebung/output_Test01.xml");

	

				
		

		
//Ausgaben		
		
	//	System.out.println("Nutzerdaten: "+typLkw1.getUserData());
	//	System.out.println(typLkw1.getCapacityDimensions());
	//	System.out.println(typLkw1.toString());	
	}

}
