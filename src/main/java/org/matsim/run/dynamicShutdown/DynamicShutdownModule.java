package org.matsim.run.dynamicShutdown;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.TerminationCriterion;

public class DynamicShutdownModule extends AbstractModule {


    /**
     * Adds all necessary GUICE bindings for Dynamic Shutdown to work.
     *
     * @author jakobrehmann
     */

    @Override
    public void install() {

        this.bind(ModeChoiceCoverageControlerListener.class).in(Singleton.class);
        this.addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

        this.bind(DynamicShutdownControlerListenerImpl.class).in(Singleton.class);
        this.addControlerListenerBinding().to(DynamicShutdownControlerListenerImpl.class);

        this.bind(TerminateDynamically.class).in(Singleton.class);
        this.bind(TerminationCriterion.class).to(TerminateDynamically.class);

    }
}
