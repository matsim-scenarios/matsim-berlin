package org.matsim.prepare.population;

import com.google.inject.Injector;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@CommandLine.Command(
	name = "compute-choice-alternatives",
	description = "Computes all choices and metrics for a dataset of trips."
)
public class ComputeChoiceAlternatives implements MATSimAppCommand {

	// TODO: move whole class to contrib when done
	private static final Logger log = LogManager.getLogger(ComputeChoiceAlternatives.class);
	private final SplittableRandom rnd = new SplittableRandom();
	private final Long2ObjectMap<Set<ActivityFacility>> zones = new Long2ObjectOpenHashMap<>();
	private final Object2LongMap<Location> features = new Object2LongOpenHashMap<>();
	/**
	 * Stores of warning for a zone was generated.
	 */
	private final Set<Location> warnings = new HashSet<>();
	@CommandLine.Mixin
	private ScenarioOptions scenario;
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Option(names = "--trips", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path input;
	@CommandLine.Option(names = "--modes", description = "Modes to include in choice set", split = ",", required = true)
	private Set<String> modes;
	@CommandLine.Option(names = "--output", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path output;

	public static void main(String[] args) {
		new ComputeChoiceAlternatives().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			log.error("No shapefile defined. Please specify a shapefile for the zones using the --shp option.");
			return 2;
		}

		if (!Files.exists(input)) {
			log.error("Input file does not exist: " + input);
			return 2;
		}

		Config config = this.scenario.getConfig();
		config.controller().setOutputDirectory("choice-output");
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		Controler controler = this.scenario.createControler();

		// Run for one iteration to collect travel times
		controler.run();

		Injector injector = controler.getInjector();

		Scenario scenario = injector.getInstance(Scenario.class);

		// Collect all zones
		for (SimpleFeature ft : shp.readFeatures()) {
			features.put(new Location((String) ft.getAttribute("raum_id"), (String) ft.getAttribute("zone")),
				(long) ft.getAttribute("id"));
		}

		ShpOptions.Index index = shp.createIndex(this.scenario.getConfig().global().getCoordinateSystem(), "id");

		// Collect all facilities per zone
		for (ActivityFacility f : scenario.getActivityFacilities().getFacilities().values()) {
			Long result = index.query(f.getCoord());
			if (result != null) {
				zones.computeIfAbsent(result, k -> new HashSet<>()).add(f);
			}
		}

		Id<Vehicle> car = Id.createVehicleId("car");
		Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(
			car, scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class))
		);
		scenario.getVehicles().addVehicle(vehicle);

		Table trips = Table.read().csv(input.toFile());

		ThreadLocal<TripRouter> ctx = ThreadLocal.withInitial(() -> injector.getInstance(TripRouter.class));

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		List<CompletableFuture<List<Entry>>> futures = new ArrayList<>();

		ProgressBar pb = new ProgressBar("Computing choices", trips.rowCount());

