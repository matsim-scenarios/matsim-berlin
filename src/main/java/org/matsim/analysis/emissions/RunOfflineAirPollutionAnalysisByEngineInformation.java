/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.emissions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author ikaddoura, kturner
 */

public class RunOfflineAirPollutionAnalysisByEngineInformation {
	//TODO: am Anfang einmal durch die Events gehen und nur die Fahrzeuge nehmen, die dann auch tatsächlich "vorkommen"? Derzeit müssten wir auch Fahrzeuge umwandeln, die gar nicht rum fahren.

	private static final Logger log = Logger.getLogger(RunOfflineAirPollutionAnalysisByEngineInformation.class);

	private final String runDirectory;
	private final String runId;
	private final String hbefaWarmFile;
	private final String hbefaColdFile;
	private final String analysisOutputDirectory;

	private final static double shareOfPrivateElectricVehicles = 0.50; // in total. if this number is below the number f electric vehicles in base scenario, nothing will happen

	/*
		Auswahl des Analyse-Runs für die Elktrifizierung der Autos - Ziel ist immer, dass man so viele elektrifiziert, bis 50% der Autos elektrisch sind:
		none: BaseCase --> Keine weiteren Elektrfahrzeuge
		random: Ziehe zufällig Fahrzeuge
		mileageDrivenIncreasing: Tagesfahrleistung: von niedrig nach viel --> Zuerst werden die Fahrzeuge mit den geringsten Tageskilometern elektrifiziert
		tripDistanceIncreasing: Nach Länge des jeweils längsten Auto-Trips am Tag sortieren und diese Fahrzeuge entsprechend elektrifizieren von kurz nach weit
		popDensityIncreasing: Nach Bevölkerungsdichte (Home-locations): je dünner besioedelt um so eher elektro. --> Idee ist dies gewichtet nach der Zonenbesetzung zu machen,
							damit nicht nur Elektrofahrzeuge in dünner besioedleten Bereichen (und dort vollständig), sondern auch in (wohlhabenden) Innenstadtgegenden zu finden sind.
		distanceFromCenterDesx: Nach Entfernung vom Stadtzentrum (Home-locations -- Beeline): je weiter weg um so eher elektro. --> Idee ist dies gewichtet nach der Entfernung zu machen,
							damit nicht nur Elektrofahrzeuge am Stadtrand, sondern auch in (wohlhabenden) Innenstadtgegenden das sind.
	 */
	private enum ElectrificationStrategy {none, random, mileageDrivenIncreasing, tripDistanceIncreasing, popDensityIncreasing, distanceFromCenterDesc};
	private static final ElectrificationStrategy electrificationStrategy = ElectrificationStrategy.none;

	public RunOfflineAirPollutionAnalysisByEngineInformation(String runDirectory, String runId, String hbefaFileWarm, String hbefaFileCold, String analysisOutputDirectory) {
		this.runDirectory = runDirectory;
		this.runId = runId;
		this.hbefaWarmFile = hbefaFileWarm;
		this.hbefaColdFile = hbefaFileCold;

		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;
	}

	public static void main(String[] args) throws IOException {

		if (args.length == 1) {
			String rootDirectory = args[0];
			if (!rootDirectory.endsWith("/")) rootDirectory = rootDirectory + "/";

			final String runDirectory = "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/";
			final String runId = "berlin-v5.5-10pct";

			final String hbefaFileCold = "shared-svn/projects/matsim-germany/hbefa/hbefa-files/v4.1/EFA_ColdStart_Concept_2020_detailed_perTechAverage_Bln_carOnly.csv";
			final String hbefaFileWarm = "shared-svn/projects/matsim-germany/hbefa/hbefa-files/v4.1/EFA_HOT_Concept_2020_detailed_perTechAverage_Bln_carOnly.csv";

			RunOfflineAirPollutionAnalysisByEngineInformation analysis = new RunOfflineAirPollutionAnalysisByEngineInformation(
					rootDirectory + runDirectory,
					runId,
					rootDirectory + hbefaFileWarm,
					rootDirectory + hbefaFileCold,
					rootDirectory + runDirectory + "emission-analysis-hbefa-v4.1_" + electrificationStrategy.name());
			analysis.run();

		} else {
			throw new RuntimeException("Please set the root directory. Aborting...");
		}
	}

