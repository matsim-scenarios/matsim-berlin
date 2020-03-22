package org.matsim.run.drt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class EpisimPerson{
        private final Id<Person> personId;
        private InfectionEventHandler.Status status = InfectionEventHandler.Status.susceptible;
        private InfectionEventHandler.QuarantineStatus quarantineStatus = InfectionEventHandler.QuarantineStatus.no;
        private int infectionDate;
        private int quarantineDate;
        private int currentPositionInTrajectory;
        private String lastFacilityId;
        private Set<EpisimPerson> tracableContactPersons = new LinkedHashSet<>();
        private List<String> trajectory = new ArrayList<>();
        EpisimPerson( Id<Person> personId ) {
                this.personId = personId;
        }
        void setStatus( InfectionEventHandler.Status status ) {
                this.status = status;
        }
        void setQuarantineStatus( InfectionEventHandler.QuarantineStatus quarantineStatus ) {
                this.quarantineStatus = quarantineStatus;
        }
        Id<Person> getPersonId(){
                return personId;
        }
        InfectionEventHandler.Status getStatus(){
                return status;
        }
        InfectionEventHandler.QuarantineStatus getQuarantineStatus(){
                return quarantineStatus;
        }
        void setInfectionDate (int date) {
                this.infectionDate = date;
        }
        int getInfectionDate () {
                return this.infectionDate;
        }
        void setQuarantineDate (int date) {
                this.quarantineDate = date;
        }
        int getQuarantineDate () {
                return this.quarantineDate;
        }
        void setLastFacilityId (String lastFacilityId) {
            this.lastFacilityId = lastFacilityId;
        }
        String getLastFacilityId () {
            return this.lastFacilityId;
        }
        void addTracableContactPerson( EpisimPerson personWrapper ) {
                tracableContactPersons.add( personWrapper );
        }
        Set<EpisimPerson> getTracableContactPersons() {
                return tracableContactPersons;
        }
        void addToTrajectory( String trajectoryElement ) {
        	trajectory.add( trajectoryElement );
        }
        List<String> getTrajectory() {
            return trajectory;
        }
        int getCurrentPositionInTrajectory () {
            return this.currentPositionInTrajectory;
        }
        void setCurrentPositionInTrajectory (int currentPositionInTrajectory) {
        	this.currentPositionInTrajectory = currentPositionInTrajectory;
        }
}
