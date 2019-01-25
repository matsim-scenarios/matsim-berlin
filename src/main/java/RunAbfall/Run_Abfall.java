package RunAbfall;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.recreate.ScoringFunction;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;


public class Run_Abfall {

	/*	Übung mit dem Ziel die Erstellung von services, VehicleTypes, Carriers etc.
	 * zu üben
	 */

	private static final String SCENARIOS_EQUIL_NETWORK_BEISPIEL_XML = "scenarios/equil/networkBeispiel.xml";

	public static void main(String[] args) {
		
		//MATSim config
		Config config = ConfigUtils.createConfig();
		
		// (the directory structure is needed for jsprit output, which is before the controler starts.  Maybe there is a better alternative ...)
		config.controler().setOutputDirectory("output/Uebung/MATsim1");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		new OutputDirectoryHierarchy( config.controler().getOutputDirectory(), config.controler().getRunId(), config.controler().getOverwriteFileSetting() ) ;
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		config.network().setInputFile(SCENARIOS_EQUIL_NETWORK_BEISPIEL_XML);
		config.controler().setLastIteration(0);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);	

		//Create carrier with services and shipments
		Carriers anbieter = new Carriers() ;
		Carrier myCarrier = createShipments();

		//FahrzeugTyp erstellen
		CarrierVehicleType carrierVehType = createVehicleType();
		// FahrzeugTyp zu FahrzeugTypen hinzufügen
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);

		// Fahrzeug erstellen		
		CarrierVehicle carrierVehicle1 = CarrierVehicle.Builder.newInstance(Id.create("Müllwagen1", org.matsim.vehicles.Vehicle.class), Id.createLinkId(130))
				.setEarliestStart(21600.0).setLatestEnd(54000.0).setTypeId(carrierVehType.getId()).build();
		CarrierVehicle carrierVehicle2 = CarrierVehicle.Builder.newInstance(Id.create("Müllwagen2", org.matsim.vehicles.Vehicle.class), Id.createLinkId(130))
				.setEarliestStart(21600.0).setLatestEnd(54000.0).setTypeId(carrierVehType.getId()).build();

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
		new MatsimNetworkReader(network).readFile(SCENARIOS_EQUIL_NETWORK_BEISPIEL_XML); 
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
		//new CarrierPlanXmlWriterV2(anbieter).write("output/Uebung/output_Test01.xml");
		//als Diagramm plotten
		//new Plotter(problem,bestSolution.getRoutes()).plot("output/Uebung/plott_Test01", "carrier1");

		CarrierPlanWriter planWriter = new CarrierPlanWriter(anbieter.getCarriers().values());
		planWriter.write(scenario.getConfig().controler().getOutputDirectory() + "/plans_Test01.xml");

		final org.matsim.core.controler.Controler controler = new org.matsim.core.controler.Controler(scenario); //Warum auch immer so umständlich

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = createMyStrategymanager(); 

		CarrierModule listener = new CarrierModule(anbieter, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
	//	new CarrierPlanXmlWriterV2(anbieter).write("output/Uebung/output_MatSimCarrier.xml");

	}

	/**
	 * @return
	 */
	private static CarrierVehicleType createVehicleType() {
		return CarrierVehicleType.Builder.newInstance(Id.create("MüllwagenTyp1", org.matsim.vehicles.VehicleType.class))

				.setCapacity(100)
				.setMaxVelocity(10)
				.setCostPerDistanceUnit(0.0001)
				.setCostPerTimeUnit(0.001)
				.setFixCost(130)
				.setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.015))
				.build();
	}

	/**
	 * @return
	 */
	private static Carrier createShipments() {
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));

		//Service erstellen
		CarrierShipment shipment1 = CarrierShipment.Builder.newInstance(Id.create("Ship1",CarrierShipment.class),Id.createLinkId(7), Id.createLinkId(14), 10)
				.setPickupServiceTime(300)
				.setPickupTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryServiceTime(3600)
				.build();
		CarrierShipment shipment2 = CarrierShipment.Builder.newInstance(Id.create("Ship2",CarrierShipment.class),Id.createLinkId(12), Id.createLinkId(14), 15)
				.setPickupServiceTime(300)
				.setPickupTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryServiceTime(3600)
				.build();
		CarrierShipment shipment3 = CarrierShipment.Builder.newInstance(Id.create("Ship3",CarrierShipment.class),Id.createLinkId(6), Id.createLinkId(14), 8)
				.setPickupServiceTime(300)
				.setPickupTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryServiceTime(3600)
				.build();
		CarrierShipment shipment4 = CarrierShipment.Builder.newInstance(Id.create("Ship4",CarrierShipment.class),Id.createLinkId(8), Id.createLinkId(14), 12)
				.setPickupServiceTime(300)
				.setPickupTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600,54000))
				.setDeliveryServiceTime(3600)
				.build();
	
		myCarrier.getShipments().add(shipment1);
		myCarrier.getShipments().add(shipment2);
		myCarrier.getShipments().add(shipment3);
		myCarrier.getShipments().add(shipment4);
		return myCarrier;
	}

	private static CarrierPlanStrategyManagerFactory createMyStrategymanager(){
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};

	}

	private static CarrierScoringFunctionFactoryImpl createMyScoringFunction2 (final Scenario scenario) {

		return new CarrierScoringFunctionFactoryImpl(scenario.getNetwork());
//		return new CarrierScoringFunctionFactoryImpl (scenario, scenario.getConfig().controler().getOutputDirectory()) {
//
//			public ScoringFunction createScoringFunction(final Carrier carrier){
//				SumScoringFunction sumSf = new SumScoringFunction() ;
//
//				VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
//				sumSf.addScoringFunction(fixCost);
//
//				LegScoring legScoring = new LegScoring(carrier);
//				sumSf.addScoringFunction(legScoring);
//
//				//Score Activity w/o correction of waitingTime @ 1st Service.
//				//			ActivityScoring actScoring = new ActivityScoring(carrier);
//				//			sumSf.addScoringFunction(actScoring);
//
//				//Alternativ:
//				//Score Activity with correction of waitingTime @ 1st Service.
//				ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
//				sumSf.addScoringFunction(actScoring);
//
//				return sumSf;
//			}
//		};
	}

//shapfile qgis, Netzwerk aus Link, shipments


	//Ausgaben		

	//	System.out.println("Nutzerdaten: "+typLkw1.getUserData());
	//	System.out.println(typLkw1.getCapacityDimensions());
	//	System.out.println(typLkw1.toString());	
}



