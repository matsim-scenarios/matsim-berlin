package org.matsim.contrib.ev.infrastructure;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CustomCharingInfrastructureModule extends AbstractModule {
    private final ChargingInfrastructureSpecification infrastructureSpecification;

    public CustomCharingInfrastructureModule(ChargingInfrastructureSpecification infrastructureSpecification) {
        this.infrastructureSpecification = infrastructureSpecification;
    }

    @Override
    public void install() {
        bind(ChargingInfrastructureSpecification.class).toInstance(infrastructureSpecification);
        addControllerListenerBinding().to(InfrastructureWriter.class);
    }

    @Provides
    @Singleton
    InfrastructureWriter provideInfrastructureWriter(OutputDirectoryHierarchy outputDirectoryHierarchy,
            ChargingInfrastructureSpecification infrastructureSpecification) {
        return new InfrastructureWriter(outputDirectoryHierarchy, infrastructureSpecification);
    }

    static private class InfrastructureWriter implements StartupListener {
        private final String CHARGERS_FILE = "initial_chargers.xml.gz";

        private final OutputDirectoryHierarchy outputDirectoryHierarchy;
        private final ChargingInfrastructureSpecification infrastructureSpecification;

        public InfrastructureWriter(OutputDirectoryHierarchy outputDirectoryHierarchy,
                ChargingInfrastructureSpecification infrastructureSpecification) {
            this.outputDirectoryHierarchy = outputDirectoryHierarchy;
            this.infrastructureSpecification = infrastructureSpecification;
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            String outputPath = outputDirectoryHierarchy.getOutputFilename(CHARGERS_FILE);

            new ChargerWriter(infrastructureSpecification.getChargerSpecifications().values().stream())
                    .write(outputPath);
        }
    }
}
