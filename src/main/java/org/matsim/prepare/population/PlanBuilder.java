package org.matsim.prepare.population;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility class to build plans from activity data.
 */
public class PlanBuilder {

	private static final Logger log = LogManager.getLogger(PlanBuilder.class);
	private static final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

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
	 *
	 * @return -1 if not known
	 */
	public long findHomeZone(String personId) {

		List<CSVRecord> acts = activities.get(personId);

		Optional<CSVRecord> home = acts.stream().filter(r -> r.get("type").equals("home")).findFirst();
		if (home.isEmpty())
			return -1;

		Location loc = new Location(home.get().get("location"), home.get().get("zone"));
		return features.getOrDefault(loc, -1);
	}

	/**
	 * Assigns location from reference data to a person.
	 *
	 * @return whether the assignment was successful
	 */
	public boolean assignLocationsFromZones(String personId, Plan plan, Coord homeCoord) {

		List<CSVRecord> acts = activities.get(personId);
		List<Activity> existing = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		// If activities don't match, this entry is skipped
		// this can happen if an end home activity has been added at the end
		if (acts.size() != existing.size())
			return false;

		ActLocation home = new ActLocation(null, homeCoord);

		List<List<ActLocation>> possibleLocations = new ArrayList<>();

		// Distances between activities in meter
		DoubleList dists = new DoubleArrayList();

		for (int i = 0; i < acts.size(); i++) {

			CSVRecord ref = acts.get(i);
			Activity activity = existing.get(i);

			String type = activity.getType();

			dists.add(InitLocationChoice.beelineDist(Double.parseDouble(ref.get("leg_dist"))));

			if (type.equals("home")) {
				possibleLocations.add(List.of(home));
				continue;
			}

			Location loc = new Location(ref.get("location"), ref.get("zone"));
			long id = features.getOrDefault(loc, -1);
			if (id == -1) {
				return false;
			}

			Set<ActivityFacility> facilities = zones.get(id);

			List<ActivityFacility> subSet = facilities.stream().filter(f -> f.getActivityOptions().containsKey(type)).toList();

			if (subSet.isEmpty()) {
				// If there is no location with the correct type, choose from all possible coordinates
				possibleLocations.add(
					facilities.stream().map(f -> new ActLocation(null, f.getCoord())).toList()
				);
			} else {
				possibleLocations.add(
					subSet.stream().map(f -> new ActLocation(f, f.getCoord())).toList()
				);
			}
		}

		List<ActLocation> chosen = sampleLocation(possibleLocations, dists);

		// No valid locations or matching error was too large
		if (chosen == null)
			return false;

		for (int i = 0; i < chosen.size(); i++) {
			ActLocation loc = chosen.get(i);
			Activity activity = existing.get(i);

			activity.setLinkId(null);
			if (loc.facility() != null) {
				activity.setFacilityId(loc.facility().getId());
			} else {
				activity.setCoord(loc.coord());
			}
		}


		return true;
	}

	/**
	 * Chooses from a list of possible locations such that difference to the references distances is minimized.
	 */
	private List<ActLocation> sampleLocation(List<List<ActLocation>> locations, DoubleList dists) {

		double err = Double.POSITIVE_INFINITY;
		List<ActLocation> best = null;

		for (int k = 0; k < 100; k++) {
			List<ActLocation> current = new ArrayList<>();
			for (List<ActLocation> locs : locations) {
				current.add(locs.get(rnd.nextInt(locs.size())));
			}

			double currentErr = 0;
			for (int i = 1; i < current.size(); i++) {
				double dist = CoordUtils.calcEuclideanDistance(current.get(i - 1).coord(), current.get(i).coord());
				currentErr += Math.abs(dist - dists.getDouble(i));
			}

			if (currentErr < err || best == null) {
				err = currentErr;
				best = current;
			}
		}

		double total = dists.doubleStream().sum() / (locations.size() - 1);
		double perActErr = err / (locations.size() - 1);

		// threshold for deviation
		if (perActErr > Math.max(300, total * 0.03))
			return null;

		return best;
	}

	private record Location(String name, String zone) {
	}

	private record ActLocation(ActivityFacility facility, Coord coord) {
	}

}
