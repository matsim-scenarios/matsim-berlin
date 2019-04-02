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

/**
 * @author Ricardo Ewert MA: Modellierung und Simulation der staedtischen
 *         Abfallwirtschaft am Beispiel Berlins
 *
 */
public class Run_Abfall {

	static final Logger log = Logger.getLogger(Run_Abfall.class);

	private static final String original_Chessboard = "scenarios/networks/originalChessboard9x9.xml";
	private static final String modified_Chessboard = "scenarios/networks/modifiedChessboard9x9.xml";
	private static final String berlin = "original-input-data/berlin-v5.2-1pct.output_network.xml.gz";
	private static final String berlinDistrictsWithGarbageInformations = "scenarios/garbageInput/districtsWithGarbageInformations.shp";

	private enum netzwerkAuswahl {
		originalChessboard, modifiedChessboard, berlinNetwork
	};

	private enum scenarioAuswahl {
		chessboardTotalGarbageToCollect, chessboardGarbagePerMeterToCollect, berlinSelectedDistricts,
		berlinDistrictsWithInputTotalGarbagePerDistrict, berlinDistrictsWithInputGarbagePerMeter,
		berlinCollectedGarbageForOneDay

	};

	public static void main(String[] args) {

		FleetSize fleetSize = null;
		String day = null;

		log.setLevel(Level.INFO);

		/*
		 * You have to decide the network. If you choose one of the chessboard networks,
		 * you have to select a chessboard scenario and if you select the Berlin
		 * network, you have to select one of the Berlin cases. The beginning of the
		 * name of the scenario shows you the needed network.
		 */
		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl = scenarioAuswahl.berlinCollectedGarbageForOneDay;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/03_InfiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			// modified with some different freespeeds and lengths for some links
			config.controler().setOutputDirectory("output/modified_Chessboard/02_InfiniteSize");
			config.network().setInputFile(modified_Chessboard);
			break;
		case berlinNetwork:
			// Berlin scenario network
			config.controler().setOutputDirectory("output/Berlin/08_InfiniteSize_MO");
			config.network().setInputFile(berlin);
			break;
		default:
			new RuntimeException("no network selected.");
		}
		int lastIteration = 0;
		config = Run_AbfallUtils.prepareConfig(config, lastIteration);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// creates carrier
		Carriers carriers = new Carriers();
		HashMap<String, Carrier> carrierMap = Run_AbfallUtils.createCarrier();

		// creates a garbage truck type and ads this type to the carrier
		Run_AbfallUtils.createAndAddVehicles();

		// create shipments
		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		HashMap<String, Id<Link>> garbageDumps = Run_AbfallUtils.createDumpMap();

		Collection<SimpleFeature> districtsWithGarbage = ShapeFileReader
				.getAllFeatures(berlinDistrictsWithGarbageInformations);
		Run_AbfallUtils.createMapWithLinksInDistricts(districtsWithGarbage, allLinks);

		double volumeBigTrashcan = 1100; // in liter
		double secondsServiceTimePerBigTrashcan = 41;

		switch (scenarioWahl) {
		case chessboardTotalGarbageToCollect:
			int kgGarbageToCollect = 40 * 1000;
			Run_AbfallChessboardUtils.createShipmentsForChessboardI(carrierMap, kgGarbageToCollect, allLinks,
					garbageLinks, volumeBigTrashcan, secondsServiceTimePerBigTrashcan, scenario, carriers);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize);
			break;
		case chessboardGarbagePerMeterToCollect:
			double kgGarbagePerMeterToCollect = 0.2;
			Run_AbfallChessboardUtils.createShipmentsForChessboardII(carrierMap, kgGarbagePerMeterToCollect, allLinks,
					garbageLinks, volumeBigTrashcan, secondsServiceTimePerBigTrashcan, scenario, carriers);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize);
			break;
		case berlinSelectedDistricts:
			// day input: MO or DI or MI or DO or FR
			List<String> districtsForShipments = Arrays.asList("Malchow", "Mitte");
			day = "MI";
			Run_AbfallUtils.createShipmentsForSelectedArea(districtsWithGarbage, districtsForShipments, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, garbageLinks, volumeBigTrashcan,
					secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			break;
		case berlinDistrictsWithInputGarbagePerMeter:
			// day input: MO or DI or MI or DO or FR
			// input for Map .put("district", double kgGarbagePerMeterToCollect)
			HashMap<String, Double> areasForShipmentPerMeterMap = new HashMap<String, Double>();
			areasForShipmentPerMeterMap.put("Malchow", 1.04);
			day = "MI";
			Run_AbfallUtils.createShipmentsWithGarbagePerMeter(districtsWithGarbage, areasForShipmentPerMeterMap, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, garbageLinks, volumeBigTrashcan,
					secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			break;
		case berlinDistrictsWithInputTotalGarbagePerDistrict:
			// day input: MO or DI or MI or DO or FR
			// input for Map .put("district", int kgGarbageToCollect)
			HashMap<String, Integer> areasForShipmentPerVolumeMap = new HashMap<String, Integer>();
			areasForShipmentPerVolumeMap.put("Malchow", 5 * 1000);
			// areasForShipmentPerVolumeMap.put("Hansaviertel", 20 * 1000);
			day = "MI";
			Run_AbfallUtils.createShipmentsGarbagePerVolume(districtsWithGarbage, areasForShipmentPerVolumeMap, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, garbageLinks, volumeBigTrashcan,
					secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			break;
		case berlinCollectedGarbageForOneDay:
			// MO or DI or MI or DO or FR
			day = "FR";
			Run_AbfallUtils.createShipmentsForSelectedDay(districtsWithGarbage, day, garbageDumps, scenario, carriers,
					carrierMap, allLinks, garbageLinks, volumeBigTrashcan, secondsServiceTimePerBigTrashcan);
			fleetSize = FleetSize.INFINITE;
			Run_AbfallUtils.createCarriersBerlin(carriers, carrierMap, fleetSize);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}
		/*
		 * This xml output gives a summary with information about the created shipments,
		 * so that you can already have this information, while jsprit and matsim are
		 * still running.
		 */
		Run_AbfallUtils.outputSummaryShipments(scenario, day, carrierMap);
		// jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, carrierMap);

		final Controler controler = new Controler(scenario);

		Run_AbfallUtils.scoringAndManagerFactory(scenario, carriers, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

		Run_AbfallUtils.outputSummary(districtsWithGarbage, scenario, carrierMap, day);

	}

}
