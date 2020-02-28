package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.*;

import javax.inject.Inject;
import java.util.*;

public class JRDynamicShutdownControlerListener implements IterationStartsListener, StartupListener {
    private static final double SHUTDOWN_CONDITION_SCORE = 0.5;
    private static final double SHUTDOWN_CONDITION_MODECHOICECOVERAGE = 0.5;
    private static final int MINIMUM_ITERATION = 3;
    private final ControlerConfigGroup controlerConfigGroup;
    private Scenario scenario;
    private ScoreStats scoreStats;
    private StrategyManager strategyManager;
    private static int dynamicShutdownIteration;
    private static boolean dynamicShutdownInitiated;
    private StrategyConfigGroup strategyConfigGroup;
    private static final Logger log = Logger.getLogger(StrategyManager.class);

    private static Map<String, List<Double>> pctChangesForModeShare = new HashMap<>();
    private static Map<String, List<Double>> pctChangesForScore = new HashMap<>();


    @Inject
    JRDynamicShutdownControlerListener(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, StrategyManager strategyManager,
                                       StrategyConfigGroup strategyConfigGroup, Scenario scenario) {

        this.scenario = scenario;
        this.scoreStats = scoreStats;
        this.strategyManager = strategyManager;
        this.strategyConfigGroup = strategyConfigGroup;
        this.controlerConfigGroup = controlerConfigGroup ;


    }

    static int getDynamicShutdownIteration() {
        return dynamicShutdownIteration;
    }

