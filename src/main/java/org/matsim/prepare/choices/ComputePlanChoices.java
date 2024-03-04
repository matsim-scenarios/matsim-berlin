package org.matsim.prepare.choices;

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
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import picocli.CommandLine;

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
	private final MainModeIdentifier mmi = new DefaultAnalysisMainModeIdentifier();
	@CommandLine.Mixin
	private ScenarioOptions scenario;
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Option(names = "--trips", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path input;
	@CommandLine.Option(names = "--facilities", description = "Shp file with facilities", required = true)
	private Path facilities;
	@CommandLine.Option(names = "--top-k", description = "Use top k estimates", defaultValue = "9")
	private int topK;
	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", split = ",")
	private Set<String> modes;
	@CommandLine.Option(names = "--output", description = "Input trips from survey data, in matsim-python-tools format.", required = true)
	private Path output;
	private ThreadLocal<Ctx> thread;
	private ProgressBar pb;

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
			.withConstraint(RelaxedMassConservationConstraint.class)
			.withActivityEstimator(DefaultActivityEstimator.class)
			.build());

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(topK);
		imc.setModes(modes);
		imc.setConstraintCheck(InformedModeChoiceConfigGroup.ConstraintCheck.none);

		controler.run();

		Injector injector = controler.getInjector();

		PlanBuilder.addVehiclesToScenario(injector.getInstance(Scenario.class));

		Population population = PopulationUtils.createPopulation(config);

		PlanBuilder builder = new PlanBuilder(shp, new ShpOptions(facilities, null, null), population.getFactory());

		builder.createPlans(input).forEach(population::addPerson);

		thread = ThreadLocal.withInitial(() ->
			new Ctx(
				new PlanRouter(injector.getInstance(TripRouter.class),
					TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration,
						PlansConfigGroup.TripDurationHandling.ignoreDelays)),
				new DiversePlanCandidateGenerator(topK, injector.getInstance(TopKChoicesGenerator.class)),
				new PseudoScorer(injector, population)
			)
		);


		pb = new ProgressBar("Computing plan choices", population.getPersons().size());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors() - 1, this);

		pb.close();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {

			// header
			List<Object> header = new ArrayList<>();
			header.add("person");
			header.add("choice");

			for (int i = 1; i <= topK; i++) {

				for (String mode : modes) {
					header.add(String.format("plan_%d_%s_usage", i, mode));
					header.add(String.format("plan_%d_%s_km", i, mode));
					header.add(String.format("plan_%d_%s_hours", i, mode));
					header.add(String.format("plan_%d_%s_ride_hours", i, mode));
					header.add(String.format("plan_%d_%s_n_switches", i, mode));
				}

				header.add(String.format("plan_%d_act_util", i));
				header.add(String.format("plan_%d_valid", i));
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

		List<Object> row = new ArrayList<>();

		row.add(person.getId());
		// choice, always the first one
		row.add(1);

		List<PlanCandidate> candidates = ctx.generator.generate(model);

		int i = 0;
		for (PlanCandidate candidate : candidates) {

			if (i >= topK)
				break;

			// TODO: apply method might also shift times to better fit the schedule
			candidate.applyTo(plan);
			ctx.router.run(plan);

			row.addAll(convert(plan, ctx.scorer));
			// available choice
			row.add(1);
			i++;
		}

		for (int j = i; j < topK; j++) {
			row.addAll(convert(null, ctx.scorer));
			// not available
			row.add(0);
		}

		// Clean up routes, which use a lot of memory
		plan.getPlanElements().forEach(el -> {
			if (el instanceof Leg leg) {
				leg.setRoute(null);
			}
		});

		model.reset();

		rows.add(row);

		pb.step();
	}

	/**
	 * Create one csv entry row for a plan.
	 */
	private List<Object> convert(@Nullable Plan plan, PseudoScorer scorer) {

		List<Object> row = new ArrayList<>();
		if (plan == null) {
			for (String ignored : modes) {
				row.addAll(List.of(0, 0, 0, 0, 0));
			}

			row.add(0);

			return row;
		}

		Map<String, ModeStats> stats = collect(plan);


		for (String mode : modes) {
			ModeStats modeStats = stats.get(mode);
			row.add(modeStats.usage);
			row.add(modeStats.travelDistance / 1000);
			row.add(modeStats.travelTime / 3600);
			row.add(modeStats.rideTime / 3600);
			row.add(modeStats.numSwitches);
		}

		// pseudo scorer commented out currently
//		Object2DoubleMap<String> scores = scorer.score(plan);
//		row.add(scores.getDouble("score"));
		row.add(0);


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
			double rideTime = 0;
			double travelDistance = 0;
			long switches = 0;

			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
				List<Leg> legs = trip.getLegsOnly();
				String mainMode = mmi.identifyMainMode(legs);
				if (mode.equals(mainMode)) {
					usage++;
					travelTime += legs.stream().mapToDouble(l -> l.getRoute().getTravelTime().seconds()).sum();
					travelDistance += legs.stream().mapToDouble(l -> l.getRoute().getDistance()).sum();
					rideTime += legs.stream().filter(l -> l.getMode().equals(mode))
						.mapToDouble(l -> l.getRoute().getTravelTime().seconds()).sum();

					// This is mainly used for PT, to count the number of switches
					switches += legs.stream().filter(l -> l.getMode().equals(mode)).count() - 1;
				}
			}

			stats.put(mode, new ModeStats(usage, travelTime, travelDistance, rideTime, switches));
		}

		return stats;
	}

	private record ModeStats(int usage, double travelTime, double travelDistance, double rideTime, long numSwitches) {
	}

	private record Ctx(PlanRouter router, DiversePlanCandidateGenerator generator, PseudoScorer scorer) {
	}
}
