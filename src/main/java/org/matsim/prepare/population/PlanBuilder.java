package org.matsim.prepare.population;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import tech.tablesaw.api.Row;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to build plans from activity data.
 */
public class PlanBuilder {

	private static final Logger log = LogManager.getLogger(PlanBuilder.class);
	private static final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	/**
	 * Stores of warning for a zone was generated.
	 */
	private final Set<Location> warnings = new HashSet<>();

	/**
	 * Maps zone ids to contained facilities.
	 */
	private final Long2ObjectMap<Set<ActivityFacility>> zones = new Long2ObjectOpenHashMap<>();

	private final ShpOptions.Index zoneIndex;

	/**
	 * Maps location key to zone id.
	 */
	private final Object2LongMap<Location> features = new Object2LongOpenHashMap<>();

	private final Map<String, List<CSVRecord>> activities = new HashMap<>();

	private final SplittableRandom rnd = new SplittableRandom();

	/**
	 * Drop plans with more than this number of trips.
	 */
	private int maxTripNumber = 0;

	public PlanBuilder(ShpOptions zones, FacilityIndex facilities, Path activityPath) throws IOException {
		// Collect all zones
		for (SimpleFeature ft : zones.readFeatures()) {
			features.put(new Location((String) ft.getAttribute("raum_id"), (String) ft.getAttribute("zone")),
				(long) ft.getAttribute("id"));
		}

		zoneIndex = zones.createIndex("id");

		for (ActivityFacility ft : facilities.all.getFacilities().values()) {
			Long result = zoneIndex.query(ft.getCoord());
			if (result != null) {
				this.zones.computeIfAbsent(result, k -> new HashSet<>()).add(ft);
			}
		}

		try (CSVParser parser = csv.createParser(activityPath)) {
			readActivities(parser, "p_id");
		}
	}

	private void readActivities(CSVParser csv, String idColumn) {

		String currentId = null;
		List<CSVRecord> current = null;

		int i = 0;
		for (CSVRecord r : csv) {

			String pId = r.get(idColumn);

			if (!Objects.equals(pId, currentId)) {
				if (current != null)
					activities.put(currentId, current);

				currentId = pId;
				current = new ArrayList<>();
			}

			current.add(r);
			i++;
		}

		if (current != null && !current.isEmpty()) {
			activities.put(currentId, current);
		}

		log.info("Read {} activities for {} persons", i, activities.size());
	}

	/**
	 * Return all read activities.
	 */
	public Map<String, List<CSVRecord>> getActivities() {
		return activities;
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
	 * Create a map for each zone to set of persons living there.
	 */
	public Long2ObjectMap<List<Person>> createHomeIndex(Population population) {

		Long2ObjectMap<List<Person>> personsByHome = new Long2ObjectOpenHashMap<>();
		for (Person person : population.getPersons().values()) {

			double homeX = (double) person.getAttributes().getAttribute(Attributes.HOME_X);
			double homeY = (double) person.getAttributes().getAttribute(Attributes.HOME_Y);

			Long home = zoneIndex.query(new Coord(homeX, homeY));

			if (home != null) {
				personsByHome.computeIfAbsent(home, k -> new ArrayList<>()).add(person);
			}
		}

		return personsByHome;
	}

	/**
	 * Find the home zone for a person.
	 * @return -1 if not known
	 */
	public long findHomeZone(String personId) {

		List<CSVRecord> activities = this.activities.get(personId);

		Optional<CSVRecord> home = activities.stream().filter(r -> r.get("type").equals("home")).findFirst();
		if (home.isEmpty())
			return -1;

		Location loc = new Location(home.get().get("location"), home.get().get("zone"));
		return features.getOrDefault(loc, -1);
	}

	/**
	 * Assigns location from reference data to a person.
	 * @return whether the assignment was successful
	 */
	public boolean assignLocationsFromZones(String personId, Plan plan) {

		List<CSVRecord> activities = this.activities.get(personId);
		List<Activity> existing = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		// If activities don't match, this entry is skipped
		// this can happen if and end home activity has been added at the end
		if (activities.size() != existing.size())
			return false;

		// The location happen at the same place (if in same zone)
		Map<TypeLocation, ActivityFacility> fixedLocations = new HashMap<>();

		for (int i = 0; i < activities.size(); i++) {

			CSVRecord ref = activities.get(i);
			Activity activity = existing.get(i);

			String type = activity.getType();

			TypeLocation tLoc = new TypeLocation(type, ref.get("location"), ref.get("zone"));
			if (fixedLocations.containsKey(tLoc)) {
				activity.setFacilityId(fixedLocations.get(tLoc).getId());
				continue;
			}


			Location loc = new Location(ref.get("location"), ref.get("zone"));
			long id = features.getOrDefault(loc, -1);
			if (id == -1) {
				return false;
			}

			Set<ActivityFacility> facilities = zones.get(id);

			// TODO: think of matching and selection here



			if (type.equals("work") || type.startsWith("edu")) {
				// TODO: store the location
			}
		}

		return true;
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

		Set<ActivityFacility> facilities = zones.get(id);

		if (facilities == null) {
			if (warnings.add(loc))
				log.error("No facilities found in zone {}", loc);

			return null;
		}

		return facilities.stream().map(ActivityFacility::getCoord).collect(Collectors.toSet());
	}

	private record Location(String name, String zone) {
	}

	private record TypeLocation(String type, String name, String zone) {
	}

}
