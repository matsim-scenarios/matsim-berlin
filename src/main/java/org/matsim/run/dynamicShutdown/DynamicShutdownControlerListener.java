package org.matsim.run.dynamicShutdown;

public interface DynamicShutdownControlerListener {

    int getDynamicShutdownIteration();

    boolean isDynamicShutdownInitiated();

}
