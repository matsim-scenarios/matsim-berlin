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

import static org.matsim.run.dynamicShutdown.DynamicShutdownConfigGroup.slopeWindowOption.EXPANDING;

/**
 * Initiats dynamic shutdown when a when the MATSim run has stabilized: 1) Turns off innovation in next iteration and
 * 2) informs TerminateDynamically Module of the iteration at which MATSim should shut down.
 *
 * The simulation is deemed stable, when a group of metrics converge; currently, this includes score, mode stats,
 * and mode choice coverage. A metric converges when the slope of the metric consistently lies below a user-specified
 * threshold for number of iterations (default=50).
 *
 * @author jakobrehmann
 */


public class DynamicShutdownControlerListenerImpl implements IterationStartsListener, StartupListener, ShutdownListener, DynamicShutdownControlerListener {

    private static final Logger log = Logger.getLogger(StrategyManager.class);

    private final ControlerConfigGroup controlerConfigGroup;
    private final ScoreStats scoreStats;
    private final PlanCalcScoreConfigGroup scoreConfig;
    private final StrategyConfigGroup strategyConfigGroup;
    private final ModeStatsControlerListener modeStatsControlerListener;
    private final ModeChoiceCoverageControlerListener modeChoiceCoverageControlerListener;
    private final Scenario scenario;
    private final StrategyManager strategyManager;

    private final DynamicShutdownConfigGroup cfg;
    private final String outputFileName;
    private final BufferedWriter slopesOut ;
    private final int globalInnovationDisableAfter;

    private int dynamicShutdownIteration;
    private boolean dynamicShutdownInitiated;
    private int dynamicInnovationDisableIteration;

    private static final Map<String, Map<Integer,Double>> slopesScore = new HashMap<>();
    private static final Map<String, Map<Integer,Double>> slopesMode = new HashMap<>();
    private static final Map<String, Map<Integer,Double>> slopesModeChoiceCoverage = new HashMap<>();

    private static final Map<String, Map<Integer,Integer>> convergenceScore = new HashMap<>();
    private static final Map<String, Map<Integer,Integer>> convergenceMode = new HashMap<>();
    private static final Map<String, Map<Integer,Integer>> convergenceModeCC = new HashMap<>();

    private List<String> activeMetricsScore = new ArrayList<>();
    private List<String> activeMetricsMode = new ArrayList<>();
    private List<String> activeMetricsModeCC = new ArrayList<>();

