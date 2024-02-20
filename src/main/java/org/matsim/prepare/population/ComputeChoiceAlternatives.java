package org.matsim.prepare.population;

import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.FacilityWrapperActivity;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import picocli.CommandLine;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


@CommandLine.Command(
	name = "compute-choice-alternatives",
	description = "Computes all choices and metrics for a dataset of trips."
)
public class ComputeChoiceAlternatives implements MATSimAppCommand {

	// TODO: move whole class to contrib when done
	private static final Logger log = LogManager.getLogger(ComputeChoiceAlternatives.class);

	@CommandLine.Mixin
	private ScenarioOptions scenario;

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Option(names = "--trips", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path input;

	@CommandLine.Option(names = "--modes", description = "Modes to include in choice set", split = ",", required = true)
	private Set<String> modes;

	@CommandLine.Option(names = "--output", description = "Input trips from survey data, in matsim-python-tools format.")
	private Path output;

	private SplittableRandom rnd = new SplittableRandom();

	public static void main(String[] args) {
		new ComputeChoiceAlternatives().execute(args);
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
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		Controler controler = this.scenario.createControler();

		// Run for one iteration
		// controler.run();

		Injector injector = controler.getInjector();
		TripRouter router = injector.getInstance(TripRouter.class);

		Table trips = Table.read().csv(input.toFile());

		List<Entry> results = new ArrayList<>();

		for (Row row : trips) {
			results.addAll(computeAlternatives(router, row));
		}

		return 0;
	}

	/**
	 * Compute all alternatives for a given trip.
	 */
	private Collection<Entry> computeAlternatives(TripRouter router, Row row) {

		for (String mode : modes) {

			Facility from = FacilitiesUtils.wrapLink(null);
			Facility to = FacilitiesUtils.wrapLink(null);

			router.calcRoute(mode, from, to, 0, null, null);

		}

		// TODO

		return null;
	}


	/**
	 * One row in the output results.
	 */
	private record Entry(String mode, double travelDistance, double beelineDistance, double traveledTime, double waitTime, int ptSwitches, double walkDistance) {

	}

}
