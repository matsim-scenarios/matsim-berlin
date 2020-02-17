package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

    public class JRTerminateScoreConverganceWithInnovation implements TerminationCriterion {
        private static final double SCORE_PRECISION_4_SHUTDOWN = 0.50;
        private static final int MINIMUM_ITERATION = 10 ;
        private final int lastIteration;
        private ScoreStats scoreStats ;
        private StrategyManager strategyManager ;
        private static int dynamicShutdownIteration;
        private static boolean dynamicShutdownInitiated = false ;
        private StrategyConfigGroup strategyConfigGroup ;
        Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ;
        private static final Logger log = Logger.getLogger(StrategyManager.class);


        @Inject
        JRTerminateScoreConverganceWithInnovation(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, StrategyManager strategyManager,
                StrategyConfigGroup strategyConfigGroup, Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies ) {
            this.lastIteration = controlerConfigGroup.getLastIteration();

            this.scoreStats = scoreStats ;
            this.strategyManager = strategyManager ;
            this.strategyConfigGroup = strategyConfigGroup ;
            this.planStrategies = planStrategies ;

            if (dynamicShutdownIteration == 0) {
                dynamicShutdownIteration = lastIteration ;
            }


        }

        @Override
        public boolean continueIterations(int iteration) {

            boolean modeChoiceCriteriaSatisfied = false;
            if (iteration > 2) {
                modeChoiceCriteriaSatisfied = checkModeChoice(iteration);
            }

            boolean scoreCoverageCriteriaSatisfied = false;
            if (iteration > 7) {

                scoreCoverageCriteriaSatisfied = checkScoreConvergance(iteration);
            }





            System.out.println("Hello, here is the Terminator");

            if (iteration > MINIMUM_ITERATION && modeChoiceCriteriaSatisfied && scoreCoverageCriteriaSatisfied && !dynamicShutdownInitiated) {

                shutdownInnovation(iteration);

            }

            return (iteration <= lastIteration && iteration <= dynamicShutdownIteration);
        }

        private void shutdownInnovation(int iteration) {
            dynamicShutdownIteration = (int) (iteration / strategyConfigGroup.getFractionOfIterationsToDisableInnovation()) + 1;
            for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
                PlanStrategy strategy = entry.getValue();
                StrategyConfigGroup.StrategySettings settings = entry.getKey();
                strategyManager.addChangeRequest(iteration,strategy, settings.getSubpopulation(), 0.0);
            }

            log.error("Innovation Shutdown at iteration " + (iteration));
            log.error("Full Shutdown at iteration " + dynamicShutdownIteration);
            dynamicShutdownInitiated = true;
        }

        private boolean checkModeChoice(int iteration) {
            Map<Integer, Map<String, Map<Integer, Double>>> modeHistory = JRModeChoiceCoverageControlerListener.getModeHistory();
            Double shareForPt = modeHistory.get(new Integer("1")).get("pt").get(iteration-1);

            System.out.println("share for pt " + shareForPt);

            return shareForPt >= 0.2;

        }

        private boolean checkScoreConvergance(int iteration){
            Map<Integer, Double> scoreHistory = scoreStats.getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average);
            List<Double> lastFiveScorePctChanges = new ArrayList<>();
            double comparisonScore = scoreHistory.get(iteration - 6);
            for (int i = iteration -5; i < iteration; i++) {

                Double currentScore = scoreHistory.get(i);
                lastFiveScorePctChanges.add(Math.abs(currentScore - comparisonScore)/comparisonScore) ;
                comparisonScore = currentScore ;
            }
            double pctChange = lastFiveScorePctChanges.stream().reduce(0., Double::sum)/lastFiveScorePctChanges.size();

            log.info("average percent change over last 5 iterations: " + pctChange);

            return (pctChange < SCORE_PRECISION_4_SHUTDOWN);

        }

    }

