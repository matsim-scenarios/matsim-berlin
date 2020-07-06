package org.matsim.run.dynamicShutdown;

import org.apache.log4j.Logger;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
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
 * When user-specified criteria are met, dynamic shutdown is initiated: 1) Turns off innovation in next iteration and
 * 2) informs TerminateDynamically Module of the iteration at which MATSim should shut down.
 *
 * The criteria to used must be user specified (i.e. score convergence), as well as the smoothing method and associated
 * parameters.
 *
 * @author jakobrehmann
 */

// TODO: How should I deal with an interface & static methods?

public class ConvergenceDynamicShutdownImpl implements IterationStartsListener, StartupListener, ShutdownListener, DynamicShutdownControlerListener {

    // Dynamic Shutdown Config Group
    private final int MINIMUM_ITERATION = 0; // 500 TODO: Revert
    private final int ITERATION_TO_START_FINDING_SLOPES = 3;
    private final int MINIMUM_WINDOW_SIZE = 3;
    private final boolean EXPANDING_WINDOW = true;
    private final double EXPANDING_WINDOW_PCT_RETENTION = 0.25;
    private final int ITERATIONS_IN_ZONE_TO_CONVERGE = 50;

    private static final int minIterationForGraphics = 5;


    // Score Config Group
    private static final boolean SCORE_CONDITION_ACTIVE = true;
    private static final double SCORE_THRESHOLD = 0.001;
    private final List<String> scoreMetrics = new ArrayList<>(Arrays.asList(
            ScoreItem.executed.name(),
            ScoreItem.average.name(),
            ScoreItem.best.name(),
            ScoreItem.worst.name()));


    // Mode Convergence Config Group
    private static final boolean MODE_CONDITION_ACTIVE = true;
    private static final double MODE_THRESHOLD = 0.00003;
    private final List<String> modeMetrics = new ArrayList<>(Arrays.asList()); // if empty, all will be examined


    // Mode Choice Coverage Config Group
    private static final boolean MODECHOICECOVERAGE_CONDITION_ACTIVE = true;
    private static final double MODECHOICECOVERAGE_THRESHOLD = 0.0001;
    private final List<String> modeCCMetrics = new ArrayList<>(Arrays.asList()); // if empty, all will be examined


    private final ControlerConfigGroup controlerConfigGroup;
    private final OutputDirectoryHierarchy controlerIO;
    private final Scenario scenario;
    private final ScoreStats scoreStats;
    private final ModeStatsControlerListener modeStatsControlerListener;
    private static final String FILENAME_DYNAMIC_SHUTDOWN = "dynShutdown_";
    private static String outputFileName;
    private final int globalInnovationDisableAfter;

    private final StrategyManager strategyManager;
    private static int dynamicShutdownIteration;
    private static boolean dynamicShutdownInitiated;
    private final StrategyConfigGroup strategyConfigGroup;
    private static final Logger log = Logger.getLogger(StrategyManager.class);


    private static final Map<String, Map<Integer,Double>> slopesScore = new HashMap<>();
    private static final Map<String, Map<Integer,Double>> slopesMode = new HashMap<>();
    private static final Map<String, Map<Integer,Double>> slopesModeChoiceCoverage = new HashMap<>();


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
        outputFileName = controlerIO.getOutputFilename(FILENAME_DYNAMIC_SHUTDOWN);

