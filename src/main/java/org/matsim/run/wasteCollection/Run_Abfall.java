package org.matsim.run.wasteCollection;

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
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
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

	private static final String original_Chessboard = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
	private static final String berlin = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
	private static final String berlinDistrictsWithGarbageInformations = "scenarios/berlin-v5.5-10pct/input/wasteCollection/garbageInput/districtsWithGarbageInformations.shp";
	private static final String inputVehicleTypes = "scenarios/berlin-v5.5-10pct/input/wasteCollection/vehicleTypes.xml";
	private static final String inputCarriersWithDieselVehicle = "scenarios/berlin-v5.5-10pct/input/wasteCollection/carriers_diesel_vehicle.xml";
	private static final String inputCarriersWithMediumBatteryVehicle = "scenarios/berlin-v5.5-10pct/input/wasteCollection/carriers_medium_EV.xml";
	private static final String inputCarriersWithSmallBatteryVehicle = "scenarios/berlin-v5.5-10pct/input/wasteCollection/carriers_small_EV.xml";

	private enum netzwerkAuswahl {
		originalChessboard, berlinNetwork
	};

	private enum scenarioAuswahl {
		chessboardTotalGarbageToCollect, chessboardGarbagePerMeterToCollect, berlinSelectedDistricts,
		berlinDistrictsWithInputTotalGarbagePerDistrict, berlinDistrictsWithInputGarbagePerMeter,
		berlinCollectedGarbageForOneDay

	};

	private enum carrierChoice {
		carriersWithDieselVehicle, carriersWithMediumBattereyVehicle, carriersWithSmallBatteryVehicle,
		carriersFromInputFile
	}

	public static void main(String[] args) throws Exception {

		/*
		 * You have to decide the network. If you choose one of the chessboard networks,
		 * you have to select a chessboard scenario and if you select the Berlin
		 * network, you have to select one of the Berlin cases. The beginning of the
		 * name of the scenario shows you the needed network.
		 */

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl = null;
		carrierChoice chosenCarrier = null;
		int jspritIterations;
		double volumeDustbinInLiters;
		double secondsServiceTimePerDustbin;
		String outputLocation;
		String day;
		String networkChangeEventsFileLocation;
		String carriersFileLocation = null;
		String vehicleTypesFileLocation = null;
		String shapeFileLocation;
		boolean oneCarrierForOneDistrict;

		for (String arg : args) {
			log.info(arg);
		}
		if (args.length == 0) {
			chosenCarrier = carrierChoice.carriersWithDieselVehicle;
			scenarioWahl = scenarioAuswahl.berlinSelectedDistricts;
			shapeFileLocation = berlinDistrictsWithGarbageInformations;
			oneCarrierForOneDistrict = true;
			jspritIterations = 100;
			volumeDustbinInLiters = 1100; // in liter
			secondsServiceTimePerDustbin = 41;
			outputLocation = "output/wasteCollection/Scenario1";
			day = "MO";
			networkChangeEventsFileLocation = "";
		} else {
			scenarioWahl = scenarioAuswahl.berlinCollectedGarbageForOneDay;
			jspritIterations = Integer.parseInt(args[0]);
			volumeDustbinInLiters = Double.parseDouble(args[1]); // in liter
			secondsServiceTimePerDustbin = Double.parseDouble(args[2]);
			day = args[3];
			outputLocation = args[4];
			vehicleTypesFileLocation = args[5];
			networkChangeEventsFileLocation = args[6];
			carriersFileLocation = args[7];
			shapeFileLocation = args[8];
			oneCarrierForOneDistrict = Boolean.parseBoolean(args[9]);
			chosenCarrier = carrierChoice.carriersFromInputFile;
		}

		log.setLevel(Level.INFO);

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/04_Distances");
			config.network().setInputFile(original_Chessboard);
			break;
		case berlinNetwork:
			// Berlin scenario network
			config.controler().setOutputDirectory(outputLocation);
			config.network().setInputFile(berlin);
			if (networkChangeEventsFileLocation != "") {
				log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFileLocation);
				config.network().setTimeVariantNetwork(true);
				config.network().setChangeEventsInputFile(networkChangeEventsFileLocation);
			}
			break;
		default:
			new RuntimeException("no network selected.");
		}
		switch (chosenCarrier) {
		case carriersWithDieselVehicle:
			vehicleTypesFileLocation =  inputVehicleTypes;
			carriersFileLocation = inputCarriersWithDieselVehicle;
			break;
		case carriersWithSmallBatteryVehicle:
			vehicleTypesFileLocation =  inputVehicleTypes;
			carriersFileLocation = inputCarriersWithSmallBatteryVehicle;
			break;
		case carriersWithMediumBattereyVehicle:
			vehicleTypesFileLocation =  inputVehicleTypes;
			carriersFileLocation = inputCarriersWithMediumBatteryVehicle;
			break;
		case carriersFromInputFile:			
			break;
		default:
			new RuntimeException("no carriers selected.");
		}
		config = AbfallUtils.prepareConfig(config, 0, vehicleTypesFileLocation, carriersFileLocation);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		// creates carrier
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		HashMap<String, Carrier> carrierMap = AbfallUtils.createCarrier(carriers);

		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		HashMap<String, Id<Link>> garbageDumps = AbfallUtils.createDumpMap();

		Collection<SimpleFeature> districtsWithGarbage = ShapeFileReader
				.getAllFeatures(shapeFileLocation);
		AbfallUtils.createMapWithLinksInDistricts(districtsWithGarbage, allLinks);

		switch (scenarioWahl) {
		case chessboardTotalGarbageToCollect:
			int kgGarbageToCollect = 12 * 1000;
			CarrierVehicleTypes carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
			AbfallChessboardUtils.createShipmentsForChessboardI(carrierMap, kgGarbageToCollect, allLinks,
					volumeDustbinInLiters, secondsServiceTimePerDustbin, scenario, carriers);
			FleetSize fleetSize = FleetSize.INFINITE;
			AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize, carrierVehicleTypes);
			break;
		case chessboardGarbagePerMeterToCollect:
			double kgGarbagePerMeterToCollect = 0.2;
			CarrierVehicleTypes carrierVehicleTypes2 = FreightUtils.getCarrierVehicleTypes(scenario);
			AbfallChessboardUtils.createShipmentsForChessboardII(carrierMap, kgGarbagePerMeterToCollect, allLinks,
					volumeDustbinInLiters, secondsServiceTimePerDustbin, scenario, carriers);
			FleetSize fleetSize2 = FleetSize.INFINITE;
			AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize2, carrierVehicleTypes2);
			break;
		case berlinSelectedDistricts:
			// day input: MO or DI or MI or DO or FR
			List<String> districtsForShipments = Arrays.asList("Malchow");
			day = "MI";
			AbfallUtils.createShipmentsForSelectedArea(districtsWithGarbage, districtsForShipments, day, garbageDumps,
					scenario, carriers, carrierMap, allLinks, volumeDustbinInLiters, secondsServiceTimePerDustbin);
			break;
		case berlinDistrictsWithInputGarbagePerMeter:
			// day input: MO or DI or MI or DO or FR
			// input for Map .put("district", double kgGarbagePerMeterToCollect)
			HashMap<String, Double> areasForShipmentPerMeterMap = new HashMap<String, Double>();
			areasForShipmentPerMeterMap.put("Malchow", 1.0);
			day = "MI";
			AbfallUtils.createShipmentsWithGarbagePerMeter(districtsWithGarbage, areasForShipmentPerMeterMap, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, volumeDustbinInLiters,
					secondsServiceTimePerDustbin);
			break;
		case berlinDistrictsWithInputTotalGarbagePerDistrict:
			// day input: MO or DI or MI or DO or FR
			// input for Map .put("district", int kgGarbageToCollect)
			HashMap<String, Integer> areasForShipmentPerVolumeMap = new HashMap<String, Integer>();
			areasForShipmentPerVolumeMap.put("Malchow", 5 * 1000);
			// areasForShipmentPerVolumeMap.put("Hansaviertel", 20 * 1000);
			day = "MI";
			AbfallUtils.createShipmentsGarbagePerVolume(districtsWithGarbage, areasForShipmentPerVolumeMap, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, volumeDustbinInLiters,
					secondsServiceTimePerDustbin);
			break;
		case berlinCollectedGarbageForOneDay:
			// MO or DI or MI or DO or FR
			AbfallUtils.createShipmentsForSelectedDay(districtsWithGarbage, day, garbageDumps, scenario, carriers,
					carrierMap, allLinks, volumeDustbinInLiters, secondsServiceTimePerDustbin, oneCarrierForOneDistrict);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}
		/*
		 * This xml output gives a summary with information about the created shipments,
		 * so that you can already have this information, while jsprit and matsim are
		 * still running.
		 */
		AbfallUtils.outputSummaryShipments(scenario, day, carrierMap);
		// jsprit

		AbfallUtils.solveWithJsprit(scenario, carriers, carrierMap, jspritIterations);

		// final Controler controler = new Controler(scenario);
		Controler controler = AbfallUtils.prepareControler(scenario);

//		AbfallUtils.scoringAndManagerFactory(scenario, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans.xml");

		AbfallUtils.outputSummary(districtsWithGarbage, scenario, carrierMap, day, volumeDustbinInLiters,
				secondsServiceTimePerDustbin);
		AbfallUtils.createResultFile(scenario, carriers);
	}
}
