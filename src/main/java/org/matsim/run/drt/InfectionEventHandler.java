package org.matsim.run.drt;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

class InfectionEventHandler implements BasicEventHandler {
        private static final Logger log = Logger.getLogger( InfectionEventHandler.class );

        @Inject private Scenario scenario;

        private Map<Id<Person>,PersonWrapper> personMap = new HashMap<>();
        private Map<Id<Vehicle>,VehicleWrapper> vehicleMap = new HashMap<>();
        private Map<String,PseudoFacilityWrapper> pseudoFacilityMap = new HashMap<>();
        private Map<Id<Link>,LinkWrapper> linkMap = new HashMap<>();

        private int cnt = 10 ;
        private int noOfInfectedPersons = cnt;
        private int noOfInfectedDrivers = 0;

        private BufferedWriter writer;

        private enum Fields { time, nInfected, nInfectedDrivers, nInfectedPersons }

        private int iteration=0;

        private Random rnd = MatsimRandom.getLocalInstance();

        @Inject InfectionEventHandler() {
                writer = IOUtils.getBufferedWriter( "infection.txt" );
                StringBuilder line = new StringBuilder();
                for( Fields field : Fields.values() ){
                        line.append( field.name() ).append( "\t" );
                }
                try{
                        writer.write( line.toString() );
                        writer.newLine();
                } catch( IOException e ){
                        throw new RuntimeException( e );
                }
        }

        @Override public void handleEvent( Event event ){
                if ( event instanceof PersonEntersVehicleEvent ) {

                        VehicleWrapper vehicleWrapper = this.vehicleMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getVehicleId(), VehicleWrapper::new );

                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getPersonId(), PersonWrapper::new );

                        vehicleWrapper.addPerson( personWrapper );

                        handleInitialInfections( personWrapper );

                        infectionDynamics( vehicleWrapper.getPersons(), event.getTime() );
                }
                if ( event instanceof PersonArrivalEvent ) {
                	
                        this.linkMap.computeIfAbsent( ((PersonArrivalEvent) event).getLinkId(), LinkWrapper::new );

                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonArrivalEvent) event).getPersonId(), PersonWrapper::new );
                        
                        personWrapper.setLastLink(((PersonArrivalEvent) event).getLinkId());