    @Inject
    DynamicShutdownControlerListenerImpl(ControlerConfigGroup controlerConfigGroup, ScoreStats scoreStats,
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
        this.outputFileName = controlerIO.getOutputFilename("dynShutdown_");
        this.modeChoiceCoverageControlerListener = modeChoiceCoverageControlerListener;

        this.cfg = (DynamicShutdownConfigGroup) scenario.getConfig().getModules().get(DynamicShutdownConfigGroup.GROUP_NAME);

        this.globalInnovationDisableAfter = (int) ((controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration())
                * strategyConfigGroup.getFractionOfIterationsToDisableInnovation() + controlerConfigGroup.getFirstIteration());

        this.slopesOut = IOUtils.getBufferedWriter(this.outputFileName + "allMetrics.txt");

        this.scoreConfig = scoreConfig;
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
        dynamicInnovationDisableIteration = Integer.MAX_VALUE;

        generateMetricLists(scoreConfig);

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
            this.slopesOut.write("\tnotes");
            this.slopesOut.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {

        int iteration = iterationStartsEvent.getIteration();
        int prevIteration = iteration - 1;

        if (cfg.isDynamicShutdownModuleActive()== DynamicShutdownConfigGroup.dynamicShutdownOptions.OFF) {
            log.warn("Dynamic shutdown module not active");
        }
        
        if (activeMetricsScore.isEmpty() && activeMetricsMode.isEmpty() && activeMetricsModeCC.isEmpty()) {
            log.warn("No criteria specified for dynamic shutdown module");
            return;
        }
        
        if (iteration < cfg.getIterationToStartFindingSlopes()) {
            return;
        }

        try {
            this.slopesOut.write("\n" + prevIteration);
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean scoreConverged = false;
        if (!activeMetricsScore.isEmpty()) {
            String metricType = "score";
            Map<ScoreItem, Map<Integer, Double>> scoreHistory = scoreStats.getScoreHistory();
            Map<String, Map<Integer, Double>> scoreHistoryMod = new HashMap<>();
            for (ScoreItem scoreItem : scoreHistory.keySet()) {
                scoreHistoryMod.put(scoreItem.name(), scoreHistory.get(scoreItem));
            }

            bestFitLineGeneric(prevIteration, scoreHistoryMod, slopesScore, activeMetricsScore);
            produceDynShutdownGraphs(scoreHistoryMod, slopesScore, metricType, activeMetricsScore, cfg.getScoreThreshold(), iteration);

            scoreConverged = metricTypeConverges(slopesScore, convergenceScore, activeMetricsScore, cfg.getScoreThreshold(), prevIteration);

            writeSlopeAndConvergence(slopesScore, convergenceScore, activeMetricsScore, prevIteration);

        }

        boolean modeConverged = false;
        if (!activeMetricsMode.isEmpty()) {
            String metricType = "mode";
            Map<String, Map<Integer, Double>> modeHistories = modeStatsControlerListener.getModeHistories();
            bestFitLineGeneric(prevIteration, modeHistories, slopesMode, activeMetricsMode);
            produceDynShutdownGraphs(modeHistories, slopesMode, metricType, activeMetricsMode, cfg.getModeThreshold(), iteration);

            modeConverged = metricTypeConverges(slopesMode, convergenceMode, activeMetricsMode, cfg.getModeThreshold(), prevIteration);

            writeSlopeAndConvergence(slopesMode, convergenceMode, activeMetricsMode, prevIteration);
        }

        boolean modeCCConverged = false;
        if (!activeMetricsModeCC.isEmpty()) {

            String metricType = "modeChoiceCoverage";
            int mCCLimit = 1;
            Map<String, Map<Integer, Double>> mCCHistory = modeChoiceCoverageControlerListener.getModeChoiceCoverageHistory().get(mCCLimit);
            bestFitLineGeneric(prevIteration, mCCHistory, slopesModeChoiceCoverage, activeMetricsModeCC);
            produceDynShutdownGraphs(mCCHistory,slopesModeChoiceCoverage, metricType, activeMetricsModeCC, cfg.getModeChoiceCoverageThreshold(),iteration);

            modeCCConverged = metricTypeConverges(slopesModeChoiceCoverage, convergenceModeCC, activeMetricsModeCC, cfg.getModeChoiceCoverageThreshold(), prevIteration);

            writeSlopeAndConvergence(slopesModeChoiceCoverage, convergenceModeCC, activeMetricsModeCC, prevIteration);
        }


        try {
            if (iteration == dynamicInnovationDisableIteration) {
                this.slopesOut.write("\tNOTE: innovation turned off");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (cfg.isDynamicShutdownModuleActive() == DynamicShutdownConfigGroup.dynamicShutdownOptions.ON_ANALYSIS_ONLY) {
            return;
        }

        if (dynamicShutdownInitiated) {
            log.info("Dynamic shutdown was previously initiated");
            return;
        }

        if (iteration < cfg.getMinimumIteration()) {
            return ;
        }

        if (iteration >= globalInnovationDisableAfter) {
            return;
        }

        if (!activeMetricsScore.isEmpty() && !scoreConverged) {
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
                                          Map<String, Map<Integer, Integer>> convergenceMap,
                                          List<String> metricsToInclude,int prevIteration) {
        try{
            for (String metric : metricsToInclude) {
                Double slope = slopesMap.get(metric).get(prevIteration);

                int convCnt = convergenceMap.get(metric).get(prevIteration);
                String convStr;
                if (convCnt >= cfg.getIterationsInZoneToConverge()) {
                    convStr = "true";
                } else {
                    convStr = convCnt + "/" + cfg.getIterationsInZoneToConverge();
                }
                this.slopesOut.write("\t"+ slope + "\t" + convStr);
            }
            this.slopesOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean metricTypeConverges(Map<String, Map<Integer, Double>> slopesMap,
                                       Map<String, Map<Integer, Integer>> convergenceMap,
                                       List<String> metricsToInclude,
                                       double threshold,
                                       int prevIteration) {

        if (slopesMap.isEmpty()) {
            return false;
        }

        for (String metric : metricsToInclude) {
            Map<Integer,Integer> convergenceCntPerMetric = convergenceMap.computeIfAbsent(metric,  v -> new HashMap<>());
            Double slope = slopesMap.get(metric).get(prevIteration);
            if (slope > -1 * threshold && slope < threshold) {
                int convergenceCountSoFar = convergenceCntPerMetric.getOrDefault(prevIteration - 1, 0);
                convergenceCntPerMetric.put(prevIteration, convergenceCountSoFar + 1);
            } else {
                convergenceCntPerMetric.put(prevIteration, 0);
            }
        }


        for (String metric : metricsToInclude) {
            if (convergenceMap.get(metric).get(prevIteration) < cfg.getIterationsInZoneToConverge()) {
                return false;
            }
        }
        return true;
    }

    private void bestFitLineGeneric(int prevIteration,
                                    Map<String, Map<Integer, Double>> history,
                                    Map<String, Map<Integer, Double>> slopes,
                                    List<String> metricsToInclude) {

        for (Map.Entry<String, Map<Integer, Double>> entry : history.entrySet()) {

            String metricName = entry.getKey();

            if (!metricsToInclude.isEmpty() &&  !metricsToInclude.contains(metricName)) {
                continue;
            }

            double slope = computeLineSlope(entry.getValue());

            Map<Integer,Double> slopesForMetric = slopes.computeIfAbsent(metricName, v -> new HashMap<>());
            slopesForMetric.put(prevIteration,slope);
        }
    }


    private double computeLineSlope(Map<Integer,Double> inputMap) {

        int currentIter = Collections.max(inputMap.keySet());
        int startIteration = currentIter - cfg.getMinimumWindowSize() + 1; // fixed window
        int startIterationExpanding = (int) ((1.0 - cfg.getExpandingWindowPctRetention()) * currentIter + 1); // expanding window

        if (cfg.getSlopeWindowPolicy() == EXPANDING && startIterationExpanding < startIteration) {
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
        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX  += x.get(i);
            sumY  += y.get(i);
        }
        double xBar = sumX / n;
        double yBar = sumY / n;

        double xxBar = 0.0;
        double xyBar = 0.0;
        for (int i = 0; i < n; i++) {
            xxBar += (x.get(i) - xBar) * (x.get(i) - xBar);
            xyBar += (x.get(i) - xBar) * (y.get(i) - yBar);
        }

        return xyBar / xxBar;

    }


    private void produceDynShutdownGraphs(Map<String, Map<Integer, Double>> history,
                                          Map<String, Map<Integer, Double>> slopes,
                                          String metricType,
                                          List<String> metricsToInclude,
                                          double convergenceThreshold,
                                          int iteration) {

        if (iteration <= cfg.getMinIterationForGraphics()) {
            return;
        }


        for (String metricName : metricsToInclude) {
            try {
                XYLineChartDualYAxis chart = new XYLineChartDualYAxis("Convergence of " + metricType + "-" + metricName, "iteration", metricType + " : " + metricName, "slope of " + metricName);

                chart.addSeries(metricName, history.get(metricName));
                chart.addSeries2("slope of " + metricName, slopes.get(metricName));

                chart.addVerticalRange(-convergenceThreshold, convergenceThreshold);
                chart.addMatsimLogo();

                chart.saveAsPng(outputFileName + metricType + "_" + metricName + ".png", 800, 600);
            } catch (NullPointerException e) {
                    log.error("Too early to produce Could not produce Dynamic Shutdown Graphs (probably too early)");
            }
        }
    }


    private void shutdownInnovation(int iteration) {
        dynamicInnovationDisableIteration = iteration + 1; // New weights are in effect in following iteration.
        double innoPct = strategyConfigGroup.getFractionOfIterationsToDisableInnovation();
        int firstIter = controlerConfigGroup.getFirstIteration();
        dynamicShutdownIteration = (int) ((dynamicInnovationDisableIteration - firstIter) / innoPct) + firstIter;
        dynamicShutdownInitiated = true;


        Set<String> subpopulations = new HashSet<>();
        for (StrategyConfigGroup.StrategySettings setting : this.scenario.getConfig().strategy().getStrategySettings()) {
            subpopulations.add(setting.getSubpopulation());
            if (subpopulations.size() == 0) subpopulations.add(null);
        }

        for (String subpopulation : subpopulations) {
            for (GenericPlanStrategy<Plan, Person> planStrategy : strategyManager.getStrategies(subpopulation)) {
                PlanStrategyImpl planStrategyImpl = (PlanStrategyImpl) planStrategy;
                if (!(ReplanningUtils.isOnlySelector(planStrategyImpl))) { // if (innovation strategy)
                    strategyManager.addChangeRequest(dynamicInnovationDisableIteration, planStrategyImpl, subpopulation, 0.);
                }
            }
        }

        log.info("********** DYNAMIC SHUTDOWN INITIATED ***********");
        log.info("Innovation strategies deactivated in iteration " + (dynamicInnovationDisableIteration));
        log.info("Full shutdown will occur in iteration " + dynamicShutdownIteration);

        try {
            this.slopesOut.write("\tNOTE: dynamic shutdown initiated");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateMetricLists(PlanCalcScoreConfigGroup scoreConfig) {
        switch (cfg.getScorePolicyChosen()) {
            case ON_FULL:
                activeMetricsScore = new ArrayList<>(Arrays.asList(
                        ScoreItem.executed.name(),
                        ScoreItem.average.name(),
                        ScoreItem.best.name(),
                        ScoreItem.worst.name()));
                break;
            case ON_EXECUTED_ONLY:
                activeMetricsScore.add(ScoreItem.executed.name());
                break;
            case OFF:
                break;
            default:
                break;
        }

        switch (cfg.getModePolicyChosen()) {
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

        switch (cfg.getModeCCPolicyChosen()) {
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