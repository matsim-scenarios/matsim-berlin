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
    private static final int MINIMUM_ITERATION = 0; // 500
    private static final int ITERATION_TO_START_FINDING_SLOPES = 5;
    private static final int MINIMUM_WINDOW_SIZE = 5;
    private static final boolean EXPANDING_WINDOW = true;
    private static final double EXPANDING_WINDOW_PCT_RETENTION = 0.25;
    private int ITERATIONS_IN_ZONE_TO_CONVERGE = 50;



    // Score Config Group
    private static final boolean SCORE_CONDITION_ACTIVE = true;
    private static final double SCORE_CONDITION_THRESHOLD = 1. ;//0.001; //TODO: Change back to prev value


    // Mode Convergence Config Group
    private static final boolean MODE_CONDITION_ACTIVE = true;
    private static final double MODE_CONDITION_THRESHOLD = 0.00003;


    // Mode Choice Coverage Config Group
    private static final boolean MODECHOICECOVERAGE_CONDITION_ACTIVE = true;
    private static final double MODECHOICECOVERAGE_CONDITION_THRESHOLD = 0.0001;





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


    private static Map<String, Map<Integer,Double>> slopesScore = new HashMap<>();
    private static Map<String, Map<Integer,Double>> slopesMode = new HashMap<>();
    private static Map<String, Map<Integer,Double>> slopesModeChoiceCoverage = new HashMap<>();

    private static Map<String, Map<Integer, Boolean>> xxx = new HashMap<>();


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
//        bestFitLineModeChoiceCoverage(iteration);
//        bestFitLineScore(iteration);
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


        Map<Integer, Boolean> xxxItBoolTOTAL = xxx.computeIfAbsent("TOTAL" , v -> new HashMap<>()); //tmp
        xxxItBoolTOTAL.put(iteration,false); //tmp


        // Check 5: returns if the mode choice coverage criteria has not yet been met (assuming the criteria is active)
        //     step A: Add newest percent difference to the pctDifference ArrayList
        //     step B: Check Last x percent changes, to see if innovation shutdown should be initiated
        if (MODECHOICECOVERAGE_CONDITION_ACTIVE) {
            bestFitLineModeChoiceCoverage(iteration);
            for (String mode : slopesModeChoiceCoverage.keySet()) {
                Map<Integer, Boolean> xxxItBool = xxx.computeIfAbsent("mcc_" + mode, v -> new HashMap<>()); //tmp
                xxxItBool.put(iteration,false); //tmp
                log.info("Checking mode choice coverage convergence for " + mode);
                List<Double> slopes = new ArrayList<>(slopesModeChoiceCoverage.get(mode).values());
                if (didntConverge(slopes, MODECHOICECOVERAGE_CONDITION_THRESHOLD)) return;
                xxxItBool.put(iteration, true); //tmp
            }
        }

        // Check 6: returns if the score criteria has not yet been met (assuming the criteria is active)
        if (SCORE_CONDITION_ACTIVE) {
            bestFitLineScore(iteration);
            for (String scoreItem : slopesScore.keySet()) {
                Map<Integer, Boolean> xxxItBool = xxx.computeIfAbsent("score_" + scoreItem, v -> new HashMap<>()); //tmp
                xxxItBool.put(iteration,false); //tmp
                log.info("Checking score convergence for " + scoreItem);
                List<Double> slopes = new ArrayList<>(slopesScore.get(scoreItem).values());
                if (didntConverge(slopes, SCORE_CONDITION_THRESHOLD)) return;
                xxxItBool.put(iteration, true); //tmp
            }
        }

        // Mode Convergence
        if (MODE_CONDITION_ACTIVE) {
            bestFitLineMode(iteration);
            for (String mode : slopesMode.keySet()) {
                Map<Integer, Boolean> xxxItBool = xxx.computeIfAbsent("mode_" + mode, v -> new HashMap<>()); //tmp
                xxxItBool.put(iteration,false); //tmp
                log.info("Checking mode convergence for " + mode);
                List<Double> slopes = new ArrayList<>(slopesModeChoiceCoverage.get(mode).values());
                if (didntConverge(slopes, MODE_CONDITION_THRESHOLD)) return;
                xxxItBool.put(iteration, true); //tmp
            }
        }

        // FINALLY: if none of the previous checks terminated the process, then dynamic shutdown can be initiated.
        log.info("JR: At this iteration, DynamicShutdown would have been initiated"); //tmp
        xxxItBoolTOTAL.put(iteration,true); //tmp
