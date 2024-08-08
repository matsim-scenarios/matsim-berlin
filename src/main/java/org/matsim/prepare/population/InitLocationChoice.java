package org.matsim.prepare.population;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.facilities.ActivityFacility;
import org.matsim.prepare.RunOpenBerlinCalibration;
import org.matsim.prepare.facilities.AttributedActivityFacility;
import picocli.CommandLine;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.matsim.prepare.facilities.CreateMATSimFacilities.IGNORED_LINK_TYPES;

@CommandLine.Command(
	name = "init-location-choice",
	description = "Assign initial locations to agents"
)
@SuppressWarnings("unchecked")
public class InitLocationChoice implements MATSimAppCommand, PersonAlgorithm {

	/**
	 * Detour factor for routes > 3000m. Factor is based on data, but adjusted to better match distance distribution.
	 */
	private static final double DETOUR_FACTOR = 1.25;

	/**
	 * Factor for short trips < 3000m. Factor was calculated based on data.
	 */
	private static final double DETOUR_FACTOR_SHORT = 1.3;

	private static final Logger log = LogManager.getLogger(InitLocationChoice.class);

	@CommandLine.Option(names = "--input", description = "Path to input population.")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--k", description = "Number of choices to generate", defaultValue = "5")
	private int k;

	@CommandLine.Option(names = "--commuter", description = "Path to commuter.csv", required = true)
	private Path commuterPath;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file", required = true)
	private Path facilityPath;

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--sample", description = "Sample size of the population", defaultValue = "0.25")
	private double sample;

	@CommandLine.Option(names = "--seed", description = "Seed used to sample locations", defaultValue = "1")
	private long seed;

	@CommandLine.Mixin
	private ShpOptions shp;

	private FacilityIndex facilities;

	private Long2ObjectMap<SimpleFeature> zones;

	private CommuterAssignment commuter;

	private Network network;

	private AtomicLong total = new AtomicLong();

	private AtomicLong warning = new AtomicLong();

	private ProgressBar pb;

	public static void main(String[] args) {
		new InitLocationChoice().execute(args);
	}

	/**
	 * Approximate beeline dist from known traveled distance. Distance will be reduced by a fixed detour factor.
	 *
	 * @param travelDist distance in km
	 * @return beeline distance in meters
	 */
	public static double beelineDist(double travelDist) {
		double detourFactor = travelDist <= 3 ? DETOUR_FACTOR_SHORT : DETOUR_FACTOR;
		return travelDist * 1000 / detourFactor;
	}

	private static Coord rndCoord(SplittableRandom rnd, double dist, Coord origin) {
		double angle = rnd.nextDouble() * Math.PI * 2;

		double x = Math.cos(angle) * dist;
		double y = Math.sin(angle) * dist;

		return new Coord(RunOpenBerlinCalibration.roundNumber(origin.getX() + x), RunOpenBerlinCalibration.roundNumber(origin.getY() + y));
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file with commuter zones is required.");
			return 2;
		}

		Network completeNetwork = NetworkUtils.readNetwork(networkPath.toString());
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(completeNetwork);
		network = NetworkUtils.createNetwork();
		filter.filter(network, Set.of(TransportMode.car));

		facilities = new FacilityIndex(facilityPath.toString());

		zones = new Long2ObjectOpenHashMap<>(shp.readFeatures().stream()
			.collect(Collectors.toMap(ft -> Long.parseLong((String) ft.getAttribute("ARS")), ft -> ft)));

		log.info("Read {} zones", zones.size());

		log.info("Using input file: {}", input);

		List<Population> populations = new ArrayList<>();

		for (int i = 0; i < k; i++) {

			log.info("Generating plan {} with seed {}", i, seed);

			commuter = new CommuterAssignment(zones, commuterPath, sample);

			Population population = PopulationUtils.readPopulation(input.toString());

			pb = new ProgressBar("Performing location choice " + i, population.getPersons().size());

			ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors() - 1, this);

			populations.add(population);

			log.info("Processed {} activities with {} warnings", total.get(), warning.get());

