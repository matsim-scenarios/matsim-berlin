package org.matsim.analysis;

import com.google.inject.Injector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ActivityAttributeTypicalDurationCalculator;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.utils.tablesaw.TablesawUtils;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.HistogramTrace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

/**
 * Agent-wise welfare comparison of a policy run against a base run, after Kai Nagel's {@code AgentWiseComparisonKN}
 * (vsp contrib, {@code org.matsim.application.analysis.population}): per person, the score of the executed plan in
 * both runs, split into its parts, and the differences, summed over persons and scaled to 100 %, in utils and in EUR.
 * <p>
 * What is different from the original is where the numbers come from. AgentWiseComparisonKN rebuilds the scoring
 * function from the output config plus bindings of its own, so it has to agree with the run's scoring setup by
 * construction, and for the Berlin scoring it does not: it imposes income-dependent money with exponent 1 where the
 * run uses {@code tasteVariations.incomeExponent}, it takes the mode constants from the config where the run adds the
 * person-specific {@code modeTasteVariations}, it scores activities against the typical duration of their type where
 * the run uses the per-activity {@code typicalDuration} attribute, and it does not know the pt submode constants of
 * {@link org.matsim.run.scoring.TransitTripScoring} or the pseudo-random trip term. This class reads whatever the run
 * has already computed and recomputes only what it has to:
 * <ul>
 * <li>The parts of the score are the run's own. Berlin runs write a score explanation for every executed plan
 * ({@code scoring.explainScores}, set in {@link OpenBerlinScenario}), and the selected plan in output_plans carries
 * the one from the last iteration: activities, legs, the pseudo-random trip term, the pt submodes, money, stuck.</li>
 * <li>Only the leg part is split further, into constants, travel time, line switches and money, as in the original.
 * The split takes each person's scoring parameters from the run's own injector
 * ({@link OpenBerlinScenario#prepareControlerForAnalysis}), so taste variations and income-dependent money are
 * applied exactly as in the run, and it is checked per person against the leg part of the explanation. A split that
 * does not add up is logged and shows up in the {@code legs_check} row; the totals do not depend on it.</li>
 * </ul>
 * As in the original, only the {@code person} subpopulation is compared, persons without trips or stuck in either run
 * are left out, and the comparison is split into remainers (same sequence of main modes in both runs) and switchers.
 * EUR values divide each person's score difference by their marginal utility of money in the base run.
 * <p>
 * The score explanation is the score of the last iteration, while the plan score in output_plans is averaged over the
 * iterations after {@code fractionOfIterationsToStartScoreMSA}; the difference of the latter is reported as
 * {@code matsim_score} for comparison.
 * <p>
 * Also as in the original, the base run's MUSE, the marginal utility of starting an activity earlier, and VSE, its
 * value in EUR (MUSE divided by the marginal utility of money), are written as histograms over persons, each person
 * with the mean over their activities. They are computed with the run's own scoring function, so with the
 * per-activity typical durations and the person's own marginal utility of money, and the activity part it computes
 * for the unchanged plan is checked against the run's ({@code acts_check}). The rule-of-half part of the original is
 * not ported.
 */
