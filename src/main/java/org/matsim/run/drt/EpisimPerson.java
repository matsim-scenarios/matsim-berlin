package org.matsim.run.drt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.HashSet;
import java.util.Set;

class EpisimPerson{
        private final Id<Person> personId;
        private InfectionEventHandler.Status status = InfectionEventHandler.Status.susceptible;
        private InfectionEventHandler.QuarantineStatus quarantineStatus = InfectionEventHandler.QuarantineStatus.no;
        private int infectionDate;
        private int quarantineDate;
        private Set<EpisimPerson> tracableContactPersons = new HashSet<>();
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
        void addTracableContactPerson( EpisimPerson personWrapper ) {
                tracableContactPersons.add( personWrapper );
        }
        Set<EpisimPerson> getTracableContactPersons() {
                return tracableContactPersons;
        }
}
