package RunAbfall;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.opengis.feature.simple.SimpleFeature;

public class Run_Abfall {

	static int stunden = 3600;
	static int minuten = 60;
	static int tonnen = 1000;

	private static final Logger log = Logger.getLogger(Run_Abfall.class);

	private static final String original_Chessboard = "scenarios/networks/originalChessboard9x9.xml";
	private static final String modified_Chessboard = "scenarios/networks/modifiedChessboard9x9.xml";
	private static final String berlin = "original-input-data/berlin-v5.2-1pct.output_network.xml.gz";
	private static final String Berlin_garbage = "C:\\Users\\erica\\OneDrive\\Dokumente\\Studium\\0 Masterarbeit\\Shape-Files\\Netzwerk_Abfall\\BerlinOnly\\berlinOnly_garbageNetwork.shp";

	private enum netzwerkAuswahl {
		originalChessboard, modifiedChessboard, berlinNetwork
	};

	private enum scenarioAuswahl {
		chessboard, berlinSelectedAreas, perWeek, perMeterAndWeek
	};

	public static void main(String[] args) {

		FleetSize fleetSize;
		String linkIdDump;
		String linkIdDepot;
		double garbagePerMeterToCollect = 0;
		int garbageToCollect = 0;
		List<String> areaForShipments = null;
		String day = null;

		log.setLevel(Level.INFO);

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.modifiedChessboard;
		scenarioAuswahl scenarioWahl = scenarioAuswahl.chessboard;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/02_InfiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			config.controler().setOutputDirectory("output/modified_Chessboard/06_InfiniteSize_newClassStyle");
			config.network().setInputFile(modified_Chessboard);
			break;
		case berlinNetwork:
			config.controler().setOutputDirectory("output/Berlin/07_InfiniteSize_Test_allShape");
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

		// create a garbage truck type
		String vehicleTypeId = "TruckType1";
		int capacityTruck = 11 * tonnen; // Berliner Zeitung
		double maxVelocity = 80 / 3.6;
		double costPerDistanceUnit = 0.000369; // Berechnung aus Excel
		double costPerTimeUnit = 0.0; // Lohnkosten bei Fixkosten integriert
		double fixCosts = 957.17; // Berechnung aus Excel
		FuelType engineInformation = FuelType.diesel;
		double literPerMeter = 0.003; // Berechnung aus Ecxel
		CarrierVehicleType carrierVehType = Run_AbfallUtils.createGarbageTruckType(vehicleTypeId, capacityTruck,
				maxVelocity, costPerDistanceUnit, costPerTimeUnit, fixCosts, engineInformation, literPerMeter);
		CarrierVehicleTypes vehicleTypes = Run_AbfallUtils.adVehicleType(carrierVehType);

		// create shipments
		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(Berlin_garbage);
		HashMap<String, Id<Link>> garbageDumps = Run_AbfallUtils.createDumpMap();

		double volumeBigTrashcan = 1100;
		double serviceTimePerBigTrashcan = 41;
		switch (scenarioWahl) {
		case chessboard:
			linkIdDump = "j(0,9)R";
			linkIdDepot = "j(9,9)";
			String vehicleIdDepot = "TruckChessboard";
			garbagePerMeterToCollect = 0.2;
			garbageToCollect = 2 * tonnen;
			Run_AbfallUtils.createShipmentsForChessboardI(garbageToCollect, allLinks, garbageLinks, volumeBigTrashcan,
					serviceTimePerBigTrashcan, capacityTruck, scenario, carriers, myCarrier, linkIdDump);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersForChessboard(linkIdDepot,vehicleIdDepot, carriers, myCarrier, carrierVehType, vehicleTypes, fleetSize);
			break;
		case berlinSelectedAreas:
			areaForShipments = Arrays.asList("Malchow", "Hansaviertel");
			day = "MI";
			Run_AbfallUtils.createShipmentsForSelectedArea(areaForShipments, day, garbageDumps, scenario, carriers,
					myCarrier, capacityTruck, allLinks, garbageLinks, features, volumeBigTrashcan,
					serviceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, myCarrier, carrierVehType, vehicleTypes, fleetSize);
			break;
		case perMeterAndWeek:
		//	garbagePerMeterToCollect = 3.04;
		//	Run_AbfallUtils.createShipmentsForCarrierI(garbagePerMeterToCollect, volumeBigTrashcan,
		//			serviceTimePerBigTrashcan, capacityTruck, garbageLinks, scenario, myCarrier, linkIdMhkwRuhleben,
		//			carriers);
			break;
		case perWeek:
			garbageToCollect = 500 * tonnen;
			// garbageDumpId = "142010";
			// Run_AbfallUtils.createShipmentsForCarrierII(garbageToCollect,
			// volumeBigTrashcan, serviceTimePerBigTrashcan,
			// distanceWithShipments, capacityTruck, garbageLinks, scenario, myCarrier,
			// garbageDumpId, carriers);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}

		// create vehicle at depot

		// jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, myCarrier, vehicleTypes);

		final Controler controler = new Controler(scenario);

		Run_AbfallUtils.scoringAndManagerFactory(scenario, carriers, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");
		switch (scenarioWahl) {
		case chessboard:
			break;
		case berlinSelectedAreas:
			Run_AbfallUtils.outputSummary(scenario, myCarrier, areaForShipments, day);
		case perMeterAndWeek:
			Run_AbfallUtils.outputSummary(scenario, myCarrier, areaForShipments, day);
		case perWeek:
			Run_AbfallUtils.outputSummary(scenario, myCarrier, areaForShipments, day);
		default:
			new RuntimeException("no scenario selected.");
		}
	}

}
