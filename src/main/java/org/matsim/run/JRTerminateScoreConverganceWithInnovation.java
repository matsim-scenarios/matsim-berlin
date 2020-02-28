package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.*;
import scala.Int;

import javax.inject.Inject;
import java.lang.reflect.Array;
import java.util.*;

public class JRTerminateScoreConverganceWithInnovation implements TerminationCriterion {
        private static final double SCORE_PRECISION_4_SHUTDOWN = 0.5;
        private static final int MINIMUM_ITERATION = 5 ;
        private final int lastIteration;
        private Scenario scenario;
        private ScoreStats scoreStats ;
        private StrategyManager strategyManager ;
        private static int dynamicShutdownIteration;
        private static boolean dynamicShutdownInitiated = false ;
        private StrategyConfigGroup strategyConfigGroup ;
        private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ;
        private static final Logger log = Logger.getLogger(StrategyManager.class);
        private int nextDisableInnovativeStrategiesIteration;

    private static Map<String, Double[]> percentChangesMegaMap ;


    @Inject
    JRTerminateScoreConverganceWithInnovation(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, StrategyManager strategyManager,
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
        public boolean continueIterations(int iteration) {


            // Calc Percent Diff



            boolean modeChoiceCriteriaSatisfied = false;
            if (iteration > 5) {
                modeChoiceCriteriaSatisfied = checkModeChoice(iteration);
            }

            boolean scoreCoverageCriteriaSatisfied = false;
            if (iteration > 5) {

                scoreCoverageCriteriaSatisfied = checkScoreConvergance(iteration);
            }


            if (iteration > MINIMUM_ITERATION && modeChoiceCriteriaSatisfied && scoreCoverageCriteriaSatisfied && !dynamicShutdownInitiated) {

                shutdownInnovation(iteration);

            }

            return (iteration <= lastIteration && iteration <= dynamicShutdownIteration);
        }

        private void shutdownInnovation(int iteration) {
            dynamicShutdownIteration = (int) (iteration / strategyConfigGroup.getFractionOfIterationsToDisableInnovation()) + 3;
            nextDisableInnovativeStrategiesIteration = iteration + 1 ;

            // ihab help
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
            // ihab

//            for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
//                PlanStrategy strategy = entry.getValue();
//                StrategyConfigGroup.StrategySettings settings = entry.getKey();
////                strategyManager.addChangeRequest(iteration+1,strategy, settings.getSubpopulation(), 0.0);
//                if (!ReplanningUtils.isOnlySelector(strategy)) {
//                    strategyManager.changeWeightOfStrategy(strategy, settings.getSubpopulation(), 0.0);
//                }
//            }

            log.error("Innovation Shutdown at iteration " + (nextDisableInnovativeStrategiesIteration));
            log.error("Full Shutdown at iteration " + dynamicShutdownIteration);
            dynamicShutdownInitiated = true;
        }

        private boolean checkModeChoice(int iteration) {
            Map<Integer, Map<String, Map<Integer, Double>>> modeHistory = JRModeChoiceCoverageControlerListener.getModeHistory();
            Integer limit = 1 ;
            Map<String, Boolean> modeConvergesCheck = new HashMap<>();
            for (Map.Entry<String, Map<Integer, Double>> entry : modeHistory.get(limit).entrySet()) {
                String mode = entry.getKey();
                log.info("Mode choice coverage checked for " + mode);
                Map<Integer, Double> map = entry.getValue();



                int howManyIterationsInluded = 2;
                if (!map.containsKey(iteration - howManyIterationsInluded)) {
                    modeConvergesCheck.put(mode, false);
                    continue ;
                }

                List<Double> inputList = new ArrayList<>();
                for (int i = iteration - howManyIterationsInluded; i <= iteration; i++) {
                    inputList.add(map.get(i));
                }

                double pctChange = averagePercentChange(inputList) ;
                modeConvergesCheck.put(mode, pctChange <= SCORE_PRECISION_4_SHUTDOWN);
            }

            for (String mode : modeConvergesCheck.keySet()) {

                log.info("Mode choice coverage for " + mode + ": " + modeConvergesCheck.get(mode));
            }

            for (boolean bool : modeConvergesCheck.values()) {
                if (!bool) {
                    return false ;
                }
            }
            return true ;

//            Double shareForPt = modeHistory.get(new Integer("1")).get("pt").get(iteration-1);
//            System.out.println("share for pt " + shareForPt);
//            return shareForPt >= 0.2;

        }


        private boolean isInnovativeStrategy( GenericPlanStrategy<Plan, Person> strategy) {
            return ! ( ReplanningUtils.isOnlySelector( strategy ) );
        }

        private boolean checkScoreConvergance(int iteration){
            Map<Integer, Double> scoreHistory = scoreStats.getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average);

//            List<Double> lastFiveScorePctChanges = new ArrayList<>();
//            double comparisonScore = scoreHistory.get(iteration - 6);
//            for (int i = iteration -5; i < iteration; i++) {
//
//                Double currentScore = scoreHistory.get(i);
//                lastFiveScorePctChanges.add(Math.abs(currentScore - comparisonScore)/comparisonScore) ;
//                comparisonScore = currentScore ;
//            }
//            double pctChange = lastFiveScorePctChanges.stream().reduce(0., Double::sum)/lastFiveScorePctChanges.size();
//            double pctChange = Math.abs(scoreHistory.get(iteration - 1) - scoreHistory.get(iteration - 2)) / scoreHistory.get(iteration - 1);
            List<Double> inputList = new ArrayList<>();
            int numOfValues = 2;
            for (int i = numOfValues; i >= 1; i--) {

                int index = scoreHistory.size() - i;
                if (!scoreHistory.containsKey(index)) {
                    return  false ;
                }
                inputList.add(scoreHistory.get(index));

            }
            double pctChange = averagePercentChange(inputList) ;
            log.info("average percent change: " + pctChange);

            return (pctChange < SCORE_PRECISION_4_SHUTDOWN);

        }

        private double averagePercentChange(List<Double> list) {

            List<Double> pctChanges = new ArrayList<>();
            double val1 = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                double val2 = list.get(i);
                pctChanges.add(Math.abs(val1 - val2)/val1) ;
                val1 = val2 ;
            }
            return pctChanges.stream().reduce(0., Double::sum)/pctChanges.size();
        }

    }

