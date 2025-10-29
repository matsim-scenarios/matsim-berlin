package org.matsim.prepare.choices;

import com.google.inject.Injector;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.prepare.population.Attributes;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@CommandLine.Command(
	name = "compute-trip-choices",
	description = "Computes all choices and metrics for a dataset of trips."
)
public class ComputeTripChoices implements MATSimAppCommand {

	// TODO: move whole class to contrib when done, can probably go into imc
	private static final Logger log = LogManager.getLogger(ComputeTripChoices.class);

	@CommandLine.Mixin
	private ScenarioOptions scenario;

	@CommandLine.Option(names = "--modes", description = "Modes to include in choice set", split = ",", required = true)
	private List<String> modes;

	@CommandLine.Option(names = "--output", description = "Path to output csv..", defaultValue = "trip-choices.csv")
	private Path output;

	@CommandLine.Option(names = "--max-plan-length", description = "Maximum plan length", defaultValue = "7")
	private int maxPlanLength;

	private double globalAvgIncome;

	public static void main(String[] args) {
		new ComputeTripChoices().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = this.scenario.getConfig();
		config.controller().setOutputDirectory("choice-output");
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		sw.setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);

		Controler controler = this.scenario.createControler();

		// Run for one iteration to collect travel times
		controler.run();

		Injector injector = controler.getInjector();

		Scenario scenario = injector.getInstance(Scenario.class);

		ThreadLocal<TripRouter> ctx = ThreadLocal.withInitial(() -> injector.getInstance(TripRouter.class));
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		List<CompletableFuture<List<Object>>> futures = new ArrayList<>();

		Collection<? extends Person> persons = scenario.getPopulation().getPersons().values();

		globalAvgIncome = persons.stream()
			.map(PersonUtils::getIncome)
			.filter(Objects::nonNull)
			.mapToDouble(d -> d)
			.filter(dd -> dd > 0)
			.average()
			.orElse(Double.NaN);

		// Progress bar will be inaccurate
		ProgressBar pb = new ProgressBar("Computing choices", persons.size() * 3L);

		for (Person person : persons) {

			if (person.getAttributes().getAttribute(Attributes.REF_MODES) == null) {
				continue;
			}

			Plan plan = person.getSelectedPlan();

			int seq = 0;
			TimeTracker tt = new TimeTracker(TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration, PlansConfigGroup.TripDurationHandling.ignoreDelays));

			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);

			if (trips.size() > maxPlanLength) {
				continue;
			}

			for (TripStructureUtils.Trip trip : trips) {

				tt.addActivity(trip.getOriginActivity());

				double departure = tt.getTime().orElseThrow(() -> new IllegalStateException("No departure time for trip"));
				int n = seq++;

				futures.add(CompletableFuture.supplyAsync(() -> {
					TripRouter r = ctx.get();

					List<Object> entries = computeAlternatives(r, scenario.getNetwork(), person, trip, departure, n);
					pb.step();

					return entries;

				}, executor));

				tt.addElements(trip.getLegsOnly());
			}
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
			.exceptionally(ex -> {
				log.error("Error while computing trip choices", ex);
				return null;
			})
			.join();

		executor.shutdown();
		pb.close();

		log.info("Writing {} trip choices to {}", futures.size(), output);

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT.builder().setCommentMarker('#').build())) {

			List<String> header = new ArrayList<>(List.of("person", "weight", "income", "util_money", "trip_n", "choice", "beelineDist"));
			for (String mode : modes) {
				header.add(mode + "_km");
				header.add(mode + "_hours");
				header.add(mode + "_walking_km");
				header.add(mode + "_switches");
				header.add(mode + "_valid");
			}

			csv.printComment("Average global income: " + globalAvgIncome);
			csv.printRecord(header);

			for (CompletableFuture<List<Object>> f : futures) {

				List<Object> entries = f.get();
				if (entries != null) {
					csv.printRecord(entries);
				}
			}
		}

		return 0;
	}

	/**
	 * Compute all alternatives for a given trip.
	 */
	private List<Object> computeAlternatives(TripRouter router, Network network, Person person, TripStructureUtils.Trip trip, double departure, int seq) {

		double beelineDist = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());

		Facility origin = FacilitiesUtils.wrapLinkAndCoord(NetworkUtils.getNearestLink(network, trip.getOriginActivity().getCoord()), trip.getOriginActivity().getCoord());
		Facility destination = FacilitiesUtils.wrapLinkAndCoord(NetworkUtils.getNearestLink(network, trip.getDestinationActivity().getCoord()), trip.getDestinationActivity().getCoord());

		String[] choices = ((String) person.getAttributes().getAttribute(TripAnalysis.ATTR_REF_MODES)).split("-");
		String choice = choices[seq];

		List<Object> row = new ArrayList<>(List.of(
			person.getAttributes().getAttribute(TripAnalysis.ATTR_REF_ID),
			person.getAttributes().getAttribute(TripAnalysis.ATTR_REF_WEIGHT),
			PersonUtils.getIncome(person),
			globalAvgIncome / PersonUtils.getIncome(person),
			seq,
			modes.indexOf(choice) + 1,
			beelineDist / 1000)
		);

		for (String mode : modes) {

			double travelTime = 0;
			double travelDistance = 0;
			double walkDistance = 0;
			boolean valid = false;

			List<? extends PlanElement> route = router.calcRoute(mode, origin, destination, departure, person, new AttributesImpl());
			for (PlanElement el : route) {

				if (el instanceof Leg leg) {

					travelTime += leg.getTravelTime().seconds();
					travelDistance += leg.getRoute().getDistance();

					if (leg.getMode().equals(TransportMode.walk)) {
						walkDistance += leg.getRoute().getDistance();
					}

					if (leg.getMode().equals(mode))
						valid = true;
				}
			}

			// This is mainly used for PT, to count the number of switches
			long switches = route.stream().filter(r -> r instanceof Leg l && l.getMode().equals(mode)).count() - 1;

			if (!PersonUtils.canUseCar(person) && mode.equals(TransportMode.car)) {
				valid = false;
			}

			// Filter rows that have been chosen, but would not be valid
			if (choice.equals(mode) && !valid) {
				return null;
			}

			row.addAll(List.of(travelDistance / 1000, travelTime / 3600, walkDistance / 1000, switches, valid));
		}

		return row;
	}

}
