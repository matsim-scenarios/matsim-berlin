package org.matsim.run.drt;

import org.apache.log4j.Logger;
import org.matsim.run.drt.EpisimConfigGroup.InfectionParams;
import org.matsim.run.drt.InfectionEventHandler.EpisimFacility;
import org.matsim.run.drt.InfectionEventHandler.EpisimVehicle;

import java.util.Random;

import static org.matsim.run.drt.InfectionEventHandler.*;

class EpisimUtils{
        private static final Logger log = Logger.getLogger( EpisimUtils.class );
        private static double lastNow = -1 ;
        static double getCorrectedTime( double time, long iteration ) {
                final double now = Math.min( time, 3600. * 24 ) + iteration * 24. * 3600;
                if ( now < lastNow ) {
                        throw new RuntimeException( "we are going backwards in time; something is wrong" );
                }
                lastNow = now;
                return now;
        }
        /** */
        private static boolean hasStatusRelevantForInfectionDynamics( EpisimPerson personWrapper ){
                switch( personWrapper.getDiseaseStatus() ) {
                        case susceptible:
                                return true;
                        case infectedButNotContagious:
                                return false;
                        case contagious:
                                return true;
                        case seriouslySick:
                                return false; // assume is in hospital
                        case critical:
                                return false; // assume is in hospital
                        case recovered:
                                return false;
                        default:
                                throw new IllegalStateException( "Unexpected value: " + personWrapper.getDiseaseStatus() );
                }
        }
        private static boolean activityRelevantForInfectionDynamics( EpisimPerson person, EpisimConfigGroup episimConfig, int iteration, Random rnd ) {
                String act = person.getTrajectory().get(person.getCurrentPositionInTrajectory());
                return actIsRelevant( rnd,act, iteration, episimConfig );
        }
        private static boolean actIsRelevant( Random rnd, String act, long iteration, EpisimConfigGroup episimConfig ){
                boolean actRelevant = true;
                for( InfectionParams infectionParams : episimConfig.getContainerParams().values() ){
                        if ( act.contains( infectionParams.getContainerName() ) ) {
                                if ( infectionParams.getShutdownDay() <= iteration ) {
                                        if ( rnd.nextDouble() >= infectionParams.getRemainingFraction() ) {
                                                actRelevant=false;
                                        }
                                }
                        }
                }
                return actRelevant;
        }
        private static boolean tripRelevantForInfectionDynamics( EpisimPerson person, EpisimConfigGroup episimConfig, int iteration, Random rnd ) {
                String lastAct = "";
                if (person.getCurrentPositionInTrajectory() != 0) {
                        lastAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory()-1);
                }

                String nextAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory());

                return actIsRelevant( rnd, lastAct, iteration, episimConfig ) && actIsRelevant( rnd, nextAct, iteration, episimConfig );

        }

        /** @noinspection BooleanMethodIsAlwaysInverted*/
        static boolean isRelevantForInfectionDynamics( EpisimPerson personLeavingContainer, EpisimContainer<?> container,
                                                       EpisimConfigGroup episimConfig, int iteration, Random rnd ){
                if ( !hasStatusRelevantForInfectionDynamics( personLeavingContainer ) ) {
                        return false ;
                }
                if ( personLeavingContainer.getQuarantineStatus() == QuarantineStatus.full ) {
                        return false ;
                }
                if (container instanceof EpisimFacility && activityRelevantForInfectionDynamics(personLeavingContainer, episimConfig, iteration, rnd )){
                        return true;
                }
                if ( container instanceof EpisimVehicle && tripRelevantForInfectionDynamics(personLeavingContainer, episimConfig, iteration, rnd )) {
                        return true ;
                }
                return false;
        }
}
