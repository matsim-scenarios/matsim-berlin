package org.matsim.run.dynamicShutdown;

import org.apache.log4j.Logger;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningUtils;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.utils.io.IOUtils;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * When user-specified criteria are met, initiates dynamic shutdown: 1) Turns off innovation in next iteration and
 * 2) informs TerminateDynamically Module of the iteration at which MATSim should shut down. \
 *
 * The criteria to used must be user specified (i.e. score convergence), as well as the smoothing method and associated
 * parameters.
 *
 * @author jakobrehmann
 */

public class ConvergenceDynamicShutdownImpl implements IterationStartsListener, StartupListener, ShutdownListener {

    // Dynamic Shutdown Config Group
    private static final int MINIMUM_ITERATION = 10000;

    // Score Config Group
    private static final boolean SCORE_CONDITION_ACTIVE = true;
    private static final double SCORE_CONDITION_THRESHOLD = 0.5;
    private static final int SCORE_CONDITION_SMOOTHING_INTERVAL = 50 ;

    // Mode Convergence Config Group
    private static final boolean MODE_CONDITION_ACTIVE = true;
    private static final double MODE_CONDITION_THRESHOLD = 0.0005;
    private static final int MODE_CONDITION_SMOOTHING_INTERVAL = 50 ;

    // Mode Choice Coverage Config Group
    private static final boolean MODECHOICECOVERAGE_CONDITION_ACTIVE = true;
    private static final double MODECHOICECOVERAGE_CONDITION_THRESHOLD = 0.0005;
    private static final int MODECHOICECOVERAGE_CONDITION_SMOOTHING_INTERVAL = 50 ;


    private int lastIteration;
    private double fractionOfIterationsToDisableInnovation;
    private final ControlerConfigGroup controlerConfigGroup;
    private final OutputDirectoryHierarchy controlerIO;
    private Scenario scenario;
    private ScoreStats scoreStats;
    private ModeStatsControlerListener modeStats;
    ModeStatsControlerListener modeStatsControlerListener;

    private StrategyManager strategyManager;
    private static int dynamicShutdownIteration;
    private static boolean dynamicShutdownInitiated;
    private StrategyConfigGroup strategyConfigGroup;
    private static final Logger log = Logger.getLogger(StrategyManager.class);


    private static Map<String, List<Double>> slopesScore = new HashMap<>();
    private static Map<String, List<Double>> pctChangesForMode = new HashMap<>();
    private static Map<String, List<Double>> slopesModeChoiceCoverage = new HashMap<>();

    private int itersInZoneToConverge = 50;
    private double convergenceTheshold = 0.0001 ;


    @Inject
    ConvergenceDynamicShutdownImpl(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats, ModeStatsControlerListener modeStatsControlerListener, StrategyManager strategyManager,
                                   StrategyConfigGroup strategyConfigGroup, Scenario scenario, OutputDirectoryHierarchy controlerIO) {

        this.scenario = scenario;
        this.scoreStats = scoreStats;
        this.strategyManager = strategyManager;
        this.strategyConfigGroup = strategyConfigGroup;
        this.controlerConfigGroup = controlerConfigGroup ;
        this.controlerIO = controlerIO ;
        this.modeStatsControlerListener = modeStatsControlerListener ;
    }

    public static int getDynamicShutdownIteration() {
        return dynamicShutdownIteration;
    }

    public static boolean isDynamicShutdownInitiated() {
        return dynamicShutdownInitiated;
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        dynamicShutdownInitiated = false ;
        dynamicShutdownIteration = controlerConfigGroup.getLastIteration();

        fractionOfIterationsToDisableInnovation = strategyConfigGroup.getFractionOfIterationsToDisableInnovation();
        lastIteration = scenario.getConfig().controler().getLastIteration();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        int iteration = iterationStartsEvent.getIteration();

        // Checks 1:  at least one shutdown criteria is being active. Otherwise this module is superfluous.
        if (!MODECHOICECOVERAGE_CONDITION_ACTIVE && !SCORE_CONDITION_ACTIVE && !MODE_CONDITION_ACTIVE) {
            log.warn("dynamic shutdown should not be used if no criteria are specified. " +
                    "Therefore, the standard shutdown iteration will be used");
            return;
        }


        // Check 2: checks whether dynamic shutdown was already initiated.
        if (dynamicShutdownInitiated) {
            log.info("dynamic shutdown was previously initiated");
            return;
        }

        // jr temporary:
        bestFitLineModeChoiceCoverage(iteration);
        bestFitLineScore(iteration);
//        pctChangeForMode(iteration);
//
        // Check 3: checks whether the minimum iteration was reached ;
        if (iteration < MINIMUM_ITERATION) {
            return ;
        }


//        // Check 4: check whether innovation shutdown has already occured
//        if (iteration >= fractionOfIterationsToDisableInnovation * lastIteration) {
//            return;
//        }

        // Check 5: returns if the mode choice coverage criteria has not yet been met (assuming the criteria is active)
        //     step A: Add newest percent difference to the pctDifference ArrayList
        //     step B: Check Last x percent changes, to see if innovation shutdown should be initiated
        if (MODECHOICECOVERAGE_CONDITION_ACTIVE) {
            bestFitLineModeChoiceCoverage(iteration);
            for (String mode : slopesModeChoiceCoverage.keySet()) {
                log.info("Checking mode choice coverage convergence for " + mode);
                List<Double> slopes = slopesModeChoiceCoverage.get(mode);
                if (didntConverge(slopes, 0.0001)) return;
            }
        }

        // Check 6: returns if the score criteria has not yet been met (assuming the criteria is active)
        if (SCORE_CONDITION_ACTIVE) {
            bestFitLineScore(iteration);
            for (String scoreItem : slopesScore.keySet()) {
                log.info("Checking score convergence for " + scoreItem);
                List<Double> slopes = slopesModeChoiceCoverage.get(scoreItem);
                if (didntConverge(slopes, 0.001)) return;
            }
        }

        // Mode Convergence - work in progress
        if (MODE_CONDITION_ACTIVE) {
//            pctChangeForMode(iteration);
//            if (!checkModeCriteria(MODE_CONDITION_SMOOTHING_INTERVAL)) {
//                return;
//            }
        }

        // FINALLY: if none of the previous checks terminated the process, then dynamic shutdown can be initiated.
        shutdownInnovation(iteration);
    }

