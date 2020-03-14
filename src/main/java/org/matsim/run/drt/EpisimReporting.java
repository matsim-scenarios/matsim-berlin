package org.matsim.run.drt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

class EpisimReporting{
        enum InfectionsWriterFields{ time, nInfected, nInQuarantine, nRecovered, nSusceptible, nContagious, nInfectedButNotContagious,
                nInfectedCumulative }

        enum InfectionEventsWriterFields{ time, infector, infected, infectionType }

        private static final Logger log = Logger.getLogger( EpisimReporting.class );
        private static int specificInfectionsCnt = 300;
        private final BufferedWriter infectionsWriter;
        private final BufferedWriter infectionEventsWriter;
        EpisimReporting( Config config ){
                EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule( config, EpisimConfigGroup.class );
                infectionsWriter = prepareWriter( episimConfig.getRunId() + "-infections.txt", InfectionsWriterFields.class );
                infectionEventsWriter = prepareWriter( episimConfig.getRunId() + "-infectionEvents.txt" , InfectionEventsWriterFields.class );
        }

        void reporting( Map<Id<Person>, EpisimPerson> personMap, int iteration ){
                long nSusceptible = 0;
                long nInfectedButNotContagious = 0;
                long nContagious = 0;
                long nRecovered = 0;
                long nQuarantined = 0;
                for( EpisimPerson person : personMap.values() ){
                        switch( person.getStatus() ) {
                                case susceptible:
                                        nSusceptible++;
                                        break;
                                case infectedButNotContagious:
                                        nInfectedButNotContagious++;
                                        break;
                                case contagious:
                                        nContagious++;
                                        break;
                                case recovered:
                                        nRecovered++;
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getStatus() );
                        }
                        switch( person.getQuarantineStatus() ) {
                                case yes:
                                        nQuarantined++;
                                        break;
                                case no:
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getQuarantineStatus() );
                        }
                }

                log.warn("===============================" );
                log.warn("Beginning day " + iteration );
                log.warn("No of susceptible persons=" + nSusceptible );
                log.warn( "No of infected but not contagious persons=" + nInfectedButNotContagious );
                log.warn( "No of contagious persons=" + nContagious );
                log.warn( "No of recovered persons=" + nRecovered );
                log.warn( "---" );
                log.warn( "No of persons in quarantaine=" + nQuarantined );
                log.warn("===============================" );

                String[] array = new String[InfectionsWriterFields.values().length];

                array[InfectionsWriterFields.time.ordinal()] = Double.toString( EpisimUtils.getCorrectedTime( 0.,iteration ) );
                array[InfectionsWriterFields.nSusceptible.ordinal()] = Long.toString( nSusceptible );
                array[InfectionsWriterFields.nInfectedButNotContagious.ordinal()] = Long.toString( nInfectedButNotContagious );
                array[InfectionsWriterFields.nContagious.ordinal()] = Long.toString( nContagious );
                array[InfectionsWriterFields.nRecovered.ordinal()] = Long.toString( nRecovered );

                array[InfectionsWriterFields.nInfected.ordinal()] = Long.toString( (nInfectedButNotContagious + nContagious) ) ;
                array[InfectionsWriterFields.nInfectedCumulative.ordinal()] = Long.toString( (nInfectedButNotContagious + nContagious + nRecovered) );

                array[InfectionsWriterFields.nInQuarantine.ordinal()] = Long.toString( nQuarantined );

                write( array, infectionsWriter );
        }
        private static void write( String[] array, BufferedWriter writer ){
                StringBuilder line = new StringBuilder();
                for( String str : array ){
                        line.append( str ).append( "\t" );
                }
                try{
                        writer.write( line.toString() );
                        writer.newLine();
                        writer.flush();
                } catch( IOException e ){
                        throw new RuntimeException( e );
                }
        }
        void reportInfection( EpisimPerson personWrapper, EpisimPerson infector, double now, String infectionType ){
                if ( specificInfectionsCnt-- > 0 ){
                        log.warn( "infection of personId=" + personWrapper.getPersonId() + " by person=" + infector.getPersonId() + " at/in " + infectionType );
                }
                {
                        String[] array = new String[InfectionEventsWriterFields.values().length];
                        array[InfectionEventsWriterFields.time.ordinal()] = Double.toString( now );
                        array[InfectionEventsWriterFields.infector.ordinal()] = infector.getPersonId().toString();
                        array[InfectionEventsWriterFields.infected.ordinal()] = personWrapper.getPersonId().toString();
                        array[InfectionEventsWriterFields.infectionType.ordinal()] = infectionType;

                        write( array, infectionEventsWriter );
                }
        }
        private static BufferedWriter prepareWriter( String filename, Class<? extends Enum<?>> enumClass ){
                BufferedWriter writer = IOUtils.getBufferedWriter( filename );
                StringBuilder line = new StringBuilder();
                for( Enum<?> enumConstant : enumClass.getEnumConstants() ){
                        line.append( enumConstant.name() ).append( "\t" );
                }
                try{
                        writer.write( line.toString() );
                        writer.newLine();
                } catch( IOException e ){
                        throw new RuntimeException( e );
                }
                return writer;
        }
}
