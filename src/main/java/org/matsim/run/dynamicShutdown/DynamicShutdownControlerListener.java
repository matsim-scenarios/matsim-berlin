package org.matsim.run.dynamicShutdown;

/**
 *
 * @author jakobrehmann
 */

public interface DynamicShutdownControlerListener {

    int getDynamicShutdownIteration();

    boolean isDynamicShutdownInitiated();

}