@CommandLine.Command(
	name = "agent-wise-comparison",
	description = "Compare the scores of the executed plans of a policy run against a base run, person by person.",
	mixinStandardHelpOptions = true
)
public final class AgentWiseComparison implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AgentWiseComparison.class);

	private static final double CHECK_TOLERANCE = 1e-6;

	/** Sub-entries of the explanation, which detail a part but are not parts of the score themselves. */
	private static final Pattern EXPLANATION_DETAIL = Pattern.compile("act\\w+_s|legs_\\d+_util|trip_\\d+|money_price");

	@CommandLine.Parameters(arity = "1", description = "Output directory of the policy run.")
	private Path policyPath;

	@CommandLine.Option(names = "--base-path", required = true, description = "Output directory of the base run.")
	private Path basePath;

	@CommandLine.Option(names = "--output", description = "Directory for the result files. Default: analysis/agent-wise-comparison in the policy run.")
	private Path output;

	@CommandLine.Option(names = "--subpopulation", defaultValue = "person", description = "Subpopulation to compare.")
	private String subpopulation;

	@CommandLine.Option(names = "--sample-size", description = "Sample size the sums are scaled up from. Default: simwrapper.sampleSize of the base run, else its qsim.flowCapacityFactor.")
	private Double sampleSize;

	public AgentWiseComparison() {
	}

	/** For the run class's post-processing. */
	public AgentWiseComparison(Path basePath, Path policyPath) {
		this.basePath = basePath;
		this.policyPath = policyPath;
		this.subpopulation = "person";
	}

	public static void main(String[] args) {
		new AgentWiseComparison().execute(args);
	}

	/**
	 * The parts of a person's score in one run, in utils unless the name says otherwise. The comment is the row
	 * description in the summary.
	 */
	enum Part {
		SCORE("score", "overall benefit (the score of the executed plan in the last iteration), made up of:"),
		MONEY_SCORE("money_score", "... money (leg distance costs and daily monetary constants, plus money events)"),
		ACTS("acts_score", "... activities"),
		DIRECT_TRAVEL("u_trav_direct_all", "... direct travel, made up of:"),
		U_TRAV("u_trav", "... ... travel time (incl. pt waiting)"),
		U_DIST("u_dist", "... ... distance"),
		LINE_SWITCHES("u_lineswitches", "... ... pt line switches"),
		ASCS("ascs", "... ... mode constants (per trip and per day, incl. person-specific taste variations)"),
		PT_SUBMODES("pt_submodes", "... ... pt submode terms (TransitTripScoring)"),
		PSEUDO_RANDOM("pseudo_random", "... pseudo-random trip term"),
		STUCK("stuck", "... stuck"),
		SCORE_EVENTS("score_events", "... score events"),
		OTHER("other", "... parts of the explanation this analysis does not know"),
		LEGS_CHECK("legs_check", "split of the leg part minus the leg part of the explanation (should be 0)"),
		ACTS_CHECK("acts_check", "re-scored activity part minus the activity part of the explanation (should be 0)"),
		MATSIM_SCORE("matsim_score", "difference of the plan scores in output_plans (MSA-averaged)"),
		LEGS("legs", null),
		MONEY_EVENTS_SCORE("money_events_score", null),
		MONEY_LEGS_SCORE("money_legs_score", null),
		MONEY_EUR("money_eur", null),
		TTIME_H("ttime_h", null);

		final String key;
		final String comment;

		Part(String key, String comment) {
			this.key = key;
			this.comment = comment;
		}

		/** Parts in utils that are summed into the summary; the others are only in the per-person file. */
		boolean inSummary() {
			return comment != null;
		}
	}

	/** One person in one run. */
	record PersonScore(String modeSequence, double income, double marginalUtilityOfMoney, double[] parts, boolean stuck,
					   List<ActivityMuse> muse) {
		double get(Part part) {
			return parts[part.ordinal()];
		}

		/** The person's MUSE in utils per hour: the mean over their activities, as in the original. */
		double museH() {
			return muse.stream().mapToDouble(ActivityMuse::museH).average().orElse(Double.NaN);
		}
	}

	/**
	 * The MUSE of one main activity of an executed plan; {@code index} counts the main activities from 0, the trip
	 * arriving at it is the one before.
	 */
	record ActivityMuse(int index, String type, double startTime, double endTime, double typicalDuration,
						String arrivingMode, double museH) {
	}

	@Override
	public Integer call() throws Exception {

		Path outputDir = output != null ? output : policyPath.resolve("analysis").resolve("agent-wise-comparison");
		Files.createDirectories(outputDir);

		Config baseConfig = loadOutputConfig(basePath);
		double sample = sampleSize != null ? sampleSize : sampleSizeOf(baseConfig);
		log.info("Scaling sums by 1/{} to 100 %.", sample);

		Map<Id<Person>, PersonScore> base = readRun(basePath, baseConfig);
		Map<Id<Person>, PersonScore> policy = readRun(policyPath, loadOutputConfig(policyPath));

		List<Id<Person>> compared = new ArrayList<>();
		int stuck = 0;
		for (Map.Entry<Id<Person>, PersonScore> e : base.entrySet()) {
			PersonScore p = policy.get(e.getKey());
			if (p == null)
				continue;
			if (e.getValue().stuck() || p.stuck()) {
				stuck++;
				continue;
			}
			compared.add(e.getKey());
		}
		log.info("Persons with trips: {} in the base run, {} in the policy run; {} in both, of which {} are stuck in either run and left out; {} compared.",
			base.size(), policy.size(), compared.size() + stuck, stuck, compared.size());

		long differentMoney = compared.stream()
			.filter(id -> Math.abs(base.get(id).marginalUtilityOfMoney() - policy.get(id).marginalUtilityOfMoney()) > CHECK_TOLERANCE)
			.count();
		if (differentMoney > 0)
			log.warn("{} persons have a different marginal utility of money in the policy run; EUR values use the base run's.", differentMoney);

		Map<Id<Person>, Integer> deciles = incomeDeciles(compared, base);

		writePersons(outputDir.resolve("agent_wise_comparison_persons.csv.gz"), compared, base, policy, deciles);

		Map<String, List<Id<Person>>> groups = new LinkedHashMap<>();
		groups.put("all", compared);
		groups.put("remainers", compared.stream().filter(id -> base.get(id).modeSequence().equals(policy.get(id).modeSequence())).toList());
		groups.put("switchers", compared.stream().filter(id -> !base.get(id).modeSequence().equals(policy.get(id).modeSequence())).toList());

		writeSummary(outputDir.resolve("agent_wise_comparison_summary.csv"), groups, base, policy, sample);

		writeMuse(outputDir, base);

		return 0;
	}

	static Config loadOutputConfig(Path runDir) {
		return ConfigUtils.loadConfig(ApplicationUtils.globFile(runDir, "*output_config.xml").toString());
	}

	private static double sampleSizeOf(Config config) {
		ConfigGroup simwrapper = config.getModules().get("simwrapper");
		if (simwrapper != null && simwrapper.getParams().containsKey("sampleSize"))
			return Double.parseDouble(simwrapper.getParams().get("sampleSize"));
		return config.qsim().getFlowCapFactor();
	}

	/**
	 * The persons of one run with trips, with the parts of their score.
	 */
	private Map<Id<Person>, PersonScore> readRun(Path runDir, Config config) throws IOException {

		log.info("Reading run {}", runDir);

		// the injector does not write anything, but keep it away from the run's output in case some binding does
		Path scratch = Files.createTempDirectory("agent-wise-comparison");
		config.controller().setOutputDirectory(scratch.toString());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		// read by an eager binding, and not needed
		config.counts().setInputFile(null);

		// the whole population, not just the compared persons: the average income of income-dependent scoring is
		// taken over it, as in the run
		Population population = readSelectedPlans(config, ApplicationUtils.globFile(runDir, "*output_plans.xml*"));

		MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
		scenario.setPopulation(population);

		Injector injector = OpenBerlinScenario.prepareControlerForAnalysis(scenario).getInjector();
		ScoringParametersForPerson parametersForPerson = injector.getInstance(ScoringParametersForPerson.class);
		AnalysisMainModeIdentifier mainModeIdentifier = injector.getInstance(AnalysisMainModeIdentifier.class);
		ScoringFunctionFactory scoringFunctionFactory = injector.getInstance(ScoringFunctionFactory.class);
		Set<String> ptModes = config.transit().getTransitModes();

		Map<Id<Person>, PersonScore> result = new HashMap<>();
		Set<String> unknownParts = new TreeSet<>();
		int[] mismatches = {0};
		double[] maxMismatch = {0};
		int[] actsMismatches = {0};
		double[] maxActsMismatch = {0};
		double[] sumScoreMinusMatsim = {0};
		int[] averaged = {0};

		MutableScenario tmp = ScenarioUtils.createMutableScenario(config);
		StreamingPopulationReader reader = new StreamingPopulationReader(tmp);
		reader.addAlgorithm(experienced -> {

			Person person = population.getPersons().get(experienced.getId());
			if (person == null || !subpopulation.equals(PopulationUtils.getSubpopulation(person)))
				return;

			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(experienced.getSelectedPlan());
			if (trips.isEmpty())
				return;

			Plan selected = person.getSelectedPlan();
			Object explanation = selected.getAttributes().getAttribute(ScoringFunction.SCORE_EXPLANATION_ATTR);
			if (explanation == null)
				throw new IllegalStateException("The selected plan of person " + person.getId() + " in " + runDir + " has no "
					+ ScoringFunction.SCORE_EXPLANATION_ATTR + ". This analysis needs runs with scoring.explainScores=true.");

			ScoringParameters params = parametersForPerson.getScoringParameters(person);
			double[] parts = new double[Part.values().length];

			addExplanation(explanation.toString(), parts, unknownParts);
			splitLegs(trips, params, ptModes, parts);

			double legsCheck = parts[Part.U_TRAV.ordinal()] + parts[Part.U_DIST.ordinal()] + parts[Part.LINE_SWITCHES.ordinal()]
				+ parts[Part.ASCS.ordinal()] + parts[Part.MONEY_LEGS_SCORE.ordinal()] - parts[Part.LEGS.ordinal()];
			parts[Part.LEGS_CHECK.ordinal()] = legsCheck;
			if (Math.abs(legsCheck) > CHECK_TOLERANCE) {
				if (mismatches[0] < 10)
					log.error("Person {}: the split of the leg part adds up to {}, the run's leg part is {}.",
						person.getId(), parts[Part.LEGS.ordinal()] + legsCheck, parts[Part.LEGS.ordinal()]);
				mismatches[0]++;
				maxMismatch[0] = Math.max(maxMismatch[0], Math.abs(legsCheck));
			}

			parts[Part.MONEY_SCORE.ordinal()] = parts[Part.MONEY_LEGS_SCORE.ordinal()] + parts[Part.MONEY_EVENTS_SCORE.ordinal()];
			parts[Part.DIRECT_TRAVEL.ordinal()] = parts[Part.U_TRAV.ordinal()] + parts[Part.U_DIST.ordinal()]
				+ parts[Part.LINE_SWITCHES.ordinal()] + parts[Part.ASCS.ordinal()] + parts[Part.PT_SUBMODES.ordinal()];
			parts[Part.MATSIM_SCORE.ordinal()] = selected.getScore();

			double score = parts[Part.SCORE.ordinal()];
			if (Math.abs(score - selected.getScore()) > CHECK_TOLERANCE)
				averaged[0]++;
			sumScoreMinusMatsim[0] += score - selected.getScore();

			List<String> mainModes = trips.stream().map(trip -> mainModeIdentifier.identifyMainMode(trip.getTripElements())).toList();

			List<ActivityMuse> muse = new ArrayList<>();
			double acts = muse(person, experienced.getSelectedPlan(), mainModes, scoringFunctionFactory, muse);
			double actsCheck = acts - parts[Part.ACTS.ordinal()];
			parts[Part.ACTS_CHECK.ordinal()] = actsCheck;
			if (Math.abs(actsCheck) > CHECK_TOLERANCE) {
				if (actsMismatches[0] < 10)
					log.error("Person {}: re-scoring the activities gives {}, the run's activity part is {}.",
						person.getId(), acts, parts[Part.ACTS.ordinal()]);
				actsMismatches[0]++;
				maxActsMismatch[0] = Math.max(maxActsMismatch[0], Math.abs(actsCheck));
			}

			Double income = PersonUtils.getIncome(person);
			result.put(person.getId(), new PersonScore(String.join("-", mainModes), income != null ? income : Double.NaN,
				params.marginalUtilityOfMoney, parts, parts[Part.STUCK.ordinal()] != 0, muse));
		});
		reader.readFile(ApplicationUtils.globFile(runDir, "*output_experienced_plans.xml*").toString());

		if (!unknownParts.isEmpty())
			log.warn("The score explanations contain parts this analysis does not know; they are summed as 'other': {}", unknownParts);
		if (mismatches[0] > 0)
			log.error("For {} of {} persons the split of the leg part does not add up to the run's leg part (largest difference {}). " +
				"The leg rows of the summary are not reliable; the totals are.", mismatches[0], result.size(), maxMismatch[0]);
		else
			log.info("The split of the leg part adds up to the run's leg part for all {} persons.", result.size());
		if (actsMismatches[0] > 0)
			log.error("For {} of {} persons re-scoring the activities does not give the run's activity part (largest difference {}). " +
				"MUSE and VSE are not reliable.", actsMismatches[0], result.size(), maxActsMismatch[0]);
		else
			log.info("Re-scoring the activities gives the run's activity part for all {} persons.", result.size());
		log.info("Score of the last iteration differs from the (averaged) plan score for {} of {} persons; mean difference {}.",
			averaged[0], result.size(), result.isEmpty() ? 0 : sumScoreMinusMatsim[0] / result.size());

		try (var files = Files.list(scratch)) {
			if (files.findAny().isEmpty())
				Files.delete(scratch);
		}

		return result;
	}

	/**
	 * MUSE, the marginal utility of starting an activity earlier, as in the original's {@code MuseComputation}: for
	 * every main activity with a start time, the change of the activity score when it starts one second earlier, in
	 * utils per hour. The main activities are scored with the run's scoring function, which resolves their typical
	 * durations from the selected plan as in the run. Stage activities are not handed: they are not scored, and the
	 * population reader drops their times. Only activities are handed, so only the activity part of the score
	 * changes.
	 *
	 * @return the activity part of the score of the unchanged plan, to be checked against the run's
	 */
	static double muse(Person person, Plan experienced, List<String> arrivingModes, ScoringFunctionFactory factory,
					   List<ActivityMuse> result) {

		List<Activity> activities = TripStructureUtils.getActivities(experienced, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		List<Activity> planned = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		double unchanged = scoreActivities(person, activities, factory);

		for (int main = 0; main < activities.size(); main++) {
			Activity act = activities.get(main);
			if (act.getStartTime().isUndefined())
				continue;

			Activity early = PopulationUtils.createActivity(act);
			early.setStartTime(act.getStartTime().seconds() - 1);
			List<Activity> changed = new ArrayList<>(activities);
			changed.set(main, early);
			double museH = (scoreActivities(person, changed, factory) - unchanged) * 3600;

			double typicalDuration = Double.NaN;
			if (main < planned.size() && planned.get(main).getType().equals(act.getType())
				&& planned.get(main).getAttributes().getAttribute(ActivityAttributeTypicalDurationCalculator.TYPICAL_DURATION_ATTRIBUTE) instanceof Number n)
				typicalDuration = n.doubleValue();

			result.add(new ActivityMuse(main, act.getType(), act.getStartTime().seconds(), act.getEndTime().orElse(Double.NaN),
				typicalDuration, main - 1 < arrivingModes.size() ? arrivingModes.get(main - 1) : null, museH));
		}
		return unchanged;
	}

	private static double scoreActivities(Person person, List<Activity> activities, ScoringFunctionFactory factory) {
		ScoringFunction sf = factory.createNewScoringFunction(person);
		activities.forEach(sf::handleActivity);
		sf.finish();
		return sf.getScore();
	}

	/**
	 * The MUSE and VSE of the base run: the histograms over persons, as in the original, and every activity.
	 */
	private static void writeMuse(Path outputDir, Map<Id<Person>, PersonScore> base) throws IOException {

		List<PersonScore> persons = base.values().stream().filter(p -> !p.stuck() && !p.muse().isEmpty()).toList();

		DoubleColumn muse = DoubleColumn.create("MUSE [utils/h]");
		DoubleColumn vse = DoubleColumn.create("VSE [EUR/h]");
		for (PersonScore p : persons) {
			muse.append(p.museH());
			vse.append(p.museH() / p.marginalUtilityOfMoney());
		}
		writeHistogram(outputDir.resolve("muse.html"), muse, "MUSE of the base run, persons (mean over their activities)");
		writeHistogram(outputDir.resolve("vse.html"), vse, "VSE of the base run, persons (MUSE / marginal utility of money)");

		// the original's population average weighs by trips, i.e. by activities with a start time
		double museTrips = persons.stream().flatMap(p -> p.muse().stream()).mapToDouble(ActivityMuse::museH).average().orElse(Double.NaN);
		log.info("MUSE of the base run: mean over trips {} utils/h; over persons: mean {}, quartiles {} {} {} utils/h.",
			museTrips, muse.mean(), muse.percentile(25), muse.median(), muse.percentile(75));
		log.info("VSE of the base run over persons: mean {}, quartiles {} {} {} EUR/h.",
			vse.mean(), vse.percentile(25), vse.median(), vse.percentile(75));

		Path file = outputDir.resolve("agent_wise_comparison_muse_activities.csv.gz");
		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(file.toString()), CSVFormat.DEFAULT.builder()
			.setHeader("person", "activity_index", "type", "start_time", "end_time", "typical_duration", "arriving_mode", "muse_h", "mUoM", "vse_eur_h").build())) {
			for (Map.Entry<Id<Person>, PersonScore> e : base.entrySet()) {
				PersonScore p = e.getValue();
				if (p.stuck())
					continue;
				for (ActivityMuse a : p.muse())
					csv.printRecord(e.getKey(), a.index(), a.type(), a.startTime(), a.endTime(), a.typicalDuration(), a.arrivingMode(),
						a.museH(), p.marginalUtilityOfMoney(), a.museH() / p.marginalUtilityOfMoney());
			}
		}
		log.info("Wrote {}", file);
	}

	private static void writeHistogram(Path file, DoubleColumn values, String title) {
		Layout layout = Layout.builder()
			.title(title)
			.width(1000)
			.xAxis(Axis.builder().title(values.name()).build())
			.yAxis(Axis.builder().title("persons").build())
			.build();
		TablesawUtils.writeFigureToHtmlFile(file.toString(), new Figure(layout, HistogramTrace.builder(values).nBinsX(100).build()));
		log.info("Wrote {}", file);
	}

	/**
	 * The population with only the selected plans and without routes, which are all this analysis needs from it.
	 */
	private static Population readSelectedPlans(Config config, Path plansFile) {
		Population population = PopulationUtils.createPopulation(config);
		MutableScenario tmp = ScenarioUtils.createMutableScenario(config);
		StreamingPopulationReader reader = new StreamingPopulationReader(tmp);
		reader.addAlgorithm(person -> {
			PersonUtils.removeUnselectedPlans(person);
			for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan()))
				leg.setRoute(null);
			population.addPerson(person);
		});
		reader.readFile(plansFile.toString());
		return population;
	}

	/**
	 * Adds the parts of a score explanation. The keys are those of the scoring functions the Berlin scoring is
	 * assembled from; keys of unknown parts are collected and the parts summed as {@link Part#OTHER}.
	 */
	static void addExplanation(String explanation, double[] parts, Set<String> unknown) {
		for (String entry : explanation.split(ScoringFunction.SCORE_DELIMITER)) {
			String[] kv = entry.trim().split("=", 2);
			if (kv.length != 2 || kv[0].isEmpty())
				continue;
			String key = kv[0];
			double value = Double.parseDouble(kv[1]);
			if (key.equals("money_price")) {
				parts[Part.MONEY_EUR.ordinal()] += value;
				continue;
			}
			if (EXPLANATION_DETAIL.matcher(key).matches())
				continue;
			Part part = switch (key) {
				case "actPerforming_util", "actWaiting_util", "actLateArrival_util", "actEarlyDeparture_util" -> Part.ACTS;
				case "legs_util" -> Part.LEGS;
				case "trips_util" -> Part.PSEUDO_RANDOM;
				case "transit_score" -> Part.PT_SUBMODES;
				case "money_util" -> Part.MONEY_EVENTS_SCORE;
				case "agentStuck_util" -> Part.STUCK;
				case "scoreEvents_util" -> Part.SCORE_EVENTS;
				default -> {
					unknown.add(key);
					yield Part.OTHER;
				}
			};
			parts[part.ordinal()] += value;
			parts[Part.SCORE.ordinal()] += value;
		}
	}

	/**
	 * Splits the leg part of the score into its terms, adding them up the way
	 * {@link org.matsim.core.scoring.functions.CharyparNagelLegScoring} does: the constant once per mode and trip,
	 * the daily constants once per mode and day, a line switch for every pt leg after the first of a trip.
	 */
	static void splitLegs(List<TripStructureUtils.Trip> trips, ScoringParameters params, Set<String> ptModes, double[] parts) {
		Set<String> modesOfDay = new HashSet<>();
		double money = 0;
		for (TripStructureUtils.Trip trip : trips) {
			Set<String> modesOfTrip = new HashSet<>();
			int ptLegs = 0;
			for (Leg leg : trip.getLegsOnly()) {
				ModeUtilityParameters p = modeParams(params, leg.getMode());
				double travelTime = leg.getTravelTime().seconds();
				parts[Part.TTIME_H.ordinal()] += travelTime / 3600;
				parts[Part.U_TRAV.ordinal()] += travelTime * p.marginalUtilityOfTraveling_s;

				double waitingUtility = params.marginalUtilityOfWaitingPt_s - p.marginalUtilityOfTraveling_s;
				if (leg.getRoute() instanceof PassengerRoute route && waitingUtility != 0)
					parts[Part.U_TRAV.ordinal()] += (route.getBoardingTime().seconds() - leg.getDepartureTime().seconds()) * waitingUtility;

				if (p.marginalUtilityOfDistance_m != 0 || p.monetaryDistanceCostRate != 0) {
					double distance = leg.getRoute().getDistance();
					parts[Part.U_DIST.ordinal()] += distance * p.marginalUtilityOfDistance_m;
					money += distance * p.monetaryDistanceCostRate;
				}
				if (modesOfTrip.add(leg.getMode()))
					parts[Part.ASCS.ordinal()] += p.constant;
				if (modesOfDay.add(leg.getMode())) {
					parts[Part.ASCS.ordinal()] += p.dailyUtilityConstant;
					money += p.dailyMoneyConstant;
				}
				if (ptModes.contains(leg.getMode()) && ++ptLegs > 1)
					parts[Part.LINE_SWITCHES.ordinal()] += params.utilityOfLineSwitch;
			}
		}
		parts[Part.MONEY_LEGS_SCORE.ordinal()] += money * params.marginalUtilityOfMoney;
		parts[Part.MONEY_EUR.ordinal()] += money;
	}

	private static ModeUtilityParameters modeParams(ScoringParameters params, String mode) {
		ModeUtilityParameters p = params.modeParams.get(mode);
		if (p == null && (mode.equals("transit_walk") || mode.equals("non_network_walk")))
			p = params.modeParams.get("walk");
		if (p == null)
			throw new IllegalStateException("No scoring parameters for mode " + mode);
		return p;
	}

	/**
	 * Income deciles 0..9 of the compared persons in the base run, -1 for persons without income.
	 */
	private static Map<Id<Person>, Integer> incomeDeciles(List<Id<Person>> persons, Map<Id<Person>, PersonScore> base) {
		List<Id<Person>> withIncome = persons.stream()
			.filter(id -> !Double.isNaN(base.get(id).income()))
			.sorted(Comparator.comparingDouble(id -> base.get(id).income()))
			.toList();
		Map<Id<Person>, Integer> deciles = new HashMap<>();
		for (Id<Person> id : persons)
			deciles.put(id, -1);
		for (int i = 0; i < withIncome.size(); i++)
			deciles.put(withIncome.get(i), i * 10 / withIncome.size());
		return deciles;
	}

	private static void writePersons(Path file, List<Id<Person>> persons, Map<Id<Person>, PersonScore> base,
									 Map<Id<Person>, PersonScore> policy, Map<Id<Person>, Integer> deciles) throws IOException {

		List<String> header = new ArrayList<>(List.of("person", "income", "income_decile", "mUoM", "mUoM_policy", "modes_base", "modes_policy",
			"muse_h_base", "muse_h_policy", "vse_eur_h_base", "vse_eur_h_policy"));
		for (Part part : Part.values()) {
			header.add(part.key + "_base");
			header.add(part.key + "_policy");
			header.add("d_" + part.key);
		}

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(file.toString()), CSVFormat.DEFAULT.builder().setHeader(header.toArray(String[]::new)).build())) {
			for (Id<Person> id : persons) {
				PersonScore b = base.get(id);
				PersonScore p = policy.get(id);
				List<Object> row = new ArrayList<>(List.of(id.toString(), b.income(), deciles.get(id), b.marginalUtilityOfMoney(),
					p.marginalUtilityOfMoney(), b.modeSequence(), p.modeSequence(),
					b.museH(), p.museH(), b.museH() / b.marginalUtilityOfMoney(), p.museH() / p.marginalUtilityOfMoney()));
				for (Part part : Part.values()) {
					row.add(b.get(part));
					row.add(p.get(part));
					row.add(p.get(part) - b.get(part));
				}
				csv.printRecord(row);
			}
		}
		log.info("Wrote {}", file);
	}

	private static void writeSummary(Path file, Map<String, List<Id<Person>>> groups, Map<Id<Person>, PersonScore> base,
									 Map<Id<Person>, PersonScore> policy, double sample) throws IOException {

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(file.toString()),
			CSVFormat.DEFAULT.builder().setHeader("group", "persons", "row", "utils", "eur", "comment").build())) {

			for (Map.Entry<String, List<Id<Person>>> group : groups.entrySet()) {
				List<Id<Person>> ids = group.getValue();
				StringBuilder table = new StringBuilder();
				table.append(String.format("%n%s: %d persons, sums scaled to 100 %% by 1/%s%n", group.getKey(), ids.size(), sample));
				table.append(String.format("%16s %16s  %-22s %s%n", "utils", "EUR", "row", "comment"));

				for (Part part : Part.values()) {
					if (!part.inSummary())
						continue;
					ToDoubleFunction<Id<Person>> delta = id -> policy.get(id).get(part) - base.get(id).get(part);
					double utils = ids.stream().mapToDouble(delta).sum() / sample;
					double eur = ids.stream().mapToDouble(id -> delta.applyAsDouble(id) / base.get(id).marginalUtilityOfMoney()).sum() / sample;
					csv.printRecord(group.getKey(), ids.size(), part.key, utils, eur, part.comment);
					table.append(String.format("%16.0f %16.0f  %-22s %s%n", utils, eur, part.key, part.comment));
				}
				log.info(table);
			}
		}
		log.info("Wrote {}", file);
	}

}
