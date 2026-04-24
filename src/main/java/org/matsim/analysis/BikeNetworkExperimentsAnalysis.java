package org.matsim.analysis;

import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
	name = "bike-network-experiments",
	description = "Event Handler for analysing the impact of different simulation handling of bike."
)
public class BikeNetworkExperimentsAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(BikeNetworkExperimentsAnalysis.class);
	private static final String LOG_MESSAGE = "This is not possible, as each leg should have a travel time and travel distance. Aborting!";

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which analysis should be performed.")
	private List<Path> inputPaths;
	@CommandLine.Mixin
	private CsvOptions csv;

	public static void main(String[] args) {
		new BikeNetworkExperimentsAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		for (Path runDir : inputPaths) {
			log.info("Running analysis on run dir {}", runDir);

			String networkFile = globFile(runDir, "*output_network.xml*").toString();
			String eventsFile = globFile(runDir, "*output_events.xml*").toString();
			String configFile = globFile(runDir, "*output_config.xml*").toString();

			Config config = ConfigUtils.loadConfig(configFile);
			Network network = NetworkUtils.readNetwork(networkFile);

			Map<Id<Person>, NetworkExperimentsHandler.SimulationData> simulationDataMap = new HashMap<>();
			NetworkExperimentsHandler handler = new NetworkExperimentsHandler(simulationDataMap, network);
			EventsManager manager = EventsUtils.createEventsManager();
			manager.addHandler(handler);
			manager.initProcessing();

			MatsimEventsReader reader = new MatsimEventsReader(manager);
			reader.readFile(eventsFile);
			manager.finishProcessing();

			boolean bikeInQSim = config.qsim().getMainModes().contains(TransportMode.bike);

			double sumBikeTravelTime = 0.0;
			double sumBikeTravelDistance = 0.0;
			double sumCarTravelTime = 0.0;
			double sumCarTravelDistance = 0.0;
			int bikeLegCount = 0;
			int carLegCount = 0;

			Map<DistanceBin, List<Double>> bikeDistanceBinsToTravelTimes = new HashMap<>();
			Map<DistanceBin, List<Double>> carDistanceBinsToTravelTimes = new HashMap<>();
			fillMapWithDistanceBins(bikeDistanceBinsToTravelTimes);
			fillMapWithDistanceBins(carDistanceBinsToTravelTimes);

			for (NetworkExperimentsHandler.SimulationData data : simulationDataMap.values()) {
//				first: check if there are any not logical things.
//				filter maps for bike
				List<Double> bikeNetworkTravelTimes = data.networkTravelTimesPerMode.getOrDefault(TransportMode.bike, new ArrayList<>());
				List<Double> bikeNetworkTravelDistances = data.networkTravelDistancesPerMode.getOrDefault(TransportMode.bike, new ArrayList<>());
				List<Double> bikeTeleportedTravelTimes = data.teleportedTravelTimesPerMode.getOrDefault(TransportMode.bike, new ArrayList<>());
				List<Double> bikeTeleportedTravelDistances = data.teleportedTravelDistancesPerMode.getOrDefault(TransportMode.bike, new ArrayList<>());

				List<Double> carNetworkTravelTimes = data.networkTravelTimesPerMode.getOrDefault(TransportMode.car, new ArrayList<>());
				List<Double> carNetworkTravelDistances = data.networkTravelDistancesPerMode.getOrDefault(TransportMode.car, new ArrayList<>());

//				there should be only teleported or only qsim legs for bike.
				if (!bikeNetworkTravelTimes.isEmpty() && !bikeTeleportedTravelTimes.isEmpty()) {
					log.fatal("For person {} using mode bike we registered {} network travel times and {} teleported travel times." +
						"Legs should be either network only or teleported only for the whole simulation. Aborting!",
						data.personId, bikeNetworkTravelTimes.size(), bikeTeleportedTravelTimes.size());
					throw new IllegalStateException("");
				}

				if (!bikeNetworkTravelDistances.isEmpty() && !bikeTeleportedTravelDistances.isEmpty()) {
					log.fatal("For person {} using mode bike we registered {} network distances and {} teleported distances." +
							"Legs should be either network only or teleported only for the whole simulation. Aborting!",
						data.personId, bikeNetworkTravelTimes.size(), bikeTeleportedTravelTimes.size());
					throw new IllegalStateException("");
				}

//				number of travel distances and travel times has to be the same
				if (bikeNetworkTravelTimes.size() != bikeNetworkTravelDistances.size()) {
					log.fatal("Person {} travelling by bike has {} recorded network travel times and {} recorded network travel distances. " +
							LOG_MESSAGE,
						data.personId, bikeNetworkTravelTimes.size(), bikeNetworkTravelDistances.size());
				}

				if (bikeTeleportedTravelTimes.size() != bikeTeleportedTravelDistances.size()) {
					log.fatal("Person {} travelling by bike has {} recorded teleported travel times and {} recorded teleported travel distances. " +
							LOG_MESSAGE,
						data.personId, bikeTeleportedTravelTimes.size(), bikeTeleportedTravelDistances.size());
				}

				if (carNetworkTravelTimes.size() != carNetworkTravelDistances.size()) {
					log.fatal("Person {} travelling by car has {} recorded network travel times and {} recorded network travel distances. " +
							LOG_MESSAGE,
						data.personId, carNetworkTravelTimes.size(), carNetworkTravelDistances.size());
				}

				List<Double> bikeTravelTimes;
				List<Double> bikeTravelDistances;
				if (bikeInQSim) {
					bikeTravelTimes = bikeNetworkTravelTimes;
					bikeTravelDistances = bikeNetworkTravelDistances;
				} else {
					bikeTravelTimes = bikeTeleportedTravelTimes;
					bikeTravelDistances = bikeTeleportedTravelDistances;
				}

//				calc mean tt per dist group
				for (double tt : bikeTravelTimes) {
					sumBikeTravelTime += tt;
					bikeLegCount++;
				}

				for (double dist : bikeTravelDistances) {
					sumBikeTravelDistance += dist;
//					leg count is already done for tt, so we do not do it here.
//					determine distance bin and save respective tt to distance bin map.
					bikeDistanceBinsToTravelTimes.get(determineDistanceBin(new BigDecimal(Double.toString(dist)).setScale(2, RoundingMode.HALF_UP).doubleValue(), bikeDistanceBinsToTravelTimes.keySet()))
						.add(bikeTravelTimes.get(bikeTravelDistances.indexOf(dist)));
				}

				for (double tt : carNetworkTravelTimes) {
					sumCarTravelTime += tt;
					carLegCount++;
				}

				for (double dist : carNetworkTravelDistances) {
					sumCarTravelDistance += dist;
//					leg count is already done for tt, so we do not do it here.
//					determine distance bin and save respective tt to distance bin map.
					carDistanceBinsToTravelTimes.get(determineDistanceBin(new BigDecimal(Double.toString(dist)).setScale(2, RoundingMode.HALF_UP).doubleValue(), carDistanceBinsToTravelTimes.keySet()))
						.add(carNetworkTravelTimes.get(carNetworkTravelDistances.indexOf(dist)));
				}
			}

//			calc general mean values
			assert bikeLegCount != 0;
			assert carLegCount != 0;
			double meanBikeTravelTime = sumBikeTravelTime / bikeLegCount;
			double meanBikeTravelDistance = sumBikeTravelDistance / bikeLegCount;
			double meanCarTravelTime = sumCarTravelTime / carLegCount;
			double meanCarTravelDistance = sumCarTravelDistance / carLegCount;

//			calc mean tt per mode per distance bin.
//			the following uses Lists, but we only use the first entry.
			Map<DistanceBin, List<Double>> bikeMeanTravelTimesPerDistanceBin = new HashMap<>();
			Map<DistanceBin, List<Double>> carMeanTravelTimesPerDistanceBin = new HashMap<>();
			fillMapWithDistanceBins(bikeMeanTravelTimesPerDistanceBin);
			fillMapWithDistanceBins(carMeanTravelTimesPerDistanceBin);

			for (Map.Entry<DistanceBin, List<Double>> e : bikeDistanceBinsToTravelTimes.entrySet()) {
				double sum = 0.;
				int count = 0;

				for (double tt : e.getValue()) {
					sum += tt;
					count++;
				}
				if (count == 0) {
					bikeMeanTravelTimesPerDistanceBin.get(e.getKey()).add(0.);
				} else {
					bikeMeanTravelTimesPerDistanceBin.get(e.getKey()).add(sum / count);
				}
			}

			for (Map.Entry<DistanceBin, List<Double>> e : carDistanceBinsToTravelTimes.entrySet()) {
				double sum = 0.;
				int count = 0;

				for (double tt : e.getValue()) {
					sum += tt;
					count++;
				}

				if (count == 0) {
					carMeanTravelTimesPerDistanceBin.get(e.getKey()).add(0.);
				} else {
					carMeanTravelTimesPerDistanceBin.get(e.getKey()).add(sum / count);
				}
			}


//			write stats to csv
			try (CSVPrinter printer = csv.createPrinter(runDir.resolve("mean_tt_per_distance_bin.csv"))) {
				printer.printRecord("dist_group", "mode", "mean_tt_s");
				for (Map.Entry<DistanceBin, List<Double>> e : bikeMeanTravelTimesPerDistanceBin.entrySet()) {
					DistanceBin bin = e.getKey();

					List<Double> carMeanTTList = carMeanTravelTimesPerDistanceBin.get(e.getKey());

					if (e.getValue().size() != 1 || carMeanTTList.size() != 1) {
						log.error("Expecting 1 mean tt per distance bin, but there are {} for distance bin {} and bike and {} for car in the same distance bin. Aborting!",
							e.getValue().size(), bin, carMeanTTList.size());
						throw new IllegalStateException("Only one mean tt per distance bin and mode allowed!");
					}
					printer.printRecord(bin.minIncl + " - " + bin.maxExcl, TransportMode.bike, e.getValue().getFirst());
					printer.printRecord(bin.minIncl + " - " + bin.maxExcl, TransportMode.car, carMeanTTList.getFirst());
				}
			}

			try (CSVPrinter printer = csv.createPrinter(runDir.resolve("general_stats_bike_analysis.csv"))) {
				printer.printRecord("mode", "mean_tt_s", "sum_tt_s", "mean_trav_dist_m", "sum_trav_dist_m");
				printer.printRecord(TransportMode.bike, meanBikeTravelTime, sumBikeTravelTime, meanBikeTravelDistance, sumBikeTravelDistance);
				printer.printRecord(TransportMode.car, meanCarTravelTime, sumCarTravelTime, meanCarTravelDistance, sumCarTravelDistance);
			}
		}


		return 0;
	}

	private void fillMapWithDistanceBins(Map<DistanceBin, List<Double>> modalDistanceBinMap) {
		modalDistanceBinMap.put(new DistanceBin(0., 999.99), new ArrayList<>());
		modalDistanceBinMap.put(new DistanceBin(1000., 1999.99), new ArrayList<>());
		modalDistanceBinMap.put(new DistanceBin(2000., 4999.99), new ArrayList<>());
		modalDistanceBinMap.put(new DistanceBin(5000., 9999.99), new ArrayList<>());
		modalDistanceBinMap.put(new DistanceBin(10000., 19999.99), new ArrayList<>());
		modalDistanceBinMap.put(new DistanceBin(20000., Double.MAX_VALUE), new ArrayList<>());
	}

	private static DistanceBin determineDistanceBin(double distance, Set<DistanceBin> distanceBins) {
		for (DistanceBin bin : distanceBins) {
			if (distance >= bin.minIncl() && distance < bin.maxExcl()) {
				return bin;
			}
		}
		return null;
	}


	private static final class NetworkExperimentsHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, LinkLeaveEventHandler, TeleportationArrivalEventHandler, PersonDepartureEventHandler {
//		For network vehicles, the sequence is:
//		PersonDeparture, VehicleEntersTraffic, LinkLeave, LinkEnter, [...], LinkEnter, VehicleLeavesTraffic
//		for teleported modes:
//		PersonDeparture, TeleportationArrival
		private final Map<Id<Person>, SimulationData> simulationDataMap;
		private final Network network;
		private final Map<Id<Vehicle>, Id<Person>> vehiclesToPersonsInTraffic = new HashMap<>();
		private final Map<Id<Person>, Double> personsInTraffic = new HashMap<>();
		private final Map<Id<Person>, Double> travelDistances = new HashMap<>();
		private final Map<Id<Person>, Double> travelTimes = new HashMap<>();

		private NetworkExperimentsHandler(Map<Id<Person>, SimulationData> simulationDataMap, Network network) {
			this.simulationDataMap = simulationDataMap;
			this.network = network;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
//			teleported and "qsim" routes share the usage of PersonDepartureEvent.
//			so we register the agent here, but delete it from the map for qsim routes in VehicleEntersTrafficEvent to differ between teleported and qsim.
//			only for bike because for all other modes the simulation method is not changed.
			if (event.getLegMode().equals(TransportMode.bike)) {
				personsInTraffic.put(event.getPersonId(), event.getTime());
			}
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
//			register person and vehicle if bike or car
			if (!event.getPersonId().toString().contains("pt_") && (event.getNetworkMode().equals(TransportMode.bike) || event.getNetworkMode().equals(TransportMode.car))) {
//				remove bike person from map for teleported agents as this is a qsim route; add agent to vehicle2Person map
				personsInTraffic.remove(event.getPersonId());
				vehiclesToPersonsInTraffic.put(event.getVehicleId(), event.getPersonId());

				travelDistances.put(event.getPersonId(), 0.);
				travelTimes.put(event.getPersonId(), event.getTime());
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (vehiclesToPersonsInTraffic.containsKey(event.getVehicleId())) {
				Id<Person> personId = vehiclesToPersonsInTraffic.get(event.getVehicleId());
//			update travel dist
				travelDistances.put(personId,
					travelDistances.get(personId) + network.getLinks().get(event.getLinkId()).getLength());
			}
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			if (vehiclesToPersonsInTraffic.containsKey(event.getVehicleId())) {
				Id<Person> personId = event.getPersonId();

//			add last distance bit to travel distance of leg
				travelDistances.put(personId,
					travelDistances.get(personId) + network.getLinks().get(event.getLinkId()).getLength());

//			save travel time and distance to data structure map
				double travelDistance = travelDistances.remove(personId);
				double travelTime = event.getTime() - travelTimes.remove(personId);
				simulationDataMap.putIfAbsent(personId, new SimulationData(personId));
				simulationDataMap.get(personId).updateNetworkTravelDistance(event.getNetworkMode(), travelDistance);
				simulationDataMap.get(personId).updateNetworkTravelTime(event.getNetworkMode(), travelTime);
				vehiclesToPersonsInTraffic.remove(event.getVehicleId());
			}
		}

		@Override
		public void handleEvent(TeleportationArrivalEvent event) {
			Id<Person> personId = event.getPersonId();

			if (personsInTraffic.containsKey(personId)) {
				double travelTime = event.getTime() - personsInTraffic.get(personId);
				simulationDataMap.putIfAbsent(personId, new SimulationData(personId));
				simulationDataMap.get(personId).updateTeleportedTravelDistance(event.getMode(), event.getDistance());
				simulationDataMap.get(personId).updateTeleportedTravelTime(event.getMode(), travelTime);
				personsInTraffic.remove(personId);
			}
		}

		@Override
		public void reset(int iteration) {
			simulationDataMap.clear();
			vehiclesToPersonsInTraffic.clear();
			personsInTraffic.clear();
			travelDistances.clear();
			travelTimes.clear();
		}

		private static class SimulationData {
			private final Id<Person> personId;
			private final Map<String, List<Double>> networkTravelTimesPerMode = new HashMap<>();
			private final Map<String, List<Double>> networkTravelDistancesPerMode = new HashMap<>();
			private final Map<String, List<Double>> teleportedTravelTimesPerMode = new HashMap<>();
			private final Map<String, List<Double>> teleportedTravelDistancesPerMode = new HashMap<>();

			SimulationData(Id<Person> personId) {
				this.personId = personId;
			}

			private SimulationData updateNetworkTravelTime(String mode, double travelTime) {
				this.networkTravelTimesPerMode.putIfAbsent(mode, new ArrayList<>());
				this.networkTravelTimesPerMode.get(mode).add(travelTime);
				return this;
			}

			private SimulationData updateNetworkTravelDistance(String mode, double travelDistance) {
				this.networkTravelDistancesPerMode.putIfAbsent(mode, new ArrayList<>());
				this.networkTravelDistancesPerMode.get(mode).add(travelDistance);
				return this;
			}

			private SimulationData updateTeleportedTravelTime(String mode, double travelTime) {
				this.teleportedTravelTimesPerMode.putIfAbsent(mode, new ArrayList<>());
				this.teleportedTravelTimesPerMode.get(mode).add(travelTime);
				return this;
			}

			private SimulationData updateTeleportedTravelDistance(String mode, double travelDistance) {
				this.teleportedTravelDistancesPerMode.putIfAbsent(mode, new ArrayList<>());
				this.teleportedTravelDistancesPerMode.get(mode).add(travelDistance);
				return this;
			}
		}
	}

	private record DistanceBin(double minIncl, double maxExcl) {


	}
}
