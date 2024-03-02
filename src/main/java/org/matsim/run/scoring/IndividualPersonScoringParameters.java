package org.matsim.run.scoring;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.NormalDistribution;
import org.apache.commons.statistics.distribution.TruncatedNormalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scoring parameters for {@link AdvancedScoringConfigGroup}. This makes use of the standard scoring and adds persons specific differences.
 */
public class IndividualPersonScoringParameters implements ScoringParametersForPerson {

	private static final Logger log = LogManager.getLogger(IndividualPersonScoringParameters.class);
	/**
	 * Header for info file.
	 */
	final Set<String> header = new LinkedHashSet<>();
	/**
	 * This map contains debug information to be written out after first iteration.
	 */
	final Map<Id<Person>, Object2DoubleMap<String>> info = new ConcurrentHashMap<>();
	/**
	 * Cache instances of {@link ActivityUtilityParameters} for each subpopulation.
	 */
	private final Map<String, Map<String, ActivityUtilityParameters>> actUtils = new ConcurrentHashMap<>();
	/**
	 * Cache instances of {@link ScoringParameters} for each person.
	 */
	private final IdMap<Person, ScoringParameters> cache;
	/**
	 * Cache and reuse distance group arrays.
	 */
	private final Map<DoubleList, DistanceGroup[]> distGroups = new ConcurrentHashMap<>();
	/**
	 * Thread-local random number generator.
	 */
	private final ThreadLocal<Context> rnd = ThreadLocal.withInitial(() -> new Context());
	private final Scenario scenario;
	private final ScoringConfigGroup basicScoring;
	private final TransitConfigGroup transitConfig;
	private final AdvancedScoringConfigGroup scoring;
	/**
	 * Average income of all agents with this attribute. Can be NaN if not used.
	 */
	private final double globalAvgIncome;

	@Inject
	public IndividualPersonScoringParameters(Scenario scenario) {
		this.scenario = scenario;
		this.basicScoring = scenario.getConfig().scoring();
		this.scoring = ConfigUtils.addOrGetModule(scenario.getConfig(), AdvancedScoringConfigGroup.class);
		this.transitConfig = scenario.getConfig().transit();
		this.globalAvgIncome = computeAvgIncome(scenario.getPopulation());
		this.cache = new IdMap<>(Person.class, scenario.getPopulation().getPersons().size());
	}


	static DistanceGroup[] calcDistanceGroups(List<Integer> dists, DoubleList distUtils) {

		// Nothing to do if no distance groups are defined.
		if (dists == null || dists.isEmpty() || distUtils.isEmpty()) {
			return null;
		}

		List<Integer> copy = new ArrayList<>(dists);

		if (copy.get(0) != 0)
			copy.add(0, 0);

		// Effectively no distance groups present
		if (copy.size() <= 1) {
			return null;
		}

		// No delta for distance groups
		if (distUtils.doubleStream().allMatch(d -> d == 0))
			return null;

		DistanceGroup[] groups = new DistanceGroup[copy.size()];

		if (groups.length - 1 != distUtils.size()) {
			log.error("Distance groups: {}, utils: {}", dists, distUtils);
			throw new IllegalArgumentException("Distance groups and distance utilities must have the same size.");
		}


		for (int i = 0; i < copy.size() - 1; i++) {

			double dist = copy.get(i);
			double nextDist = copy.get(i + 1);

			double constant = i == 0 ? 0 : distUtils.getDouble(i - 1);
			double next = distUtils.getDouble(i);


			groups[i] = new DistanceGroup(dist, constant, (next - constant) / (nextDist - dist));
		}

		// Last open ended dist group
		int lastDist = copy.get(copy.size() - 1);
		double lastUtil = distUtils.getDouble(distUtils.size() - 1);
		groups[copy.size() - 1] = new DistanceGroup(lastDist, lastUtil, lastUtil / lastDist);

		return groups;
	}

