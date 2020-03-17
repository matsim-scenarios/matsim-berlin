package org.matsim.run.drt;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;

import java.io.IOException;
import java.util.Arrays;

class KNEventsInfection2{

        public static void main( String[] args ) throws IOException{
                OutputDirectoryLogging.catchLogEntries();

                Config config = ConfigUtils.createConfig( new EpisimConfigGroup() );
                EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule( config, EpisimConfigGroup.class );

//                episimConfig.setInputEventsFile( "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_wo_linkEnterLeave.xml.gz" );
                episimConfig.setInputEventsFile( "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz" );
                episimConfig.setSample(0.01);
                episimConfig.setCalibrationParameter(2);

                //                                filename = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/output-berlin-v5.4-10pct/berlin-v5.4-10pct.output_events_reduced.xml.gz";

//                episimConfig.setInputEventsFile( "../snzDrt220.0.events.reduced.xml.gz" );
//                episimConfig.setSample(0.25);
//                episimConfig.setCalibrationParameter(0.000_000_5);

                config.controler().setOutputDirectory( "output-2-wo-pt-no-work-no-leisure" );

                episimConfig.setUsePt( EpisimConfigGroup.UsePt.no );
                episimConfig.setClosedActivity1( "work" );
//                episimConfig.setClosedActivity1Sample( 1. );
                episimConfig.setClosedActivity2( "leisure" );
//                episimConfig.setClosedActivity2Sample( 1. );

                ConfigUtils.applyCommandline( config, Arrays.copyOfRange( args, 0, args.length ) ) ;

                OutputDirectoryLogging.initLoggingWithOutputDirectory( config.controler().getOutputDirectory() );

                EventsManager events = EventsUtils.createEventsManager();

                events.addHandler( new InfectionEventHandler( config ) );
                InfectionEventHandler.scenarioWithFacilites = false ;

                ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "Just before starting iterations");
                for ( int iteration=0 ; iteration<=100 ; iteration++ ){
                        events.resetHandlers( iteration );
                        EventsUtils.readEvents( events, episimConfig.getInputEventsFile() );
                }

                OutputDirectoryLogging.closeOutputDirLogging();

        }

}
