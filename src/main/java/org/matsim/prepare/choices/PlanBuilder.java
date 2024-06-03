package org.matsim.prepare.choices;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.population.SplitActivityTypesDuration;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.prepare.population.Attributes;
import org.matsim.prepare.population.InitLocationChoice;
import org.matsim.prepare.population.PersonMatcher;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.*;

/**
 * Utility class to build plans from data.
 */
public class PlanBuilder {

	private static final Logger log = LogManager.getLogger(PlanBuilder.class);

	/**
	 * Stores of warning for a zone was generated.
	 */
	private final Set<Location> warnings = new HashSet<>();

	/**
	 * Maps zone ids to contained coordinates.
	 */
	private final Long2ObjectMap<Set<Coord>> zones = new Long2ObjectOpenHashMap<>();

	private final ShpOptions.Index zoneIndex;

	/**
	 * Maps location key to zone id.
	 */
	private final Object2LongMap<Location> features = new Object2LongOpenHashMap<>();

	private final SplitActivityTypesDuration splitDuration = new SplitActivityTypesDuration();

	private final SplittableRandom rnd = new SplittableRandom();

	private final PopulationFactory f;

	/**
	 * Drop plans with more than this number of trips.
	 */
	private int maxTripNumber = 0;

	public PlanBuilder(ShpOptions zones, ShpOptions facilities, PopulationFactory f) {
		// Collect all zones
		for (SimpleFeature ft : zones.readFeatures()) {
			features.put(new Location((String) ft.getAttribute("raum_id"), (String) ft.getAttribute("zone")),
				(long) ft.getAttribute("id"));
		}

		zoneIndex = zones.createIndex("id");

		for (SimpleFeature ft : ProgressBar.wrap(facilities.readFeatures(), "Reading facilities")) {
			Geometry geom = (Geometry) ft.getDefaultGeometry();
			Coord coord = new Coord(geom.getCentroid().getX(), geom.getCentroid().getY());
			Long result = zoneIndex.query(coord);
			if (result != null) {
				this.zones.computeIfAbsent(result, k -> new HashSet<>()).add(coord);
			}
		}

		this.f = f;
	}

	/**
	 * Set the maximum number of trips in a plan.
	 */
	public PlanBuilder setMaxTripNumber(int maxTripNumber) {
		this.maxTripNumber = maxTripNumber;
		return this;
	}