	private double computeAvgIncome(Population population) {
		if (!scoring.incomeDependent)
			return Double.NaN;

		log.info("reading income attribute using " + PersonUtils.class + " of all agents and compute global average.\n" +
			"Make sure to set this attribute only to appropriate agents (i.e. true 'persons' and not freight agents) \n" +
			"Income values <= 0 are ignored. Agents that have negative or 0 income will use the marginalUtilityOfMoney in their subpopulation's scoring params..");
		OptionalDouble averageIncome = population.getPersons().values().stream()
			//consider only agents that have a specific income provided
			.filter(person -> PersonUtils.getIncome(person) != null)
			.mapToDouble(PersonUtils::getIncome)
			.filter(dd -> dd > 0)
			.average();

		if (averageIncome.isEmpty()) {
			throw new RuntimeException("you have enabled income dependent scoring but there is not a single income attribute in the population! " +
				"If you are not aiming for person-specific marginalUtilityOfMoney, better use other PersonScoringParams, e.g. SubpopulationPersonScoringParams, which have higher performance." +
				"Otherwise, please provide income attributes in the population...");
		} else {
			log.info("global average income is " + averageIncome);
			return averageIncome.getAsDouble();
		}
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		return this.cache.computeIfAbsent(person.getId(), id -> {

			String subpopulation = PopulationUtils.getSubpopulation(person);
			ScoringConfigGroup.ScoringParameterSet scoringParameters = basicScoring.getScoringParameters(subpopulation);

			// Activity params can be reused per subpopulation
			Map<String, ActivityUtilityParameters> activityParams = actUtils.computeIfAbsent(subpopulation, k -> {
				Map<String, ActivityUtilityParameters> ap = new TreeMap<>();
				for (ScoringConfigGroup.ActivityParams params : scoringParameters.getActivityParams()) {
					ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params);
					ap.put(params.getActivityType(), factory.build());
				}

				// The code to add this activity type is always copied between different scoring implementations
				// it might not be actually needed anymore (because default staging activities are also added elsewhere)
				// but it's not clear if it's safe to remove it.
				if (transitConfig.isUseTransit()) {
					ScoringConfigGroup.ActivityParams transitActivityParams = new ScoringConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
					transitActivityParams.setTypicalDuration(120.0);
					transitActivityParams.setOpeningTime(0.);
					transitActivityParams.setClosingTime(0.);
					ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
					modeParamsBuilder.setScoreAtAll(false);
					ap.put(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder.build());
				}

				return ap;
			});

			ScoringParameters.Builder builder = new ScoringParameters.Builder(basicScoring,
				scoringParameters, activityParams, scenario.getConfig().scenario());

			Double personalIncome = PersonUtils.getIncome(person);
			// Income dependent scoring might be disabled
			if (!Double.isNaN(globalAvgIncome) && personalIncome != null) {
				if (personalIncome != 0) {
					builder.setMarginalUtilityOfMoney(scoringParameters.getMarginalUtilityOfMoney() * globalAvgIncome / personalIncome);
				} else {
					log.warn("you have set income to " + personalIncome + " for person " + person + ". This is invalid and gets ignored." +
						"Instead, the marginalUtilityOfMoney is derived from the subpopulation's scoring parameters.");
				}
			}

			Map<String, DistanceGroupModeUtilityParameters.DeltaBuilder> deltaParams = new HashMap<>();

			for (AdvancedScoringConfigGroup.ScoringParameters parameter : scoring.getScoringParameters()) {

				if (parameter.matchObject(person.getAttributes())) {
					for (Map.Entry<String, AdvancedScoringConfigGroup.ModeParams> mode : parameter.getModeParams().entrySet()) {

						DistanceGroupModeUtilityParameters.DeltaBuilder b =
							deltaParams.computeIfAbsent(mode.getKey(), k -> new DistanceGroupModeUtilityParameters.DeltaBuilder());

						b.addUtilsDistance(mode.getValue());
						addDeltaParams(b, mode.getValue(), person);
					}
				}
			}

			for (Map.Entry<String, DistanceGroupModeUtilityParameters.DeltaBuilder> mode : deltaParams.entrySet()) {
				ModeUtilityParameters params = builder.getModeParameters(mode.getKey());
				DistanceGroupModeUtilityParameters.DeltaBuilder delta = mode.getValue();

				// These arrays are re-used if possible
				DistanceGroup[] groups = distGroups.computeIfAbsent(delta.getPerDistGroup(), k -> calcDistanceGroups(scoring.distGroups, k));

				DistanceGroupModeUtilityParameters p = new DistanceGroupModeUtilityParameters(params, delta, groups);
				builder.setModeParameters(mode.getKey(), p);

				// Collect final adjustments information
				Object2DoubleMap<String> values = info.computeIfAbsent(person.getId(), k -> new Object2DoubleOpenHashMap<>());

				if (delta.constant != 0)
					values.put(mode.getKey() + "_constant", delta.constant);

				if (delta.dailyUtilityConstant != 0)
					values.put(mode.getKey() + "_dailyConstant", delta.dailyUtilityConstant);

				if (groups != null) {
					for (DistanceGroup group : groups) {
						values.put("%s_dist_%.0f".formatted(mode.getKey(), group.dist()), group.utilPerM());
					}
				}

				header.addAll(values.keySet());
			}

			return builder.build();
		});
	}

	/**
	 * Compute or retrieve delta params for person.
	 */
	private void addDeltaParams(DistanceGroupModeUtilityParameters.DeltaBuilder delta, AdvancedScoringConfigGroup.ModeParams params, Person person) {

		ContinuousDistribution.Sampler normal = this.rnd.get().normal;
		ContinuousDistribution.Sampler tn = this.rnd.get().tn;

		switch (params.varConstant) {
			case fixed -> delta.constant += params.deltaConstant;
			case normal -> delta.constant += normal.sample() * params.deltaConstant;
			case truncatedNormal -> delta.constant += tn.sample() * params.deltaConstant;
			default -> throw new IllegalArgumentException("Unsupported varConstant: " + params.varConstant);
		}

		switch (params.varDailyConstant) {
			case fixed -> delta.dailyUtilityConstant += params.deltaDailyConstant;
			case normal -> delta.dailyUtilityConstant += normal.sample() * params.deltaDailyConstant;
			case truncatedNormal -> delta.dailyUtilityConstant += tn.sample() * params.deltaDailyConstant;
			default -> throw new IllegalArgumentException("Unsupported varDailyConstant: " + params.varDailyConstant);
		}
	}

	private record Context(ContinuousDistribution.Sampler normal, ContinuousDistribution.Sampler tn) {

		Context() {
			this(NormalDistribution.of(0, 1).createSampler(RandomSource.KISS.create(123L)),
				TruncatedNormalDistribution.of(0, 1, 0, Double.POSITIVE_INFINITY)
					.createSampler(RandomSource.KISS.create(123L))
			);
		}
	}
}
