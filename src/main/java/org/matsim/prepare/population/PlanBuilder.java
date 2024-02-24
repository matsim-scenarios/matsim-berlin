package org.matsim.prepare.population;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;
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

	/**
	 * Maps location key to zone id.
	 */
	private final Object2LongMap<Location> features = new Object2LongOpenHashMap<>();

	private final SplittableRandom rnd = new SplittableRandom();

	private final PopulationFactory f;

	public PlanBuilder(ShpOptions zones, ShpOptions facilities, PopulationFactory f) {
		// Collect all zones
		for (SimpleFeature ft : zones.readFeatures()) {
			features.put(new Location((String) ft.getAttribute("raum_id"), (String) ft.getAttribute("zone")),
				(long) ft.getAttribute("id"));
		}

		ShpOptions.Index index = zones.createIndex("id");

		for (SimpleFeature ft : ProgressBar.wrap(facilities.readFeatures(), "Reading facilities")) {
			Geometry geom = (Geometry) ft.getDefaultGeometry();
			Coord coord = new Coord(geom.getCentroid().getX(), geom.getCentroid().getY());
			Long result = index.query(coord);
			if (result != null) {
				this.zones.computeIfAbsent(result, k -> new HashSet<>()).add(coord);
			}
		}

		this.f = f;
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
	public List<Person> createPlans(Path input) {

		Table table = Table.read().csv(input.toFile());

		String currentPerson = null;
		int currentSeq = -1;

		List<Person> result = new ArrayList<>();

		List<Row> trips = new ArrayList<>();

		try (ProgressBar pb = new ProgressBar("Reading trips", table.rowCount())) {

			for (int i = 0; i < table.rowCount(); i++) {

				Row row = table.row(i);

				String pId = row.getString("p_id");
				int seq = row.getInt("seq");

				if (!pId.equals(currentPerson) || seq != currentSeq) {
					if (!trips.isEmpty()) {
						Person person = createPerson(pId, seq, trips);
						if (person != null)
							result.add(person);

						trips.clear();
					}

					currentPerson = pId;
					currentSeq = seq;
				}

				trips.add(row);
				pb.step();
			}
		}

		return result;
	}

	/**
	 * Create person from row data.
	 */
	private Person createPerson(String id, int seq, List<Row> trips) {

		Person person = f.createPerson(Id.createPersonId(id + "_" + seq));

		PersonUtils.setCarAvail(person, trips.get(0).getInt("p_age") >= 18 ? "always" : "never");

		VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, Map.of("car", Id.createVehicleId("car"),
			"ride", Id.createVehicleId("ride")));

		Plan plan = f.createPlan();

		Pair<Coord, Coord> trip = sampleOD(trips.get(0));

		if (trip == null)
			return null;

		Activity act = f.createActivityFromCoord("act", trip.first());
		act.setEndTime(trips.get(0).getInt("departure") * 60);
		act.getAttributes().putAttribute("n", trips.get(0).getInt("n"));

		plan.addActivity(act);
		plan.addLeg(f.createLeg(trips.get(0).getString("main_mode")));

		act = f.createActivityFromCoord("act", trip.second());
		plan.addActivity(act);

		for (int i = 1; i < trips.size(); i++) {

			Row row = trips.get(i);

			Coord dest = sampleDest(row, act.getCoord());
			if (dest == null)
				return null;

			act.setEndTime(row.getInt("departure") * 60);
			act.getAttributes().putAttribute("n", row.getInt("n"));

			plan.addLeg(f.createLeg(row.getString("main_mode")));

			act = f.createActivityFromCoord("act", dest);
			plan.addActivity(act);
		}


		person.getAttributes().putAttribute("seq", seq);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		return person;
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
}
