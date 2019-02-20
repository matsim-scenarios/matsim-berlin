package RunAbfall;

import java.util.HashMap;
import java.util.Map;

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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation.FuelType;

public class Run_Abfall {

	static int stunden = 3600;
	static int minuten = 60;
	Id<Link> garbageDumpId;

	private static final Logger log = Logger.getLogger(Run_Abfall.class);

	private static final String original_Chessboard = "scenarios/networks/originalChessboard9x9.xml";
	private static final String modified_Chessboard = "scenarios/networks/modifiedChessboard9x9.xml";
	private static final String berlin = "original-input-data/berlin-v5.2-1pct.output_network.xml.gz";

	private enum netzwerkAuswahl {
		originalChessboard, modifiedChessboard, berlinNetwork
	};

	private enum scenarioAuswahl {
		chessboard, berlinScenario
	};

	public static void main(String[] args) {
		String garbageDumpId = null;
		String depotId = null;
		log.setLevel(Level.INFO);

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl = scenarioAuswahl.berlinScenario;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/02_InfiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			config.controler().setOutputDirectory("output/modified_Chessboard/03_FiniteSize_Tests");
			config.network().setInputFile(modified_Chessboard);
			break;
		case berlinNetwork:
			config.controler().setOutputDirectory("output/Berlin/01_FiniteSize_Tests");
			config.network().setInputFile(berlin);
			break;
		default:
			new RuntimeException("no network selected.");
		}
		int lastIteration = 0;
		config = Run_AbfallUtils.prepareConfig(config, lastIteration);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));

		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();

		switch (scenarioWahl) {
		case chessboard:
			garbageDumpId = ("j(0,9)R");
			depotId = "j(9,9)";
			for (Link link : allLinks.values()) {
				if (link.getCoord().getX() < 8000 && link.getFreespeed() < 12) {
					garbageLinks.put(link.getId(), link);
				}
			}
			break;
		case berlinScenario:
			garbageDumpId = ("142010");	//Muellheizkraftwerk Ruhleben
			depotId = "28457";	//zufall
			for (Link link : allLinks.values()) {
				if (link.getCoord().getX() > 4587375.819194021 &&
					link.getCoord().getX() < 4589012.681349432 && 
					link.getCoord().getY() < 5833272.254176694 &&
					link.getCoord().getY() > 5832969.565900505 ){
					garbageLinks.put(link.getId(), link);
				}
			}
			break;
		default:
			new RuntimeException("no scenario selected.");
		}

		Run_AbfallUtils.createShipmentsForCarrier(garbageLinks, scenario, myCarrier, garbageDumpId, carriers);
		// create a garbage truck type
		String vehicleTypeId = "TruckType1";
		int capacity = 300;
		double maxVelocity = 50 / 3.6;
		double costPerDistanceUnit = 1;
		double costPerTimeUnit = 0.01;
		double fixCosts = 200;
		FuelType engineInformation = FuelType.diesel;
		double literPerMeter = 0.01;
		CarrierVehicleType carrierVehType = Run_AbfallUtils.createGarbageTruckType(vehicleTypeId, capacity, maxVelocity,
				costPerDistanceUnit, costPerTimeUnit, fixCosts, engineInformation, literPerMeter);
		CarrierVehicleTypes vehicleTypes = Run_AbfallUtils.adVehicleType(carrierVehType);

		// create vehicle at depot
		String vehicleId = "GargabeTruck";
		double earliestStartingTime = 6 * stunden;
		double latestFinishingTime = 15 * stunden;

		CarrierVehicle garbageTruck1 = Run_AbfallUtils.createGarbageTruck(vehicleId, depotId, earliestStartingTime,
				latestFinishingTime, carrierVehType);

		// define Carriers
		FleetSize fleetSize = FleetSize.FINITE;
		Run_AbfallUtils.defineCarriers(carriers, myCarrier, carrierVehType, vehicleTypes, garbageTruck1, fleetSize);

		// jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, myCarrier, vehicleTypes);

		final Controler controler = new Controler(scenario);

		Run_AbfallUtils.scoringAndManagerFactory(scenario, carriers, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

	}

}
