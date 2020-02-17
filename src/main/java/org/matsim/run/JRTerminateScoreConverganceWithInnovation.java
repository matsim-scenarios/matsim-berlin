package org.matsim.run;

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
import java.util.Map;

    public class JRTerminateScoreConverganceWithInnovation implements TerminationCriterion {
        // Based on TerminateAtFixedIterationNumber

        private static final double SCORE_PRECISION_4_SHUTDOWN = 0.00005;
        private static final int MINIMUM_ITERATION = 2 ;
        private final int lastIteration;
        private ScoreStats scoreStats ;
        private StrategyManager strategyManager ;
        //    ControlerConfigGroup controlerConfigGroup ;
//    PlanCalcScoreConfigGroup planCalcScoreConfigGroup ;
        private StrategyConfigGroup strategyConfigGroup ;
        Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ;


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

        }

        @Override
        public boolean continueIterations(int iteration) {
            boolean dynamicShutdownInitiated = false ;
            if (iteration > MINIMUM_ITERATION & !dynamicShutdownInitiated) {

                Map<Integer, Double> scoreHistory = scoreStats.getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average);
                double currentScore = scoreHistory.get(iteration - 1);
                double previousScore = scoreHistory.get(iteration - 2);
                double pctChange = Math.abs(currentScore - previousScore) / previousScore;

                if (pctChange < SCORE_PRECISION_4_SHUTDOWN) {
                    System.err.println("Dynamic Shutdown initiated to following Iteration, " + iteration);
                    int newShutdownIteration = (int) (iteration / strategyConfigGroup.getFractionOfIterationsToDisableInnovation()) + 1;
                    for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {

                        for (GenericPlanStrategy strategy : strategyManager.getStrategies(null)) {
                            strategyManager.addChangeRequest(iteration + 1, (PlanStrategy) strategy, null, 0.0);
                        }
                        dynamicShutdownInitiated = true;
                        return false;
                    }


                }
                System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& percent change:" + pctChange);
//            planCalcScoreConfigGroup.subp



            }

            return (iteration <= lastIteration);
        }

    }

