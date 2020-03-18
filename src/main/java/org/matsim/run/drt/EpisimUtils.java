package org.matsim.run.drt;

import org.matsim.run.drt.InfectionEventHandler.EpisimFacility;
import org.matsim.run.drt.InfectionEventHandler.EpisimVehicle;

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
                switch( personWrapper.getStatus() ) {
                        case susceptible:
                                return true;
                        case infectedButNotContagious:
                                return false;
                        case contagious:
                                return true;
                        case recovered:
                                return false;
                        default:
                                throw new IllegalStateException( "Unexpected value: " + personWrapper.getStatus() );
                }
        }
        static boolean activityRelevantForInfectionDynamics( EpisimPerson person, EpisimConfigGroup episimConfig, int iteration) {
                boolean actRelevant = true;
                String act = person.getTrajectory().get(person.getCurrentPositionInTrajectory());
                if (episimConfig.getClosedActivity1() != null) {
                        if ( act.contains( episimConfig.getClosedActivity1() )&& episimConfig.getClosedActivity1Date() <= iteration) {
                                actRelevant = false;
                        }
                }
                if (episimConfig.getClosedActivity2() != null ) {
                        if ( act.contains( episimConfig.getClosedActivity2() ) && episimConfig.getClosedActivity2Date() <= iteration) {
                                actRelevant = false;
                        }
                }
                return actRelevant;
        }
        static boolean tripRelevantForInfectionDynamics( EpisimPerson person, EpisimConfigGroup episimConfig, int iteration ) {
                boolean tripRelevant = true;
                String lastAct = "";
                if (person.getCurrentPositionInTrajectory() != 0) {
                        lastAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory()-1);

                }
                String nextAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory());

                if (episimConfig.getClosedActivity1() != null && episimConfig.getClosedActivity1Date() <= iteration) {
                        if (lastAct.contains(episimConfig.getClosedActivity1()) || nextAct.contains(episimConfig.getClosedActivity1())) {
                                tripRelevant = false;
                        }
                }
                if (episimConfig.getClosedActivity2() != null && episimConfig.getClosedActivity2Date() <= iteration) {
                        if (lastAct.contains(episimConfig.getClosedActivity2()) || nextAct.contains(episimConfig.getClosedActivity2())) {
                                tripRelevant = false;
                        }
                }

                return tripRelevant;
        }
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
}
