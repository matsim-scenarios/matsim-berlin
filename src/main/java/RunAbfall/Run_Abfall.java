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
		chessboard, berlinSelectedAreas
	};

	private enum garbageVolume {
		perWeek, perMeterAndWeek
	};

	public static void main(String[] args) {
		
		String linkIdMhkwRuhleben = "142010";
		String linkIdMpsPankow = "145812";
		String linkIdMpsReinickendorf = "59055";
		String linkIdUmladestationGradestrasse = "71781";
		String linkIdGruenauerStr = "97944";
		String depotForckenbeck = "27766";
		String depotMalmoeerStr = "116212";
		String depotNordring = "42882";
		String depotGradestrasse = "71781";
		double garbagePerMeterAndWeek = 0;
		double distanceWithShipments = 0;
		int garbagePerWeek = 0;
		List<String> areaForShipments = null;
		String day = null;
		Id<Link> dumpId = null;
		log.setLevel(Level.INFO);
		HashMap<String, Id<Link>> garbageDumps = new HashMap<String, Id<Link>>();
		garbageDumps.put("Ruhleben", Id.createLinkId(linkIdMhkwRuhleben));
		garbageDumps.put("Pankow", Id.createLinkId(linkIdMpsPankow));
		garbageDumps.put("Gradestr", Id.createLinkId(linkIdUmladestationGradestrasse));
		garbageDumps.put("ReinickenD", Id.createLinkId(linkIdMpsReinickendorf));
		garbageDumps.put("GruenauerStr", Id.createLinkId(linkIdGruenauerStr));

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl = scenarioAuswahl.berlinSelectedAreas;
		garbageVolume garbageVolumeChoice = garbageVolume.perWeek;

		// MATSim config
		Config config = ConfigUtils.createConfig();

		switch (netzwerkWahl) {
		case originalChessboard:
			config.controler().setOutputDirectory("output/original_Chessboard/02_InfiniteSize");
			config.network().setInputFile(original_Chessboard);
			break;
		case modifiedChessboard:
			config.controler().setOutputDirectory("output/modified_Chessboard/05_InfiniteSize_newTruckType");
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
		/*
		 * ShapeFileReader shapeFileReader = new ShapeFileReader();
		 * shapeFileReader.readFileAndInitialize(Berlin_garbage);
		 * Collection<SimpleFeature> features = shapeFileReader.getFeatureSet();
		 */

		switch (scenarioWahl) {
		case chessboard:
			linkIdMhkwRuhleben = ("j(0,9)R");
			depotForckenbeck = "j(9,9)";
			garbagePerMeterAndWeek = 0.2;
			garbagePerWeek = 2 * tonnen;
			areaForShipments = Arrays.asList("Chessboard");
			for (Link link : allLinks.values()) {
				if (link.getCoord().getX() < 8000 && link.getFreespeed() < 12) {
					garbageLinks.put(link.getId(), link);
					distanceWithShipments = distanceWithShipments + link.getLength();
				}
			}
			break;
		case berlinSelectedAreas:
			
			double volumeBigTrashcan = 1100;
			double serviceTimePerBigTrashcan = 41;
			garbagePerMeterAndWeek = 3.04; // Berechnung aus Excel
			// garbagePerWeek = 500 * tonnen; // noch Zufallseingabe, da Gebiet unbestimmt
			areaForShipments = Arrays.asList("Malchow", "Hansaviertel");
			day = "MI";
			for (String area : areaForShipments) {
				for (SimpleFeature simpleFeature : features) {
					if (simpleFeature.getAttribute("Ortsteilna").equals(area)) {
						garbagePerWeek = (int) ((double)simpleFeature.getAttribute(day) * tonnen);
						dumpId = garbageDumps.get(simpleFeature.getAttribute("Mi-Ent"));
						for (Link link : allLinks.values()) {
							if (Id.createLinkId(simpleFeature.getAttribute("ID").toString()) == link.getId()) {
								if (link.getFreespeed() < 12 && link.getAllowedModes().contains("car")) {

									garbageLinks.put(link.getId(), link);
									distanceWithShipments = distanceWithShipments + link.getLength();

								}
							}
						}

					}

				}
				Run_AbfallUtils.createShipmentsForCarrierII(garbagePerWeek, volumeBigTrashcan,
						serviceTimePerBigTrashcan, distanceWithShipments, capacityTruck, garbageLinks, scenario,
						myCarrier, dumpId, carriers);
				distanceWithShipments = 0;
				garbageLinks.clear();
			}
			carriers.addCarrier(myCarrier);
			break;
		default:
			new RuntimeException("no scenario selected.");
		}
		double volumeBigTrashcan = 1100;
		double serviceTimePerBigTrashcan = 41;
		// Auswahl, ob die für das Netzwerk eine Abfallmenge definiert wird oder die
		// Menge aus der länge der Straßen ermittelt wird
		switch (garbageVolumeChoice) {
		case perMeterAndWeek:
			Run_AbfallUtils.createShipmentsForCarrierI(garbagePerMeterAndWeek, volumeBigTrashcan,
					serviceTimePerBigTrashcan, capacityTruck, garbageLinks, scenario, myCarrier, linkIdMhkwRuhleben,
					carriers);
			break;
		case perWeek:
			/*
			 * Run_AbfallUtils.createShipmentsForCarrierII(garbagePerWeek,
			 * volumeBigTrashcan, serviceTimePerBigTrashcan, distanceWithShipments,
			 * capacityTruck, garbageLinks, scenario, myCarrier, garbageDumpId, carriers);
			 */
			break;
		default:
			new RuntimeException("no garbageVolume selected.");
		}
		// create vehicle at depot
		String vehicleIdForckenbeck = "TruckForckenbeck";
		String vehicleIdMalmoeer = "TruckMalmoeer";
		String vehicleIdNordring = "TruckNordring";
		String vehicleIdGradestrasse = "TruckGradestrasse";
		double earliestStartingTime = 6 * stunden;
		double latestFinishingTime = 15 * stunden;

		CarrierVehicle vehicleForckenbeck = Run_AbfallUtils.createGarbageTruck(vehicleIdForckenbeck, depotForckenbeck,
				earliestStartingTime, latestFinishingTime, carrierVehType);
		CarrierVehicle vehicleMalmoeerStr = Run_AbfallUtils.createGarbageTruck(vehicleIdMalmoeer, depotMalmoeerStr,
				earliestStartingTime, latestFinishingTime, carrierVehType);
		CarrierVehicle vehicleNordring = Run_AbfallUtils.createGarbageTruck(vehicleIdNordring, depotNordring,
				earliestStartingTime, latestFinishingTime, carrierVehType);
		CarrierVehicle vehicleGradestrasse = Run_AbfallUtils.createGarbageTruck(vehicleIdGradestrasse,
				depotGradestrasse, earliestStartingTime, latestFinishingTime, carrierVehType);

		// define Carriers
		FleetSize fleetSize = FleetSize.INFINITE;
		Run_AbfallUtils.defineCarriers(carriers, myCarrier, carrierVehType, vehicleTypes, vehicleForckenbeck,
				vehicleMalmoeerStr, vehicleNordring, vehicleGradestrasse, fleetSize);
		// jsprit
		Run_AbfallUtils.solveWithJsprit(scenario, carriers, myCarrier, vehicleTypes);

		final Controler controler = new Controler(scenario);

		Run_AbfallUtils.scoringAndManagerFactory(scenario, carriers, controler);

		controler.run();

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans_Test01.xml");

		Run_AbfallUtils.outputSummary(scenario, myCarrier,areaForShipments,day);
	}
}