	void run() throws IOException {

		Config config = prepareConfig();

		File folder = new File(analysisOutputDirectory);
		folder.mkdirs();

		final String eventsFile = runDirectory + runId + ".output_events.xml.gz";

		final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
		final String linkEmissionAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLink.csv";
		final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLinkPerM.csv";
		final String vehicleTypeFile = analysisOutputDirectory + runId + ".emissionVehicleInformation.csv";

		Scenario scenario = ScenarioUtils.loadScenario(config);
		assignHBEFARoadTypeToNetwork(scenario);

		//Set all vehicles to NON_HBEFA_VEHICLE and ignore them in the analysis. In the next steps we will assign HBEFA types to the cars.
		for (VehicleType vehicleType : scenario.getVehicles().getVehicleTypes().values()) {
			VehicleUtils.setHbefaVehicleCategory(vehicleType.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
		}

		// combustion vehicles
		VehicleType petrolCarVehicleType = prepareVehicleType(scenario, "car_petrol", "petrol (4S)");
		VehicleType dieselCarVehicleType = prepareVehicleType(scenario, "car_diesel", "diesel");
		VehicleType cngVehicleType = prepareVehicleType(scenario, "car_cng", "bifuel CNG/petrol");
		VehicleType lpgVehicleType = prepareVehicleType(scenario, "car_lpg", "bifuel LPG/petrol");
		// electric vehicles
		VehicleType electricVehicleType = prepareVehicleType(scenario, "car_electric", "electricity");
		// plug-in hybrid petrol vehicles
		VehicleType pluginHybridPetrolVehicleType = prepareVehicleType(scenario, "car_pluginHybridPetrol", "Plug-in Hybrid petrol/electric");
		// plug-in hybrid petrol vehicles
		VehicleType pluginHybridDieselVehicleType = prepareVehicleType(scenario, "car_pluginHybridDiesel", "Plug-in Hybrid diesel/electric");


		VehicleType defaultCarVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class));

		List<Id<Vehicle>> vehiclesToChangeToElectric = new ArrayList<>();
		List<Id<Vehicle>> carVehiclesToChangeToSpecificType = new ArrayList<>();
		List<Id<Vehicle>> carNonElectricType = new ArrayList<>();
		List<Id<Vehicle>> carElectricType = new ArrayList<>();

		final Random rnd = MatsimRandom.getLocalInstance();
		int totalVehiclesCounter = 0;

