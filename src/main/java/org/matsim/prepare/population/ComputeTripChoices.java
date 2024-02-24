package org.matsim.prepare.population;

import com.google.inject.Injector;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
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
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Option(names = "--trips", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path input;

	@CommandLine.Option(names = "--facilities", description = "Shp file with facilities", required = true)
	private Path facilities;

	@CommandLine.Option(names = "--modes", description = "Modes to include in choice set", split = ",", required = true)
	private List<String> modes;

	@CommandLine.Option(names = "--output", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path output;

	public static void main(String[] args) {
		new ComputeTripChoices().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!shp.isDefined()) {
			log.error("No shapefile defined. Please specify a shapefile for the zones using the --shp option.");
			return 2;
		}

		if (!Files.exists(input)) {
			log.error("Input file does not exist: " + input);
			return 2;
		}


		Config config = this.scenario.getConfig();
		config.controller().setOutputDirectory("choice-output");
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		Controler controler = this.scenario.createControler();

		// Run for one iteration to collect travel times
//		controler.run();

		Injector injector = controler.getInjector();

		Scenario scenario = injector.getInstance(Scenario.class);

		PlanBuilder builder = new PlanBuilder(shp, new ShpOptions(facilities, null, null),
			scenario.getPopulation().getFactory());

		PlanBuilder.addVehiclesToScenario(scenario);

		ThreadLocal<TripRouter> ctx = ThreadLocal.withInitial(() -> injector.getInstance(TripRouter.class));
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		List<CompletableFuture<List<Object>>> futures = new ArrayList<>();

		List<Person> persons = builder.createPlans(input);
		// Progress bar will be inaccurate
		ProgressBar pb = new ProgressBar("Computing choices",  persons.size() * 2L);

		SplittableRandom rnd = new SplittableRandom();

		for (Person person : persons) {

			Plan plan = person.getSelectedPlan();

			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {

				// Randomize departure times
				double departure = trip.getOriginActivity().getEndTime().seconds() + rnd.nextInt(-600, 600);

				futures.add(CompletableFuture.supplyAsync(() -> {
					TripRouter r = ctx.get();

					List<Object> entries = computeAlternatives(r, scenario.getNetwork(), person, trip, departure);
					pb.step();

					return entries;

				}, executor));

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

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {

			List<String> header = new ArrayList<>(List.of("p_id", "seq", "trip_n", "choice", "beelineDist"));

			for (String mode : modes) {
				header.add(mode + "_km");
				header.add(mode + "_hours");
				header.add(mode + "_walk_km");
				header.add(mode + "_valid");
			}

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
	private List<Object> computeAlternatives(TripRouter router, Network network, Person person, TripStructureUtils.Trip trip, double departure) {

		double beelineDist = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());

		Facility origin = FacilitiesUtils.wrapLinkAndCoord(NetworkUtils.getNearestLink(network, trip.getOriginActivity().getCoord()), trip.getOriginActivity().getCoord());
		Facility destination = FacilitiesUtils.wrapLinkAndCoord(NetworkUtils.getNearestLink(network, trip.getDestinationActivity().getCoord()), trip.getDestinationActivity().getCoord());

		String choice = trip.getLegsOnly().get(0).getMode();

		List<Object> row = new ArrayList<>(List.of(person.getId(), person.getAttributes().getAttribute("seq"),
			trip.getTripAttributes().getAttribute("n"), modes.indexOf(choice) + 1, beelineDist / 1000));

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

			// Filter rows that have been chosen, but would not be valid
			if (choice.equals(mode) && !valid) {
				return null;
			}

			row.addAll(List.of(travelDistance / 1000, travelTime / 3600, walkDistance / 1000, valid));
		}

		return row;
	}

}
