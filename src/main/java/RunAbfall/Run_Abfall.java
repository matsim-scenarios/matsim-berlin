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
		chessboard, berlinSelectedDistricts, perVolume, perMeter, day
	};

	public static void main(String[] args) {

		FleetSize fleetSize = null;
		String linkDump;
		String linkDepot;
		double garbagePerMeterToCollect = 0;
		int garbageToCollect = 0;
		String day = null;

		log.setLevel(Level.INFO);

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl = scenarioAuswahl.berlinSelectedDistricts;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/02_InfiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			config.controler().setOutputDirectory("output/modified_Chessboard/07_InfiniteSize_newClassStyle");
			config.network().setInputFile(modified_Chessboard);
			break;
		case berlinNetwork:
			config.controler().setOutputDirectory("output/Berlin/08_InfiniteSize_Test_SelectedArea");
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
		Run_AbfallUtils.craeteMapEnt();

		double volumeBigTrashcan = 1100;
		double serviceTimePerBigTrashcan = 41;

		switch (scenarioWahl) {
		case chessboard:
			// select garbageToCollect OR garbagePerMeterToCollect; the not used should be 0
			linkDump = "j(0,9)R";
			linkDepot = "j(9,9)";
			String vehicleIdDepot = "TruckChessboard";
			garbagePerMeterToCollect = 0.2;
			garbageToCollect = 2 * tonnen;

			if (garbageToCollect != 0) {
				Run_AbfallUtils.createShipmentsForChessboardI(garbageToCollect, allLinks, garbageLinks,
						volumeBigTrashcan, serviceTimePerBigTrashcan, capacityTruck, scenario, carriers, myCarrier,
						linkDump);
			} else
				Run_AbfallUtils.createShipmentsForChessboardII(garbagePerMeterToCollect, allLinks, garbageLinks,
						volumeBigTrashcan, serviceTimePerBigTrashcan, capacityTruck, scenario, carriers, myCarrier,
						linkDump);

			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersForChessboard(linkDepot, vehicleIdDepot, carriers, myCarrier, carrierVehType,
					vehicleTypes, fleetSize);
			break;
		case berlinSelectedDistricts:
			List<String>districtsForShipments = Arrays.asList("Malchow", "Heinersdorf", "Grunewald");
			day = "MI";
			Run_AbfallUtils.createShipmentsForSelectedArea(districtsForShipments, day, garbageDumps, scenario, carriers,
					myCarrier, capacityTruck, allLinks, garbageLinks, features, volumeBigTrashcan,
					serviceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, myCarrier, carrierVehType, vehicleTypes, fleetSize);
			break;
		case perMeter:
			HashMap<String, Double> areasForShipmentPerMeterMap = new HashMap<String, Double>();
			areasForShipmentPerMeterMap.put("Malchow", 1.04);
			areasForShipmentPerMeterMap.put("Hansaviertel", 3.04);
			day = "MI";
			Run_AbfallUtils.createShipmentsGarbagePerMeter(features, areasForShipmentPerMeterMap, day, garbageDumps,
					scenario, carriers, myCarrier, capacityTruck, allLinks, garbageLinks, volumeBigTrashcan,
					serviceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, myCarrier, carrierVehType, vehicleTypes, fleetSize);
			break;
		case perVolume:
			HashMap<String, Integer> areasForShipmentPerVolumeMap = new HashMap<String, Integer>();
			areasForShipmentPerVolumeMap.put("Malchow", 5 * tonnen);
			areasForShipmentPerVolumeMap.put("Hansaviertel", 20 * tonnen);
			day = "MI";
			Run_AbfallUtils.createShipmentsGarbagePerVolume(features, areasForShipmentPerVolumeMap, day, garbageDumps,
					scenario, carriers, myCarrier, capacityTruck, allLinks, garbageLinks, volumeBigTrashcan,
					serviceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, myCarrier, carrierVehType, vehicleTypes, fleetSize);
			break;
		case day:
			day = "MO";
			Run_AbfallUtils.createShipmentsForSelectedDay(day, garbageDumps, scenario, carriers, myCarrier,
					capacityTruck, allLinks, garbageLinks, features, volumeBigTrashcan, serviceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, myCarrier, carrierVehType, vehicleTypes, fleetSize);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}

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
		case berlinSelectedDistricts:
			Run_AbfallUtils.outputSummary(scenario, myCarrier, day);
			break;
		case perMeter:
			Run_AbfallUtils.outputSummary(scenario, myCarrier, day);
			break;
		case perVolume:
			Run_AbfallUtils.outputSummary(scenario, myCarrier, day);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}
	}

}
