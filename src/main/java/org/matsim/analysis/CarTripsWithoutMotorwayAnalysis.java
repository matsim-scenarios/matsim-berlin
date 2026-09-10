package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Writes trips of a selected mode for persons whose routes do not use selected link types.
 */
@CommandLine.Command(
	name = "car-trips-without-motorway",
	description = "Filter trip statistics to trips of persons whose routes do not use selected network link types."
)
public class CarTripsWithoutMotorwayAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(CarTripsWithoutMotorwayAnalysis.class);

	@CommandLine.Option(names = "--experienced-plans", required = true,
		description = "Path to the experienced-plans XML or XML.GZ file.")
	private Path experiencedPlans;

	@CommandLine.Option(names = "--network", required = true,
		description = "Path to the MATSim network XML or XML.GZ file.")
	private Path networkFile;

	@CommandLine.Option(names = "--output", required = true,
		description = "Output path for the filtered output_trips table.")
	private Path output;

	@CommandLine.Option(names = "--trips", required = true,
		description = "Path to output_trips.csv or output_trips.csv.gz.")
	private Path tripsFile;

	@CommandLine.Option(names = "--summary-output",
		description = "CSV path for the speed summary. Defaults to <output>.summary.csv.")
	private Path summaryOutput;

	@CommandLine.Option(names = "--mode", defaultValue = "car",
		description = "Trip mode to retain (default: car).")
	private String mode;

	@CommandLine.Option(names = "--network-link-types", split = ",", defaultValue = "highway.motorway",
		description = "Comma-separated network link types that a retained route must not use (default: highway.motorway).")
	private List<String> excludedLinkTypes;

	public static void main(String[] args) {
		new CarTripsWithoutMotorwayAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Network network = NetworkUtils.readNetwork(networkFile.toString());
		Population population = PopulationUtils.readPopulation(experiencedPlans.toString());
		Set<String> eligibleTripIds = findEligibleTripIds(population, network);

		Path parent = output.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}


		List<Double> speeds = new ArrayList<>();
		SpeedSummary summary = filterTripsAndCalculateSpeed(eligibleTripIds, speeds);

		Double sum = 0.;
		for (Double speed : speeds) {
			sum += speed;
		}

		log.info("Mean speed: {}", sum / speeds.size());

		writeSummary(summary);

		return 0;
	}

	private Set<String> findEligibleTripIds(Population population, Network network) {
		Set<String> eligibleTripIds = new HashSet<>();
		for (Person person : population.getPersons().values()) {
			// Exclude freight agents; output_trips contains these alongside persons.
			if (person.getId().toString().contains("goodsTraffic") || person.getId().toString().contains("commercial") ||
				person.getId().toString().contains("freight")) {
				continue;
			}

			for (int tripId : getEligibleTrips(person, network)) {
				eligibleTripIds.add(person.getId() + "_" + tripId);
			}
		}
		return eligibleTripIds;
	}

	private SpeedSummary filterTripsAndCalculateSpeed(Set<String> eligibleTripIds, List<Double> speeds) throws Exception {
		CSVFormat inputFormat = CSVFormat.DEFAULT.builder()
			.setDelimiter(';')
			.setHeader()
			.setSkipHeaderRecord(true)
			.build();
		long tripCount = 0;
		double sumTripSpeedKmh = 0;
		double totalDistanceMeters = 0;
		long totalTravelTimeSeconds = 0;

		try (BufferedReader reader = IOUtils.getBufferedReader(tripsFile.toString());
			 CSVParser trips = new CSVParser(reader, inputFormat);
			 CSVPrinter filtered = new CSVPrinter(Files.newBufferedWriter(output),
				CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader(trips.getHeaderNames().toArray(String[]::new)).build())) {
			for (CSVRecord trip : trips) {
				if (!eligibleTripIds.contains(trip.get("trip_id"))) {
					continue;
				}
				filtered.printRecord(trip);

				double distanceMeters = Double.parseDouble(trip.get("traveled_distance"));
				long travelTimeSeconds = parseDurationSeconds(trip.get("trav_time"));
				if (travelTimeSeconds > 0) {
					tripCount++;
					sumTripSpeedKmh += distanceMeters / travelTimeSeconds * 3.6;
					totalDistanceMeters += distanceMeters;
					totalTravelTimeSeconds += travelTimeSeconds;
					speeds.add(distanceMeters / travelTimeSeconds * 3.6);
				}
			}
		}

		return new SpeedSummary(tripCount, sumTripSpeedKmh, totalDistanceMeters, totalTravelTimeSeconds);
	}

	private void writeSummary(SpeedSummary summary) throws Exception {
		Path summaryPath = summaryOutput != null
			? summaryOutput
			: output.resolveSibling(output.getFileName() + ".summary.csv");
		Path parent = summaryPath.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(summaryPath),
			CSVFormat.DEFAULT.builder().setHeader("trip_count", "mean_trip_speed_km_h", "overall_speed_km_h").build())) {
			csv.printRecord(summary.tripCount(), summary.meanTripSpeedKmh(), summary.overallSpeedKmh());
		}
	}

	private static long parseDurationSeconds(String duration) {
		String[] parts = duration.split(":");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Expected duration in HH:MM:SS format but got: " + duration);
		}
		return Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2]);
	}

	/**
 * A person is retained only if every selected-mode trip has a complete network route that avoids the excluded link types.
	 * Trip ids are one-based positions amongst all trips in the selected plan.
	 */
	private List<Integer> getEligibleTrips(Person person, Network network) {
		List<Integer> selectedModeTrips = new ArrayList<>();
		List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

		for (int tripIndex = 0; tripIndex < trips.size(); tripIndex++) {
			TripStructureUtils.Trip trip = trips.get(tripIndex);
			List<Leg> selectedModeLegs = trip.getLegsOnly().stream()
				.filter(leg -> mode.equals(leg.getMode()))
				.toList();

			if (selectedModeLegs.isEmpty()) {
				continue;
			}

			if (selectedModeLegs.stream().anyMatch(leg -> !isCompleteAllowedRoute(leg, network))) {
				return List.of();
			}
			selectedModeTrips.add(tripIndex + 1);
		}

		return selectedModeTrips;
	}

	private boolean isCompleteAllowedRoute(Leg leg, Network network) {
		if (!(leg.getRoute() instanceof NetworkRoute route)) {
			return false;
		}

		return routeLinkIds(route).allMatch(linkId -> {
			Link link = network.getLinks().get(linkId);
			return link != null && !excludedLinkTypes.contains(link.getAttributes().getAttribute("type"));
		});
	}

	private static Stream<Id<Link>> routeLinkIds(NetworkRoute route) {
		return Stream.concat(
			Stream.of(route.getStartLinkId()),
			Stream.concat(route.getLinkIds().stream(), Stream.of(route.getEndLinkId()))
		);
	}

	private record SpeedSummary(long tripCount, double totalTripSpeedKmh, double totalDistanceMeters,
								long totalTravelTimeSeconds) {
		double meanTripSpeedKmh() {
			return tripCount == 0 ? Double.NaN : totalTripSpeedKmh / tripCount;
		}

		double overallSpeedKmh() {
			return totalTravelTimeSeconds == 0 ? Double.NaN : totalDistanceMeters / totalTravelTimeSeconds * 3.6;
		}
	}
}
