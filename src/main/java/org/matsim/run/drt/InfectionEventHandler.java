package org.matsim.run.drt;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.*;

class InfectionEventHandler implements BasicEventHandler {
        private static final Logger log = Logger.getLogger( InfectionEventHandler.class );

        @Inject private Scenario scenario;

        private Map<Id<Person>,PersonWrapper> personMap = new HashMap<>();
        private Map<Id<Vehicle>,VehicleWrapper> vehicleMap = new HashMap<>();
        private Map<Id<Link>,LinkWrapper> linkMap = new HashMap<>();

        private int cnt = 10 ;

        @Override public void handleEvent( Event event ){
                if ( event instanceof PersonEntersVehicleEvent ) {

                        VehicleWrapper vehicleWrapper = this.vehicleMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getVehicleId(), VehicleWrapper::new );

                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getPersonId(), PersonWrapper::new );

                        vehicleWrapper.addPerson( personWrapper );

                        handleInitialInfections( personWrapper );

                        infectionDynamics( vehicleWrapper.getPersons() );
                }
                if ( event instanceof PersonArrivalEvent ) {

                        LinkWrapper linkWrapper = this.linkMap.computeIfAbsent( ((PersonArrivalEvent) event).getLinkId(), LinkWrapper::new );

                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonArrivalEvent) event).getPersonId(), PersonWrapper::new );

                        linkWrapper.addPerson( personWrapper );

                        infectionDynamics( linkWrapper.getPersons() );

                }
                if (event instanceof PersonLeavesVehicleEvent ) {
                        // the fact that nothing is done here means that an infected person that enters a vehicle leaves the virus in the vehicle forever
                }
                if ( event instanceof PersonDepartureEvent ) {
                        // the fact that nothing is done here means that an infected person that arrives at a link leaves the virus at the link forever
                }

        }
        private void handleInitialInfections( PersonWrapper personWrapper ){
                // initial infections:
                if( cnt > 0 ){
                        final Person person = PopulationUtils.findPerson( personWrapper.personId, scenario );
                        if( person != null ){
                                person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                                cnt--;
                                personWrapper.setStatus( Status.infected );
                        }
                }
        }
        private void infectionDynamics( Set<PersonWrapper> persons ){
                // this very simplified infection dynamics assumes that one infected person on a link or in a vehicle infects everybody
                // including drivers!

                boolean infected = false;
                for( PersonWrapper person : persons ){
                        if( person.getStatus() == Status.infected ){
                                infected = true;
                                break;
                        }
                }
                if( infected ){
                        for( PersonWrapper person : persons ){
                                infectPerson( person );
                        }
                }
        }
        private void infectPerson( PersonWrapper personWrapper ){
                Status prevStatus = personWrapper.getStatus();
                personWrapper.setStatus( Status.infected );
                final Person person = PopulationUtils.findPerson( personWrapper.personId, scenario );
                if ( person!=null ){
                        person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                }
                if ( prevStatus!= Status.infected ) {
                        log.warn( "infection of personId=" + personWrapper.getPersonId() );
                }
        }
        @Override public void reset( int iteration ){
        }

        private static class LinkWrapper {
                private final Id<Link> linkId;
                private Set<PersonWrapper> persons = new HashSet<>();
                LinkWrapper( Id<Link> vehicleId ) {
                        this.linkId = vehicleId;
                }
                void addPerson( PersonWrapper person ) {
                        persons.add( person );
                }
                public Id<Link> getLinkId(){
                        return linkId;
                }
                public Set<PersonWrapper> getPersons(){
                        return Collections.unmodifiableSet( persons );
                }
        }
        private static class VehicleWrapper {
                private final Id<Vehicle> vehicleId;
                private Set<PersonWrapper> persons = new HashSet<>();
                VehicleWrapper( Id<Vehicle> vehicleId ) {
                        this.vehicleId = vehicleId;
                }
                void addPerson( PersonWrapper person ) {
                        persons.add( person );
                }
                public Id<Vehicle> getVehicleId(){
                        return vehicleId;
                }
                public Set<PersonWrapper> getPersons(){
                        return Collections.unmodifiableSet( persons );
                }
        }
        private static class PersonWrapper {
                private final Id<Person> personId;
                private Status status = Status.susceptible;
                PersonWrapper( Id<Person> personId ) {
                        this.personId = personId;
                }
                void setStatus( Status status ) {
                        this.status = status;
                }
                public Id<Person> getPersonId(){
                        return personId;
                }
                Status getStatus(){
                        return status;
                }
        }
        enum Status {susceptible, infected, immune};

}

