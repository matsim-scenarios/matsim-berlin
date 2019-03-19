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
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class Run_Abfall {

	private static final Logger log = Logger.getLogger(Run_Abfall.class);

	private static final String original_Chessboard = "scenarios/networks/originalChessboard9x9.xml";
	private static final String modified_Chessboard = "scenarios/networks/modifiedChessboard9x9.xml";
	private static final String berlin = "original-input-data/berlin-v5.2-1pct.output_network.xml.gz";
	private static final String Berlin_garbage = "C:\\\\Users\\\\erica\\\\OneDrive\\\\Dokumente\\\\Studium\\\\0 Masterarbeit\\\\Shape-Files\\\\Netzwerk_Abfall\\\\BerlinOnly\\\\berlinOnly_garbageNetwork.shp";

	private enum netzwerkAuswahl {
		originalChessboard, modifiedChessboard, berlinNetwork
	};

	private enum scenarioAuswahl {
		chessboard, berlinSelectedDistricts, berlinDistrictsWithInputTotalGarbagePerDistrict,
		berlinDistrictsWithInputGarbagePerMeter, berlinCollectedGarbageForOneDay
	};

	public static void main(String[] args) {

		FleetSize fleetSize = null;
		String day = null;

		log.setLevel(Level.INFO);

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl = scenarioAuswahl.berlinSelectedDistricts;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/01_FiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			config.controler().setOutputDirectory("output/modified_Chessboard/02_InfiniteSize");
			config.network().setInputFile(modified_Chessboard);
			break;
		case berlinNetwork:
			config.controler().setOutputDirectory("output/Berlin/07_InfiniteSize_Test_newStructure");
			config.network().setInputFile(berlin);
			break;
		default:
			new RuntimeException("no network selected.");
		}
		int lastIteration = 0;
		config = Run_AbfallUtils.prepareConfig(config, lastIteration);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();
		HashMap<String, Carrier> carrierMap = Run_AbfallUtils.createCarrier();

		// creates a garbage truck type and ads this type to the carrier
		Run_AbfallUtils.createAndAddVehicles();

		// create shipments
		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(Berlin_garbage);
		HashMap<String, Id<Link>> garbageDumps = Run_AbfallUtils.createDumpMap();

		double volumeBigTrashcan = 1100;
		double secondsServiceTimePerBigTrashcan = 41;

		switch (scenarioWahl) {
		case chessboard:
			// select garbageToCollect OR garbagePerMeterToCollect; the not used should be 0
			String linkDump = "j(0,9)R";
			String linkDepot = "j(9,9)";
			String vehicleIdDepot = "TruckChessboard";
			double kgGarbagePerMeterToCollect = 0.2;
			int kgGarbageToCollect = 0;

			if (kgGarbageToCollect != 0) {
				Run_AbfallUtils.createShipmentsForChessboardI(kgGarbageToCollect, allLinks, garbageLinks,
						volumeBigTrashcan, secondsServiceTimePerBigTrashcan, scenario, carriers,
						carrierMap.get("Chessboard"), linkDump);
			} else
				Run_AbfallUtils.createShipmentsForChessboardII(kgGarbagePerMeterToCollect, allLinks, garbageLinks,
						volumeBigTrashcan, secondsServiceTimePerBigTrashcan, scenario, carriers,
						carrierMap.get("Chessboard"), linkDump);

			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersForChessboard(linkDepot, vehicleIdDepot, carriers, carrierMap.get("Chessboard"), fleetSize);
			Run_AbfallUtils.outputSummaryShipments(scenario, day);
			break;
		case berlinSelectedDistricts:
			List<String> districtsForShipments = Arrays.asList("Malchow", "Wilhelmsruh", "Hansaviertel");
			day = "MI";
			Run_AbfallUtils.createShipmentsForSelectedArea(districtsForShipments, day, garbageDumps, scenario, carriers,
					carrierMap, allLinks, garbageLinks, features, volumeBigTrashcan, secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			Run_AbfallUtils.outputSummaryShipments(scenario, day);
			break;
		case berlinDistrictsWithInputGarbagePerMeter:
			HashMap<String, Double> areasForShipmentPerMeterMap = new HashMap<String, Double>();
			areasForShipmentPerMeterMap.put("Malchow", 1.04);
			// areasForShipmentPerMeterMap.put("Hansaviertel", 3.04);
			day = "MI";
			Run_AbfallUtils.createShipmentsGarbagePerMeter(features, areasForShipmentPerMeterMap, day, garbageDumps,
					scenario, carriers, carrierMap, allLinks, garbageLinks, volumeBigTrashcan,
					secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			Run_AbfallUtils.outputSummaryShipments(scenario, day);
			break;
		case berlinDistrictsWithInputTotalGarbagePerDistrict:
			HashMap<String, Integer> areasForShipmentPerVolumeMap = new HashMap<String, Integer>();
			areasForShipmentPerVolumeMap.put("Malchow", 5 * 1000);
			areasForShipmentPerVolumeMap.put("Hansaviertel", 20 * 1000);
			day = "MI";
			Run_AbfallUtils.createShipmentsGarbagePerVolume(features, areasForShipmentPerVolumeMap, day, garbageDumps,
					scenario, carriers, carrierMap, allLinks, garbageLinks, volumeBigTrashcan,
					secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			Run_AbfallUtils.outputSummaryShipments(scenario, day);
			break;
		case berlinCollectedGarbageForOneDay:
			// MO or DI or MI or DO or FR
			day = "MO";
			Run_AbfallUtils.createShipmentsForSelectedDay(day, garbageDumps, scenario, carriers, carrierMap, allLinks,
					garbageLinks, features, volumeBigTrashcan, secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			Run_AbfallUtils.outputSummaryShipments(scenario, day);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}

		// jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, carrierMap);

		final Controler controler = new Controler(scenario);

		Run_AbfallUtils.scoringAndManagerFactory(scenario, carriers, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

		Run_AbfallUtils.outputSummary(scenario, carrierMap, day);

	}

}
