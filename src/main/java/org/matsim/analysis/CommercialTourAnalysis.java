package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.Pair;
import org.matsim.freight.carriers.*;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.DurationsBounds;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.ServiceDurationPerCategoryKey;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficSegment;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.TourStartAndDuration;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.ZoneAttribute;
import org.matsim.smallScaleCommercialTrafficGeneration.data.CommercialTourSpecifications;
import org.matsim.smallScaleCommercialTrafficGeneration.data.DefaultTourSpecificationsByUsingKID2002;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Aggregate statistics for the carrier plans produced by the small scale commercial traffic generation, i.e. for what
 * jsprit actually did with the OD matrix.
 * <p>
 * The headline number is stops per vehicle, which the contrib does not report anywhere and which is not constrained by
 * any input either: KiD supplies tour start times, tour durations and service durations, but no trip chain length, and
 * every service is created with {@code capacityDemand == 0}, so vehicle capacity never binds. Stops per tour is
 * therefore purely a result of the time arithmetic and worth watching.
 * <p>
 * The second thing worth watching is the time budget. Fleets are sized so that the vehicle availability of a carrier
 * covers {@code factorForTravelBufferCalculation} (1.2 by default) times its total service duration, i.e. travel is
 * assumed to be 20 percent on top of service time. That is an assumption about the whole vehicle pool, but only
 * dispatched vehicles become tours, so the realised ratio per tour can be much worse. {@code realisedTravelFactor}
 * below is the number to compare against the assumption.
 */
