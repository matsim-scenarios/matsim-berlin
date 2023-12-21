package org.matsim.prepare.opt;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.counts.*;
import org.matsim.prepare.RunOpenBerlinCalibration;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@CommandLine.Command(name = "run-count-opt", description = "Select plans to match counts data")
public class RunCountOptimization implements MATSimAppCommand {

	static final Logger log = LogManager.getLogger(RunCountOptimization.class);

	private static final int H = 24;

	@CommandLine.Option(names = "--input", description = "Path to input plans (Usually experienced plans).", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Output plan selection csv.", required = true)
	private Path output;

	@CommandLine.Option(names = "--network", description = "Path to network", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--counts", description = "Path to counts", required = true)
	private Path countsPath;

	@CommandLine.Option(names = "--network-mode", description = "Path to vehicle types", defaultValue = TransportMode.car)
	private String networkMode;

	@CommandLine.Option(names = "--all-car", description = "Plans have been created with the all car option and counts should be scaled. ", defaultValue = "false")
	private boolean allCar;

	@CommandLine.Option(names = "--metric")
	private ErrorMetric metric = ErrorMetric.abs_error;

	@CommandLine.Option(names = "--sample-size", defaultValue = "0.25")
	private double sampleSize;

	@CommandLine.Option(names = "--k", description = "Number of plans to use from each agent", defaultValue = "5")
	private int maxK;

	@CommandLine.Mixin
	private CsvOptions csv;

	private Object2IntMap<Id<Link>> linkMapping;

	private PlanAssignmentProblem problem;

	public static void main(String[] args) {
		new RunCountOptimization().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Counts<Link> linkCounts = new Counts<>();

		new MatsimCountsReader(linkCounts).readFile(countsPath.toString());

		Map<Id<Link>, MeasurementLocation<Link>> countStations = linkCounts.getMeasureLocations();

		int[] counts = new int[countStations.size() * H];

		linkMapping = new Object2IntLinkedOpenHashMap<>();

		int k = 0;
		for (MeasurementLocation<Link> station : countStations.values()){
			// hard coded to car calibration
			Measurable volumes = station.getVolumesForMode(networkMode);
			for (int i = 0; i < H; i++) {
				OptionalDouble v = volumes.getAtHour(i);
				if (v.isPresent()) {
					int idx = k * H + i;
					counts[idx] = (int) v.getAsDouble();
					if (allCar)
						counts[idx] = (int) (counts[idx] * RunOpenBerlinCalibration.CAR_FACTOR);
				}
			}

			linkMapping.put(station.getRefId(), k++);
		}

		Network network = NetworkUtils.readNetwork(networkPath.toString());

		List<PlanPerson> persons = processPopulation(input, network, linkCounts);

		problem = new PlanAssignmentProblem(maxK, metric, persons, counts);

		log.info("Collected {} relevant plans", persons.size());

		if (allCar)
			log.info("Scaled counts by car factor of {}", RunOpenBerlinCalibration.CAR_FACTOR);

		// Error scales are very different so different betas are needed
		double beta = switch (metric) {
			case abs_error -> 1;
			case log_error -> 100;
			case symmetric_percentage_error -> 300;
		};

		problem.iterate(5000, 0.5, beta, 0.01);

		PlanAssignmentProblem solution = solve(problem);

		try (CSVPrinter printer = csv.createPrinter(output)) {

			printer.printRecord("id", "idx");

			for (PlanPerson person : solution) {
				printer.printRecord(person.getId(), person.getK() - person.getOffset());
			}
		}

		return 0;
	}

	/**
	 * Create an array for each person.
	 */
	private List<PlanPerson> processPopulation(Path input, Network network, Counts<Link> linkCounts) {

		Population population = PopulationUtils.readPopulation(input.toString());
		List<PlanPerson> persons = new ArrayList<>();

		Set<Id<Link>> links = linkCounts.getMeasureLocations().keySet();

		SplittableRandom rnd = new SplittableRandom(0);

		for (Person person : population.getPersons().values()) {

			int scale = (int) (1 / sampleSize);

			Int2IntMap[] plans = new Int2IntMap[maxK];
			for (int i = 0; i < plans.length; i++) {
				plans[i] = new Int2IntOpenHashMap();
			}

			boolean keep = false;

			int offset = 0;
			// Commercial traffic, which can be chosen to not be included at all
			if (person.getId().toString().startsWith("commercialPersonTraffic")) {
				// if other trips have been scaled, these unscaled trips are scaled as well
				if (allCar)
					// scale with mean of CAR_FACTOR
					scale += (rnd.nextDouble() < 0.85 ? 5: 4);
			}

			// Index for plan
			int k = offset;
			for (Plan plan : person.getPlans()) {

				if (k >= maxK)
					break;

				for (PlanElement el : plan.getPlanElements()) {
					if (el instanceof Leg leg) {

						// TODO: other leg modes
						if (!leg.getMode().equals(TransportMode.car))
							continue;

						// TODO: scale travel time with factor from the leg

						if (leg.getRoute() instanceof NetworkRoute route) {
							boolean relevant = route.getLinkIds().stream().anyMatch(links::contains);

							double time = leg.getDepartureTime().seconds();

							if (relevant) {
								keep = true;
								for (Id<Link> linkId : route.getLinkIds()) {

									Link link = network.getLinks().get(linkId);

									// Assume free speed travel time
									time += link.getLength() / link.getFreespeed() + 1;

									if (linkMapping.containsKey(linkId)) {
										int idx = linkMapping.getInt(linkId);
										int hour = (int) Math.floor(time / 3600);
										if (hour >= H)
											continue;

										plans[k].merge(idx * H + hour, scale, Integer::sum);
									}
								}
							}
						}
					}
				}
				k++;
			}

			if (keep) {
				for (int i = 0; i < plans.length; i++) {
					if (plans[i].isEmpty())
						plans[i] = PlanPerson.NOOP_PLAN;
				}

				persons.add(new PlanPerson(person.getId(), offset, plans));
			}
		}

		return persons;
	}

	private PlanAssignmentProblem solve(PlanAssignmentProblem problem) {

		// Loading fails if xerces is on the classpath

		SolverFactory<PlanAssignmentProblem> factory = SolverFactory.createFromXmlResource("solver.xml");

		Solver<PlanAssignmentProblem> solver = factory.buildSolver();

		AtomicLong ts = new AtomicLong(System.currentTimeMillis());

		solver.addEventListener(event -> {

			// Only log every x seconds
			if (ts.get() + 60_000 < System.currentTimeMillis()) {
				log.info("New best solution: {}", event.getNewBestScore());
				ts.set(System.currentTimeMillis());
			}
		});

		return solver.solve(problem);
	}
}
