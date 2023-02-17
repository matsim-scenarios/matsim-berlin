package org.matsim.synthetic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.matsim.run.RunOpenBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "init-location-choice",
		description = "Assign initial locations to agents"
)
@SuppressWarnings("unchecked")
public class InitLocationChoice implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(InitLocationChoice.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file", required = true)
	private Path facilityPath;

	@CommandLine.Mixin
	private ShpOptions shp;

	private Map<String, STRtree> trees;

	private Map<Long, SimpleFeature> zones;

	private ThreadLocal<Context> ctxs;

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

		zones = shp.readFeatures().stream()
				.collect(Collectors.toMap(ft -> Long.parseLong((String) ft.getAttribute("ARS")), ft -> ft));

		Population population = PopulationUtils.readPopulation(input.toString());

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
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

		ParallelPersonAlgorithmUtils.run(population, 8, this);

		PopulationUtils.writePopulation(population, output.toString());


		return 0;
	}

	@Override
	public void run(Person person) {

		Coord homeCoord = Attributes.getHomeCoord(person);

		for (Plan plan : person.getPlans()) {
			List<Activity> acts = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			Coord lastCoord = homeCoord;

			for (Activity act : acts) {

				String type = act.getType();

				if (Attributes.isLinkUnassigned(act.getLinkId())) {

					act.setLinkId(null);
					ActivityFacility location = null;

					// TODO: unemployed persons have WORK activities
					// bug in actitopp?

					if (type.equals("work") && PersonUtils.isEmployed(person)) {


						// Select work facility
						if (person.getAttributes().getAttribute(Attributes.ARS) == person.getAttributes().getAttribute(Attributes.COMMUTE)) {
							location = sampleDistAndZone(homeCoord,
									trees.get(type),
									(Geometry) zones.get((long) person.getAttributes().getAttribute(Attributes.COMMUTE)).getDefaultGeometry(),
									(double) person.getAttributes().getAttribute(Attributes.COMMUTE_KM),
									ctxs.get().rnd
							);

							// Filter brandenburg zones
						} else
							location = sampleZone(
									trees.get(type),
									(Geometry) zones.get((long) person.getAttributes().getAttribute(Attributes.COMMUTE)).getDefaultGeometry(),
									ctxs.get().rnd
							);

						// Location can be outside network area / outside available facilities
						// these people will work from home
						if (location == null) {
							act.setCoord(homeCoord);
							continue;
						}
					}

					if (location == null) {

						for (int i = 0; i < 15 && location == null; i++) {
							List<ActivityFacility> query = trees.get(type).query(MGC.coord2Point(lastCoord).buffer(3000 + 1000 * i).getEnvelopeInternal());
							if (!query.isEmpty()) {
								location = query.get(ctxs.get().rnd.nextInt(query.size()));
							}
						}
					}

					lastCoord = location.getCoord();
					act.setFacilityId(location.getId());
				}
			}
		}
	}

	/**
	 * Sample facility within distance from coordinate, that is located in the desired target zone.
	 */
	private ActivityFacility sampleDistAndZone(Coord coord, STRtree index, Geometry zone, double dist, SplittableRandom rnd) {

		ActivityFacility location = null;
		for (int i = 0; i < 500 && location == null; i++) {

			// TODO: full degree angle can be very inefficient for sampling far away zones
			var angle = rnd.nextDouble() * Math.PI * 2;

			var x = Math.cos(angle) * dist;
			var y = Math.sin(angle) * dist;

			Coord c = new Coord(coord.getX() + x, coord.getY() + y);

			List<ActivityFacility> query = index.query(MGC.coord2Point(c).buffer(3000).getEnvelopeInternal());
			while (!query.isEmpty()) {
				ActivityFacility af = query.remove(rnd.nextInt(query.size()));
				if (zone.contains(MGC.coord2Point(af.getCoord()))) {
					location = af;
					break;
				}
			}
		}

		return location;
	}

	/**
	 * Only samples randomly from the zone, ignoring the distance.
	 */
	private ActivityFacility sampleZone(STRtree index, Geometry zone, SplittableRandom rnd) {

		ActivityFacility location = null;

		List<ActivityFacility> query = index.query(zone.getBoundary().getEnvelopeInternal());
		while (!query.isEmpty()) {
			ActivityFacility af = query.remove(rnd.nextInt(query.size()));
			if (zone.contains(MGC.coord2Point(af.getCoord()))) {
				location = af;
				break;
			}
		}


		return location;
	}

	private static final class Context {
		private final SplittableRandom rnd = new SplittableRandom(1);

	}

}