        this.globalInnovationDisableAfter = (int) ((controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration())
                * strategyConfigGroup.getFractionOfIterationsToDisableInnovation() + controlerConfigGroup.getFirstIteration());

    }

    public int getDynamicShutdownIteration() {
        return dynamicShutdownIteration;
    }

    public boolean isDynamicShutdownInitiated() {
        return dynamicShutdownInitiated;
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        dynamicShutdownInitiated = false ;
        dynamicShutdownIteration = Integer.MAX_VALUE;

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



        // If we cannot start finding slopes, then we shouldn't do anything further.
        if (iteration < ITERATION_TO_START_FINDING_SLOPES) {
            return;
        }

        // For every active condition, calculate and plot the slopes. This will later be used to check convergence
        if (SCORE_CONDITION_ACTIVE) {
            String metricType = "Score";
            Map<ScoreItem, Map<Integer, Double>> scoreHistory = scoreStats.getScoreHistory();
            Map<String, Map<Integer, Double>> scoreHistoryMod = new HashMap<>();
            for (ScoreItem scoreItem : scoreHistory.keySet()) {
                scoreHistoryMod.put(scoreItem.name(), scoreHistory.get(scoreItem));
            }

            bestFitLineGeneric(iteration, scoreHistoryMod, slopesScore, scoreMetrics, metricType);
            produceDynShutdownGraphs(scoreHistoryMod,slopesScore,metricType, SCORE_THRESHOLD, iteration);
        }

        if (MODE_CONDITION_ACTIVE) {
            String metricType = "Mode";
            Map<String, Map<Integer, Double>> modeHistories = modeStatsControlerListener.getModeHistories();
            bestFitLineGeneric(iteration, modeHistories, slopesMode, modeMetrics, metricType);
            produceDynShutdownGraphs(modeHistories, slopesMode, metricType, MODE_THRESHOLD, iteration);

        }

        if (MODECHOICECOVERAGE_CONDITION_ACTIVE) {
            String metricType = "Mode Choice Coverage";
            int mCCLimit = 1;
            Map<String, Map<Integer, Double>> mCCHistory = ModeChoiceCoverageControlerListener.getModeHistory().get(mCCLimit);
            bestFitLineGeneric(iteration, mCCHistory, slopesModeChoiceCoverage, modeCCMetrics, metricType);
            produceDynShutdownGraphs(mCCHistory,slopesModeChoiceCoverage, metricType, MODECHOICECOVERAGE_THRESHOLD,iteration);

        }

        // Check 2: checks whether dynamic shutdown was already initiated.
        if (dynamicShutdownInitiated) {
            log.info("dynamic shutdown was previously initiated");
            return;
        }

        // Check 3: checks whether the minimum iteration was reached ;
        if (iteration < MINIMUM_ITERATION) {
            return ;
        }


//        // Check 4: check whether innovation shutdown has already occured
        if (iteration >= globalInnovationDisableAfter) {
            return;
        }

        // Check if mode choice coverage has converged
        if (MODECHOICECOVERAGE_CONDITION_ACTIVE) {

            if (slopesModeChoiceCoverage.isEmpty()) {
                return;
            }

            for (String mode : slopesModeChoiceCoverage.keySet()) {

                List<Double> slopes = new ArrayList<>(slopesModeChoiceCoverage.get(mode).values());
                if (didntConverge(slopes, MODECHOICECOVERAGE_THRESHOLD)){
                    log.info("mode choice coverage - " + mode + " = NOT converged");
                    return;
                }
                log.info("mode choice coverage - " + mode + " = converged");
            }
        }

        // Check if score has converged
        if (SCORE_CONDITION_ACTIVE) {

            if (slopesScore.isEmpty()) {
                return;
            }

            for (String scoreItem : slopesScore.keySet()) {
                log.info("Checking score convergence for " + scoreItem);
                List<Double> slopes = new ArrayList<>(slopesScore.get(scoreItem).values());
                if (didntConverge(slopes, SCORE_THRESHOLD)) {
                    log.info("score - " + scoreItem + " = NOT converged");
                    return;
                }
                log.info("score - " + scoreItem + " = converged");
            }
        }

        // Mode Convergence
        if (MODE_CONDITION_ACTIVE) {

            if (slopesMode.isEmpty()) {
                return;
            }

            for (String mode : slopesMode.keySet()) {
                log.info("Checking mode convergence for " + mode);
                List<Double> slopes = new ArrayList<>(slopesMode.get(mode).values());
                if (didntConverge(slopes, MODE_THRESHOLD)) {
                    log.info("mode - " + mode + " = NOT converged");
                    return;
                }
                log.info("mode - " + mode + " = converged");
            }
        }

        // FINALLY: if none of the previous checks terminated the process, then dynamic shutdown can be initiated.
        shutdownInnovation(iteration);

    }

    private boolean didntConverge(List<Double> slopes, double threshold) {

        int startIteration = slopes.size() - ITERATIONS_IN_ZONE_TO_CONVERGE;

        if (startIteration < 0) {
            log.info("Not enough slopes computed to check for convergence");
            return true;
        }

        for (int i = startIteration; i < slopes.size(); i++) {
            if (slopes.get(i) < -1 * threshold || slopes.get(i) > threshold) {
                return true;
            }
        }
        return false;
    }



    private double computeLineSlope(Map<Integer,Double> inputMap) {


        int currentIter = Collections.max(inputMap.keySet());
        System.out.println("QQQ current iteration " + currentIter);

        int startIteration = currentIter - MINIMUM_WINDOW_SIZE + 1; // fixed window
        System.out.println("QQQ start iteration, fixed: " + startIteration);
        int startIterationExpanding = (int) ((1.0 - EXPANDING_WINDOW_PCT_RETENTION) * currentIter); // expanding window
        System.out.println("QQQ start iteration, expanding : " + startIterationExpanding);
        if (EXPANDING_WINDOW && startIterationExpanding < startIteration) {
            startIteration = startIterationExpanding;
        }
        System.out.println("QQQ start iteration, final: " + startIteration);

        ArrayList<Integer> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();

        int tmpCount = 0;
        for (Integer it : inputMap.keySet()) {
            if (it >= startIteration) {
                x.add(it);
                y.add(inputMap.get(it));
                tmpCount++;
            }
        }
        System.out.println("QQQ iterations for slope " +tmpCount);

        if (x.size() != y.size()) {
            throw new IllegalArgumentException("array lengths are not equal");
        }
        int n = x.size();
        System.out.println("QQQ n size used to find slope " + n);

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

    private void bestFitLineGeneric(int iteration,
                                    Map<String, Map<Integer, Double>> history,
                                    Map<String, Map<Integer, Double>> slopes,
                                    List<String> metricsToInclude,
                                    String metricType) {

        for (Map.Entry<String, Map<Integer, Double>> entry : history.entrySet()) {

            String metricName = entry.getKey();

            if (!metricsToInclude.isEmpty() &&  !metricsToInclude.contains(metricName)) {
                continue;
            }

            log.info(metricType + " checked for " + metricName);

            double slope = computeLineSlope(entry.getValue());

            Map<Integer,Double> slopesForMetric = slopes.computeIfAbsent(metricName, v -> new HashMap<>());
            slopesForMetric.put(iteration-1,slope); // calculation for the previous iteration
        }
    }


    private static void produceDynShutdownGraphs(Map<String, Map<Integer, Double>> history,
                                                 Map<String, Map<Integer, Double>> slopes,
                                                 String metricType,
                                                 double convergenceThreshold,
                                                 int iteration) {

        if (iteration <= minIterationForGraphics) {
            return;
        }


        try {
            for (String metricName : history.keySet()) {

                XYLineChartDualYAxis chart = new XYLineChartDualYAxis("Dynamic Shutdown for " + metricType + " : " + metricName, "iteration", metricType + " : " + metricName, "slope of " + metricName);
                Map<Integer, Double> metric = history.get(metricName);
                chart.addSeries(metricName, metric);

                chart.addSeries2("d/dx(" + metricName + ")", slopes.get(metricName));
                chart.addMatsimLogo();
                chart.addVerticalRange(-convergenceThreshold, convergenceThreshold);

                chart.saveAsPng(outputFileName + "_" + metricType + "_" + metricName + ".png", 800, 600);
            }
        } catch (NullPointerException e) {
            log.error("Could not produce Dynamic Shutdown Graphs (probably too early)");
        }
    }


    private void shutdownInnovation(int iteration) {
        int innoShutoffIter = iteration + 1; // New weights are in effect in following iteration.
        double innoPct = strategyConfigGroup.getFractionOfIterationsToDisableInnovation();
        int firstIter = controlerConfigGroup.getFirstIteration();
        dynamicShutdownIteration = (int) ((innoShutoffIter - firstIter) / innoPct) + firstIter;
        dynamicShutdownInitiated = true;


        Set<String> subpopulations = new HashSet<>();
        for (StrategyConfigGroup.StrategySettings setting : this.scenario.getConfig().strategy().getStrategySettings()) {
            subpopulations.add(setting.getSubpopulation());
            if (subpopulations.size() == 0) subpopulations.add(null);
        }

        for (String subpopulation : subpopulations) {
            for (GenericPlanStrategy<Plan, Person> planStrategy : strategyManager.getStrategies(subpopulation)) {
                PlanStrategyImpl planStrategyImpl = (PlanStrategyImpl) planStrategy;
                if (isInnovativeStrategy(planStrategyImpl)) {
                    strategyManager.addChangeRequest(innoShutoffIter, planStrategyImpl, subpopulation, 0.);
                }
            }
        }

        log.info("********** DYNAMIC SHUTDOWN INITIATED ***********");
        log.info("Innovation strategies deactivated in iteration " + (innoShutoffIter));
        log.info("Full shutdown will occur in iteration " + dynamicShutdownIteration);
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

//        try (BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("DYNAMIC_SHUTDOWN_INFO.txt"))){
//            for (String metric : xxx.keySet()) {
//                bw.write("\n Iterations ; ");
//                for (Integer it : xxx.get(metric).keySet()) {
//                    bw.write(it + " ; ");
//                }
//
//                bw.write("\n"+metric+" ; ");
//                for (Boolean bool : xxx.get(metric).values()) {
//                    bw.write(bool + " ; ");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}