@CommandLine.Command(
	name = "commercial-tours",
	description = "Aggregate statistics for the solved small scale commercial traffic carrier plans."
)
public class CommercialTourAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CommercialTourAnalysis.class);

	/** Start hour bands of the KiD 2002 tour distribution, so the output can be diffed against the input. */
	private static final int[] START_BANDS = {0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 24};

	/** Duration bands in minutes, likewise. */
	private static final double[] DURATION_BANDS = {0, 30, 60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720, 780, 840, 1080};

	private static int startBand(double hour) {
		for (int i = 0; i < START_BANDS.length - 1; i++) {
			if (hour >= START_BANDS[i] && hour < START_BANDS[i + 1])
				return i;
		}
		return START_BANDS.length - 2;
	}

	private static int durationBand(double minutes) {
		for (int i = 0; i < DURATION_BANDS.length - 1; i++) {
			if (minutes >= DURATION_BANDS[i] && minutes < DURATION_BANDS[i + 1])
				return i;
		}
		return DURATION_BANDS.length - 2;
	}

	private static String startBandLabel(int i) {
		return START_BANDS[i] + "-" + START_BANDS[i + 1];
	}

	private static String durationBandLabel(int i) {
		return fmt(DURATION_BANDS[i]) + "-" + fmt(DURATION_BANDS[i + 1]);
	}

	/** Weighted histogram over a fixed set of bands. */
	private static final class Hist {
		final double[] bins;

		Hist(int n) {
			bins = new double[n];
		}

		void add(int b, double w) {
			if (b >= 0 && b < bins.length)
				bins[b] += w;
		}

		double sum() {
			return Arrays.stream(bins).sum();
		}

		double[] shares() {
			double s = sum();
			double[] o = new double[bins.length];
			if (s > 0) {
				for (int i = 0; i < bins.length; i++)
					o[i] = bins[i] / s;
			}
			return o;
		}
	}

	@CommandLine.Option(names = "--carriers", required = true,
		description = "Solved carrier file, e.g. output_carriers_solvedVRP.xml.gz")
	private String carriersFile;

	@CommandLine.Option(names = "--vehicle-types", description = "Carrier vehicle types file. "
		+ "Defaults to output_carriersVehicleTypes.xml.gz next to the carrier file.")
	private String vehicleTypesFile;

	@CommandLine.Option(names = "--network", required = true, description = "Network the tours were routed on.")
	private String networkFile;

	@CommandLine.Option(names = "--output", required = true, description = "Output folder for the csv files.")
	private Path output;

	public static void main(String[] args) {
		new CommercialTourAnalysis().execute(args);
	}

	/** One scheduled tour. */
	private record TourStat(String carrierId, String subpopulation, String purpose, String vehicleId, String vehicleType,
							int stops, double departure, double serviceSeconds, double travelSeconds,
							double tourSeconds, double distanceKm, double maxDepotDistanceKm,
							double vehicleWindowSeconds) {
	}

	@Override
	public Integer call() throws Exception {

		Files.createDirectories(output);

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		CarrierVehicleTypes types = new CarrierVehicleTypes();
		String typesPath = vehicleTypesFile != null ? vehicleTypesFile
			: Path.of(carriersFile).resolveSibling("output_carriersVehicleTypes.xml.gz").toString();
		if (Files.exists(Path.of(typesPath)))
			new CarrierVehicleTypeReader(types).readFile(typesPath);
		else
			log.warn("No carrier vehicle types found at {}, continuing without them", typesPath);

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, types).readFile(carriersFile);
		log.info("Read {} carriers from {}", carriers.getCarriers().size(), carriersFile);

		List<TourStat> tours = new ArrayList<>();
		int carriersWithoutPlan = 0;
		long servicesPlanned = 0;
		long servicesHandled = 0;
		long vehiclesProvided = 0;
		Set<String> vehiclesUsed = new HashSet<>();
		int carriersWithUnhandled = 0;

		Marginals marginals = new Marginals();

		for (Carrier carrier : carriers.getCarriers().values()) {
			servicesPlanned += carrier.getServices().size();
			vehiclesProvided += carrier.getCarrierCapabilities().getCarrierVehicles().size();

			marginals.addCarrier(carrier);

			CarrierPlan plan = carrier.getSelectedPlan();
			if (plan == null) {
				carriersWithoutPlan++;
				continue;
			}

			long handledHere = 0;
			for (ScheduledTour st : plan.getScheduledTours()) {
				TourStat ts = describe(carrier, st, network);
				tours.add(ts);
				handledHere += ts.stops();
				vehiclesUsed.add(st.getVehicle().getId().toString());
				marginals.addTour(carrier, st, ts);
			}
			servicesHandled += handledHere;
			if (handledHere < carrier.getServices().size())
				carriersWithUnhandled++;
		}

		log.info("{} tours over {} carriers, {} of {} services handled",
			tours.size(), carriers.getCarriers().size(), servicesHandled, servicesPlanned);

		writeTours(tours);
		writeStopsPerTour(tours, marginals);
		writeTimeBudget(tours);
		writeStartHours(tours);
		writeDurations(tours);
		writeKpi(tours, carriers.getCarriers().size(), carriersWithoutPlan, carriersWithUnhandled,
			servicesPlanned, servicesHandled, vehiclesProvided, vehiclesUsed.size());
		marginals.write(output);

		return 0;
	}

	private TourStat describe(Carrier carrier, ScheduledTour st, Network network) {

		int stops = 0;
		double service = 0;
		double travel = 0;
		double distance = 0;

		Coordish depot = coordOf(st.getVehicle().getLinkId(), network);
		double maxDepotDist = 0;

		for (Tour.TourElement el : st.getTour().getTourElements()) {
			if (el instanceof Tour.ServiceActivity sa) {
				stops++;
				service += sa.getDuration();
				Coordish c = coordOf(sa.getLocation(), network);
				if (depot != null && c != null)
					maxDepotDist = Math.max(maxDepotDist, depot.distanceKm(c));
			} else if (el instanceof Tour.Leg leg) {
				travel += leg.getExpectedTransportTime();
				distance += legDistanceMeters(leg.getRoute(), network);
			}
		}

		// the tour occupies the vehicle from its departure until it is back, service plus travel is a tight lower bound
		double tourSeconds = service + travel;
		double window = st.getVehicle().getLatestEndTime() - st.getVehicle().getEarliestStartTime();

		return new TourStat(
			carrier.getId().toString(),
			attr(carrier, "subpopulation"),
			attr(carrier, "purpose"),
			st.getVehicle().getId().toString(),
			st.getVehicle().getType() != null ? st.getVehicle().getType().getId().toString() : "",
			stops, st.getDeparture(), service, travel, tourSeconds,
			distance / 1000d, maxDepotDist, window);
	}

	private static String attr(Carrier carrier, String key) {
		Object v = carrier.getAttributes().getAttribute(key);
		return v == null ? "" : v.toString();
	}

	private record Coordish(double x, double y) {
		double distanceKm(Coordish o) {
			return Math.hypot(x - o.x, y - o.y) / 1000d;
		}
	}

	private static Coordish coordOf(Id<Link> linkId, Network network) {
		Link l = network.getLinks().get(linkId);
		if (l == null)
			return null;
		return new Coordish(l.getCoord().getX(), l.getCoord().getY());
	}

	private static double legDistanceMeters(Route route, Network network) {
		if (route == null)
			return 0;
		if (route instanceof NetworkRoute nr) {
			// relPosOnDeparture=1 drops the start link, relPosOnArrival=1 keeps the end link in full. The vehicle is
			// already at the end of the start link when the leg begins, so this counts every link exactly once over
			// the tour instead of double counting each stop's link against the following leg.
			return RouteUtils.calcDistance(nr, 1, 1, network);
		}
		return Double.isNaN(route.getDistance()) ? 0 : route.getDistance();
	}

	// ------------------------------------------------------------------ writing

	private void writeTours(List<TourStat> tours) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_tours.csv")),
			CSVFormat.DEFAULT.builder().setHeader("carrierId", "subpopulation", "purpose", "vehicleId", "vehicleType",
				"stops", "departureHour", "serviceMinutes", "travelMinutes", "tourMinutes", "distanceKm",
				"maxDepotDistanceKm", "vehicleWindowMinutes", "windowUsedShare").get())) {

			for (TourStat t : tours) {
				csv.printRecord(t.carrierId(), t.subpopulation(), t.purpose(), t.vehicleId(), t.vehicleType(),
					t.stops(), round(t.departure() / 3600), round(t.serviceSeconds() / 60), round(t.travelSeconds() / 60),
					round(t.tourSeconds() / 60), round(t.distanceKm()), round(t.maxDepotDistanceKm()),
					round(t.vehicleWindowSeconds() / 60),
					t.vehicleWindowSeconds() > 0 ? round(t.tourSeconds() / t.vehicleWindowSeconds()) : "");
			}
		}
	}

	/**
	 * The one the contrib does not report, together with what the model's own KiD inputs imply it should be.
	 */
	private void writeStopsPerTour(List<TourStat> tours, Marginals marginals) throws Exception {

		Map<String, Map<Integer, Long>> observed = new TreeMap<>();
		Map<String, double[]> travelPerStop = new TreeMap<>();
		for (TourStat t : tours) {
			String seg = "goodsTraffic".equals(t.subpopulation()) ? "goodsTraffic" : "commercialPersonTraffic";
			for (String k : List.of(seg, "*")) {
				observed.computeIfAbsent(k, x -> new TreeMap<>()).merge(t.stops(), 1L, Long::sum);
				double[] acc = travelPerStop.computeIfAbsent(k, x -> new double[2]);
				acc[0] += t.travelSeconds();
				acc[1] += t.stops();
			}
		}

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_stops_per_tour.csv")),
			CSVFormat.DEFAULT.builder().setHeader("segment", "series", "stops", "tours", "share").get());
			 CSVPrinter sum = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_stops_per_tour_summary.csv")),
				 CSVFormat.DEFAULT.builder().setHeader("segment", "series", "meanStops", "shareZeroStops",
					 "meanStopsGivenAtLeastOne", "travelSecondsPerStopAssumed").get())) {

			for (Map.Entry<String, Map<Integer, Long>> e : observed.entrySet()) {
				String seg = e.getKey();
				long n = e.getValue().values().stream().mapToLong(Long::longValue).sum();
				double mean = 0;
				for (Map.Entry<Integer, Long> b : e.getValue().entrySet()) {
					csv.printRecord(seg, "observed", b.getKey(), b.getValue(), round((double) b.getValue() / n));
					mean += (double) b.getKey() * b.getValue() / n;
				}
				// a dispatched tour always has at least one stop, so observed and conditional mean coincide
				sum.printRecord(seg, "observed", round(mean), 0, round(mean), "");

				double[] acc = travelPerStop.get(seg);
				double travel = acc[1] > 0 ? acc[0] / acc[1] : 0;
				writeImplied(csv, sum, marginals, seg, "impliedServiceOnly", 0);
				writeImplied(csv, sum, marginals, seg, "impliedWithObservedTravel", travel);
			}
		}
	}

	private void writeImplied(CSVPrinter csv, CSVPrinter sum, Marginals marginals, String segment,
							  String series, double travelSecondsPerStop) throws Exception {
		Map<Integer, Double> implied = marginals.impliedStopsPerTour(segment, travelSecondsPerStop, 200_000);
		if (implied.isEmpty())
			return;
		double mean = 0;
		for (Map.Entry<Integer, Double> b : implied.entrySet()) {
			csv.printRecord(segment, series, b.getKey(), Math.round(b.getValue() * 200_000), round(b.getValue()));
			mean += b.getKey() * b.getValue();
		}
		// A draw can yield zero stops when the sampled tour duration cannot hold even one sampled service. Those are
		// exactly the carriers the unhandled-services loop has to repair, and they cannot appear among dispatched
		// tours, so the like for like comparison against the observed tours is the conditional mean.
		double zero = implied.getOrDefault(0, 0d);
		double conditional = zero < 1 ? mean / (1 - zero) : Double.NaN;
		sum.printRecord(segment, series, round(mean), round(zero), round(conditional), Math.round(travelSecondsPerStop));
	}

	/**
	 * Service against travel against slack, per tour and aggregated. realisedTravelFactor is
	 * (service + travel) / service, directly comparable to --factorForTravelBufferCalculation.
	 */
	private void writeTimeBudget(List<TourStat> tours) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_tour_time_budget.csv")),
			CSVFormat.DEFAULT.builder().setHeader("subpopulation", "tours", "meanStops", "meanServiceMinutes",
				"meanTravelMinutes", "meanTourMinutes", "travelShareOfTour", "realisedTravelFactor",
				"meanVehicleWindowMinutes", "meanWindowUsedShare", "meanDistanceKm", "meanKmPerStop").get())) {

			Map<String, List<TourStat>> groups = new TreeMap<>();
			for (TourStat t : tours) {
				groups.computeIfAbsent(t.subpopulation().isEmpty() ? "unknown" : t.subpopulation(), k -> new ArrayList<>()).add(t);
				groups.computeIfAbsent("*", k -> new ArrayList<>()).add(t);
			}

			for (Map.Entry<String, List<TourStat>> e : groups.entrySet()) {
				List<TourStat> g = e.getValue();
				double service = g.stream().mapToDouble(TourStat::serviceSeconds).sum();
				double travel = g.stream().mapToDouble(TourStat::travelSeconds).sum();
				double tour = service + travel;
				double window = g.stream().mapToDouble(TourStat::vehicleWindowSeconds).sum();
				double km = g.stream().mapToDouble(TourStat::distanceKm).sum();
				long stops = g.stream().mapToLong(TourStat::stops).sum();
				int n = g.size();

				csv.printRecord(e.getKey(), n, round((double) stops / n), round(service / n / 60), round(travel / n / 60),
					round(tour / n / 60), round(tour > 0 ? travel / tour : 0),
					round(service > 0 ? tour / service : 0),
					round(window / n / 60), round(window > 0 ? tour / window : 0),
					round(km / n), round(stops > 0 ? km / stops : 0));
			}
		}
	}

	private void writeStartHours(List<TourStat> tours) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_tour_start_hour.csv")),
			CSVFormat.DEFAULT.builder().setHeader("subpopulation", "band", "tours", "share").get())) {
			writeBanded(csv, tours, t -> t.departure() / 3600d, START_BANDS.length - 1,
				v -> {
					for (int i = 0; i < START_BANDS.length - 1; i++) {
						if (v >= START_BANDS[i] && v < START_BANDS[i + 1])
							return i;
					}
					return START_BANDS.length - 2;
				},
				i -> START_BANDS[i] + "-" + START_BANDS[i + 1]);
		}
	}

	private void writeDurations(List<TourStat> tours) throws Exception {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_tour_duration.csv")),
			CSVFormat.DEFAULT.builder().setHeader("subpopulation", "band", "tours", "share").get())) {
			writeBanded(csv, tours, t -> t.tourSeconds() / 60d, DURATION_BANDS.length - 1,
				v -> {
					for (int i = 0; i < DURATION_BANDS.length - 1; i++) {
						if (v >= DURATION_BANDS[i] && v < DURATION_BANDS[i + 1])
							return i;
					}
					return DURATION_BANDS.length - 2;
				},
				i -> fmt(DURATION_BANDS[i]) + "-" + fmt(DURATION_BANDS[i + 1]));
		}
	}

	private interface Binner {
		int bin(double value);
	}

	private interface Labeller {
		String label(int bin);
	}

	private interface Extractor {
		double value(TourStat t);
	}

	private void writeBanded(CSVPrinter csv, List<TourStat> tours, Extractor ex, int nBins, Binner binner, Labeller lab) throws Exception {
		Map<String, long[]> groups = new TreeMap<>();
		for (TourStat t : tours) {
			int b = binner.bin(ex.value(t));
			groups.computeIfAbsent(t.subpopulation().isEmpty() ? "unknown" : t.subpopulation(), k -> new long[nBins])[b]++;
			groups.computeIfAbsent("*", k -> new long[nBins])[b]++;
		}
		for (Map.Entry<String, long[]> e : groups.entrySet()) {
			long n = Arrays.stream(e.getValue()).sum();
			for (int i = 0; i < nBins; i++)
				csv.printRecord(e.getKey(), lab.label(i), e.getValue()[i], round(n > 0 ? (double) e.getValue()[i] / n : 0));
		}
	}

	private void writeKpi(List<TourStat> tours, int carriers, int carriersWithoutPlan, int carriersWithUnhandled,
						  long servicesPlanned, long servicesHandled, long vehiclesProvided, int vehiclesUsed) throws Exception {

		long stops = tours.stream().mapToLong(TourStat::stops).sum();
		double km = tours.stream().mapToDouble(TourStat::distanceKm).sum();
		List<Integer> stopList = tours.stream().map(TourStat::stops).sorted().toList();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_tour_kpi.csv")),
			CSVFormat.DEFAULT.builder().setHeader("indicator", "value").get())) {

			csv.printRecord("carriers", carriers);
			csv.printRecord("carriersWithoutPlan", carriersWithoutPlan);
			csv.printRecord("carriersWithUnhandledServices", carriersWithUnhandled);
			csv.printRecord("tours", tours.size());
			csv.printRecord("servicesPlanned", servicesPlanned);
			csv.printRecord("servicesHandled", servicesHandled);
			csv.printRecord("servicesUnhandled", servicesPlanned - servicesHandled);
			csv.printRecord("vehiclesProvided", vehiclesProvided);
			csv.printRecord("vehiclesUsed", vehiclesUsed);
			csv.printRecord("fleetUtilisation", vehiclesProvided > 0 ? round((double) vehiclesUsed / vehiclesProvided) : "");
			csv.printRecord("toursPerCarrier", carriers > 0 ? round((double) tours.size() / carriers) : "");
			csv.printRecord("meanStopsPerTour", tours.isEmpty() ? "" : round((double) stops / tours.size()));
			csv.printRecord("medianStopsPerTour", stopList.isEmpty() ? "" : stopList.get(stopList.size() / 2));
			csv.printRecord("p90StopsPerTour", stopList.isEmpty() ? "" : stopList.get((int) (stopList.size() * 0.9)));
			csv.printRecord("maxStopsPerTour", stopList.isEmpty() ? "" : stopList.getLast());
			csv.printRecord("toursWithOneStop", tours.stream().filter(t -> t.stops() == 1).count());
			csv.printRecord("totalVehicleKm", Math.round(km));
			csv.printRecord("meanKmPerTour", tours.isEmpty() ? "" : round(km / tours.size()));
			csv.printRecord("meanKmPerStop", stops > 0 ? round(km / stops) : "");
			csv.printRecord("meanMaxDepotDistanceKm", tours.isEmpty() ? ""
				: round(tours.stream().mapToDouble(TourStat::maxDepotDistanceKm).average().orElse(0)));
		}
	}

	private static String fmt(double v) {
		return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	private static double round(double v) {
		return Math.round(v * 1000d) / 1000d;
	}

	/**
	 * Compares the three KiD 2002 marginals that go into the generation against what comes out of it. The reference is
	 * read from {@link DefaultTourSpecificationsByUsingKID2002}, i.e. from the very object the generator samples from,
	 * so it cannot drift away from the input.
	 * <p>
	 * For tour start and tour duration three output series are compared, because the KiD draw does not become a tour
	 * directly. It sets a vehicle's availability window, jsprit then picks which of those vehicles to dispatch, and the
	 * dispatched vehicle fills only part of its window:
	 * <ul>
	 *     <li>{@code provided} - every vehicle the fleet sizing created. Tests whether the sampling itself is faithful.</li>
	 *     <li>{@code used} - only dispatched vehicles. The gap to {@code provided} is jsprit's selection bias.</li>
	 *     <li>{@code realised} - the actual tours. The gap to {@code used} is how much of the window a tour really uses.</li>
	 * </ul>
	 * Service durations are compared as {@code planned} (every service on a carrier, so including anything the
	 * unhandled-services loop redrew) and {@code handled} (services that made it into a tour). Their reference is a
	 * mixture: every service contributes the pmf of its own employee category, vehicle type and segment.
	 */
	private static final class Marginals {

		private static final String START = "tourStartHour";
		private static final String DURATION = "tourDuration";
		private static final String SERVICE = "serviceDuration";

		private final Map<SmallScaleCommercialTrafficSegment, EnumeratedDistribution<TourStartAndDuration>> tourRef;
		private final Map<ServiceDurationPerCategoryKey, EnumeratedDistribution<DurationsBounds>> stopRef;
		private final Map<String, Hist> hists = new LinkedHashMap<>();
		private final Set<String> segments = new TreeSet<>();
		/** segment -> service duration band -> weight, i.e. the reference mixture in its original bands. */
		private final Map<String, Map<DurationsBounds, Double>> serviceMixture = new HashMap<>();
		private int missingStopRef;

		Marginals() {
			CommercialTourSpecifications spec = new DefaultTourSpecificationsByUsingKID2002();
			// the rng only seeds the samplers, the pmf we read back is deterministic
			tourRef = spec.createTourDistribution(new MersenneTwister(4711));
			stopRef = spec.createStopDurationDistributionPerCategory(new MersenneTwister(4711));
		}

		private Hist hist(String marginal, String segment, String series, int n) {
			segments.add(segment);
			return hists.computeIfAbsent(marginal + "|" + segment + "|" + series, k -> new Hist(n));
		}

		private void add(String marginal, String segment, String series, int nBins, int bin, double w) {
			hist(marginal, segment, series, nBins).add(bin, w);
			hist(marginal, "*", series, nBins).add(bin, w);
		}

		/** Vehicle pool and planned services of one carrier. */
		void addCarrier(Carrier carrier) {
			String segment = segmentOf(carrier);

			for (CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				add(START, segment, "provided", START_BANDS.length - 1, startBand(v.getEarliestStartTime() / 3600d), 1);
				add(DURATION, segment, "provided", DURATION_BANDS.length - 1,
					durationBand((v.getLatestEndTime() - v.getEarliestStartTime()) / 60d), 1);
			}

			EnumeratedDistribution<DurationsBounds> ref = stopReference(carrier);
			for (CarrierService svc : carrier.getServices().values()) {
				add(SERVICE, segment, "planned", DURATION_BANDS.length - 1, durationBand(svc.getServiceDuration() / 60d), 1);
				if (ref == null) {
					missingStopRef++;
					continue;
				}
				// one service contributes its whole pmf, so the reference is the mixture over the actual composition
				for (Pair<DurationsBounds, Double> pair : ref.getPmf()) {
					add(SERVICE, segment, "kid", DURATION_BANDS.length - 1, durationBand(pair.getFirst().minDuration()), pair.getSecond());
					serviceMixture.computeIfAbsent(segment, k -> new HashMap<>()).merge(pair.getFirst(), pair.getSecond(), Double::sum);
					serviceMixture.computeIfAbsent("*", k -> new HashMap<>()).merge(pair.getFirst(), pair.getSecond(), Double::sum);
				}
			}
		}

		void addTour(Carrier carrier, ScheduledTour st, TourStat ts) {
			String segment = segmentOf(carrier);

			add(START, segment, "used", START_BANDS.length - 1, startBand(st.getVehicle().getEarliestStartTime() / 3600d), 1);
			add(START, segment, "realised", START_BANDS.length - 1, startBand(ts.departure() / 3600d), 1);

			double window = (st.getVehicle().getLatestEndTime() - st.getVehicle().getEarliestStartTime()) / 60d;
			add(DURATION, segment, "used", DURATION_BANDS.length - 1, durationBand(window), 1);
			add(DURATION, segment, "realised", DURATION_BANDS.length - 1, durationBand(ts.tourSeconds() / 60d), 1);

			for (Tour.TourElement el : st.getTour().getTourElements()) {
				if (el instanceof Tour.ServiceActivity sa)
					add(SERVICE, segment, "handled", DURATION_BANDS.length - 1, durationBand(sa.getDuration() / 60d), 1);
			}
		}

		private static String segmentOf(Carrier carrier) {
			Object sub = carrier.getAttributes().getAttribute("subpopulation");
			return sub != null && sub.toString().contains("goods") ? "goodsTraffic" : "commercialPersonTraffic";
		}

		private EnumeratedDistribution<DurationsBounds> stopReference(Carrier carrier) {
			boolean goods = "goodsTraffic".equals(segmentOf(carrier));
			ZoneAttribute category = categoryOf(carrier);
			if (category == null)
				return null;
			SmallScaleCommercialTrafficSegment seg = goods
				? SmallScaleCommercialTrafficSegment.goodsTraffic
				: SmallScaleCommercialTrafficSegment.commercialPersonTraffic;
			// the key carries the vehicle type for goods traffic only; it is the last token of the carrier id
			return stopRef.get(new ServiceDurationPerCategoryKey(category, goods ? vehTypeOf(carrier) : null, seg));
		}

		private static ZoneAttribute categoryOf(Carrier carrier) {
			Object raw = carrier.getAttributes().getAttribute("startCategory");
			if (raw == null)
				return null;
			if (raw instanceof ZoneAttribute za)
				return za;
			String v = raw.toString();
			return ZoneAttribute.fromLabel(v).orElseGet(() -> {
				try {
					return ZoneAttribute.valueOf(v);
				} catch (IllegalArgumentException e) {
					return null;
				}
			});
		}

		private static String vehTypeOf(Carrier carrier) {
			String id = carrier.getId().toString().replaceAll("_part_\\d+$", "");
			int i = id.lastIndexOf('_');
			return i < 0 ? null : id.substring(i + 1);
		}

		/**
		 * How many stops should a tour have, given only the model's own two KiD marginals? Draws a tour duration from
		 * the KiD tour distribution and fills it with KiD service durations until the budget is used up. This is a
		 * self consistency check rather than an external reference: KiD does record trip chain lengths, but the contrib
		 * exposes no such distribution, so there is nothing to compare against other than what the inputs imply.
		 *
		 * @param travelSecondsPerStop observed travel time per stop, 0 gives the upper bound where driving is free
		 */
		Map<Integer, Double> impliedStopsPerTour(String segment, double travelSecondsPerStop, int draws) {

			List<Pair<TourStartAndDuration, Double>> tourPmf = new ArrayList<>();
			for (Map.Entry<SmallScaleCommercialTrafficSegment, EnumeratedDistribution<TourStartAndDuration>> e : tourRef.entrySet()) {
				if (segment.equals("*") || e.getKey().toString().equals(segment))
					tourPmf.addAll(e.getValue().getPmf());
			}
			Map<DurationsBounds, Double> mixture = serviceMixture.get(segment);
			if (tourPmf.isEmpty() || mixture == null || mixture.isEmpty())
				return Map.of();

			List<Map.Entry<DurationsBounds, Double>> servicePmf = new ArrayList<>(mixture.entrySet());
			double tourTotal = tourPmf.stream().mapToDouble(Pair::getSecond).sum();
			double serviceTotal = servicePmf.stream().mapToDouble(Map.Entry::getValue).sum();

			Random rnd = new Random(4711);
			Map<Integer, Double> out = new TreeMap<>();
			for (int d = 0; d < draws; d++) {
				TourStartAndDuration t = pick(tourPmf, rnd.nextDouble() * tourTotal);
				// same uniform draw inside the band that the generator makes
				double budget = t.minDuration() == 0 ? t.maxDuration() * 60
					: rnd.nextDouble(t.minDuration() * 60, t.maxDuration() * 60);

				int stops = 0;
				double used = 0;
				while (stops < 100) {
					DurationsBounds b = pickService(servicePmf, rnd.nextDouble() * serviceTotal);
					double svc = rnd.nextInt(b.minDuration() * 60, b.maxDuration() * 60);
					if (used + svc + travelSecondsPerStop > budget)
						break;
					used += svc + travelSecondsPerStop;
					stops++;
				}
				out.merge(stops, 1d, Double::sum);
			}
			out.replaceAll((k, v) -> v / draws);
			return out;
		}

		private static TourStartAndDuration pick(List<Pair<TourStartAndDuration, Double>> pmf, double r) {
			double c = 0;
			for (Pair<TourStartAndDuration, Double> p : pmf) {
				c += p.getSecond();
				if (r <= c)
					return p.getFirst();
			}
			return pmf.getLast().getFirst();
		}

		private static DurationsBounds pickService(List<Map.Entry<DurationsBounds, Double>> pmf, double r) {
			double c = 0;
			for (Map.Entry<DurationsBounds, Double> p : pmf) {
				c += p.getValue();
				if (r <= c)
					return p.getKey();
			}
			return pmf.getLast().getKey();
		}

		/** Reference share per band, for one marginal and segment. */
		private double[] reference(String marginal, String segment) {
			if (marginal.equals(SERVICE)) {
				Hist h = hists.get(SERVICE + "|" + segment + "|kid");
				return h == null ? null : h.shares();
			}
			Hist h = new Hist(marginal.equals(START) ? START_BANDS.length - 1 : DURATION_BANDS.length - 1);
			for (Map.Entry<SmallScaleCommercialTrafficSegment, EnumeratedDistribution<TourStartAndDuration>> e : tourRef.entrySet()) {
				if (!segment.equals("*") && !e.getKey().toString().equals(segment))
					continue;
				for (Pair<TourStartAndDuration, Double> pair : e.getValue().getPmf()) {
					TourStartAndDuration t = pair.getFirst();
					h.add(marginal.equals(START) ? startBand(t.hourLower()) : durationBand(t.minDuration()), pair.getSecond());
				}
			}
			return h.shares();
		}

		void write(Path output) throws Exception {
			if (missingStopRef > 0)
				log.warn("No KiD stop duration reference for {} services, they are excluded from the reference mixture", missingStopRef);

			List<String> series = List.of("provided", "used", "realised", "planned", "handled");

			try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_input_marginal_fit.csv")),
				CSVFormat.DEFAULT.builder().setHeader("marginal", "segment", "band", "series", "count", "share", "kidShare", "diff").get());
				 CSVPrinter tvd = new CSVPrinter(Files.newBufferedWriter(output.resolve("commercial_input_marginal_tvd.csv")),
					 CSVFormat.DEFAULT.builder().setHeader("marginal", "segment", "series", "n", "totalVariationDistance").get())) {

				for (String marginal : List.of(START, DURATION, SERVICE)) {
					int nBins = marginal.equals(START) ? START_BANDS.length - 1 : DURATION_BANDS.length - 1;
					for (String segment : segments) {
						double[] ref = reference(marginal, segment);
						if (ref == null)
							continue;
						for (String s : series) {
							Hist h = hists.get(marginal + "|" + segment + "|" + s);
							if (h == null)
								continue;
							double[] obs = h.shares();
							double dist = 0;
							for (int i = 0; i < nBins; i++) {
								dist += Math.abs(obs[i] - ref[i]);
								csv.printRecord(marginal, segment, marginal.equals(START) ? startBandLabel(i) : durationBandLabel(i),
									s, Math.round(h.bins[i]), round(obs[i]), round(ref[i]), round(obs[i] - ref[i]));
							}
							tvd.printRecord(marginal, segment, s, Math.round(h.sum()), round(dist / 2));
						}
					}
				}
			}
		}
	}
}