	/**
	 * Add necesarry vehicles to the scenario.
	 */
	public static void addVehiclesToScenario(Scenario scenario) {
		Id<Vehicle> car = Id.createVehicleId("car");
		Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(
			car, scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class))
		);
		scenario.getVehicles().addVehicle(vehicle);

		Id<Vehicle> ride = Id.createVehicleId("ride");
		vehicle = scenario.getVehicles().getFactory().createVehicle(
			ride, scenario.getVehicles().getVehicleTypes().get(Id.create("ride", VehicleType.class))
		);
		scenario.getVehicles().addVehicle(vehicle);

	}

	/**
	 * Create persons with plans from a table.
	 */
	public List<Person> createPlans(Path trips) {
		return handleTrips(trips, null, this::createPerson);
	}

	/**
	 * Reads reference trips from input and merges it into existing population.
	 * @see #createPlans(Path)
	 */
	public List<Person> mergePlans(Population population, Path trips, Path persons) {

		// TODO: index home locations of the population
		// TODO: prob to reduce number of reference agents ?

		// Index home locations of the population
		Long2ObjectMap<Set<Person>> personsByHome = new Long2ObjectOpenHashMap<>();
		for (Person person : population.getPersons().values()) {

			double homeX = (double) person.getAttributes().getAttribute(Attributes.HOME_X);
			double homeY = (double) person.getAttributes().getAttribute(Attributes.HOME_Y);

			Long home = zoneIndex.query(new Coord(homeX, homeY));

			if (home != null) {
				personsByHome.computeIfAbsent(home, k -> new HashSet<>()).add(person);
			}
		}


		return handleTrips(trips, persons, (pId, seq, person, t) -> {

			// Duplicated persons are ignored for this approach
			if (seq > 0)
				return null;

			Location loc = getHomeLocation(t);
			long home = features.getOrDefault(loc, -1);

			if (home == -1 || !personsByHome.containsKey(home))
				return null;

			return mergePerson(personsByHome.get(home), person, t);
		});
	}

	/**
	 * Guess home location from trips data.
	 */
	private Location getHomeLocation(List<Row> trips) {
		return new Location(trips.get(0).getString("from_location"), trips.get(0).getString("from_zone"));
	}

	/**
	 * Helper function to iterate through trips data and process it.
	 */
	private List<Person> handleTrips(Path trips, Path persons, EntryHandler handler) {

		Table table = Table.read().csv(trips.toFile());

		String currentPerson = null;
		int currentSeq = -1;

		PersonMatcher matcher = new PersonMatcher("p_id", persons);

		List<Person> result = new ArrayList<>();

		List<Row> tripRows = new ArrayList<>();

		try (ProgressBar pb = new ProgressBar("Reading trips", table.rowCount())) {

			for (int i = 0; i < table.rowCount(); i++) {

				Row row = table.row(i);

				String pId = row.getString("p_id");
				int seq = row.getInt("seq");

				if (!pId.equals(currentPerson) || seq != currentSeq) {
					if (!tripRows.isEmpty()) {

						// Filter person with too many trips
						if (maxTripNumber <= 0 || tripRows.size() <= maxTripNumber) {
							Person person = handler.process(currentPerson, currentSeq,
								matcher.getPerson(pId), tripRows);
							if (person != null)
								result.add(person);
						}

						tripRows.clear();
					}

					currentPerson = pId;
					currentSeq = seq;
				}

				tripRows.add(row);
				pb.step();
			}
		}

		return result;
	}

	/**
	 * Create person from row data.
	 */
	private Person createPerson(String id, int seq, CSVRecord p, List<Row> trips) {

		Person person = f.createPerson(Id.createPersonId(id + "_" + seq));

		PopulationUtils.putSubpopulation(person, "person");
		PersonUtils.setCarAvail(person, trips.get(0).getInt("p_age") >= 18 ? "always" : "never");

		VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, Map.of("car", Id.createVehicleId("car"),
			"ride", Id.createVehicleId("ride")));

		Plan plan = f.createPlan();

		Pair<Coord, Coord> trip = sampleOD(trips.get(0));

		if (trip == null)
			return null;

		// source-destination purpose
		String sd = trips.get(0).getString("sd_group");

		Activity act = f.createActivityFromCoord(sd.startsWith("home") ? "home": "other", trip.first());
		int departure = trips.get(0).getInt("departure") * 60;
		act.setEndTime(departure);
		act.getAttributes().putAttribute("n", trips.get(0).getInt("n"));

		plan.addActivity(act);
		plan.addLeg(f.createLeg(trips.get(0).getString("main_mode")));

		act = f.createActivityFromCoord(trips.get(0).getString("purpose"), trip.second());
		act.setStartTime(departure + trips.get(0).getInt("duration") * 60);
		plan.addActivity(act);

		for (int i = 1; i < trips.size(); i++) {

			Row row = trips.get(i);

			Coord dest = sampleDest(row, act.getCoord());
			if (dest == null)
				return null;

			departure = row.getInt("departure") * 60;
			act.setEndTime(departure);
			act.getAttributes().putAttribute("n", row.getInt("n"));

			plan.addLeg(f.createLeg(row.getString("main_mode")));

			act = f.createActivityFromCoord(row.getString("purpose"), dest);
			act.setStartTime(departure + row.getInt("duration") * 60);
			plan.addActivity(act);
		}


		person.getAttributes().putAttribute("seq", seq);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		splitDuration.run(person);

		return person;
	}

	/**
	 * Assign the trips to an existing person.
	 */
	private Person mergePerson(Set<Person> persons, CSVRecord person, List<Row> trips) {

		// TODO

		return null;
	}

	/**
	 * Sample pair of from and to facility. Tries to find relation with similar distance to the input.
	 */
	private Pair<Coord, Coord> sampleOD(Row row) {

		Set<Coord> from = matchLocation(row.getString("from_location"), row.getString("from_zone"));
		Set<Coord> to = matchLocation(row.getString("to_location"), row.getString("to_zone"));
		if (from == null || from.isEmpty() || to == null || to.isEmpty())
			return null;

		double targetDist = InitLocationChoice.beelineDist(row.getDouble("gis_length") * 1000);

		double dist = Double.POSITIVE_INFINITY;
		Coord f = null;
		Coord t = null;
		int i = 0;
		do {

			Coord newF = from.stream().skip(rnd.nextInt(from.size())).findFirst().orElseThrow();
			Coord newT = to.stream().skip(rnd.nextInt(to.size())).findFirst().orElseThrow();

			double newDist = CoordUtils.calcEuclideanDistance(newF, newT);
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
	 * Sample destination from a row and existing location.
	 */
	private Coord sampleDest(Row row, Coord coord) {

		Set<Coord> to = matchLocation(row.getString("to_location"), row.getString("to_zone"));
		if (to == null || to.isEmpty())
			return null;

		double targetDist = InitLocationChoice.beelineDist(row.getDouble("gis_length") * 1000);
		double dist = Double.POSITIVE_INFINITY;
		Coord f = null;

		int i = 0;
		do {

			Coord newT = to.stream().skip(rnd.nextInt(to.size())).findFirst().orElseThrow();

			double newDist = CoordUtils.calcEuclideanDistance(coord, newT);

			if (Math.abs(newDist - targetDist) < Math.abs(dist - targetDist)) {
				dist = newDist;
				f = newT;
			}

			i++;
		} while (i < 8);


		return f;
	}


	/**
	 * Select a random facility for a person.
	 */
	private Set<Coord> matchLocation(String location, String zone) {

		Location loc = new Location(location, zone);
		long id = features.getOrDefault(loc, -1);

		if (id == -1) {
			if (warnings.add(loc))
				log.error("No zone found for location {} and zone {}", location, zone);

			return null;
		}

		Set<Coord> facilities = zones.get(id);

		if (facilities == null) {
			if (warnings.add(loc))
				log.error("No facilities found in zone {}", loc);

			return null;
		}

		return facilities;
	}


	private record Location(String name, String zone) {
	}

	@FunctionalInterface
	private interface EntryHandler {

		Person process(String pId, int seq, CSVRecord person, List<Row> trips);

	}

}
