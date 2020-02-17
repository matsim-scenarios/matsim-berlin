package org.matsim.run;

import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.StrategyManager;

import javax.inject.Inject;
import java.util.Map;

public class JRTerminateScoreConvergance implements TerminationCriterion {
    // Based on TerminateAtFixedIterationNumber

    private static final double SCORE_PRECISION_4_SHUTDOWN = 0.00005;
    private static final int MINIMUM_ITERATION = 2 ;
    private final int lastIteration;
    private ScoreStats scoreStats ;


    @Inject
    JRTerminateScoreConvergance(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, StrategyManager strategyManager) {
        this.lastIteration = controlerConfigGroup.getLastIteration();
        this.scoreStats = scoreStats ;

    }

    @Override
    public boolean continueIterations(int iteration) {
        if (iteration > MINIMUM_ITERATION ) {

            Map<Integer, Double> scoreHistory = scoreStats.getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average);
            double currentScore = scoreHistory.get(iteration - 1);
            double previousScore = scoreHistory.get(iteration - 2);
            double pctChange = Math.abs(currentScore - previousScore) / previousScore;
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& percent change:" + pctChange);
            if (pctChange < SCORE_PRECISION_4_SHUTDOWN) {
                System.err.println("Dynamic Shutdown initiated to following Iteration, " + iteration);
                return false;
            }
        }

        return (iteration <= lastIteration);
    }

}

