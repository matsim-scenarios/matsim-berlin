package org.matsim.run.dynamicShutdown;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * Allows users to set and get parameters for DynamicShutdownControlerListener. Defaults are included for all parameters.
 *
 * @author jakobrehmann
 */

public class DynamicShutdownConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "dynamicShutdown";

    private static final String MINIMUM_ITERATION_FOR_SHUTDOWN = "minIterationForShutdown";
    private static final String ITERATION_TO_START_FINDING_SLOPES = "iterationToStartFindingSlopes";
    private static final String SLOPE_WINDOW_POLICY = "slopeWindowPolicy";
    private static final String MINIMUM_WINDOW_SIZE = "minWindowSize";
    private static final String EXPANDING_WINDOW_PCT_RETENTION = "expandingWindowPctRetention";
    private static final String ITERATIONS_IN_ZONE_TO_CONVERGE = "iterationsInZoneToConverge";
    private static final String MIN_ITERATIONS_FOR_GRAPHS = "minIterationForGraphs";

    private static final String SCORE_POLICY_CHOSEN = "scorePolicyChosen";
    private static final String MODE_POLICY_CHOSEN = "modePolicyChosen";
    private static final String MODE_CHOICE_COVERAGE_POLICY_CHOSEN = "modeChoiceCoveragePolicyChosen";
    private static final String SCORE_THRESHOLD = "scoreConvergenceThreshold";
    private static final String MODE_THRESHOLD= "modeConvergenceThreshold";
    private static final String MODE_CHOICE_COVERAGE_THRESHOLD= "modeChoiceCoverageConvergenceThreshold";

    private static final String DYNAMIC_SHUTDOWN_MODULE_ACTIVE = "dynamicShutdownModuleActive";

    public enum dynamicShutdownOptions { ON_FULL , ON_ANALYSIS_ONLY , OFF }

    public enum scorePolicyOptions { ON_FULL , ON_EXECUTED_ONLY , OFF }
    public enum modePolicyOptions { ON_FULL , OFF }
    public enum modeCCPolicyOptions { ON_FULL, OFF }

    public enum slopeWindowOption { FIXED , EXPANDING }


    private dynamicShutdownOptions dynamicShutdownModuleActive = dynamicShutdownOptions.OFF;
    private int minimumIteration = 0;
    private int iterationToStartFindingSlopes = 50;
    private slopeWindowOption slopeWindowPolicy = slopeWindowOption.EXPANDING;
    private int minimumWindowSize = 50;
    private double expandingWindowPctRetention = 0.25;
    private int iterationsInZoneToConverge = 50;
    private int minIterationForGraphics = 50;
    private scorePolicyOptions scorePolicyChosen = scorePolicyOptions.ON_EXECUTED_ONLY;
    private double scoreThreshold  = 0.001;
    private modePolicyOptions modePolicyChosen  = modePolicyOptions.ON_FULL;
    private double modeThreshold = 0.00003;
    private modeCCPolicyOptions modeCCPolicyChosen = modeCCPolicyOptions.ON_FULL;
    private double modechoicecoverageThreshold = 0.0001;

    public DynamicShutdownConfigGroup() {
        super(GROUP_NAME);
    }
    @StringGetter(DYNAMIC_SHUTDOWN_MODULE_ACTIVE)
    public dynamicShutdownOptions isDynamicShutdownModuleActive() {
        return dynamicShutdownModuleActive;
    }
    @StringSetter(DYNAMIC_SHUTDOWN_MODULE_ACTIVE)
    public void setDynamicShutdownModuleActive(final dynamicShutdownOptions dynamicShutdownModuleActive) {
        this.dynamicShutdownModuleActive = dynamicShutdownModuleActive;
    }
    @StringGetter( MINIMUM_ITERATION_FOR_SHUTDOWN )
    public int getMinimumIteration() {
        return this.minimumIteration;
    }
    @StringSetter(MINIMUM_ITERATION_FOR_SHUTDOWN)
    public void setMinimumIteration(final int minimumIteration) {
        this.minimumIteration = minimumIteration;
    }
    @StringGetter(ITERATION_TO_START_FINDING_SLOPES)
    public int getIterationToStartFindingSlopes() {
        return iterationToStartFindingSlopes;
    }
    @StringSetter(ITERATION_TO_START_FINDING_SLOPES)
    public void setIterationToStartFindingSlopes(final int iterationToStartFindingSlopes) {
        this.iterationToStartFindingSlopes = iterationToStartFindingSlopes;
    }
    @StringGetter(SLOPE_WINDOW_POLICY)
    public slopeWindowOption getSlopeWindowPolicy() {
        return slopeWindowPolicy;
    }
    @StringSetter(SLOPE_WINDOW_POLICY)
    public void setSlopeWindowPolicy(final slopeWindowOption slopeWindowPolicy) {
        this.slopeWindowPolicy = slopeWindowPolicy;
    }
    @StringGetter(MINIMUM_WINDOW_SIZE)
    public int getMinimumWindowSize() {
        return minimumWindowSize;
    }
    @StringSetter(MINIMUM_WINDOW_SIZE)
    public void setMinimumWindowSize(final int minimumWindowSize) {
        this.minimumWindowSize = minimumWindowSize;
    }
    @StringGetter(EXPANDING_WINDOW_PCT_RETENTION)
    public double getExpandingWindowPctRetention() {
        return expandingWindowPctRetention;
    }
    @StringSetter(EXPANDING_WINDOW_PCT_RETENTION)
    public void setExpandingWindowPctRetention(final double expandingWindowPctRetention) {
        this.expandingWindowPctRetention = expandingWindowPctRetention;
    }
    @StringGetter(ITERATIONS_IN_ZONE_TO_CONVERGE)
    public int getIterationsInZoneToConverge() {
        return iterationsInZoneToConverge;
    }
    @StringSetter(ITERATIONS_IN_ZONE_TO_CONVERGE)
    public void setIterationsInZoneToConverge(final int iterationsInZoneToConverge) {
        this.iterationsInZoneToConverge = iterationsInZoneToConverge;
    }
    @StringGetter(MIN_ITERATIONS_FOR_GRAPHS)
    public int getMinIterationForGraphics() {
        return minIterationForGraphics;
    }
    @StringSetter(MIN_ITERATIONS_FOR_GRAPHS)
    public void setMinIterationForGraphics(final int minIterationForGraphics) {
        this.minIterationForGraphics = minIterationForGraphics;
    }
    @StringGetter(SCORE_POLICY_CHOSEN)
    public scorePolicyOptions getScorePolicyChosen() {
        return scorePolicyChosen;
    }
    @StringSetter(SCORE_POLICY_CHOSEN)
    public void setScorePolicyChosen(final scorePolicyOptions scorePolicyChosen) {
        this.scorePolicyChosen = scorePolicyChosen;
    }
    @StringGetter(SCORE_THRESHOLD)
    public double getScoreThreshold() {
        return scoreThreshold;
    }
    @StringSetter(SCORE_THRESHOLD)
    public void setScoreThreshold(final double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }
    @StringGetter(MODE_POLICY_CHOSEN)
    public modePolicyOptions getModePolicyChosen() {
        return modePolicyChosen;
    }
    @StringSetter(MODE_POLICY_CHOSEN)
    public void setModePolicyChosen(final modePolicyOptions modePolicyChosen) {
        this.modePolicyChosen = modePolicyChosen;
    }
    @StringGetter(MODE_THRESHOLD)
    public double getModeThreshold() {
        return modeThreshold;
    }
    @StringSetter(MODE_THRESHOLD)
    public void setModeThreshold(final double modeThreshold) {
        this.modeThreshold = modeThreshold;
    }
    @StringGetter(MODE_CHOICE_COVERAGE_POLICY_CHOSEN)
    public modeCCPolicyOptions getModeCCPolicyChosen() {
        return modeCCPolicyChosen;
    }
    @StringSetter(MODE_CHOICE_COVERAGE_POLICY_CHOSEN)
    public void setModeCCPolicyChosen(final modeCCPolicyOptions modeCCPolicyChosen) {
        this.modeCCPolicyChosen = modeCCPolicyChosen;
    }
    @StringGetter(MODE_CHOICE_COVERAGE_THRESHOLD)
    public double getModeChoiceCoverageThreshold() {
        return modechoicecoverageThreshold;
    }
    @StringSetter(MODE_CHOICE_COVERAGE_THRESHOLD)
    public void setModechoicecoverageThreshold(final double modechoicecoverageThreshold) {
        this.modechoicecoverageThreshold = modechoicecoverageThreshold;
    }
}
