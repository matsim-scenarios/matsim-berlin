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
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
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
	private static final String modified_Chessboard = "scenarios/chessboard/modifiedChessboard9x9.xml";
	//TODO update to v5.5
	private static final String berlin = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
	private static final String berlinDistrictsWithGarbageInformations = "scenarios/berlin-v5.5-10pct/wasteCollection/districtsWithGarbageInformations.shp";

	private enum netzwerkAuswahl {
		originalChessboard, modifiedChessboard, berlinNetwork
	};

	private enum scenarioAuswahl {
		chessboardTotalGarbageToCollect, chessboardGarbagePerMeterToCollect, berlinSelectedDistricts,
		berlinDistrictsWithInputTotalGarbagePerDistrict, berlinDistrictsWithInputGarbagePerMeter,
		berlinCollectedGarbageForOneDay

	};

	public static void main(String[] args) {

/*		String shapeFileLocation = args[0];
		int jspritIterations = Integer.parseInt(args[1]);
		double volumeDustbin = Double.parseDouble(args[2]); // in liter
		double secondsServiceTimePerDustbin = Double.parseDouble(args[3]);
		boolean electricCar = Boolean.parseBoolean(args[4]);
		String outputLocation = args[5];
*/
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
			config.controler().setOutputDirectory("output/original_Chessboard/04_Distances");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			// modified with some different freespeeds and lengths for some links
			config.controler().setOutputDirectory("output/modified_Chessboard/03_Test_elektrisch");
			config.network().setInputFile(modified_Chessboard);
			break;
		case berlinNetwork:
			// Berlin scenario network
//			config.controler().setOutputDirectory(outputLocation);	//for cluster
			config.controler().setOutputDirectory("output/Berlin_Neu/Montag/Hellersdorf_elektr5");
			config.network().setInputFile(berlin);
			break;
		default:
			new RuntimeException("no network selected.");
		}
		int lastMATSimIteration = 0;
		int jspritIterations = 20;		//Moved to args[] for cluster
		config = AbfallUtils.prepareConfig(config, lastMATSimIteration);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// creates carrier
		Carriers carriers = FreightUtils.getOrCreateCarriers(scenario);
		HashMap<String, Carrier> carrierMap = AbfallUtils.createCarrier();

		// creates a garbage truck type and ads this type to the carrierVehicleTypes
		boolean electricCar = false;	//Moved to args[] for cluster
		AbfallUtils.createAndAddVehicles(electricCar);

		// 
		Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
		HashMap<String, Id<Link>> garbageDumps = AbfallUtils.createDumpMap();

//		Collection<SimpleFeature> districtsWithGarbage = ShapeFileReader
//				.getAllFeatures(shapeFileLocation);
				Collection<SimpleFeature> districtsWithGarbage = ShapeFileReader
				.getAllFeatures(berlinDistrictsWithGarbageInformations);
		AbfallUtils.createMapWithLinksInDistricts(districtsWithGarbage, allLinks);

		double volumeDustbin = 1100; // in liter ////Moved to args[] for cluster
		double secondsServiceTimePerDustbin = 41; //Moved to args[]	for cluster

		switch (scenarioWahl) {
		case chessboardTotalGarbageToCollect:
			int kgGarbageToCollect = 12 * 1000;
			AbfallChessboardUtils.createShipmentsForChessboardI(carrierMap, kgGarbageToCollect, allLinks,
					volumeDustbin, secondsServiceTimePerDustbin, scenario, carriers);
			fleetSize = FleetSize.INFINITE;
			AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize);
			break;
		case chessboardGarbagePerMeterToCollect:
			double kgGarbagePerMeterToCollect = 0.2;
			AbfallChessboardUtils.createShipmentsForChessboardII(carrierMap, kgGarbagePerMeterToCollect, allLinks,
					volumeDustbin, secondsServiceTimePerDustbin, scenario, carriers);
			fleetSize = FleetSize.INFINITE;
			AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize);
			break;
		case berlinSelectedDistricts:
			// day input: MO or DI or MI or DO or FR
			List<String> districtsForShipments = Arrays.asList("Lichtenberg");
			day = "MO";
			AbfallUtils.createShipmentsForSelectedArea(districtsWithGarbage, districtsForShipments, day, garbageDumps,
					scenario, carriers, carrierMap, allLinks, volumeDustbin,
					secondsServiceTimePerDustbin);
			fleetSize = FleetSize.INFINITE;
			AbfallUtils.createCarriersBerlin(districtsWithGarbage, carriers, carrierMap, fleetSize);
			break;
		case berlinDistrictsWithInputGarbagePerMeter:
			// day input: MO or DI or MI or DO or FR
			// input for Map .put("district", double kgGarbagePerMeterToCollect)
			HashMap<String, Double> areasForShipmentPerMeterMap = new HashMap<String, Double>();
			areasForShipmentPerMeterMap.put("Malchow", 1.0);
			day = "MI";
			AbfallUtils.createShipmentsWithGarbagePerMeter(districtsWithGarbage, areasForShipmentPerMeterMap, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, volumeDustbin,
					secondsServiceTimePerDustbin);
			fleetSize = FleetSize.INFINITE;
			AbfallUtils.createCarriersBerlin(districtsWithGarbage, carriers, carrierMap, fleetSize);
			break;
		case berlinDistrictsWithInputTotalGarbagePerDistrict:
			// day input: MO or DI or MI or DO or FR
			// input for Map .put("district", int kgGarbageToCollect)
			HashMap<String, Integer> areasForShipmentPerVolumeMap = new HashMap<String, Integer>();
			areasForShipmentPerVolumeMap.put("Malchow", 5 * 1000);
			// areasForShipmentPerVolumeMap.put("Hansaviertel", 20 * 1000);
			day = "MI";
			AbfallUtils.createShipmentsGarbagePerVolume(districtsWithGarbage, areasForShipmentPerVolumeMap, day,
					garbageDumps, scenario, carriers, carrierMap, allLinks, volumeDustbin,
					secondsServiceTimePerDustbin);
			fleetSize = FleetSize.INFINITE;
			AbfallUtils.createCarriersBerlin(districtsWithGarbage, carriers, carrierMap, fleetSize);
			break;
		case berlinCollectedGarbageForOneDay:
			// MO or DI or MI or DO or FR
			day = "MO";
			AbfallUtils.createShipmentsForSelectedDay(districtsWithGarbage, day, garbageDumps, scenario, carriers,
					carrierMap, allLinks, volumeDustbin, secondsServiceTimePerDustbin);
			fleetSize = FleetSize.INFINITE;
			AbfallUtils.createCarriersBerlin(districtsWithGarbage, carriers, carrierMap, fleetSize);
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

		final Controler controler = new Controler(scenario);

		Freight.configure(controler);
		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans.xml");

		AbfallUtils.outputSummary(districtsWithGarbage, scenario, carrierMap, day, electricCar, volumeDustbin, secondsServiceTimePerDustbin);

	}

}
