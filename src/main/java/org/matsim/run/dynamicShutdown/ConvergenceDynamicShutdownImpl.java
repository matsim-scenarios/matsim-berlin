package org.matsim.run.dynamicShutdown;

import org.apache.log4j.Logger;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
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
import org.matsim.core.utils.io.UncheckedIOException;

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


public class ConvergenceDynamicShutdownImpl implements IterationStartsListener, StartupListener, ShutdownListener, DynamicShutdownControlerListener {

    private final BufferedWriter slopesOut ;
    private final ControlerConfigGroup controlerConfigGroup;
    private final Scenario scenario;
    private final ScoreStats scoreStats;
    private final ModeStatsControlerListener modeStatsControlerListener;
    private final ModeChoiceCoverageControlerListener modeChoiceCoverageControlerListener;
    private final String FILENAME_DYNAMIC_SHUTDOWN = "dynShutdown_";
    private String outputFileName;
    private final int globalInnovationDisableAfter;

    private final StrategyManager strategyManager;
    private static int dynamicShutdownIteration;
    private static boolean dynamicShutdownInitiated;
    private final StrategyConfigGroup strategyConfigGroup;
    private static final Logger log = Logger.getLogger(StrategyManager.class);


    private static final Map<String, Map<Integer,Double>> slopesScore = new HashMap<>();
    private static final Map<String, Map<Integer,Double>> slopesMode = new HashMap<>();
    private static final Map<String, Map<Integer,Double>> slopesModeChoiceCoverage = new HashMap<>();


    private static final Map<String, Map<Integer,Boolean>> convergenceScore = new HashMap<>();
    private static final Map<String, Map<Integer,Boolean>> convergenceMode = new HashMap<>();
    private static final Map<String, Map<Integer,Boolean>> convergenceModeCC = new HashMap<>();

    private List<String> activeMetricsScore = new ArrayList<>();
    private List<String> activeMetricsMode = new ArrayList();
    private List<String> activeMetricsModeCC = new ArrayList();

    enum scorePolicyOptions { ON_FULL , ON_EXECUTED_ONLY , OFF }
    enum modePolicyOptions { ON_FULL , OFF }
    enum modeCCPolicyOptions { ON_FULL, OFF }

    enum slopeWindowOption { FIXED , EXPANDING}


    DynamicShutdownConfigGroup dynamicShutdownConfigGroup;
    // U S E R   I N P U T

    // Dynamic Shutdown Config Group
    private final int minimumIteration = 0; // 500 TODO: Revert
    private final int iterationToStartFindingSlopes = 3;//50; // TODO: Revert

    private final slopeWindowOption slopeWindowPolicy = slopeWindowOption.EXPANDING;
    private final int minimumWindowSize = 3;//50; // TODO: Revert
    private final double expandingWindowPctRetention = 0.25;

    private final int iterationsInZoneToConverge = 50;// 50 TODO: Revert

    private final int minIterationForGraphics = 3;


    // Score Parameters
    private final scorePolicyOptions scorePolicyChosen = scorePolicyOptions.ON_EXECUTED_ONLY;
    private final double scoreThreshold = 0.001;

    // Mode Parameters
    private final modePolicyOptions modePolicyChosen  = modePolicyOptions.ON_FULL;
    private final double modeThreshold = 0.00003;

    // Mode Choice Coverage Parameters
    private final modeCCPolicyOptions modeCCPolicyChosen = modeCCPolicyOptions.ON_FULL;
    private final double modechoicecoverageThreshold = 0.0001;



