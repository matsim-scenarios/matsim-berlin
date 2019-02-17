package RunAbfall;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation.FuelType;

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
			config.controler().setOutputDirectory("output/original_Chessboard/05_FiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case Wilmersdorf:
			// TODO
			new RuntimeException("scenario not specified");
			break;

		default:
			new RuntimeException("no scenario selected.");
		}

		config = Run_AbfallUtils.prepareConfig(config);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));

		// create shipmets from every link to the garbage dump
		Id<Link> garbageDumpId = Id.createLinkId("j(3,3)");
		Run_AbfallUtils.createShipmentsForCarrier(scenario, myCarrier, garbageDumpId, carriers);

		// create a garbage truck type
		String vehicleTypeId = "TruckType1";
		int capacity = 100;
		double maxVelocity = 50 / 3.6;
		double costPerDistanceUnit = 0.1;
		double costPerTimeUnit = 0.01;
		double fixCosts = 200;
		FuelType engineInformation = FuelType.diesel;
		double literPerMeter = 0.01;
		CarrierVehicleType carrierVehType = Run_AbfallUtils.createGarbageTruckType(vehicleTypeId, capacity,
				maxVelocity, costPerDistanceUnit, costPerTimeUnit, fixCosts, engineInformation, literPerMeter);
		CarrierVehicleTypes vehicleTypes = Run_AbfallUtils.adVehicleType(carrierVehType);

		// create vehicle at depot
		String vehicleID = "GargabeTruck";
		String linkDepot = "i(1,0)";
		double earliestStartingTime = 6 * stunden;
		double latestFinishingTime = 15 * stunden;

		CarrierVehicle garbageTruck1 = Run_AbfallUtils.createGarbageTruck(vehicleID, linkDepot, earliestStartingTime,
				latestFinishingTime, carrierVehType);

		// define Carriers
		FleetSize fleetSize = FleetSize.FINITE;
		Run_AbfallUtils.defineCarriers(carriers, myCarrier, carrierVehType, vehicleTypes, garbageTruck1, fleetSize);

		//jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, myCarrier, vehicleTypes);

		final Controler controler = new Controler(scenario);

		CarrierScoringFunctionFactory scoringFunctionFactory = Run_AbfallUtils.createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = Run_AbfallUtils.createMyStrategymanager();

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory);
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

	}

}