    private boolean didntConverge(List<Double> slopes, double threshold) {

        int startIteration = slopes.size() - itersInZoneToConverge - 1;

        if (startIteration < 0) {
            log.info("Not enough slopes computed to check for convergence");
            return true;
        }

        for (int i = startIteration; i < slopes.size()-1; i++) {
            if (slopes.get(i) < -1 * threshold || slopes.get(i) > threshold) {
                return true;
            }
        }
        return false;
    }


    private double computeLineSlope(Map<Integer,Double> inputMap) {

        double pctConsidered = 0.25;

        int currentIter = Collections.max(inputMap.keySet());
        int startIteration = (int) (1 - pctConsidered) * currentIter;

        ArrayList<Integer> iters = new ArrayList<>();
        ArrayList<Double> vals = new ArrayList<>();
        for (Integer it : inputMap.keySet()) {
            if (it >= startIteration) {
                iters.add(it);
                vals.add(inputMap.get(it));
            }
        }


        int size = iters.size();
        double[] x = new double[size];
        double[] y = new double[size];

        for (int i = 0; i < size; i++) {
            x[i] = (double) iters.get(i);
            y[i] = vals.get(i);
        }
        LinearRegression linearRegression = new LinearRegression(x, y);
        return linearRegression.slope();

    }

    private void bestFitLineModeChoiceCoverage(int iteration) {
        Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory;

        if (iteration < 50) {
            return;
        }

        modeChoiceCoverageHistory = ModeChoiceCoverageControlerListener.getModeHistory();

        Integer limit = 1;

        for (Map.Entry<String, Map<Integer, Double>> entry : modeChoiceCoverageHistory.get(limit).entrySet()) {
            String mode = entry.getKey();
            log.info("Mode choice coverage checked for " + mode);

            double slope = computeLineSlope(entry.getValue());

            List<Double> slopesForMode = slopesModeChoiceCoverage.computeIfAbsent(mode, v -> new ArrayList<>());
            slopesForMode.add(slope);
        }

    }


    private void bestFitLineScore(int iteration) {

        if (iteration < 50) {
            return;
        }

        for (Map.Entry<ScoreStatsControlerListener.ScoreItem, Map<Integer, Double>> entry : scoreStats.getScoreHistory().entrySet()) {
            String scoreItem = entry.getKey().toString();
            log.info("Score checked for " + scoreItem);

            double slope = computeLineSlope(entry.getValue());

            List<Double> slopesForScoreItem = slopesScore.computeIfAbsent(scoreItem, v -> new ArrayList<>());
            slopesForScoreItem.add(slope);
        }
    }

    private void shutdownInnovation(int iteration) {
        dynamicShutdownIteration = (int) (iteration / fractionOfIterationsToDisableInnovation) + 2; // jr review
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

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {


        // mode choice coverage
        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("PercentDifferencesModeChoiceCoverage.txt"))){
            for (String mode : slopesModeChoiceCoverage.keySet()) {
                bw.write("\n" + mode+" ; ");
                for (Double pct : slopesModeChoiceCoverage.get(mode)) {
                    bw.write(pct + " ; ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // score
        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("PercentDifferencesScore.txt"))){
            for (String scoreStat : slopesScore.keySet()) {
                bw.write("\n"+scoreStat+" ; ");
                for (Double pct : slopesScore.get(scoreStat)) {
                    bw.write(pct + " ; ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // mode
//        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("PercentDifferencesMode.txt"))){
//            for (String mode : pctChangesForMode.keySet()) {
//                bw.write("\n"+mode+" ; ");
//                for (Double pct : pctChangesForMode.get(mode)) {
//                    bw.write(pct + " ; ");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}

