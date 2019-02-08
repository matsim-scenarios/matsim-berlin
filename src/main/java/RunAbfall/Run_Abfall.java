package RunAbfall;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

public class Run_Abfall {

	private static final String SCENARIOS_UEBUNG01_GRID9X9_XML = "scenarios/Uebung01/grid9x9.xml";

	private enum scenarioAuswahl {
		chessboard, Wilmersdorf, Charlottenburg
	};

	public static void main(String[] args) {

		scenarioAuswahl scenarioWahl = scenarioAuswahl.chessboard;

		// MATSim config
		Config config = ConfigUtils.createConfig();
	
		switch (scenarioWahl) {
		case chessboard:
			config.controler().setOutputDirectory("output/Chessboard/02_InfiniteSize");
			config.network().setInputFile(SCENARIOS_UEBUNG01_GRID9X9_XML);
			break;
		case Wilmersdorf:
			// TODO
			new RuntimeException("scenario not specified");
			break;
		case Charlottenburg:
			// TODO
			new RuntimeException("scenario not specified");
			break;
		default:
			new RuntimeException("no scenario selected.");
		}
		
		config = prepareConfig(config);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers anbieter = new Carriers();
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));
		
		// jedem Link ein Shipment zuordnen
		Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();

		for (Link link : links.values()) {
			int capycityDemand = 10;																							//zzz TODO: Mange abhängig von Linklänge o.ä.
			Id<Link> dropOffLinkId = Id.createLinkId("j(9,9)");
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment " + link.getId(), CarrierShipment.class), link.getId(),
							dropOffLinkId, capycityDemand)
					.setPickupServiceTime(5 * 60).setPickupTimeWindow(TimeWindow.newInstance(6 * 3600, 15 * 3600))				//zzz TODO: PickupTime anhängig von Menge
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * 3600, 15 * 3600)).setDeliveryServiceTime(15 * 60)			//zzz TODO: DeliveryTime anhängig von Menge
					.build();
			myCarrier.getShipments().add(shipment);
		}
		anbieter.addCarrier(myCarrier);

		// FahrzeugTyp erstellen
		CarrierVehicleType carrierVehType = VehicleTypeBuilder.createVehicleType();
		
		// FahrzeugTyp zu FahrzeugTypen hinzufügen
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);

		// Fahrzeug erstellen
		
		//TODO: Bei Infinite so nicht sinnvoll, mehrere gleiche Fzge zu haben.
		CarrierVehicle carrierVehicle1 = createCarrierVehicle(carrierVehType);
		CarrierVehicle carrierVehicle2 = CarrierVehicle.Builder
				.newInstance(Id.create("GargabeTruck2", Vehicle.class), Id.createLinkId("i(1,0)"))
				.setEarliestStart(21600.0).setLatestEnd(54000.0).setTypeId(carrierVehType.getId()).build(); //auch auf Methode umstellen, Methodenaufruf configurierbar machen.-> createCarrierVehicle(...)

		// Dienstleister erstellen
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
				.addVehicle(carrierVehicle1).addVehicle(carrierVehicle2).setFleetSize(FleetSize.INFINITE);

		myCarrier.setCarrierCapabilities(ccBuilder.build());

		// Fahrzeugtypen den Anbietern zuordenen
		new CarrierVehicleTypeLoader(anbieter).loadVehicleTypes(vehicleTypes);

		// Netzwerk integrieren und Kosten für jsprit
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(SCENARIOS_UEBUNG01_GRID9X9_XML);
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();
		netBuilder.setTimeSliceWidth(1800);

		// Build jsprit, solve and route VRP for carrierService only -> need solution to
		// convert Services to Shipments
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(myCarrier, network);
		vrpBuilder.setRoutingCost(netBasedCosts);
		VehicleRoutingProblem problem = vrpBuilder.build();

		// get the algorithm out-of-the-box, search solution and get the best one.
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		// Routing bestPlan to Network
		CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(myCarrier, bestSolution);
		NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
		myCarrier.setSelectedPlan(carrierPlanServices);

		new CarrierPlanXmlWriterV2(anbieter)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.xml");

		final Controler controler = new Controler(scenario);

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = createMyStrategymanager();

		CarrierModule listener = new CarrierModule(anbieter, planStrategyManagerFactory, scoringFunctionFactory);
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener);

		controler.run();

		new CarrierPlanXmlWriterV2(anbieter)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

	}



	/**
	 * @param config
	 */
	private static Config prepareConfig(Config config) {
		// (the directory structure is needed for jsprit output, which is before the
		// controler starts. Maybe there is a better alternative ...)
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(0);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		return config;
	}



	private static CarrierPlanStrategyManagerFactory createMyStrategymanager() {
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};

	}

	private static CarrierScoringFunctionFactoryImpl createMyScoringFunction2(final Scenario scenario) {

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
	
	/**
	 * TODO: Werte mit übergeben (Name, depot, TW, vehType)
	 * 
	 * @param carrierVehType
	 * @return
	 */
	private static CarrierVehicle createCarrierVehicle(CarrierVehicleType carrierVehType) {
		return CarrierVehicle.Builder
				.newInstance(Id.create("GargabeTruck1", Vehicle.class), Id.createLinkId("i(1,0)"))
				.setEarliestStart(21600.0).setLatestEnd(54000.0).setTypeId(carrierVehType.getId()).build();
	}
}