//        shutdownInnovation(iteration);
    }

    private boolean didntConverge(List<Double> slopes, double threshold) {

        int startIteration = slopes.size() - ITERATIONS_IN_ZONE_TO_CONVERGE - 1;

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


        int currentIter = Collections.max(inputMap.keySet());
        int startIteration = currentIter - MINIMUM_WINDOW_SIZE;
        if (EXPANDING_WINDOW && (int) EXPANDING_WINDOW_PCT_RETENTION * currentIter > MINIMUM_WINDOW_SIZE) {
            startIteration = (int) (1 - EXPANDING_WINDOW_PCT_RETENTION) * currentIter;
        }

        ArrayList<Integer> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();
        for (Integer it : inputMap.keySet()) {
            if (it >= startIteration) {
                x.add(it);
                y.add(inputMap.get(it));
            }
        }

        if (x.size() != y.size()) {
            throw new IllegalArgumentException("array lengths are not equal");
        }
        int n = x.size();

        // first pass
        double sumx = 0.0, sumy = 0.0;
        for (int i = 0; i < n; i++) {
            sumx  += x.get(i);
            sumy  += y.get(i);
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x.get(i) - xbar) * (x.get(i) - xbar);
            xybar += (x.get(i) - xbar) * (y.get(i) - ybar);
        }
        return xybar / xxbar;


    }

    private void bestFitLineModeChoiceCoverage(int iteration) {
        Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory;

        if (iteration < ITERATION_TO_START_FINDING_SLOPES) {
            return;
        }

        modeChoiceCoverageHistory = ModeChoiceCoverageControlerListener.getModeHistory();

        Integer limit = 1;

        for (Map.Entry<String, Map<Integer, Double>> entry : modeChoiceCoverageHistory.get(limit).entrySet()) {
            String mode = entry.getKey();
            log.info("Mode choice coverage checked for " + mode);

            double slope = computeLineSlope(entry.getValue());

            Map<Integer,Double> slopesForMode = slopesModeChoiceCoverage.computeIfAbsent(mode, v -> new HashMap<>());
            slopesForMode.put(iteration,slope);
        }

    }


    private void bestFitLineScore(int iteration) {

        if (iteration < ITERATION_TO_START_FINDING_SLOPES) {
            return;
        }

        for (Map.Entry<ScoreStatsControlerListener.ScoreItem, Map<Integer, Double>> entry : scoreStats.getScoreHistory().entrySet()) {
            String scoreItem = entry.getKey().toString();
            log.info("Score checked for " + scoreItem);

            double slope = computeLineSlope(entry.getValue());

            Map<Integer,Double> slopesForScoreItem = slopesScore.computeIfAbsent(scoreItem, v -> new HashMap<>());
            slopesForScoreItem.put(iteration,slope);
        }
    }

    private void bestFitLineMode(int iteration) {

        if (iteration < ITERATION_TO_START_FINDING_SLOPES) {
            return;
        }


        for (Map.Entry<String, Map<Integer, Double>> entry : modeStatsControlerListener.getModeHistories().entrySet()) {
            String mode = entry.getKey();
            log.info("Mode checked for " + mode);

            double slope = computeLineSlope(entry.getValue());

            Map<Integer,Double> slopesForMode = slopesMode.computeIfAbsent(mode, v -> new HashMap<>());
            slopesForMode.put(iteration,slope);
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
        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("SlopesModeChoiceCoverage.txt"))){
            for (String mode : slopesModeChoiceCoverage.keySet()) {

                bw.write("\n Iterations ; ");
                for (Integer it : slopesModeChoiceCoverage.get(mode).keySet()) {
                    bw.write(it + " ; ");
                }

                bw.write("\n" + mode+" ; ");
                for (Double slope : slopesModeChoiceCoverage.get(mode).values()) {
                    bw.write(slope + " ; ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // score
        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("SlopesScore.txt"))){
            for (String scoreStat : slopesScore.keySet()) {
                bw.write("\n Iterations ; ");
                for (Integer it : slopesScore.get(scoreStat).keySet()) {
                    bw.write(it + " ; ");
                }

                bw.write("\n"+scoreStat+" ; ");

                for (Double slope : slopesScore.get(scoreStat).values()) {
                    bw.write(slope + " ; ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // mode
        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("SlopesMode.txt"))){
            for (String mode : slopesMode.keySet()) {
                bw.write("\n Iterations ; ");
                for (Integer it : slopesMode.get(mode).keySet()) {
                    bw.write(it + " ; ");
                }

                bw.write("\n"+mode+" ; ");
                for (Double slope : slopesMode.get(mode).values()) {
                    bw.write(slope + " ; ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("DYNAMIC_SHUTDOWN_INFO.txt"))){
            for (String metric : xxx.keySet()) {
                bw.write("\n Iterations ; ");
                for (Integer it : xxx.get(metric).keySet()) {
                    bw.write(it + " ; ");
                }

                bw.write("\n"+metric+" ; ");
                for (Boolean bool : xxx.get(metric).values()) {
                    bw.write(bool + " ; ");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

