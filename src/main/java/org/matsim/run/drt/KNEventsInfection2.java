package org.matsim.run.drt;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.run.drt.EpisimConfigGroup.FacilitiesHandling;
import org.matsim.run.drt.EpisimConfigGroup.InfectionParams;

import java.io.IOException;
import java.util.Arrays;

class KNEventsInfection2{

        public static void main( String[] args ) throws IOException{
                OutputDirectoryLogging.catchLogEntries();

                Config config = ConfigUtils.createConfig( new EpisimConfigGroup() );
                EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule( config, EpisimConfigGroup.class );

//                episimConfig.setInputEventsFile( "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_wo_linkEnterLeave.xml.gz" );
                episimConfig.setInputEventsFile( "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz" );
                episimConfig.setFacilitiesHandling( FacilitiesHandling.bln );
//                episimConfig.setSample(0.01);
                episimConfig.setCalibrationParameter(2);

                // yyyyyy I think that there should be better type matching; we should not use "contains" but match everything before the underscore exactly.
                // kai, mar'20

                int closingIteration = 10;
                // pt:
                episimConfig.addContainerParams( new InfectionParams( "tr" ).setContactIntensity( 0. ) );
                // regular out-of-home acts:
                episimConfig.addContainerParams( new InfectionParams( "work" ).setShutdownDay( closingIteration ).setRemainingFraction( 0.2 ) );
                episimConfig.addContainerParams( new InfectionParams( "leis" ).setShutdownDay( closingIteration ).setRemainingFraction( 0. ) );
                episimConfig.addContainerParams( new InfectionParams( "edu" ).setShutdownDay( closingIteration ).setRemainingFraction( 0. ) );
                episimConfig.addContainerParams( new InfectionParams( "shop" ).setShutdownDay( closingIteration ).setRemainingFraction( 0.3 ) );
                episimConfig.addContainerParams( new InfectionParams( "errands" ).setShutdownDay( closingIteration ).setRemainingFraction( 0.3 ) );
                episimConfig.addContainerParams( new InfectionParams( "business" ).setShutdownDay( closingIteration ).setRemainingFraction( 0. ) );
                episimConfig.addContainerParams( new InfectionParams( "other" ).setShutdownDay( closingIteration ).setRemainingFraction( 0.2 ) );
                // freight act:
                episimConfig.addContainerParams( new InfectionParams( "freight" ).setShutdownDay( 0 ).setRemainingFraction( 0.0 ) );
                // home act:
                episimConfig.addContainerParams( new InfectionParams( "home" ) );

                StringBuilder outdir = new StringBuilder( "output" );
                for( InfectionParams infectionParams : episimConfig.getContainerParams().values() ){
                        outdir.append( "-" );
                        outdir.append( infectionParams.getContainerName() );
                        if ( infectionParams.getShutdownDay() < Long.MAX_VALUE ){
                                outdir.append( infectionParams.getRemainingFraction() );
                                outdir.append( "it" ).append( infectionParams.getShutdownDay() );
                        }
                        if ( infectionParams.getContactIntensity()!=1. ) {
                                outdir.append( "ci" ).append( infectionParams.getContactIntensity() );
                        }
                }
                config.controler().setOutputDirectory( outdir.toString() );

                ConfigUtils.applyCommandline( config, Arrays.copyOfRange( args, 0, args.length ) ) ;

                OutputDirectoryLogging.initLoggingWithOutputDirectory( config.controler().getOutputDirectory() );

                EventsManager events = EventsUtils.createEventsManager();

                events.addHandler( new InfectionEventHandler( config ) );

                ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "Just before starting iterations");
                for ( int iteration=0 ; iteration<=100 ; iteration++ ){
                        events.resetHandlers( iteration );
                        EventsUtils.readEvents( events, episimConfig.getInputEventsFile() );
                }

                OutputDirectoryLogging.closeOutputDirLogging();

        }

}
