package RunAbfall;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.EngineInformation.FuelType;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

public class Run_AbfallUtils {

	static int stunden = 3600;
	static int minuten = 60;

	/**
	 * Delets the existing output file and sets the number of the last iteration
	 * 
	 * @param config
	 */
	public static Config prepareConfig(Config config, int lastIteration) {
		// (the directory structure is needed for jsprit output, which is before the
		// controler starts. Maybe there is a better alternative ...)
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(lastIteration);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(TransformationFactory.GK4);

		return config;
	}

	/**
	 * Creates a Shipment for every link, ads all shipments to myCarrier and ads
	 * myCarrier to carriers. The volumeGarbage is in garbage per meter and week. So
	 * the volumeGarbage of every shipment depends of the input
	 * garbagePerMeterAndWeek.
	 * 
	 * @param
	 * @return 
	 */
	public static int createShipmentsForCarrierI(double garbagePerMeterAndWeek, double volumeBigTrashcan, double serviceTimePerBigTrashcan, int capacityTruck,
			Map<Id<Link>, Link> garbageLinks, Scenario scenario, Carrier myCarrier, String garbageDumpId,
			Carriers carriers) {
		int allGarbage = 0;
		for (Link link : garbageLinks.values()) {
			double maxWeightBigTrashcan = volumeBigTrashcan * 0.1; // Umrechnung von Volumen [l] in Masse[kg]
			int volumeGarbage = (int) Math.ceil(link.getLength() * garbagePerMeterAndWeek);
			double serviceTime = Math.ceil(((double) volumeGarbage) / maxWeightBigTrashcan) * serviceTimePerBigTrashcan;
			double deliveryTime = ((double) volumeGarbage / capacityTruck) * 45 * minuten;
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment_" + link.getId(), CarrierShipment.class), link.getId(),
							Id.createLinkId(garbageDumpId), volumeGarbage)
					.setPickupServiceTime(serviceTime)
					.setPickupTimeWindow(TimeWindow.newInstance(6 * stunden, 15 * stunden))
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * stunden, 15 * stunden))
					.setDeliveryServiceTime(deliveryTime).build();
			myCarrier.getShipments().add(shipment);
			allGarbage = allGarbage + volumeGarbage;
		}
		carriers.addCarrier(myCarrier);
		return allGarbage;
	}

	/**
	 * Creates a Shipment for every link, ads all shipments to myCarrier and ads
	 * myCarrier to carriers. The volumeGarbage is in garbage per week. So the
	 * volumeGarbage of every shipment depends of the sum of all lengths from links
	 * with shipments.
	 * 
	 * @param
	 */
	public static int createShipmentsForCarrierII(double garbagePerWeek, double volumeBigTrashcan, double serviceTimePerBigTrashcan, double distanceWithShipments,
			int capacityTruck, Map<Id<Link>, Link> garbageLinks, Scenario scenario, Carrier myCarrier,
			String garbageDumpId, Carriers carriers) {
		int allGarbage = 0;
		for (Link link : garbageLinks.values()) {
			double maxWeightBigTrashcan = volumeBigTrashcan * 0.1; // Umrechnung von Volumen [l] in Masse[kg]
			int volumeGarbage = (int) Math.ceil(link.getLength() * (garbagePerWeek / distanceWithShipments));
			double serviceTime = Math.ceil(((double) volumeGarbage) / maxWeightBigTrashcan) * serviceTimePerBigTrashcan;
			double deliveryTime = ((double) volumeGarbage / capacityTruck) * 45 * minuten;
			CarrierShipment shipment = CarrierShipment.Builder
					.newInstance(Id.create("Shipment_" + link.getId(), CarrierShipment.class), link.getId(),
							Id.createLinkId(garbageDumpId), volumeGarbage)
					.setPickupServiceTime(serviceTime)
					.setPickupTimeWindow(TimeWindow.newInstance(6 * stunden, 15 * stunden))
					.setDeliveryTimeWindow(TimeWindow.newInstance(6 * stunden, 15 * stunden))
					.setDeliveryServiceTime(deliveryTime).build();
			myCarrier.getShipments().add(shipment);
			allGarbage = allGarbage + volumeGarbage;
		}
		carriers.addCarrier(myCarrier);
		return allGarbage;
	}

	/**
	 * Method creates a new garbage truck type
	 * 
	 * @param maxVelocity in m/s
	 * @return
	 */
	public static CarrierVehicleType createGarbageTruckType(String vehicleTypeId, int capacity, double maxVelocity,
			double costPerDistanceUnit, double costPerTimeUnit, double fixCosts, FuelType engineInformation,
			double literPerMeter) {
		return CarrierVehicleType.Builder.newInstance(Id.create(vehicleTypeId, VehicleType.class)).setCapacity(capacity)
				.setMaxVelocity(maxVelocity).setCostPerDistanceUnit(costPerDistanceUnit)
				.setCostPerTimeUnit(costPerTimeUnit).setFixCost(fixCosts)
				.setEngineInformation(new EngineInformationImpl(engineInformation, literPerMeter)).build();

	}

	/**
	 * Method adds a new vehicle Type to the list of vehicleTyps
	 * 
	 * @param
	 * @return
	 */
	public static CarrierVehicleTypes adVehicleType(CarrierVehicleType carrierVehType) {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);
		return vehicleTypes;
	}

	/**
	 * Method for creating a new Garbage truck
	 * 
	 * @param
	 * 
	 * @return
	 */
	public static CarrierVehicle createGarbageTruck(String VehicleId, String linkDepot, double earliestStartingTime,
			double latestFinishingTime, CarrierVehicleType carrierVehType) {

		return CarrierVehicle.Builder.newInstance(Id.create(VehicleId, Vehicle.class), Id.createLinkId(linkDepot))
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime)
				.setTypeId(carrierVehType.getId()).build();
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers
	 * 
	 * @param
	 * 
	 */
	public static void defineCarriers(Carriers carriers, Carrier myCarrier, CarrierVehicleType carrierVehType,
			CarrierVehicleTypes vehicleTypes, CarrierVehicle vehicleForckenbeck, CarrierVehicle vehicleMalmoeerStr, FleetSize fleetSize) {
		CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().addType(carrierVehType)
				.addVehicle(vehicleForckenbeck).addVehicle(vehicleMalmoeerStr).setFleetSize(fleetSize).build();

		myCarrier.setCarrierCapabilities(carrierCapabilities);

		// Fahrzeugtypen den Anbietern zuordenen
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
	}

	/**
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution
	 * 
	 * @param
	 * @return 
	 */
	public static int solveWithJsprit(Scenario scenario, Carriers carriers, Carrier myCarrier,
			CarrierVehicleTypes vehicleTypes) {
		// Netzwerk integrieren und Kosten für jsprit
		Network network = scenario.getNetwork();
		// Network network = NetworkUtils.readNetwork(original_Chessboard);
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
		algorithm.setMaxIterations(500);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		// Routing bestPlan to Network
		CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(myCarrier, bestSolution);
		NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
		myCarrier.setSelectedPlan(carrierPlanServices);
		int noPickup = bestSolution.getUnassignedJobs().size();
		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.xml");
		new Plotter(problem, bestSolution).plot(
				scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.png",
				"bestSolution");
		return noPickup;
	}

	/**
	 * @param
	 */
	public static void scoringAndManagerFactory(Scenario scenario, Carriers carriers, final Controler controler) {
		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = createMyStrategymanager();

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory);
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener);
	}

	/**
	 * @param scenario
	 * @return
	 */
	public static CarrierScoringFunctionFactoryImpl createMyScoringFunction2(final Scenario scenario) {

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
	 * @return
	 */
	public static CarrierPlanStrategyManagerFactory createMyStrategymanager() {
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}
	/** Gives an output of a .txt file with some important information 
	 * @param 
	 */
	public static void outputSummary(int allGarbage, Scenario scenario, Carrier myCarrier,
			Map<Id<Link>, Link> garbageLinks, int noPickup) {
		FileWriter writer;
		File file;
		file = new File(scenario.getConfig().controler().getOutputDirectory() + "/01_Zusammenfassung.txt");
		try {
			writer = new FileWriter(file ,true);
			writer.write("Die Summe des abzuholenden Mülls beträgt: \t"+ allGarbage + " kg\n");
			writer.write("Anzahl der Abholstellen: \t\t\t\t\t"+ garbageLinks.size()+"\n");
			writer.write("Anzahl der Abholstellen ohne Abholung: \t\t" + noPickup + "\n");
			writer.write("Anzahl der Muellfahrzeuge im Einsatz: \t\t" + myCarrier.getSelectedPlan().getScheduledTours().size()+"\n");
			writer.write("Kosten: \t\t\t\t\t\t\t\t\t"+((-1)*Math.round(myCarrier.getSelectedPlan().getScore()))+" €\n");
			writer.flush();
			writer.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (noPickup == 0) {
			System.out.println("Abfaelle wurden von "+myCarrier.getSelectedPlan().getScheduledTours().size()+" Fahrzeugen komplett eingesammelt!");
		}
		else {
			System.out.println("Abfall nicht komplett eingesammelt!");
		}
	}
}
