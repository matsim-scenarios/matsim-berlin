package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.*;

import javax.inject.Inject;
import java.util.*;

public class JRDynamicShutdownControlerListener implements StartupListener {
        private static final double SCORE_PRECISION_4_SHUTDOWN = 0.5;
        private static final int MINIMUM_ITERATION = 5 ;
        private final int lastIteration;
        private Scenario scenario;
        private ScoreStats scoreStats ;
        private StrategyManager strategyManager ;
        static int dynamicShutdownIteration;
        static boolean dynamicShutdownInitiated = false ;
        private StrategyConfigGroup strategyConfigGroup ;
        private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ;
        private static final Logger log = Logger.getLogger(StrategyManager.class);
        private int nextDisableInnovativeStrategiesIteration;

    private static Map<String, List<Double>> pctChangesForModeShare = new HashMap<>();
    private static Map<String, List<Double>> pctChangesForScore = new HashMap<>();



    @Inject
    JRDynamicShutdownControlerListener(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, StrategyManager strategyManager,
                                       StrategyConfigGroup strategyConfigGroup, Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies, Scenario scenario) {

        this.lastIteration = controlerConfigGroup.getLastIteration();
        this.scenario = scenario;
        this.scoreStats = scoreStats;
        this.strategyManager = strategyManager;
        this.strategyConfigGroup = strategyConfigGroup;
        this.planStrategies = planStrategies;


        if (dynamicShutdownIteration == 0) {
            dynamicShutdownIteration = lastIteration;
        }


    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {

        int iteration = startupEvent.getIteration();

        // Step 1: Add newest percent difference to the pctDifference ArrayList
        pctChangeForModeChoice(iteration);
        for (String mode : pctChangesForModeShare.keySet()) {
            System.out.println(mode + " *********************************");
            for (Double pc : pctChangesForModeShare.get(mode)) {
                System.out.println(pc);
            }
        }


        pctChangeForScore(iteration);

        // Step 2: Check Last x percent changes, to see if innovation shutdown should be initiated

        boolean modeChoiceCriteriaSatisfied = checkModeChoiceCriteria(2);;
        boolean scoreCoverageCriteriaSatisfied = checkScoreCriteria(2);;


        if (iteration > MINIMUM_ITERATION && modeChoiceCriteriaSatisfied  && scoreCoverageCriteriaSatisfied && !dynamicShutdownInitiated) {

            shutdownInnovation(iteration);

        }
    }


    private void pctChangeForModeChoice(int iteration) {
        Map<Integer, Map<String, Map<Integer, Double>>> modeHistory ;
        try {

            modeHistory = JRModeChoiceCoverageControlerListener.getModeHistory();

            Integer limit = 1 ;

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

            double avgPctChng = averagePercentChange(pctChangesForModeShare.get(mode),dx);
            averageModeChoiceCoveragePctChanges.put(mode, avgPctChng);
            System.out.println("average percent change (dx=1) for mode " + mode + " = " + avgPctChng + ", satisfies precision threshhold: " + (avgPctChng <= SCORE_PRECISION_4_SHUTDOWN));

        }


        // Returns false if at least one mode is above threshhold
        for (Double pc : averageModeChoiceCoveragePctChanges.values()) {
            if (pc > SCORE_PRECISION_4_SHUTDOWN) {
                return false ;
            }
        }

        return true ;
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

            double avgPctChng = averagePercentChange(pctChangesForScore.get(scoreItem),dx);
            averageScorePctChanges.put(scoreItem, avgPctChng);
            System.out.println("average percent change (dx=1) for score item " + scoreItem + " = " + avgPctChng + ", satisfies precision threshhold: " + (avgPctChng <= SCORE_PRECISION_4_SHUTDOWN));

        }


        // Returns false if at least one mode is above threshhold
        for (Double pc : averageScorePctChanges.values()) {
            if (pc > SCORE_PRECISION_4_SHUTDOWN) {
                return false ;
            }
        }

        return true ;
    }

    private void shutdownInnovation(int iteration) {
            dynamicShutdownIteration = (int) (iteration / strategyConfigGroup.getFractionOfIterationsToDisableInnovation()) + 3; // jr review
            nextDisableInnovativeStrategiesIteration = iteration + 1 ; // jr review


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
                        strategyManager.addChangeRequest(this.nextDisableInnovativeStrategiesIteration, planStrategyImpl, subpopulation, 0.);

                    }
                }
            }

            log.error("Innovation Shutdown at iteration " + (nextDisableInnovativeStrategiesIteration));
            log.error("Full Shutdown at iteration " + dynamicShutdownIteration);
            dynamicShutdownInitiated = true;
        }



        private boolean isInnovativeStrategy( GenericPlanStrategy<Plan, Person> strategy) {
            return ! ( ReplanningUtils.isOnlySelector( strategy ) );
        }


        private double averagePercentChange(List<Double> list, int dx) {

            if (dx > list.size()) {
                return 1000.00; // big number
            }

            double avgPctChange = 0.;

            for (int i = list.size() - 1; i >= list.size()-dx; i--) {
                avgPctChange += list.get(i);
            }

            return avgPctChange / dx;

        }


}

