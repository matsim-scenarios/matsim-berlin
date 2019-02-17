package RunAbfall;

import java.util.Collection;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation.FuelType;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

public class Run_Abfall {

	static int stunden = 3600;
	static int minuten = 60;

	private static final Logger log = Logger.getLogger(Run_Abfall.class);

	private static final String original_Chessboard = "scenarios/Uebung01/grid9x9.xml";

	private enum scenarioAuswahl {
		chessboard, Wilmersdorf
	};

	public static void main(String[] args) {
		log.setLevel(Level.INFO);

		scenarioAuswahl scenarioWahl = scenarioAuswahl.chessboard;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (scenarioWahl) {
		case chessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/04_InfiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case Wilmersdorf:
			// TODO
			new RuntimeException("scenario not specified");
			break;

		default:
			new RuntimeException("no scenario selected.");
		}

		config = UtilityRun_Abfall.prepareConfig(config);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));

		// Nachfrage am Link erzeugen
		Id<Link> dropOffLinkId = Id.createLinkId("j(3,3)");
		UtilityRun_Abfall.createShipmentsForCarrier(scenario, myCarrier, dropOffLinkId, carriers);

		// FahrzeugTyp erstellen und Typ hinzufügen
		String vehicleTypeId = "TruckType1";
		int capacity = 100;
		double maxVelocity = 50 / 3.6;
		double costPerDistanceUnit = 0.1;
		double costPerTimeUnit = 0.01;
		double fixCosts = 100;
		FuelType engineInformation = FuelType.diesel;
		double literPerMeter = 0.01;
		CarrierVehicleType carrierVehType = UtilityRun_Abfall.createGarbageTruckType(vehicleTypeId, capacity,
				maxVelocity, costPerDistanceUnit, costPerTimeUnit, fixCosts, engineInformation, literPerMeter);
		CarrierVehicleTypes vehicleTypes = UtilityRun_Abfall.adVehicleType(carrierVehType);

		// konkretes Fahrzeug erstellen
		String vehicleID = "GargabeTruck";
		String linkDepot = "i(1,0)";
		double earliestStartingTime = 6 * stunden;
		double latestFinishingTime = 15 * stunden;

		CarrierVehicle garbageTruck1 = UtilityRun_Abfall.createGarbageTruck(vehicleID, linkDepot, earliestStartingTime,
				latestFinishingTime, carrierVehType);

		// Dienstleister erstellen
		FleetSize fleetSize = FleetSize.INFINITE;
		UtilityRun_Abfall.defineCarriers(carriers, myCarrier, carrierVehType, vehicleTypes, garbageTruck1,fleetSize);

		// Netzwerk integrieren und Kosten für jsprit
		Network network = NetworkUtils.readNetwork(original_Chessboard);
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

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.xml");
		new Plotter(problem, bestSolution).plot(
				scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans_Test01.png",
				"bestSolution");
		
		final Controler controler = new Controler(scenario);

		CarrierScoringFunctionFactory scoringFunctionFactory = UtilityRun_Abfall.createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = UtilityRun_Abfall.createMyStrategymanager();

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory);
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

	}



}
