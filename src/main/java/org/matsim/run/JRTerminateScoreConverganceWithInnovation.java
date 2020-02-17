package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

    public class JRTerminateScoreConverganceWithInnovation implements TerminationCriterion {
        // Based on TerminateAtFixedIterationNumber

        private static final double SCORE_PRECISION_4_SHUTDOWN = 0.50;
        private static final int MINIMUM_ITERATION = 10 ;
        private final int lastIteration;
        private ScoreStats scoreStats ;
        private StrategyManager strategyManager ;
        private static int dynamicShutdownIteration;
        //    ControlerConfigGroup controlerConfigGroup ;
//    PlanCalcScoreConfigGroup planCalcScoreConfigGroup ;
        private StrategyConfigGroup strategyConfigGroup ;
        Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ;
        private static final Logger log = Logger.getLogger(StrategyManager.class);


        @Inject
        JRTerminateScoreConverganceWithInnovation(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, StrategyManager strategyManager,
                                    PlanCalcScoreConfigGroup planCalcScoreConfigGroup, StrategyConfigGroup strategyConfigGroup, Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ) {
            this.lastIteration = controlerConfigGroup.getLastIteration();
            this.scoreStats = scoreStats ;
            this.strategyManager = strategyManager ;
//        this.controlerConfigGroup = controlerConfigGroup ;
//        this.planCalcScoreConfigGroup = planCalcScoreConfigGroup;
            this.strategyConfigGroup = strategyConfigGroup ;
            this.planStrategies = planStrategies ;

            if (dynamicShutdownIteration == 0) {
                dynamicShutdownIteration = lastIteration ;
            }


        }

        @Override
        public boolean continueIterations(int iteration) {
            boolean dynamicShutdownInitiated = (dynamicShutdownIteration != lastIteration);

            if (iteration > MINIMUM_ITERATION & !dynamicShutdownInitiated) {

                Map<Integer, Double> scoreHistory = scoreStats.getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average);
                List<Double> lastFiveScorePctChanges = new ArrayList<>();
                double comparisonScore = scoreHistory.get(iteration - 6);
                for (int i = iteration -5; i < iteration; i++) {

                    Double currentScore = scoreHistory.get(i);
                    lastFiveScorePctChanges.add(Math.abs(currentScore - comparisonScore)/comparisonScore) ;
                    comparisonScore = currentScore ;
                }
                double pctChange = lastFiveScorePctChanges.stream().reduce(0., Double::sum)/lastFiveScorePctChanges.size();


//                double currentScore = scoreHistory.get(iteration - 1);
//                double previousScore = scoreHistory.get(iteration - 2);
//                double pctChange = Math.abs(currentScore - previousScore) / previousScore;
               log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& percent change:" + pctChange);

                if (pctChange < SCORE_PRECISION_4_SHUTDOWN) {

                    dynamicShutdownIteration = (int) (iteration / strategyConfigGroup.getFractionOfIterationsToDisableInnovation()) + 1;
                    for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
                        PlanStrategy strategy = entry.getValue();
                        StrategyConfigGroup.StrategySettings settings = entry.getKey();
                        strategyManager.addChangeRequest(iteration + 1, strategy, settings.getSubpopulation(), 0.0);
                    }

                    log.error("Innovation Shutdown at , " + (iteration + 1));
                    log.error("Full Shutdown at: " + dynamicShutdownIteration);
                    dynamicShutdownInitiated = true;
                }

            }

            return (iteration <= lastIteration && iteration <= dynamicShutdownIteration);
        }

    }

