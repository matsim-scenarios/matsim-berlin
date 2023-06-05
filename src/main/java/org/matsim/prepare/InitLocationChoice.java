package org.matsim.prepare;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.run.RunOpenBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.matsim.prepare.CreateMATSimFacilities.IGNORED_LINK_TYPES;

@CommandLine.Command(
	name = "init-location-choice",
	description = "Assign initial locations to agents"
)
@SuppressWarnings("unchecked")
public class InitLocationChoice implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(InitLocationChoice.class);

	@CommandLine.Option(names = "--input", description = "Path to input population, can be a pattern if * is used.", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--commuter", description = "Path to commuter.csv", required = true)
	private Path commuterPath;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file", required = true)
	private Path facilityPath;

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--sample", description = "Sample size of the population", defaultValue = "0.25")
	private double sample;

	@CommandLine.Mixin
	private ShpOptions shp;


	private Map<String, STRtree> trees;

	private Long2ObjectMap<SimpleFeature> zones;

	private CommuterAssignment commuter;

	private Network network;

	private ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

	private ThreadLocal<Context> ctxs;

	private AtomicLong total = new AtomicLong();

	private AtomicLong warning = new AtomicLong();

	public static void main(String[] args) {
		new InitLocationChoice().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file with commuter zones is required.");
			return 2;
		}

		ctxs = ThreadLocal.withInitial(Context::new);

		Network completeNetwork = NetworkUtils.readNetwork(networkPath.toString());
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(completeNetwork);
		network = NetworkUtils.createNetwork();
		filter.filter(network, Set.of(TransportMode.car));

		zones = new Long2ObjectOpenHashMap<>(shp.readFeatures().stream()
			.collect(Collectors.toMap(ft -> Long.parseLong((String) ft.getAttribute("ARS")), ft -> ft)));

		log.info("Read {} zones", zones.size());


		new MatsimFacilitiesReader(RunOpenBerlinScenario.CRS, RunOpenBerlinScenario.CRS, facilities)
			.readFile(facilityPath.toString());

		Set<String> activities = facilities.getFacilities().values().stream()
			.flatMap(a -> a.getActivityOptions().keySet().stream())
			.collect(Collectors.toSet());

		log.info("Found activity types: {}", activities);

		trees = new HashMap<>();
		for (String act : activities) {

			TreeMap<Id<ActivityFacility>, ActivityFacility> afs = facilities.getFacilitiesForActivityType(act);
			for (ActivityFacility af : afs.values()) {
				STRtree index = trees.computeIfAbsent(act, k -> new STRtree());
				index.insert(MGC.coord2Point(af.getCoord()).getEnvelopeInternal(), af);
			}
		}

		// Build all trees
		trees.values().forEach(STRtree::build);

		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + input.getFileName().toString());

		List<Path> input = Files.list(this.input.getParent())
			.filter(matcher::matches)
			.sorted()
			.toList();

		log.info("Using input files: {}", input);

		int i = 1;
		for (Path p : input) {

			commuter = new CommuterAssignment(zones, commuterPath, sample);

			Population population = PopulationUtils.readPopulation(p.toString());
			ParallelPersonAlgorithmUtils.run(population, 8, this);

			String filename = output.toString().replace("plans.xml.gz", "plans-" + i + ".xml.gz");
			PopulationUtils.writePopulation(population, filename);

			log.info("Written population to {}", filename);
			log.info("Processed {} activities with {} warnings", total.get(), warning.get());

			total.set(0);
			warning.set(0);

			i++;
		}


		return 0;
	}

	@Override
	public void run(Person person) {

		Coord homeCoord = Attributes.getHomeCoord(person);

		// Activities that only occur on one place per person
		Map<String, ActivityFacility> fixedLocations = new HashMap<>();
		Context ctx = ctxs.get();

		for (Plan plan : person.getPlans()) {
			List<Activity> acts = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			// keep track of the current coordinate
			Coord lastCoord = homeCoord;

			for (Activity act : acts) {

				String type = act.getType();

				total.incrementAndGet();

				if (Attributes.isLinkUnassigned(act.getLinkId())) {
					act.setLinkId(null);
					ActivityFacility location = null;

					// target leg distance in meter
					Object origDist = act.getAttributes().getAttribute("orig_dist");
					double dist = (double) origDist * 1000;

					if (fixedLocations.containsKey(type)) {
						location = fixedLocations.get(type);
					}

					if (location == null && type.equals("work")) {
						// sample work commute
						location = sampleCommute(ctx, dist, lastCoord, (long) person.getAttributes().getAttribute(Attributes.ARS));
					}

					if (location == null && trees.containsKey(type)) {
						// Needed for lambda
						final Coord refCoord = lastCoord;

						List<ActivityFacility> query = trees.get(type).query(MGC.coord2Point(lastCoord).buffer(dist * 1.2).getEnvelopeInternal());

						// Distance should be within the bounds
						List<ActivityFacility> res = query.stream().filter(f -> checkDistanceBound(dist, refCoord, f.getCoord(), 1)).toList();

						if (!res.isEmpty()) {
							location = query.get(ctx.rnd.nextInt(query.size()));
						}

						// Try with larger bounds again
						if (location == null) {
							res = query.stream().filter(f -> checkDistanceBound(dist, refCoord, f.getCoord(), 1.2)).toList();
							if (!res.isEmpty()) {
								location = query.get(ctx.rnd.nextInt(query.size()));
							}
						}
					}

					if (location == null) {
						// sample only coordinate if nothing else is possible
						// Activities without facility entry, or where no facility could be found
						Coord c = sampleLink(ctx.rnd, dist, lastCoord);
						act.setCoord(c);
						lastCoord = c;

						// An activity with type could not be put into correct facility.
						if (trees.containsKey(type)){
							warning.incrementAndGet();
						}

						continue;
					}

					if (type.equals("work") || type.startsWith("edu"))
						fixedLocations.put(type, location);

					act.setFacilityId(location.getId());
				}

				if (act.getCoord() != null)
					lastCoord = act.getCoord();
				else if (act.getFacilityId() != null)
					lastCoord = facilities.getFacilities().get(act.getFacilityId()).getCoord();

			}
		}
	}

	/**
	 * Sample work place by using commute and distance information.
	 */
	private ActivityFacility sampleCommute(Context ctx, double dist, Coord refCoord, long ars) {

		STRtree index = trees.get("work");

		ActivityFacility workPlace = null;

		// Only larger distances can be commuters to other zones
		if (dist > 3000) {
			workPlace = commuter.selectTarget(ctx.rnd, ars, dist, MGC.coord2Point(refCoord), zone -> sampleZone(index, dist, refCoord, zone, ctx.rnd));
		}

		if (workPlace == null) {
			// Try selecting within same zone
			workPlace = sampleZone(index, dist, refCoord, (Geometry) zones.get(ars).getDefaultGeometry(), ctx.rnd);
		}

		return workPlace;
	}


	/**
	 * Sample a coordinate for which the associated link is not one of the ignored types.
	 */
	private Coord sampleLink(SplittableRandom rnd, double dist, Coord origin) {

		Coord coord = null;
		for (int i = 0; i < 500; i++) {
			coord = rndCoord(rnd, dist, origin);
			Link link = NetworkUtils.getNearestLink(network, coord);
			if (!IGNORED_LINK_TYPES.contains(NetworkUtils.getType(link)))
				break;
		}

		return coord;
	}

	/**
	 * Only samples randomly from the zone, ignoring the distance.
	 */
	private ActivityFacility sampleZone(STRtree index, double dist, Coord refCoord, Geometry zone, SplittableRandom rnd) {

		ActivityFacility location = null;

		List<ActivityFacility> query = index.query(MGC.coord2Point(refCoord).buffer(dist * 1.2).getEnvelopeInternal());

		query = query.stream().filter(f -> checkDistanceBound(dist, refCoord, f.getCoord(), 1)).collect(Collectors.toList());

		while (!query.isEmpty()) {
			ActivityFacility af = query.remove(rnd.nextInt(query.size()));
			if (zone.contains(MGC.coord2Point(af.getCoord()))) {
				location = af;
				break;
			}
		}

		return location;
	}

	/**
	 * General logic to filter coordinate within target distance.
	 */
	private boolean checkDistanceBound(double target, Coord refCoord, Coord other, double factor) {
		// Thresholds are asymmetric because of the direct distance factor
		double lower = target * 0.7 * (2 - factor);
		double upper = target * 1.1 * factor;

		double dist = CoordUtils.calcEuclideanDistance(refCoord, other);
		return dist >= lower && dist <= upper;
	}

	private Coord rndCoord(SplittableRandom rnd, double dist, Coord origin) {
		var angle = rnd.nextDouble() * Math.PI * 2;

		var x = Math.cos(angle) * dist;
		var y = Math.sin(angle) * dist;

		return new Coord(origin.getX() + x, origin.getY() + y);
	}

	private static final class Context {
		private final SplittableRandom rnd = new SplittableRandom(1);
	}

}
