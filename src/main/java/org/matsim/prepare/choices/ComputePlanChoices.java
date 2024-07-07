package org.matsim.prepare.choices;

import com.google.inject.Injector;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.router.*;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import org.matsim.prepare.population.Attributes;
import org.matsim.simwrapper.SimWrapperConfigGroup;
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
	@CommandLine.Mixin
	private ScenarioOptions scenario;
	@CommandLine.Option(names = "--top-k", description = "Use top k estimates", defaultValue = "9")
	private int topK;
	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", split = ",")
	private Set<String> modes;
	@CommandLine.Option(names = "--time-util-only", description = "Reset scoring for estimation and only use time utility", defaultValue = "false")
	private boolean timeUtil;
	@CommandLine.Option(names = "--calc-scores", description = "Perform pseudo scoring for each plan", defaultValue = "false")
	private boolean calcScores;
	@CommandLine.Option(names = "--plan-candidates", description = "Method to generate plan candidates", defaultValue = "diverse")
	private PlanCandidates planCandidates = PlanCandidates.bestK;
	@CommandLine.Option(names = "--output", description = "Path to output csv.", defaultValue = "plan-choices.csv")
	private Path output;

	private ThreadLocal<Ctx> thread;
	private ProgressBar pb;
	private double globalAvgIncome;
	private final MainModeIdentifier mmi = new DefaultAnalysisMainModeIdentifier();

	public static void main(String[] args) {
		new ComputePlanChoices().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (!output.getFileName().toString().contains(".csv")) {
			log.error("Output file must be a csv file");
			return 2;
		}

		Config config = this.scenario.getConfig();
		config.controller().setOutputDirectory("choice-output");
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		sw.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		if (timeUtil) {
			// All utilities except travel time become zero
			config.scoring().setMarginalUtlOfWaitingPt_utils_hr(0);
			config.scoring().setUtilityOfLineSwitch(0);

			config.scoring().getModes().values().forEach(m -> {
				// Only time goes into the score
				m.setMarginalUtilityOfTraveling(-config.scoring().getPerforming_utils_hr());
				m.setConstant(0);
				m.setMarginalUtilityOfDistance(0);
				m.setDailyMonetaryConstant(0);
				m.setMonetaryDistanceRate(0);
			});
		}

		// This method only produces two choices
		if (planCandidates == PlanCandidates.carAlternative) {
			log.info("Setting top k to 2 for car alternative");
			topK = 2;
		}

		Controler controler = this.scenario.createControler();

		controler.addOverridingModule(InformedModeChoiceModule.newBuilder()
			.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car", "pt")
			.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
			.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk", "pt", "ride")
			.withConstraint(RelaxedMassConservationConstraint.class)
			.build());

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(topK);
		imc.setModes(modes);
		imc.setConstraintCheck(InformedModeChoiceConfigGroup.ConstraintCheck.none);

		controler.run();

		Injector injector = controler.getInjector();

		Population population = controler.getScenario().getPopulation();

		thread = ThreadLocal.withInitial(() ->
			new Ctx(
				new PlanRouter(injector.getInstance(TripRouter.class),
					TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration,
						PlansConfigGroup.TripDurationHandling.ignoreDelays)),
				switch (planCandidates) {
					case bestK -> new BestKPlanGenerator(topK, injector.getInstance(TopKChoicesGenerator.class));
					case diverse -> new DiversePlanGenerator(topK, injector.getInstance(TopKChoicesGenerator.class));
					case random -> new RandomPlanGenerator(topK, injector.getInstance(TopKChoicesGenerator.class));
					case carAlternative -> new ExclusiveCarPlanGenerator(injector.getInstance(TopKChoicesGenerator.class));
				},
				calcScores ? new PseudoScorer(injector, population) : null
			)
		);

		globalAvgIncome = population.getPersons().values().stream()
			.map(PersonUtils::getIncome)
			.filter(Objects::nonNull)
			.mapToDouble(d -> d)
			.filter(dd -> dd > 0)
			.average()
			.orElse(Double.NaN);

		pb = new ProgressBar("Computing plan choices", population.getPersons().size());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		pb.close();

		String out = output.toString().replace(".csv", "-%s_%d.csv".formatted(planCandidates, topK));

		log.info("Writing {} choices to {}", rows.size(), out);

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(Path.of(out)), CSVFormat.DEFAULT.builder().setCommentMarker('#').build())) {

			// header
			List<Object> header = new ArrayList<>();
			header.add("person");
			header.add("weight");
			header.add("income");
			header.add("util_money");
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

			csv.printComment("Average global income: " + globalAvgIncome);

			csv.printRecord(header);

			for (List<Object> row : rows) {
				csv.printRecord(row);
			}
		}


		return 0;
	}

	@Override
	public void run(Person person) {

		if (person.getAttributes().getAttribute(Attributes.REF_MODES) == null) {
			pb.step();
			return;
		}

		Plan plan = person.getSelectedPlan();
		PlanModel model = PlanModel.newInstance(plan);

		String refModes = (String) person.getAttributes().getAttribute(TripAnalysis.ATTR_REF_MODES);
		String[] split = refModes.strip().split("-");
		String[] currentModes = model.getCurrentModesMutable();

		if (refModes.isBlank()) {
			pb.step();
			return;
		}

		if (split.length != currentModes.length) {
			log.warn("Number of trips ref/current do not match: {} / {}", Arrays.toString(split), Arrays.toString(currentModes));
			pb.step();
			return;
		}

		// Put reference modes into the current modes
		System.arraycopy(split, 0, currentModes, 0, split.length);

		Ctx ctx = thread.get();

		List<Object> row = new ArrayList<>();

		row.add(person.getAttributes().getAttribute(TripAnalysis.ATTR_REF_ID));
		row.add(person.getAttributes().getAttribute(TripAnalysis.ATTR_REF_WEIGHT));
		row.add(PersonUtils.getIncome(person));
		row.add(globalAvgIncome / PersonUtils.getIncome(person));

		// choice, always the first one
		row.add(1);

		List<PlanCandidate> candidates = ctx.generator.generate(model, modes, null);

		// skip possible error cases
		if (candidates == null) {
			pb.step();
			return;
		}

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

		if (calcScores)
			row.add(scorer.score(plan).getDouble("score"));
		else
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

	/**
	 * Define how candidates are generated.
	 */
	public enum PlanCandidates {
		bestK, diverse, random, carAlternative
	}

	private record ModeStats(int usage, double travelTime, double travelDistance, double rideTime, long numSwitches) {
	}

	private record Ctx(PlanRouter router, CandidateGenerator generator, PseudoScorer scorer) {
	}
}