			total.set(0);
			warning.set(0);
			seed += i;
		}

		Population population = populations.get(0);

		// Merge all plans into the first population
		for (int i = 1; i < populations.size(); i++) {

			Population pop = populations.get(i);

			for (Person p : pop.getPersons().values()) {
				Person destPerson = population.getPersons().get(p.getId());
				if (destPerson == null) {
					log.warn("Person {} not present in all populations.", p.getId());
					continue;
				}

				destPerson.addPlan(p.getPlans().get(0));
			}
		}

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {

		Coord homeCoord = Attributes.getHomeCoord(person);

		// Reference persons are not assigned locations
		if (person.getAttributes().getAttribute(Attributes.REF_MODES) != null) {
			pb.step();
			return;
		}

		// Activities that only occur on one place per person
		Map<String, ActivityFacility> fixedLocations = new HashMap<>();

		int planNumber = 0;
		for (Plan plan : person.getPlans()) {
			List<Activity> acts = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			// keep track of the current coordinate
			Coord lastCoord = homeCoord;

			// Person specific rng, increment plan number for each plan
			SplittableRandom rnd = initRandomNumberGenerator(person, planNumber++);

			for (Activity act : acts) {

				total.incrementAndGet();

				if (Attributes.isLinkUnassigned(act.getLinkId())) {

					String type = act.getType();

					act.setLinkId(null);
					ActivityFacility location = null;

					// target leg distance in km
					double origDist = (double) act.getAttributes().getAttribute("orig_dist");

					// Distance will be reduced
					double dist = beelineDist(origDist);

					if (fixedLocations.containsKey(type)) {
						location = fixedLocations.get(type);
					}

					if (location == null && type.equals("work")) {
						// sample work commute
						location = sampleCommute(rnd, dist, lastCoord, (long) person.getAttributes().getAttribute(Attributes.ARS));
					}

					if (location == null && facilities.index.containsKey(type)) {
						// Needed for lambda
						final Coord refCoord = lastCoord;

						List<AttributedActivityFacility> query = facilities.index.get(type).query(MGC.coord2Point(lastCoord).buffer(dist * 1.2).getEnvelopeInternal());

						// Distance should be within the bounds
						List<AttributedActivityFacility> res = query.stream().filter(f -> checkDistanceBound(dist, refCoord, f.getCoord(), 1)).toList();

						if (!res.isEmpty()) {
							location = query.get(FacilityIndex.sampleByWeight(query, AttributedActivityFacility::getOtherAttraction, rnd));
						}

						// Try with larger bounds again
						if (location == null) {
							res = query.stream().filter(f -> checkDistanceBound(dist, refCoord, f.getCoord(), 1.2)).toList();
							if (!res.isEmpty()) {
								location = query.get(FacilityIndex.sampleByWeight(query, AttributedActivityFacility::getOtherAttraction, rnd));
							}
						}
					}

					if (location == null) {
						// sample only coordinate if nothing else is possible
						// Activities without facility entry, or where no facility could be found
						Coord c = sampleLink(rnd, dist, lastCoord);
						act.setCoord(c);
						lastCoord = c;

						// An activity with type could not be put into correct facility.
						if (facilities.index.containsKey(type)) {
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
					lastCoord = facilities.all.getFacilities().get(act.getFacilityId()).getCoord();

			}
		}

		pb.step();
	}

	/**
	 * Initializes random number generator with person specific seed.
	 */
	private SplittableRandom initRandomNumberGenerator(Person person, long planNumber) {
		BigInteger i = new BigInteger(person.getId().toString().getBytes());
		return new SplittableRandom(i.longValue() + seed * 1000 + planNumber * 10);
	}

	/**
	 * Sample work place by using commute and distance information.
	 */
	private ActivityFacility sampleCommute(SplittableRandom rnd, double dist, Coord refCoord, long ars) {

		STRtree index = facilities.index.get("work");

		ActivityFacility workPlace = null;

		// Only larger distances can be commuters to other zones
		if (dist > 3000) {
			workPlace = commuter.selectTarget(rnd, ars, dist, MGC.coord2Point(refCoord), zone -> sampleZone(index, dist, refCoord, zone, rnd));
		}

		if (workPlace == null) {
			// Try selecting within same zone
			workPlace = sampleZone(index, dist, refCoord, (Geometry) zones.get(ars).getDefaultGeometry(), rnd);
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

		List<AttributedActivityFacility> query = index.query(MGC.coord2Point(refCoord).buffer(dist * 1.2).getEnvelopeInternal());

		query = query.stream().filter(f -> checkDistanceBound(dist, refCoord, f.getCoord(), 1)).collect(Collectors.toList());

		return FacilityIndex.sampleByWeightWithRejection(query, f -> zone.contains(MGC.coord2Point(f.getCoord())), AttributedActivityFacility::getWorkAttraction, rnd);
	}

	/**
	 * General logic to filter coordinate within target distance.
	 */
	private boolean checkDistanceBound(double target, Coord refCoord, Coord other, double factor) {
		double lower = target * 0.8 * (2 - factor);
		double upper = target * 1.15 * factor;

		double dist = CoordUtils.calcEuclideanDistance(refCoord, other);
		return dist >= lower && dist <= upper;
	}

}
