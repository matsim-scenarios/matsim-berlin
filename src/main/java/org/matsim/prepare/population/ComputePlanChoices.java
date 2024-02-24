package org.matsim.prepare.population;

import com.google.inject.Injector;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.router.*;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.modechoice.*;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import picocli.CommandLine;
import scala.util.parsing.combinator.testing.Str;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


@CommandLine.Command(
	name = "compute-plan-choices",
	description = "Computes multiple plan choices for a whole day."
)
public class ComputePlanChoices implements MATSimAppCommand, PersonAlgorithm {

	// TODO: move whole class to contrib when done, can probably go into imc
	private static final Logger log = LogManager.getLogger(ComputePlanChoices.class);
	/**
	 * Rows for the result table.
	 */
	private final Queue<List<Object>> rows = new ConcurrentLinkedQueue<>();
	@CommandLine.Mixin
	private ScenarioOptions scenario;
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Option(names = "--trips", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path input;
	@CommandLine.Option(names = "--facilities", description = "Shp file with facilities", required = true)
	private Path facilities;
	@CommandLine.Option(names = "--top-k", description = "Use top k estimates", defaultValue = "10")
	private int topK;
	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", split = ",")
	private Set<String> modes;
	@CommandLine.Option(names = "--output", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path output;
	private ThreadLocal<Ctx> thread;
	private ProgressBar pb;
	private MainModeIdentifier mmi = new DefaultAnalysisMainModeIdentifier();


	public static void main(String[] args) {
		new ComputePlanChoices().execute(args);
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


		controler.addOverridingModule(InformedModeChoiceModule.newBuilder()
			.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
			.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk", "pt", "ride")
			.build());

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(topK);

		Injector injector = controler.getInjector();

		PlanBuilder.addVehiclesToScenario(injector.getInstance(Scenario.class));

		thread = ThreadLocal.withInitial(() ->
			new Ctx(
				new PlanRouter(injector.getInstance(TripRouter.class),
					TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration,
						PlansConfigGroup.TripDurationHandling.ignoreDelays)),
				injector.getInstance(TopKChoicesGenerator.class))
		);

		Population population = PopulationUtils.createPopulation(config);

		PlanBuilder builder = new PlanBuilder(shp, new ShpOptions(facilities, null, null), population.getFactory());

		builder.createPlans(input).forEach(population::addPerson);

		pb = new ProgressBar("Computing plan choices", population.getPersons().size());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		pb.close();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {

			// header
			List<Object> header = new ArrayList<>();
			header.add("person");
			header.add("choice");

			for (int i = 0; i < topK; i++) {

				for (String mode : modes) {
					header.add(String.format("plan_%d_%s_usage", i, mode));
					header.add(String.format("plan_%d_%s_km", i, mode));
					header.add(String.format("plan_%d_%s_hour", i, mode));
				}
				header.add(String.format("plan_%d_available", i));

			}

			csv.printRecord(header);

            for (List<Object> row : rows) {
                csv.printRecord(row);
            }
        }


		return 0;
	}


	@Override
	public void run(Person person) {

		Plan plan = person.getSelectedPlan();
		PlanModel model = PlanModel.newInstance(plan);

		Ctx ctx = thread.get();

		List<String[]> chosen = new ArrayList<>();
		chosen.add(model.getCurrentModes());

		// Chosen candidate from data
		PlanCandidate existing = ctx.generator.generatePredefined(model, chosen).get(0);

		List<Object> row = new ArrayList<>();

		row.add(person.getId());
		// choice, always the first one
		row.add(0);

		existing.applyTo(plan);
		ctx.router.run(plan);

		row.addAll(convert(plan));

		// top k candidates
		List<PlanCandidate> candidates = ctx.generator.generate(model);

		int i = 1;
		for (PlanCandidate candidate : candidates) {

			// Skip if the same as the existing plan
			if (Arrays.equals(candidate.getModes(), model.getCurrentModes()))
				continue;

			candidate.applyTo(plan);
			ctx.router.run(plan);
			row.addAll(convert(plan));
			// available choice
			row.add(1);
		}

		for (int j = i; j < topK; j++) {
			row.addAll(convert(null));
			// not available
			row.add(0);
		}

		rows.add(row);

		pb.step();
	}

	/**
	 * Create one csv entry row for a plan.
	 */
	private List<Object> convert(@Nullable Plan plan) {

		List<Object> row = new ArrayList<>();
		if (plan == null) {
			for (String ignored : modes) {
				row.add(0);
				row.add(0);
				row.add(0);
			}

			return row;
		}

		Map<String, ModeStats> stats = collect(plan);

		for (String mode : modes) {
			ModeStats modeStats = stats.get(mode);
			row.add(modeStats.usage);
			row.add(modeStats.travelDistance / 1000);
			row.add(modeStats.travelTime / 3600);
		}


		return row;
	}

	/**
	 * Collect aggregated mode stats.
	 */
	private Map<String, ModeStats> collect(Plan plan) {

		Map<String, ModeStats> stats = new HashMap<>();

		for (String mode : modes) {

			int usage = 0;
			double travelTime = 0;
			double travelDistance = 0;
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
				List<Leg> legs = trip.getLegsOnly();
				String mainMode = mmi.identifyMainMode(legs);
				if (mode.equals(mainMode)) {
					usage++;
					travelTime += legs.stream().mapToDouble(l -> l.getRoute().getTravelTime().seconds()).sum();
					travelDistance += legs.stream().mapToDouble(l -> l.getRoute().getDistance()).sum();
				}
			}

			stats.put(mode, new ModeStats(usage, travelTime, travelDistance));
		}

		return stats;
	}

	private record ModeStats(int usage, double travelTime, double travelDistance) {
	}

	private record Ctx(PlanRouter router, TopKChoicesGenerator generator) {
	}
}
