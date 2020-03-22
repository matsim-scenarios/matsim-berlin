package org.matsim.run.drt;

import org.matsim.run.drt.InfectionEventHandler.EpisimFacility;
import org.matsim.run.drt.InfectionEventHandler.EpisimVehicle;

import java.util.Random;

import static org.matsim.run.drt.InfectionEventHandler.*;

class EpisimUtils{
        private static double lastNow = -1 ;
        static double getCorrectedTime( double time, long iteration ) {
                final double now = Math.min( time, 3600. * 24 ) + iteration * 24. * 3600;
                if ( now < lastNow ) {
                        throw new RuntimeException( "we are going backwards in time; something is wrong" );
                }
                lastNow = now;
                return now;
        }
        /** @noinspection BooleanMethodIsAlwaysInverted*/
        static boolean hasStatusRelevantForInfectionDynamics( EpisimPerson personWrapper ){
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
        static boolean activityRelevantForInfectionDynamics( EpisimPerson person, EpisimConfigGroup episimConfig, int iteration, Random rnd ) {
                boolean actRelevant = true;
                String act = person.getTrajectory().get(person.getCurrentPositionInTrajectory());
                /*
//                if (episimConfig.getClosedActivity1() != null) {
//                        if ( act.contains( episimConfig.getClosedActivity1() )&& episimConfig.getClosedActivity1Date() <= iteration) {
//                                actRelevant = false;
//                        }
//                }
//                if (episimConfig.getClosedActivity2() != null ) {
//                        if ( act.contains( episimConfig.getClosedActivity2() ) && episimConfig.getClosedActivity2Date() <= iteration) {
//                                actRelevant = false;
//                        }
//                }
                 */
                if ( episimConfig.getShutdownDate() <= iteration ) {
                        actRelevant = actIsRelevant( rnd, act );
                }
                return actRelevant;
        }
        private static boolean actIsRelevant( Random rnd, String act ){
                boolean actRelevant;
                actRelevant = false ;
                if ( act.contains( "home" ) ) {
                        actRelevant = true ;
                } else if ( act.contains( "work" ) ) {
                        if ( rnd.nextDouble() < 0.2 ) {
                                actRelevant = true;
                        }
                } else if ( act.contains( "shop" ) ) {
                        if ( rnd.nextDouble() < 0.2 ) {
                                actRelevant = true;
                        }
                } else if ( act.contains( "leisure" ) ) {
                        if ( rnd.nextDouble() < 0.00 ) {
                                actRelevant = true;
                        }
                } else if ( act.contains( "errands" ) ) {
                        if ( rnd.nextDouble() < 0.2 ) {
                                actRelevant = true;
                        }
                } else if ( act.contains( "educ" ) ) {
                        if ( rnd.nextDouble() < 0.0 ) {
                                actRelevant = true ;
                        }
                } else if ( act.contains( "business" ) ) {
                        if ( rnd.nextDouble() < 0.2 ) {
                                actRelevant = true ;
                        }
                } else if ( act.contains( "other" ) ) {
                        if ( rnd.nextDouble() < 0.2 ) {
                                actRelevant = true ;
                        }
                } else if ( act.contains( "freight" ) ) {
                        if ( rnd.nextDouble() < 0.0 ) {
                                actRelevant = true ;
                        }
                } else {
                        throw new RuntimeException( "unexpected activity type=" + act );
                }
                return actRelevant;
        }
        static boolean tripRelevantForInfectionDynamics( EpisimPerson person, EpisimConfigGroup episimConfig, int iteration, Random rnd ) {
//                boolean tripRelevant = true;
                String lastAct = "";
                if (person.getCurrentPositionInTrajectory() != 0) {
                        lastAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory()-1);
                }

                String nextAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory());
/*
//                if (episimConfig.getClosedActivity1() != null && episimConfig.getClosedActivity1Date() <= iteration) {
//                        if (lastAct.contains(episimConfig.getClosedActivity1()) || nextAct.contains(episimConfig.getClosedActivity1())) {
//                                tripRelevant = false;
//                        }
//                }
//                if (episimConfig.getClosedActivity2() != null && episimConfig.getClosedActivity2Date() <= iteration) {
//                        if (lastAct.contains(episimConfig.getClosedActivity2()) || nextAct.contains(episimConfig.getClosedActivity2())) {
//                                tripRelevant = false;
//                        }
//                }
*/
//                return tripRelevant;

                return actIsRelevant( rnd, lastAct ) && actIsRelevant( rnd, nextAct );

        }

//        The following was a bit of a quick fix; I think we need to be able to do without it.  kai, mar'20
        /*
        static boolean isRelevantForShutdown( EpisimPerson person, EpisimConfigGroup episimConfig, EpisimContainer<?> container ) {
                boolean shutDownRelevant = false;
                if (container instanceof EpisimFacility) {
                        String act = person.getTrajectory().get(person.getCurrentPositionInTrajectory());
                        if (!act.contains("home")) {
                                shutDownRelevant = true;
                        }
                }
                else if (container instanceof EpisimVehicle) {
                        String lastAct = "";
                        if (person.getCurrentPositionInTrajectory() != 0) {
                                lastAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory()-1);

                        }
                        String nextAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory());
                        if (!nextAct.contains("home") || !lastAct.contains("home")) {
                                shutDownRelevant = true;
                        }

                }
                else {
                        throw new RuntimeException("something went wrong");
                }
                return shutDownRelevant;
        }
        */

        static boolean isRelevantForInfectionDynamics( EpisimPerson personLeavingContainer, EpisimContainer<?> container,
                                                       EpisimConfigGroup episimConfig, int iteration, Random rnd ){
                if ( !hasStatusRelevantForInfectionDynamics( personLeavingContainer ) || personLeavingContainer.getQuarantineStatus() == QuarantineStatus.full ) {
                        return true;
                }
                if (container instanceof EpisimFacility && !activityRelevantForInfectionDynamics(personLeavingContainer, episimConfig, iteration, rnd )) {
                        return true;
                }
                if (container instanceof EpisimVehicle && !tripRelevantForInfectionDynamics(personLeavingContainer, episimConfig, iteration, rnd )) {
                        return true;
                }
//                if (iteration >= episimConfig.getShutdownDate() && EpisimUtils.isRelevantForShutdown(personLeavingContainer, episimConfig, container)) {
//                		return;
//                }
                return false;
        }
}