    static boolean isDynamicShutdownInitiated() {
        return dynamicShutdownInitiated;
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        dynamicShutdownInitiated = false ;
        dynamicShutdownIteration = controlerConfigGroup.getLastIteration();

    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {

        if (dynamicShutdownInitiated) {
            log.info("dynamic shutdown was already initiated; therefore, the shutdown conditions no longer need to be evaluated");
            return;
        }

        int iteration = iterationStartsEvent.getIteration();

        // Step 1: Add newest percent difference to the pctDifference ArrayList
        pctChangeForModeChoice(iteration);
        pctChangeForScore(iteration);

        // Step 2: Check Last x percent changes, to see if innovation shutdown should be initiated

        boolean modeChoiceCriteriaSatisfied = checkModeChoiceCriteria(2);

        boolean scoreCoverageCriteriaSatisfied = checkScoreCriteria(2);


        // Step 3: turn off innovation and set new shutdown iteration if all criteria are met

        if (iteration > MINIMUM_ITERATION && modeChoiceCriteriaSatisfied && scoreCoverageCriteriaSatisfied) {

            shutdownInnovation(iteration);

        }
    }


    private void pctChangeForModeChoice(int iteration) {
        Map<Integer, Map<String, Map<Integer, Double>>> modeHistory;
        try {

            modeHistory = JRModeChoiceCoverageControlerListener.getModeHistory();

            Integer limit = 1;

            for (Map.Entry<String, Map<Integer, Double>> entry : modeHistory.get(limit).entrySet()) {
                String mode = entry.getKey();
                log.info("Mode choice coverage checked for " + mode);
                Map<Integer, Double> map = entry.getValue();

                Double val1 = map.get(iteration - 2);
                System.out.println(val1);

                Double val2 = map.get(iteration - 1);
                System.out.println(val2);

                Double pctChange = Math.abs((val1 - val2) / val2);
                List<Double> pctChangeForMode = pctChangesForModeShare.computeIfAbsent(mode, v -> new ArrayList<>());
                pctChangeForMode.add(pctChange);
            }
        } catch (NullPointerException e) {
            log.error("Too early to find percent change");
        }

    }


    private boolean checkModeChoiceCriteria(int dx) {

        // Calculates Average Percent Change for all modes
        Map<String, Double> averageModeChoiceCoveragePctChanges = new HashMap<>();
        for (String mode : pctChangesForModeShare.keySet()) {


            List<Double> pctChangesForMode = pctChangesForModeShare.get(mode);
            if (dx > pctChangesForMode.size()) {
                log.warn("An average percent change over cannot yet be found, since dx is too high.");
                return false;
            }
            double avgPctChng = averagePercentChange(pctChangesForMode, dx);
            averageModeChoiceCoveragePctChanges.put(mode, avgPctChng);
            System.out.println("average percent change (dx=1) for mode " + mode + " = " + avgPctChng + ", satisfies precision threshhold: " + (avgPctChng <= SHUTDOWN_CONDITION_MODECHOICECOVERAGE));

        }


        // Returns false if at least one mode is above threshhold
        for (Double pc : averageModeChoiceCoveragePctChanges.values()) {
            if (pc > SHUTDOWN_CONDITION_MODECHOICECOVERAGE) {
                return false;
            }
        }

        return true;
    }

    private void pctChangeForScore(int iteration) {
        try {
            for (Map.Entry<ScoreStatsControlerListener.ScoreItem, Map<Integer, Double>> entry : scoreStats.getScoreHistory().entrySet()) {
                String scoreItem = entry.getKey().toString();
                log.info("Score Percent Change  checked for " + scoreItem);
                Map<Integer, Double> map = entry.getValue();

                Double val1 = map.get(iteration - 2);
                System.out.println(val1);

                Double val2 = map.get(iteration - 1);
                System.out.println(val2);

                Double pctChange = Math.abs((val1 - val2) / val2);
                List<Double> pctChangeForScoreItem = pctChangesForScore.computeIfAbsent(scoreItem, v -> new ArrayList<>());
                pctChangeForScoreItem.add(pctChange);
            }
        } catch (NullPointerException e) {
            log.error("Too early to find percent change (score stats)");
        }
    }

    private boolean checkScoreCriteria(int dx) {

        // Calculates Average Percent Change for all score items
        Map<String, Double> averageScorePctChanges = new HashMap<>();
        for (String scoreItem : pctChangesForScore.keySet()) {

            double avgPctChng = averagePercentChange(pctChangesForScore.get(scoreItem), dx);
            averageScorePctChanges.put(scoreItem, avgPctChng);
            System.out.println("average percent change (dx=1) for score item " + scoreItem + " = " + avgPctChng + ", satisfies precision threshhold: " + (avgPctChng <= SHUTDOWN_CONDITION_SCORE));

        }


        // Returns false if at least one mode is above threshhold
        for (Double pc : averageScorePctChanges.values()) {
            if (pc > SHUTDOWN_CONDITION_SCORE) {
                return false;
            }
        }

        return true;
    }

    private double averagePercentChange(List<Double> list, int dx) {

        if (dx > list.size()) { // jr: should no longer be necessary.
            return 1000.00; // big number
        }

        double avgPctChange = 0.;

        for (int i = list.size() - 1; i >= list.size() - dx; i--) {
            avgPctChange += list.get(i);
        }

        return avgPctChange / dx;

    }

    private void shutdownInnovation(int iteration) {
        dynamicShutdownIteration = (int) (iteration / strategyConfigGroup.getFractionOfIterationsToDisableInnovation()) + 2; // jr review
        int nextDisableInnovativeStrategiesIteration = iteration + 1; // jr review


        Set<String> subpopulations = new HashSet<>();
        for (StrategyConfigGroup.StrategySettings setting : this.scenario.getConfig().strategy().getStrategySettings()) {
            subpopulations.add(setting.getSubpopulation());
            if (subpopulations.size() == 0) subpopulations.add(null);
        }

        for (String subpopulation : subpopulations) {
            for (GenericPlanStrategy<Plan, Person> planStrategy : strategyManager.getStrategies(subpopulation)) {
                PlanStrategyImpl planStrategyImpl = (PlanStrategyImpl) planStrategy;
                if (isInnovativeStrategy(planStrategyImpl)) {
//                        log.info("Setting weight for " + planStrategyImpl.toString() + " (subpopulation " + subpopulation + ") to 0.");
                    strategyManager.addChangeRequest(nextDisableInnovativeStrategiesIteration, planStrategyImpl, subpopulation, 0.);

                }
            }
        }

        log.error("Innovation Shutdown at iteration " + (nextDisableInnovativeStrategiesIteration));
        log.error("Full Shutdown at iteration " + dynamicShutdownIteration);
        dynamicShutdownInitiated = true;
    }


    private boolean isInnovativeStrategy(GenericPlanStrategy<Plan, Person> strategy) {
        return !(ReplanningUtils.isOnlySelector(strategy));
    }

}