		/**
		 * Lege fest,
		 * a) welche Fahrzeuge hier überhaupt betrachtete werden sollen --> carVehiclesToChangeToSpecificType
		 * b) welche Fahrzeuge dann noch in Elektrofahrzeuge umgewandelt werden sollen.  --> ehiclesToChangeToElectric
		 * TODO: b)  muss eigentlich in einem späteren Schritt erfolgen und in abhängigkeit von der oben gewählten Strategie
		 */
		{

			// randomly change some vehicle types
			for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
				totalVehiclesCounter++;
				if (vehicle.getId().toString().contains("freight")) {
					// some freight vehicles have the type "car", skip them...

				} else if (vehicle.getType().getId().toString().equals(defaultCarVehicleType.getId().toString())) {

					carVehiclesToChangeToSpecificType.add(vehicle.getId());

				} else {
					// ignore all other vehicles
				}
			}
		}

		//Teile die Fahrzeuge entsprechend der Wahrscheinlichkeiten auf - das ist der Basisfall
		{
			final double petrolShare = 0.512744724750519;
			final double dieselShare = 0.462841421365738;
			final double lpgShare = 0.011381645;
			final double cngShare = 0.0038579236716032;
			final double hybridPetrolShare = 0.005743607878685;
			final double hybridDieselShare = 0.00014232617104426;

			for (Id<Vehicle> id : carVehiclesToChangeToSpecificType) {
				scenario.getVehicles().removeVehicle(id);

				VehicleType vehicleType;
				double rndNumber = rnd.nextDouble();
				if (rndNumber < petrolShare) {
					// petrol
					vehicleType = petrolCarVehicleType;
					carNonElectricType.add(id);
				} else if (rndNumber >= petrolShare && rndNumber < petrolShare + dieselShare) {
					// diesel
					vehicleType = dieselCarVehicleType;
					carNonElectricType.add(id);
				} else if (rndNumber >= petrolShare + dieselShare && rndNumber < petrolShare + dieselShare + lpgShare) {
					// lpg
					vehicleType = lpgVehicleType;
					carNonElectricType.add(id);
				} else if (rndNumber >= petrolShare + dieselShare + lpgShare && rndNumber < petrolShare + dieselShare + lpgShare + cngShare) {
					// cng
					vehicleType = cngVehicleType;
					carNonElectricType.add(id);
				} else if (rndNumber >= petrolShare + dieselShare + lpgShare + cngShare && rndNumber < petrolShare + dieselShare + lpgShare + cngShare + hybridPetrolShare) {
					// hybrid petrol
					vehicleType = pluginHybridPetrolVehicleType;
					carNonElectricType.add(id);
				} else if (rndNumber >= petrolShare + dieselShare + lpgShare + cngShare + hybridPetrolShare && rndNumber < petrolShare + dieselShare + lpgShare + cngShare + hybridPetrolShare + hybridDieselShare) {
					// hybrid diesel
					vehicleType = pluginHybridDieselVehicleType;
					carNonElectricType.add(id);
				} else {
					// electric
					vehicleType = electricVehicleType;
					carElectricType.add(id);
				}

				Vehicle vehicleNew = scenario.getVehicles().getFactory().createVehicle(id, vehicleType);
				scenario.getVehicles().addVehicle(vehicleNew);
				log.info("Type for vehicle " + id + " changed to: " + vehicleType.getId().toString());
			}
		}

		writeCountsPerVehicleType(scenario);

		//Define vehicle that has to be changed to electric - dependend of the Stategy
		{
			final int totalNumberOfVehicles = (carNonElectricType.size() + carElectricType.size());
			final int numberVehiclesSwitchToElectric = (int) Math.round( totalNumberOfVehicles * shareOfPrivateElectricVehicles) - carElectricType.size();

			switch (electrificationStrategy) {
				case none:
					//do nothing
					break;
				case random:
					final List<Id<Vehicle>> carNonElectricType2 = new ArrayList<>(carNonElectricType); 	//List for shuffeling and getting the first n (=numberVehiclesSwitchToElectric) objects.
					Collections.shuffle(carNonElectricType2, MatsimRandom.getLocalInstance());
					vehiclesToChangeToElectric = carNonElectricType2.subList(0, numberVehiclesSwitchToElectric -1);
					break;
				case mileageDrivenIncreasing:
					throw new RuntimeException("Strategy not implemented: " + electrificationStrategy);
					//break;
				case tripDistanceIncreasing:
					throw new RuntimeException("Strategy not implemented: " + electrificationStrategy);
					//break;
				case popDensityIncreasing:
					throw new RuntimeException("Strategy not implemented: " + electrificationStrategy);
					//break;
				case distanceFromCenterDesc:
					//Load Population:
					new PopulationReader(scenario).readFile(runDirectory + "../input/berlin-v5.5-10pct.plans.xml.gz");

					log.info("Getting persons' home coordinates...");
					final Map<Id<Person>, Coord> personId2homeCoord = new HashMap<>();
					for (Person person : scenario.getPopulation().getPersons().values()) {
						boolean foundHome = false;
						for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
							if (planElement instanceof Activity){
								Activity act = (Activity) planElement;
								if (act.getType().startsWith("home") && foundHome == false) {
									personId2homeCoord.put(person.getId(), act.getCoord());
									foundHome = true;
								}
							}
						}
					}
					if (personId2homeCoord.isEmpty()) {
						log.warn("No person with home activity.");
						break;
					}

					log.info("Getting persons' home coordinates... Done.");
					log.info("Calculate Distance Home location from Berlin City Center");
					final Map<Id<Person>, Double> personId2DistanceFromCenter = new HashMap<>();
					for (Id<Person> personId :personId2homeCoord.keySet()){
						//Calculate BeelineDistance from CityCenter //TODO Cont. here
						Coord berlinCenterCoord = CoordUtils.createCoord(4595453.826028743,5819787.277826164);
						double distance = CoordUtils.calcEuclideanDistance(personId2homeCoord.get(personId), berlinCenterCoord);
						personId2DistanceFromCenter.put(personId, distance);
					}

					//Ziehe zufällig Anzahl der Fahrzeuge aus der Liste
					List<Pair> vehicleId2DistanceFromCenter = new ArrayList<>();
					for (Id<Vehicle> vehicleId : carNonElectricType) {
						vehicleId2DistanceFromCenter.add(new Pair(vehicleId.toString(), personId2DistanceFromCenter.get(Id.createPersonId(vehicleId.toString()))));
					}
					log.info("# of non-electric Vehicles: " + carNonElectricType.size() + " ; Map vehicleId2DistanceFromCenter.size: " + vehicleId2DistanceFromCenter.size() );
					List listOfExtractedVehicleIds = Arrays.asList(new EnumeratedDistribution(vehicleId2DistanceFromCenter).sample(numberVehiclesSwitchToElectric));
					for (Object vehIdString : listOfExtractedVehicleIds) {
						vehiclesToChangeToElectric.add(Id.createVehicleId(vehIdString.toString()));
					}
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + electrificationStrategy);
			}
			log.info("# of vehicles that should be  change to electric: " + numberVehiclesSwitchToElectric);
			log.info("# of vehicles that are planned for change to electric: " + vehiclesToChangeToElectric.size());
		}



		//Nun wandle Fahrzeuge zu die ausgewählten Elektrofahrzeugen um
		{
			for (Id<Vehicle> id : vehiclesToChangeToElectric) {
				scenario.getVehicles().removeVehicle(id);
				Vehicle vehicleNew = scenario.getVehicles().getFactory().createVehicle(id, electricVehicleType);
				scenario.getVehicles().addVehicle(vehicleNew);
				log.info("Type for vehicle " + id + " changed to electric.");
			}
		}


		// the following is copy paste from the example...

		log.info("Start emission analysis");

		EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);

		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		EmissionsOnLinkHandler emissionsEventHandler = new EmissionsOnLinkHandler();
		eventsManager.addHandler(emissionsEventHandler);

		eventsManager.initProcessing();

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

		log.info("Closing events file...");
		emissionEventWriter.closeFile();

		log.info("Emission analysis completed.");

		log.info("Total number of vehicles in scenario: " + totalVehiclesCounter);
		log.info("Number of passenger car vehicles for analysis: " + carVehiclesToChangeToSpecificType.size());
		log.info("Number of passenger car vehicles that has been changed to electric vehicles: " + vehiclesToChangeToElectric.size());
		log.info("This is NOT the number of all electric cars, since there might be some electric vehicles before changing.");

		writeCountsPerVehicleType(scenario);

		writeOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, vehicleTypeFile, scenario, emissionsEventHandler);

		log.info("### Done. ###");

	}

	private void writeCountsPerVehicleType(Scenario scenario) {
		//Count vehicles per Type
		LinkedHashMap<VehicleType, Integer> numberOfVehiclesPerType = new LinkedHashMap<VehicleType, Integer>();
		for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
			numberOfVehiclesPerType.merge(vehicle.getType(), 1, Integer::sum);
		}

		log.info("Number of vehicles per Type: ");
		for (VehicleType vehicleType : numberOfVehiclesPerType.keySet()) {
			log.info(vehicleType.getId().toString() + " : " + numberOfVehiclesPerType.get(vehicleType));
		}
	}

	private void writeOutput(String linkEmissionAnalysisFile, String linkEmissionPerMAnalysisFile, String vehicleTypeFile, Scenario scenario, EmissionsOnLinkHandler emissionsEventHandler) throws IOException {

		{
			log.info("Writing output...");

			File file1 = new File(linkEmissionAnalysisFile);

			BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

			bw1.write("linkId");

			for (Pollutant pollutant : Pollutant.values()) {
				bw1.write(";" + pollutant + " [g]");
			}
			bw1.newLine();

			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

			for (Id<Link> linkId : link2pollutants.keySet()) {
				bw1.write(linkId.toString());

				for (Pollutant pollutant : Pollutant.values()) {
					double value = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						value = link2pollutants.get(linkId).get(pollutant);
					}
					bw1.write(";" + value);
				}
				bw1.newLine();
			}

			bw1.close();
			log.info("Output written to " + linkEmissionAnalysisFile);
		}

		{
			File file1 = new File(linkEmissionPerMAnalysisFile);

			BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

			bw1.write("linkId");

			for (Pollutant pollutant : Pollutant.values()) {
				bw1.write(";" + pollutant + " [g/m]");
			}
			bw1.newLine();

			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

			for (Id<Link> linkId : link2pollutants.keySet()) {
				bw1.write(linkId.toString());

				for (Pollutant pollutant : Pollutant.values()) {
					double emission = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emission = link2pollutants.get(linkId).get(pollutant);
					}

					double emissionPerM = Double.NaN;
					Link link = scenario.getNetwork().getLinks().get(linkId);
					if (link != null) {
						emissionPerM = emission / link.getLength();
					}

					bw1.write(";" + emissionPerM);
				}
				bw1.newLine();
			}

			bw1.close();
			log.info("Output written to " + linkEmissionPerMAnalysisFile);
		}

		{
			File file2 = new File(vehicleTypeFile);

			BufferedWriter bw2 = new BufferedWriter(new FileWriter(file2));

			bw2.write("vehicleId;vehicleType;emissionsConcept");
			bw2.newLine();

			for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
				String emissionsConcept = "null";
				if (vehicle.getType().getEngineInformation() != null && VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) != null) {
					emissionsConcept = VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()).toString();
				}

				bw2.write(vehicle.getId() + ";" + vehicle.getType().getId().toString() + ";" + emissionsConcept);
				bw2.newLine();
			}

			bw2.close();
			log.info("Output written to " + vehicleTypeFile);
		}
	}

	private VehicleType prepareVehicleType(Scenario scenario, String vehicleTypeId, String emissionsConcept) {
		VehicleType carVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create(vehicleTypeId, VehicleType.class));
		EngineInformation engineInformation = carVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology(engineInformation, "average");
		VehicleUtils.setHbefaSizeClass(engineInformation, "average");
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, emissionsConcept);
		scenario.getVehicles().addVehicleType(carVehicleType);
		return carVehicleType;
	}

	private void assignHBEFARoadTypeToNetwork(Scenario scenario) {
		// network
		for (Link link : scenario.getNetwork().getLinks().values()) {

			double freespeed = Double.NaN;

			if (link.getFreespeed() <= 13.888889) {
				freespeed = link.getFreespeed() * 2;
				// for non motorway roads, the free speed level was reduced
			} else {
				freespeed = link.getFreespeed();
				// for motorways, the original speed levels seems ok.
			}

			if(freespeed <= 8.333333333){ //30kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
			} else if(freespeed <= 11.111111111){ //40kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
			} else if(freespeed <= 13.888888889){ //50kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 16.666666667){ //60kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 19.444444444){ //70kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
			} else if(freespeed <= 22.222222222){ //80kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
			} else if(freespeed > 22.222222222){ //faster
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
			} else{
				throw new RuntimeException("Link not considered...");
			}
		}
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(runDirectory + runId + ".output_allVehicles.xml.gz");
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
//		config.transit().setTransitScheduleFile(runDirectory + runId + ".output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile(runDirectory + runId + ".output_transitVehicles.xml.gz");
		config.global().setCoordinateSystem("EPSG:31468");
//		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort);
		eConfig.setDetailedColdEmissionFactorsFile(hbefaColdFile);
		eConfig.setDetailedWarmEmissionFactorsFile(hbefaWarmFile);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);
		eConfig.setWritingEmissionsEvents(true);
		return config;
	}

}

