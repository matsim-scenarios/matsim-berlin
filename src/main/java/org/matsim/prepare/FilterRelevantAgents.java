package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.run.RunOpenBerlinScenario;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@CommandLine.Command(
		name = "filter-relevant-agents",
		description = "Filter agents that have any activities or routes within the shp file."
)
public class FilterRelevantAgents implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(FilterRelevantAgents.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--facilities", description = "Path to facilities file", required = true)
	private Path facilityPath;

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private Path networkPath;

	@CommandLine.Mixin
	private ShpOptions shp;

	private ActivityFacilities facilities;
	private Network network;
	private CoordinateTransformation ct;
	private Geometry geometry;
	private ThreadLocal<LeastCostPathCalculator> ctxs;

	private Set<Id<Person>> toRemove;

	public static void main(String[] args) {
		new FilterRelevantAgents().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file argument is required.");
			return 2;
		}

		Network completeNetwork = NetworkUtils.readNetwork(networkPath.toString());
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(completeNetwork);
		network = NetworkUtils.createNetwork();
		filter.filter(network, Set.of(TransportMode.car));

		geometry = shp.getGeometry();
		ct = shp.createTransformation(RunOpenBerlinScenario.CRS);

		facilities = FacilitiesUtils.createActivityFacilities();
		new MatsimFacilitiesReader(RunOpenBerlinScenario.CRS, RunOpenBerlinScenario.CRS, facilities)
				.readFile(facilityPath.toString());

		ctxs = ThreadLocal.withInitial(() -> this.createRouter(network));
		toRemove = ConcurrentHashMap.newKeySet();

		Population population = PopulationUtils.readPopulation(input.toString());

		ParallelPersonAlgorithmUtils.run(population, 8, this);

		log.info("Removing {} out of {} agents", toRemove.size(), population.getPersons().size());

		toRemove.forEach(population::removePerson);

		PopulationUtils.writePopulation(population, output.toString());

		log.info("Written {} agents to output", population.getPersons().size());

		return 0;
	}

	@Override
	public void run(Person person) {

		boolean keep = false;

		outer:
		for (Plan plan : person.getPlans()) {
			List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			// Check activities frist, which is faster
			for (Activity act : activities) {
				Point p = MGC.coord2Point(ct.transform(getCoordinate(act)));
				if (geometry.contains(p)) {
					keep = true;
					break outer;
				}
			}

			// If not sure yet, also do the routing
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {

				LeastCostPathCalculator lc = ctxs.get();

				Node from = NetworkUtils.getNearestNode(network, getCoordinate(trip.getOriginActivity()));
				Node to = NetworkUtils.getNearestNode(network, getCoordinate(trip.getDestinationActivity()));

				LeastCostPathCalculator.Path path = lc.calcLeastCostPath(from, to, 0, null, null);

				for (Node node : path.nodes) {
					if (geometry.contains(MGC.coord2Point(ct.transform(node.getCoord())))) {
						keep = true;
						break outer;
					}
				}
			}
		}

		if (!keep) {
			toRemove.add(person.getId());
		}
	}

	private Coord getCoordinate(Activity act) {
		Coord coord;
		// Determine coord of activity
		if (act.getCoord() != null)
			coord = act.getCoord();
		else {
			coord = facilities.getFacilities().get(act.getFacilityId()).getCoord();
		}

		return coord;
	}

	private LeastCostPathCalculator createRouter(Network network) {

		FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculatorFactory factory = new SpeedyALTFactory();

		OnlyTimeDependentTravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

		return factory.createPathCalculator(network, travelDisutility, travelTime);
	}
}