    @Inject
    ConvergenceDynamicShutdownImpl(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats,
                                   ModeStatsControlerListener modeStatsControlerListener, StrategyManager strategyManager,
                                   StrategyConfigGroup strategyConfigGroup, Scenario scenario, OutputDirectoryHierarchy controlerIO,
                                   PlanCalcScoreConfigGroup scoreConfig,
                                   ModeChoiceCoverageControlerListener modeChoiceCoverageControlerListener) {

        this.scenario = scenario;
        this.scoreStats = scoreStats;
        this.strategyManager = strategyManager;
        this.strategyConfigGroup = strategyConfigGroup;
        this.controlerConfigGroup = controlerConfigGroup ;
        this.modeStatsControlerListener = modeStatsControlerListener ;
        this.outputFileName = controlerIO.getOutputFilename(FILENAME_DYNAMIC_SHUTDOWN);
        this.modeChoiceCoverageControlerListener = modeChoiceCoverageControlerListener;

        this.dynamicShutdownConfigGroup = (DynamicShutdownConfigGroup) scenario.getConfig().getModules().get(DynamicShutdownConfigGroup.GROUP_NAME);


        this.globalInnovationDisableAfter = (int) ((controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration())
                * strategyConfigGroup.getFractionOfIterationsToDisableInnovation() + controlerConfigGroup.getFirstIteration());


        switch (scorePolicyChosen) {
            case ON_FULL:
                activeMetricsScore = new ArrayList<>(Arrays.asList(
                        ScoreItem.executed.name(),
                        ScoreItem.average.name(),
                        ScoreItem.best.name(),
                        ScoreItem.worst.name()));
                break;
            case ON_EXECUTED_ONLY:
                activeMetricsScore = new ArrayList<>(Arrays.asList(
                        ScoreItem.executed.name()));
                break;
            case OFF:
                break;
            default:
                break;
        }

        switch (modePolicyChosen) {
            case ON_FULL:
                activeMetricsMode.addAll(scoreConfig.getAllModes());
                break;
            case OFF:
                activeMetricsMode.clear();
                break;
            default:
                activeMetricsMode.clear();
                break;
        }

        switch (modeCCPolicyChosen) {
            case ON_FULL:
                activeMetricsModeCC.addAll(scoreConfig.getAllModes());
                break;
            case OFF:
                activeMetricsModeCC.clear();
                break;
            default:
                activeMetricsModeCC.clear();
                break;
        }

        this.slopesOut = IOUtils.getBufferedWriter(this.outputFileName + "AllMetrics.txt");
        try {
            this.slopesOut.write("Iteration");

            for ( String scoreType : activeMetricsScore) {
                this.slopesOut.write("\tscore-" + scoreType+ "\tconverged");
            }
            for ( String mode : activeMetricsMode) {
                this.slopesOut.write("\tmode-" + mode+ "\tconverged");
            }
            for ( String mode : activeMetricsModeCC) {
                this.slopesOut.write("\tmodeCC-" + mode + "\tconverged");
            }
            this.slopesOut.write("\n"); ;
            this.slopesOut.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public int getDynamicShutdownIteration() {
        return dynamicShutdownIteration;
    }

    @Override
    public boolean isDynamicShutdownInitiated() {
        return dynamicShutdownInitiated;
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        dynamicShutdownInitiated = false ;
        dynamicShutdownIteration = Integer.MAX_VALUE;

        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX " + dynamicShutdownConfigGroup.getExpandingWindowPctRetention());

    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {

        int iteration = iterationStartsEvent.getIteration();
        int prevIteration = iteration - 1;

        // Checks 1:  at least one shutdown criteria is being active. Otherwise this module is superfluous.
        if (activeMetricsScore.isEmpty() && activeMetricsMode.isEmpty() && activeMetricsModeCC.isEmpty()) {
            log.warn("dynamic shutdown should not be used if no criteria are specified. " +
                    "Therefore, the standard shutdown iteration will be used");
            return;
        }


        // If we cannot start finding slopes, then we shouldn't do anything further.
        if (iteration < iterationToStartFindingSlopes) {
            return;
        }

        try {
            this.slopesOut.write("\n" + prevIteration);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // For every active condition, calculate and plot the slopes. This will later be used to check convergence
        boolean scoreConverged = false;
        if (!activeMetricsScore.isEmpty()) {
            String metricType = "Score";
            Map<ScoreItem, Map<Integer, Double>> scoreHistory = scoreStats.getScoreHistory();
            Map<String, Map<Integer, Double>> scoreHistoryMod = new HashMap<>();
            for (ScoreItem scoreItem : scoreHistory.keySet()) {
                scoreHistoryMod.put(scoreItem.name(), scoreHistory.get(scoreItem));
            }

            bestFitLineGeneric(iteration, scoreHistoryMod, slopesScore, activeMetricsScore, metricType);
            produceDynShutdownGraphs(scoreHistoryMod, slopesScore, metricType, activeMetricsScore, scoreThreshold, iteration);

            scoreConverged = metricTypeConverges(slopesScore, convergenceScore, activeMetricsScore, metricType, scoreThreshold, prevIteration);

            writeSlopeAndConvergence(slopesScore, convergenceScore, activeMetricsScore, prevIteration);

        }

        boolean modeConverged = false;
        if (!activeMetricsMode.isEmpty()) {
            String metricType = "Mode";
            Map<String, Map<Integer, Double>> modeHistories = modeStatsControlerListener.getModeHistories();
            bestFitLineGeneric(iteration, modeHistories, slopesMode, activeMetricsMode, metricType);
            produceDynShutdownGraphs(modeHistories, slopesMode, metricType, activeMetricsMode, modeThreshold, iteration);

            modeConverged = metricTypeConverges(slopesMode, convergenceMode, activeMetricsMode, metricType, modeThreshold, prevIteration);

            writeSlopeAndConvergence(slopesMode, convergenceMode, activeMetricsMode, prevIteration);
        }

        boolean modeCCConverged = false;
        if (!activeMetricsModeCC.isEmpty()) {

            String metricType = "Mode Choice Coverage";
            int mCCLimit = 1;
            Map<String, Map<Integer, Double>> mCCHistory = modeChoiceCoverageControlerListener.getModeChoiceCoverageHistory().get(mCCLimit);
            bestFitLineGeneric(iteration, mCCHistory, slopesModeChoiceCoverage, activeMetricsModeCC, metricType);
            produceDynShutdownGraphs(mCCHistory,slopesModeChoiceCoverage, metricType, activeMetricsModeCC, modechoicecoverageThreshold,iteration);

            modeCCConverged = metricTypeConverges(slopesModeChoiceCoverage, convergenceModeCC, activeMetricsModeCC, metricType, modechoicecoverageThreshold, prevIteration);

            writeSlopeAndConvergence(slopesModeChoiceCoverage, convergenceModeCC, activeMetricsModeCC, prevIteration);
        }

        if (dynamicShutdownInitiated) {
            log.info("dynamic shutdown was previously initiated");
            return;
        }

        if (iteration < minimumIteration) {
            return ;
        }

        if (iteration >= globalInnovationDisableAfter) {
            return;
        }

        if (!activeMetricsScore.isEmpty() &&   !scoreConverged) {
            return;
        }

        if (!activeMetricsMode.isEmpty() && !modeConverged) {
            return;
        }

        if (!activeMetricsModeCC.isEmpty() && !modeCCConverged) {
            return;
        }

        shutdownInnovation(iteration);

    }

    private void writeSlopeAndConvergence(Map<String, Map<Integer, Double>> slopesMap,
                                          Map<String, Map<Integer, Boolean>> convergenceMap,
                                          List<String> metricsToInclude,int prevIteration) {
        try{
            for (String metric : metricsToInclude) {
                Double slope = slopesMap.get(metric).get(prevIteration);
                boolean conv = convergenceMap.get(metric).get(prevIteration);
                this.slopesOut.write("\t"+ slope + "\t" + conv);
            }
            this.slopesOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean metricTypeConverges(Map<String, Map<Integer, Double>> slopesMap,
                                       Map<String, Map<Integer, Boolean>> convergenceMap,
                                       List<String> metricsToInclude,
                                       String metricType,
                                       double threshold,
                                        int prevIteration) {


        int convergenceSuccessCnt = 0;
        for (String metric : metricsToInclude) {
            boolean metricConverges = false;

            if (!slopesMap.isEmpty()) {
                List<Double> slopesPerMetric = new ArrayList<>(slopesMap.get(metric).values());
                metricConverges = metricConverges(slopesPerMetric, threshold);
            }

            Map<Integer,Boolean> convergencePerMetric = convergenceMap.computeIfAbsent(metric, v -> new HashMap<>());
            convergencePerMetric.put(prevIteration,metricConverges);

            if (metricConverges) {
                convergenceSuccessCnt++;
            }
        }

        return convergenceSuccessCnt == metricsToInclude.size();
    }

    private boolean metricConverges(List<Double> slopes, double threshold) {

        int startIteration = slopes.size() - iterationsInZoneToConverge;

        if (startIteration < 0) {
            log.info("Not enough slopes computed to check for convergence");
            return false;
        }

        for (int i = startIteration; i < slopes.size(); i++) {
            if (slopes.get(i) < -1 * threshold || slopes.get(i) > threshold) {
                return false;
            }
        }
        return true;
    }

    private void bestFitLineGeneric(int iteration,
                                    Map<String, Map<Integer, Double>> history,
                                    Map<String, Map<Integer, Double>> slopes,
                                    List<String> metricsToInclude,
                                    String metricType) {

        for (Map.Entry<String, Map<Integer, Double>> entry : history.entrySet()) {

            String metricName = entry.getKey();

            if (!metricsToInclude.isEmpty() &&  !metricsToInclude.contains(metricName)) {
                log.info(metricType + " NOT checked for " + metricName);
                continue;
            }

            log.info(metricType + " checked for " + metricName);

            double slope = computeLineSlope(entry.getValue());

            Map<Integer,Double> slopesForMetric = slopes.computeIfAbsent(metricName, v -> new HashMap<>());
            slopesForMetric.put(iteration-1,slope); // calculation for the previous iteration
        }
    }


    private double computeLineSlope(Map<Integer,Double> inputMap) {


        int currentIter = Collections.max(inputMap.keySet());
        int startIteration = currentIter - minimumWindowSize + 1; // fixed window
        int startIterationExpanding = (int) ((1.0 - expandingWindowPctRetention) * currentIter); // expanding window
        if (slopeWindowPolicy == slopeWindowOption.EXPANDING && startIterationExpanding < startIteration) { // TODO: Test whether enum works in this case
            startIteration = startIterationExpanding;
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



    private void produceDynShutdownGraphs(Map<String, Map<Integer, Double>> history,
                                          Map<String, Map<Integer, Double>> slopes,
                                          String metricType,
                                          List<String> metricsToInclude,
                                          double convergenceThreshold,
                                          int iteration) {

        if (iteration <= minIterationForGraphics) {
            return;
        }



        for (String metricName : metricsToInclude) {
            try {

                XYLineChartDualYAxis chart = new XYLineChartDualYAxis("Dynamic Shutdown for " + metricType + " : " + metricName, "iteration", metricType + " : " + metricName, "slope of " + metricName);
                Map<Integer, Double> metric = history.get(metricName);
                chart.addSeries(metricName, metric);

                chart.addSeries2("d/dx(" + metricName + ")", slopes.get(metricName));
                chart.addMatsimLogo();
                chart.addVerticalRange(-convergenceThreshold, convergenceThreshold);

                chart.saveAsPng(outputFileName + "_" + metricType + "_" + metricName + ".png", 800, 600);
            } catch (NullPointerException e) {
                    log.error("Could not produce Dynamic Shutdown Graphs (probably too early)");
            }
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
        try {
            this.slopesOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}