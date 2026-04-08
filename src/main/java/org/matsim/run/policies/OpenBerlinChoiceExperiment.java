package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.annealing.ReplanningAnnealerConfigGroup;
import org.matsim.core.replanning.choosers.BalancedInnovationStrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.PtTripEstimator;
import org.matsim.modechoice.pruning.PlanScoreThresholdPruner;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.run.scoring.AdvancedScoringConfigGroup;
import org.matsim.run.scoring.PseudoRandomTripScoreEstimator;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * This class can be used to run some synthetic mode choice experiments on the OpenBerlin scenario.
 */
public class OpenBerlinChoiceExperiment extends OpenBerlinScenario {

	private static final Logger log = LogManager.getLogger(OpenBerlinChoiceExperiment.class);

	@CommandLine.Option(names = "--bike-speed-offset", description = "Offset the default bike speed in km/h", defaultValue = "0")
	private double bikeSpeedOffset;

	@CommandLine.Option(names = "--imc", description = "Enable informed-mode-choice functionality")
	private boolean imc;

	@CommandLine.Option(names = "--pruning", description = "Plan pruning threshold. If 0 pruning is disabled", defaultValue = "0")
	private double pruning;

	@CommandLine.Option(names = "--top-k", description = "Top k value to use with IMC", defaultValue = "25")
	private int topK;

	@CommandLine.Option(names = "--balanced-innovation", description = "Use balanced innovation selection", defaultValue = "false")
	private boolean bi;

	@CommandLine.Option(names = "--no-act-est", description = "Include estimation of activity utilities into score", defaultValue = "true",
			negatable = true)
	private boolean actEst;

	@CommandLine.Option(names = "--inv-beta", description = "Beta inv value for selection", defaultValue = "-1")
	private double invBeta;

	@CommandLine.Option(names = "--strategy", description = "Mode choice strategy to use (imc needs to be enabled)",
		defaultValue = InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)
	private String strategy;

	@CommandLine.Option(names = "--innovation-rate", description = "Overwrite the innovation rate for annealing", defaultValue = "0.3")
	private double innovationRate;

	@CommandLine.Option(names = "--est-rate", description = "Probability how often estimates are re-computed", defaultValue = "0.1")
	private double estRate;

	@CommandLine.Option(names = "--mode-choice-weight", description = "Weight for mode choice (re-route is 0.15).", defaultValue = "0.15")
	private double mcWeight;

	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinChoiceExperiment.class, args);
	}

	/**
	 * Remove strategy from all subpopulations.
	 */
	private static void removeStrategy(Config config, String strategy) {

		List<ReplanningConfigGroup.StrategySettings> strategies = new ArrayList<>(config.replanning().getStrategySettings());
		strategies.removeIf(s -> s.getStrategyName().equals(strategy));

		config.replanning().clearStrategySettings();

		strategies.forEach(config.replanning()::addStrategySettings);
	}

	@Override
	protected Config prepareConfig(Config config) {

		config = super.prepareConfig(config);

		if (innovationRate >= 0) {

			// Set start value for innovation rate
			for (ReplanningAnnealerConfigGroup.AnnealingVariable v : config.replanningAnnealer().getAllAnnealingVariables()) {

				v.setStartValue(innovationRate);

				// Note that this adjusts the shape of the annealing
				if (iterations > 0)
					v.setShapeFactor(10.0/iterations);
				else if (iterations < 0)
					v.setShapeFactor(10.0/config.controller().getLastIteration());

				v.setHalfLife(2d/3d);

				log.info("Setting innovation rate for {} to {}", v.getSubpopulation(), innovationRate);
			}
		}

		if (imc) {
			// Add score information
			config.scoring().setExplainScores(true);

			InformedModeChoiceConfigGroup imcConfig = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

			imcConfig.setTopK(topK);
			imcConfig.setModes(List.of(config.subtourModeChoice().getModes()));
			imcConfig.setConstraintCheck(InformedModeChoiceConfigGroup.ConstraintCheck.repair);
			imcConfig.setProbaEstimate(estRate);

			if (invBeta >= 0)
				imcConfig.setInvBeta(invBeta);

			InformedModeChoiceModule.replaceReplanningStrategy(config, "person",
				DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice,
				strategy,
				mcWeight
			);

			// All imc strategies are run with best score selector
			InformedModeChoiceModule.replaceReplanningStrategy(config,
				DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta,
				DefaultPlanStrategiesModule.DefaultSelector.BestScore
			);

			// Best score requires disabling consistency checking
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

			// Experiments are without time mutation
			removeStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);

			if (pruning > 0)
				imcConfig.setPruning("p" + pruning);


		} else if (pruning > 0) {
			throw new IllegalArgumentException("Pruning is only available with informed-mode-choice enabled");
		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		super.prepareScenario(scenario);

		// If bike speed is adjusted, we need to remove all bike routes and travel times
		// These times will be recalculated by the router
		if (bikeSpeedOffset != 0) {

			log.info("Adjusting bike speed by {} km/h", bikeSpeedOffset);

			VehicleType bike = scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.bike, VehicleType.class));
			bike.setMaximumVelocity(bike.getMaximumVelocity() + bikeSpeedOffset / 3.6);

			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					for (Leg leg : TripStructureUtils.getLegs(plan)) {
						if (leg.getMode().equals(TransportMode.bike)) {
							leg.setRoute(null);
							leg.setTravelTimeUndefined();
						}
					}
				}
			}
		}
	}


	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		if (bi) {
			log.info("Using balanced innovation strategy chooser");
			BalancedInnovationStrategyChooser.install(controler);
		}

		if (imc) {

			InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car", "pt")
				// Modes with fixed costs need to be considered separately
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
				.withTripEstimator(PtTripEstimator.class, ModeOptions.ConsiderYesAndNo.class, "pt")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "walk", "bike", "ride")
				.withPruner("p" + pruning, new PlanScoreThresholdPruner(pruning))
				.withConstraint(RelaxedMassConservationConstraint.class);

			if (actEst) {
				log.info("Including activity estimation into score");
				builder.withActivityEstimator(DefaultActivityEstimator.class);
			} else {
				log.info("No activity estimation for score");
			}

			if (ConfigUtils.hasModule(controler.getConfig(), AdvancedScoringConfigGroup.class)) {
				log.info("Using pseudo-random trip score estimator");
				builder.withTripScoreEstimator(PseudoRandomTripScoreEstimator.class);
			}

			controler.addOverridingModule(builder.build());
		}

	}
}