//                        linkWrapper.addPerson( personWrapper );
//                       
//                        infectionDynamics( linkWrapper.getPersons() );

                }
                if (event instanceof ActivityStartEvent) {
                		
                		if(!((ActivityStartEvent) event).getPersonId().toString().startsWith("drt")) {
                		
	                		PersonWrapper personWrapper = this.personMap.get(((ActivityStartEvent) event).getPersonId());
	
	                		LinkWrapper linkWrapper = this.linkMap.get(personWrapper.getLastLink());
	                		
	                		String pseudoFacilityId =  ((ActivityStartEvent) event).getActType().toString().split("_")[0] + "_" + linkWrapper.getLinkId().toString();
	                	
	                		PseudoFacilityWrapper pseudoFacilityWrapper = this.pseudoFacilityMap.computeIfAbsent(pseudoFacilityId, PseudoFacilityWrapper::new);
	                		
	                		pseudoFacilityWrapper.addPerson(personWrapper);
	                		
	                		linkWrapper.addPseudoFacility(pseudoFacilityWrapper);
	                		
	                		infectionDynamics( pseudoFacilityWrapper.getPersons(), event.getTime());
                		}
                }
                if (event instanceof PersonLeavesVehicleEvent ) {
                        // the fact that nothing is done here means that an infected person that enters a vehicle leaves the virus in the vehicle forever
                }
                if ( event instanceof PersonDepartureEvent ) {
                        // if nothing is done here it means that an infected person that arrives at a link leaves the virus at the link forever
//                	for (LinkWrapper linkWrapper : linkMap.values()) {
//                		boolean foundPersonWrapper = false;
//                		PersonWrapper toBeDeletedPersonWrapper = null;
//                		for (PersonWrapper personWrapper : linkWrapper.getPersons()) {
//                			if (personWrapper.getPersonId().equals(((PersonDepartureEvent ) event).getPersonId())) {
//                				toBeDeletedPersonWrapper = personWrapper;
//                				foundPersonWrapper = true;
//                				break;
//                			}
//                		}
//                		if (foundPersonWrapper) {
//            				linkWrapper.deletePerson(toBeDeletedPersonWrapper);
//                			break;
//            			}
//                	}
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
                                log.warn(" person " + personWrapper.personId +" has initial infection");
                        }
                }
        }
        private void infectionDynamics( Set<PersonWrapper> persons, double now ){

                for( PersonWrapper infector : persons ){
                        if( infector.getStatus() == Status.infected ){
                                for( PersonWrapper person : persons ){
                                        if ( rnd.nextDouble() < 1. ){
                                                infectPerson( person, infector, now );
                                        }
                                }
                        }
                }

        }
        private double lastTimeStep = 0 ;
        private void infectPerson( PersonWrapper personWrapper, PersonWrapper infector, double now ){
                if ( personWrapper.getPersonId().toString().startsWith( "pt_pt" ) ) {
                        return;
                }
                Status prevStatus = personWrapper.getStatus();
                personWrapper.setStatus( Status.infected );
                final Person person = PopulationUtils.findPerson( personWrapper.personId, scenario );
                if ( person!=null ){
                        person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                }
                if ( prevStatus!= Status.infected ) {
                	if (personWrapper.getPersonId().toString().startsWith("pt_pt")) {
                		noOfInfectedDrivers++;
                	}
                	else {
                		noOfInfectedPersons++;
                	}
                	if ( now - lastTimeStep>=300 ){
                                lastTimeStep = now;
                                log.warn( "infection of personId=" + personWrapper.getPersonId() + " by person=" + infector.getPersonId() );
                                log.warn( "No of infected persons=" + noOfInfectedPersons );
                                log.warn( "No of infected drivers=" + noOfInfectedDrivers );


                                String[] array = new String[Fields.values().length];

                                array[Fields.time.ordinal()] = Double.toString( now + iteration * 3600. * 24. );
                                array[Fields.nInfectedDrivers.ordinal()] = Double.toString( noOfInfectedDrivers );
                                array[Fields.nInfectedPersons.ordinal()] = Double.toString( noOfInfectedPersons );
                                array[Fields.nInfected.ordinal()] = Double.toString( noOfInfectedDrivers + noOfInfectedPersons );

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
                }
        }
        @Override public void reset( int iteration ){
                this.iteration = iteration;
        }
        private static class LinkWrapper {
                private final Id<Link> linkId;
                private Set<PseudoFacilityWrapper> pseudoFacilites = new HashSet<>();
                LinkWrapper( Id<Link> vehicleId ) {
                        this.linkId = vehicleId;
                }
                void addPseudoFacility( PseudoFacilityWrapper pseudoFacility ) {
                	pseudoFacilites.add( pseudoFacility );
                }
                public Id<Link> getLinkId(){
                        return linkId;
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
                private Id<Link> lastLink;
                private Status status = Status.susceptible;
                PersonWrapper( Id<Person> personId ) {
                        this.personId = personId;
                        this.lastLink = null;
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
                Id<Link> getLastLink(){
                		return lastLink;
                }
                void setLastLink(Id<Link> lastLink){
            			this.lastLink = lastLink;
                }
        }
        private static class PseudoFacilityWrapper {
        	private final String pseudoFacilityId;
        	private Set<PersonWrapper> persons = new HashSet<>();
        	PseudoFacilityWrapper(String pseudoFacilityId) {
        		this.pseudoFacilityId = pseudoFacilityId;
        	}
        	void addPerson( PersonWrapper person ) {
        		persons.add( person );
        	}
        	void deletePerson( PersonWrapper person ) {
        		persons.remove( person );
        	}
            public Set<PersonWrapper> getPersons(){
                return Collections.unmodifiableSet( persons );
            }
        }
        enum Status {susceptible, infected, immune};

}