		// can not use row iterator because of parallel processing
		for (int i = 0; i < trips.rowCount(); i++) {

			Row row = trips.row(i);

			// Randomize departure times
			double departure = (rnd.nextInt(-10, 10) + row.getInt("departure")) * 60;
			String pId = row.getString("p_id");

			Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(pId));
			VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, Map.of("car", car));

			futures.add(CompletableFuture.supplyAsync(() -> {
				TripRouter r = ctx.get();

				List<Entry> entries = computeAlternatives(r, row, person, departure);
				pb.step();

				return entries;

			}, executor));

		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
			.join();

		executor.shutdown();
		pb.close();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {

			csv.printRecord("p_id", "trip_n", "mode", "choice", "beelineDist", "travelDist", "travelTime", "walkDist", "valid");

			for (CompletableFuture<List<Entry>> f : futures) {

				List<Entry> entries = f.get();
				if (entries != null) {
					for (Entry r : entries) {
						csv.printRecord(r.person, r.n, r.mode, r.choice, r.beelineDistance, r.travelDistance,
							r.travelTime, r.walkDistance, r.valid ? 1 : 0);
					}
				}
			}
		}

		return 0;
	}

	/**
	 * Compute all alternatives for a given trip.
	 */
	private List<Entry> computeAlternatives(TripRouter router, Row row, Person person, double departure) {

		Pair<ActivityFacility, ActivityFacility> f = sampleFacility(row);

		if (f == null)
			return null;

		List<Entry> result = new ArrayList<>();

		double beelineDist = CoordUtils.calcEuclideanDistance(f.left().getCoord(), f.right().getCoord());

		for (String mode : modes) {

			Entry entry = new Entry(person.getId().toString(), row.getString("main_mode"), mode, beelineDist, row.getInt("n"));

			List<? extends PlanElement> route = router.calcRoute(mode, f.left(), f.right(), departure, person, new AttributesImpl());

			for (PlanElement el : route) {

				if (el instanceof Leg leg) {

					entry.travelTime += leg.getTravelTime().seconds();
					entry.travelDistance += leg.getRoute().getDistance();

					if (leg.getMode().equals(TransportMode.walk)) {
						entry.walkDistance += leg.getRoute().getDistance();
					}

					if (leg.getMode().equals(mode))
						entry.valid = true;
				}
			}

			result.add(entry);
		}

		return result;
	}

	/**
	 * Sample pair of from and to facility. Tries to find relation with similar distance to the input.
	 */
	private Pair<ActivityFacility, ActivityFacility> sampleFacility(Row row) {

		Set<ActivityFacility> from = matchFacility(row.getString("from_location"), row.getString("from_zone"));
		if (from == null || from.isEmpty())
			return null;

		Set<ActivityFacility> to = matchFacility(row.getString("to_location"), row.getString("to_zone"));
		if (to == null || to.isEmpty())
			return null;

		double targetDist = InitLocationChoice.beelineDist(row.getDouble("gis_length") * 1000);

		double dist = Double.POSITIVE_INFINITY;
		ActivityFacility f = null;
		ActivityFacility t = null;
		int i = 0;
		do {

			ActivityFacility newF = from.stream().skip(rnd.nextInt(from.size())).findFirst().orElseThrow();
			ActivityFacility newT = to.stream().skip(rnd.nextInt(to.size())).findFirst().orElseThrow();

			double newDist = CoordUtils.calcEuclideanDistance(newF.getCoord(), newT.getCoord());
			if (Math.abs(newDist - targetDist) < Math.abs(dist - targetDist)) {
				dist = newDist;
				f = newF;
				t = newT;
			}

			i++;
		} while (i < 8);


		return Pair.of(f, t);
	}


	/**
	 * Select a random facility for a person.
	 */
	private Set<ActivityFacility> matchFacility(String location, String zone) {

		Location loc = new Location(location, zone);
		long id = features.getOrDefault(loc, -1);

		if (id == -1) {
			if (warnings.add(loc))
				log.error("No zone found for location {} and zone {}", location, zone);

			return null;
		}

		Set<ActivityFacility> facilities = zones.get(id);

		if (facilities == null) {
			if (warnings.add(loc))
				log.error("No facilities found in zone {}", loc);

			return null;
		}

		return facilities;
	}


	/**
	 * One row in the output results.
	 */
	private static final class Entry {

		private final String person;
		private final String mode;
		private final String choice;
		private final double beelineDistance;
		private final int n;

		private double travelDistance;
		private double travelTime;
		private double walkDistance;

		/**
		 * Requested mode is actually used in this trip
		 */
		private boolean valid;

		Entry(String person, String choice, String mode, double beelineDistance, int n) {
			this.person = person;
			this.choice = choice;
			this.mode = mode;
			this.n = n;
			this.beelineDistance = beelineDistance;
		}
	}

	private record Location(String name, String zone) {
	}

}
