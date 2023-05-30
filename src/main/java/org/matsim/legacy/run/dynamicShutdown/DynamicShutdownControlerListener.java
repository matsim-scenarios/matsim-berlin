package org.matsim.legacy.run.dynamicShutdown;

/**
 *
 * @author jakobrehmann
 */

public interface DynamicShutdownControlerListener {

    int getDynamicShutdownIteration();

    boolean isDynamicShutdownInitiated();

}
