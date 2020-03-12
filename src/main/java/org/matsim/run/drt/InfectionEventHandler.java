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
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

class InfectionEventHandler implements BasicEventHandler {
        // Some notes:

        // * Especially if we repeat the same events file, then we do not have complete mixing.  So it may happen that only some subpopulations gets infected.

        // * However, if with infection proba=1 almost everybody gets infected, then in our current setup (where infected people remain in the iterations),
        // this will also happen with lower probabilities, albeit slower.  This is presumably the case that we want to investigate.

        // * We seem to be getting two different exponential spreading rates.  With infection proba=1, the crossover is (currently) around 15h.


        private static final Logger log = Logger.getLogger( InfectionEventHandler.class );
        private static final boolean scenarioWithFacilites = true;

        @Inject private Scenario scenario;

        private Map<Id<Person>,PersonWrapper> personMap = new HashMap<>();
        private Map<Id<Vehicle>,VehicleWrapper> vehicleMap = new HashMap<>();
        private Map<Id<Facility>,PseudoFacilityWrapper> pseudoFacilityMap = new HashMap<>();
        private Map<Id<Link>,LinkWrapper> linkMap = new HashMap<>();
        
        private int cnt = 10 ;
        private int noOfInfectedPersons = cnt;
        private int noOfPersonsInQuarantine = 0;
        private int noOfImmunePersons = 0;
        private int populationSize = 0;

        private BufferedWriter infectionsWriter;
        private BufferedWriter infectionEventsWriter;

        private enum InfectionsWriterFields{ time, nInfectedPersons, nPersonsInQuarantine, nImmunePersons, nSusceptiblePersons }
        private enum InfectionEventsWriterFields{ time, infector, infected, infectionType }
        
        private EpisimConfigGroup episimConfig;
        
        private int iteration=0;

        private Random rnd = MatsimRandom.getLocalInstance();

        @Inject InfectionEventHandler(EpisimConfigGroup episimConfig) {
                infectionsWriter = prepareWriter( episimConfig.getRunId() + "-infections.txt", InfectionsWriterFields.class );
                infectionEventsWriter = prepareWriter( episimConfig.getRunId() + "-infectionEvents.txt" , InfectionEventsWriterFields.class );
                this.episimConfig = episimConfig;
        }
        private BufferedWriter prepareWriter( String filename, Class<? extends Enum<?>> enumClass ){
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

        @Override public void handleEvent( Event event ){
        	
        		if (event.getTime() >= 86400) {
        			return;
        		}

                // the events follow the matsim sequence, i.e. all agents start at activities.  kai, mar'20

                if ( event instanceof ActivityEndEvent ) {
            			//ignore drt and stage activities
                    	if(((ActivityEndEvent) event).getPersonId().toString().startsWith("drt") || ((ActivityEndEvent) event).getActType().endsWith("interaction")){
                    		return;
                    	}
                    	// if configured, there will be no infections at activities of a certain type
                    	// the probability that a there is no infection at a certain activity can be set via episimConfig.getClosedActivity1Sample()
                    	// if closedActivity1() is set to "work" and activity1Sample() is set to "0.5" this essentially means that 50% of working people do home office 
                    	// to do: also switch off infections in pt to / from closed activity.		SM, mar'20
                    	if (episimConfig.getClosedActivity1() != null) {
                    		if (rnd.nextDouble() < episimConfig.getClosedActivity1Sample() && episimConfig.getClosedActivity1().toString().equals(((ActivityEndEvent) event).getActType())) {
                    			return;
                    		}
                    	}

                        // find the link
                        LinkWrapper link = this.linkMap.computeIfAbsent( ((ActivityEndEvent) event).getLinkId() , LinkWrapper::new );

                        // go through all facilities on link to eventually find the person and remove it:
                        for( PseudoFacilityWrapper facilityWrapper : link.getPseudoFacilities() ){
                                PersonWrapper person = facilityWrapper.getPersons().get( ((ActivityEndEvent) event).getPersonId() );
                                if( person != null ){
                                		
                                        // run infection dynamics for this person and facility:
                                        infectionDynamicsFacility( person, facilityWrapper, event.getTime() + iteration * 3600. * 24., ((ActivityEndEvent) event).getActType() );
                                        facilityWrapper.removePerson( person.getPersonId() );
                                        handleInitialInfections( person );
                                        break;
                                }
                        }

                }  else if ( event instanceof PersonEntersVehicleEvent ) {
                		// if pt is shut down nothing happens here
                		if (episimConfig.getUsePt() == EpisimConfigGroup.UsePt.no) {
                			return;
                		}
                		// ignore pt drivers
                		if (((PersonEntersVehicleEvent) event).getPersonId().toString().startsWith("pt_pt") || ((PersonEntersVehicleEvent) event).getPersonId().toString().startsWith("pt_tr")) {
            				return;
            			}

                        // find the person:
                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getPersonId(), PersonWrapper::new );

                        // find the vehicle:
                        VehicleWrapper vehicleWrapper = this.vehicleMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getVehicleId(), VehicleWrapper::new );

                        // add person to vehicle and memorize entering time (yy this should rather be one method so):
                        vehicleWrapper.addPerson( personWrapper, event.getTime() + iteration * 3600. * 24. );

//                        handleInitialInfections( personWrapper );

                }  else if (event instanceof PersonLeavesVehicleEvent ) {
                		// if pt is shut down nothing happens here
                		if (episimConfig.getUsePt() == EpisimConfigGroup.UsePt.no) {
            				return;
            			}
                		
                		// ignore pt drivers
                		if (((PersonLeavesVehicleEvent) event).getPersonId().toString().startsWith("pt_pt") || ((PersonLeavesVehicleEvent) event).getPersonId().toString().startsWith("pt_tr")) {
            				return;
            			}

                        // find vehicle:
                        VehicleWrapper vehicle = this.vehicleMap.get( ((PersonLeavesVehicleEvent) event).getVehicleId() );

                        // remove person from vehicle:
                        PersonWrapper personWrapper = vehicle.getPersons().get( ((PersonLeavesVehicleEvent) event).getPersonId() );

                        infectionDynamicsVehicle( personWrapper, vehicle, event.getTime() + iteration * 3600. * 24.);
                        
                        vehicle.removePerson( personWrapper.getPersonId() );

                }  else if (event instanceof ActivityStartEvent) {
                		//see ActivityEndEvent for explanation
                		if (episimConfig.getClosedActivity1() != null) {
                				if (rnd.nextDouble() < episimConfig.getClosedActivity1Sample() && episimConfig.getClosedActivity1().toString().equals(((ActivityStartEvent) event).getActType())) {
                					return;
                				}
                		}
                		//ignore drt and stage activities
                        if(((ActivityStartEvent) event).getPersonId().toString().startsWith("drt") || ((ActivityStartEvent) event).getActType().endsWith("interaction")){
                                return;
                        }

                        // find the person:
                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((ActivityStartEvent) event).getPersonId(), PersonWrapper::new );

                        // find the link:
                        LinkWrapper linkWrapper = this.linkMap.computeIfAbsent( ((ActivityStartEvent) event).getLinkId(), LinkWrapper::new );

                        // create pseudo facility id that includes the activity type:
                        Id<Facility> pseudoFacilityId = createPseudoFacilityId( ((ActivityStartEvent) event), linkWrapper.getLinkId() );

                        // find the facility
                        PseudoFacilityWrapper pseudoFacilityWrapper = this.pseudoFacilityMap.computeIfAbsent(pseudoFacilityId, PseudoFacilityWrapper::new);

                        // add facility to link
                        linkWrapper.addPseudoFacility(pseudoFacilityWrapper);

                        // add person to facility
                        pseudoFacilityWrapper.addPerson(personWrapper, event.getTime() + iteration * 3600. * 24.);

                }

        }
        private Id<Facility> createPseudoFacilityId( ActivityStartEvent event, Id<Link> linkId ) {
        		if (scenarioWithFacilites ) {
        			return Id.create( event.getFacilityId(), Facility.class );
        		}
        		else {
        			return Id.create( event.getActType().split("_" )[0] + "_" + linkId.toString(), Facility.class );
        		}
                
        }
        private void handleInitialInfections( PersonWrapper personWrapper ){
                // initial infections:
                if( cnt > 0 ){
                		personWrapper.setStatus( Status.infectedButNotContagious );
                		personWrapper.setInfectionDate(iteration);
                		log.warn(" person " + personWrapper.personId +" has initial infection");
                		cnt--;
                        if ( scenario!=null ){
                                final Person person = PopulationUtils.findPerson( personWrapper.personId, scenario );
                                if( person != null ){
                                        person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                                }
                        }
                }
        }
        private void infectionDynamicsVehicle( PersonWrapper personLeavingVehicle, VehicleWrapper vehicle, double now ){
                infectionDynamicsGeneralized( personLeavingVehicle, vehicle, now, vehicle.getContainerId().toString() );
        }
        private void infectionDynamicsFacility( PersonWrapper personLeavingFacility, PseudoFacilityWrapper facility,  double now, String actType ) {
                infectionDynamicsGeneralized( personLeavingFacility, facility, now, actType );
        }
        private void infectionDynamicsGeneralized( PersonWrapper personLeavingContainer, ContainerWrapper<?> container, double now, String infectionType ) {

                if ( !hasStatusRelevantForInfectionDynamics( personLeavingContainer ) || personLeavingContainer.getQuarantineStatus() == QuarantineStatus.yes ) {
                        return ;
                }

                List<PersonWrapper> persons = new ArrayList<>( container.getPersons().values() ) ;
                Collections.shuffle( persons, rnd ); // so we do not always see the persons in the same sequence in each iteration

                final Map<Id<Person>, Double> containerEnteringTimes = container.getContainerEnteringTimes();

                int contactPersons = 0 ;
                for (PersonWrapper otherPerson : persons) {
                        // we are essentially looking at the situation when the person leaves the container.  Interactions with other persons who have
                        // already left the container were treated then.  In consequence, we have some "circle of persons around us" (yyyy which should
                        //  depend on the density), and then a probability of infection in either direction.

                        // For the time being, will just assume that the first 10 persons are the ones we interact with.  Note that because of
                        // shuffle, those are 10 different persons every day.
                		if ( personLeavingContainer.getPersonId()==otherPerson.getPersonId() ) {
                			// if person find itself nothing happens
                			continue;
                		}
                		contactPersons++;
 
                        // (we count "quarantine" as well since they essentially represent "holes", i.e. persons who are no longer there and thus the
                        // density in the transit container goes down.  kai, mar'20)

                        if ( !hasStatusRelevantForInfectionDynamics( otherPerson ) || otherPerson.getQuarantineStatus() == QuarantineStatus.yes) {
                                continue;
                        }

                        if ( personLeavingContainer.getStatus()==otherPerson.getStatus() ) {
                                // (if they have the same status, then nothing can happen between them)
                                continue;
                        }

                        Double containerEnterTimeOfPersonLeaving = containerEnteringTimes.get( personLeavingContainer.getPersonId() );
                        Double containerEnterTimeOfOtherPerson = containerEnteringTimes.get( otherPerson.getPersonId() );

                        // persons leaving their first-ever activity have no starting time for that activity.  Need to hedge against that.  Since all persons
                        // start healthy (the first seeds are set at enterVehicle), we can make some assumptions.
                        if ( containerEnterTimeOfPersonLeaving==null && containerEnterTimeOfOtherPerson==null ) {
                                throw new RuntimeException( "should not happen" );
                                // null should only happen at first activity.  However, at first activity all persons are susceptible.  So the only way we
                                // can get here if an infected person entered the container and is now leaving again, while the other person has been in the
                                // container from the beginning.  ????  kai, mar'20
                        }
                        if ( containerEnterTimeOfPersonLeaving==null ) {
                                containerEnterTimeOfPersonLeaving = Double.NEGATIVE_INFINITY;
                        }
                        if ( containerEnterTimeOfOtherPerson==null ) {
                                containerEnterTimeOfOtherPerson = Double.NEGATIVE_INFINITY;
                        }

                        double jointTimeInContainer = now - Math.max( containerEnterTimeOfPersonLeaving, containerEnterTimeOfOtherPerson );
                        if ( jointTimeInContainer  < 0 || jointTimeInContainer > 86400) {
                        	throw new RuntimeException("joint time in cointainer is not plausible for personLeavingContainer=" + personLeavingContainer.getPersonId() + " and otherPerson=" + otherPerson.getPersonId() + ". Joint time is=" + jointTimeInContainer);
                        }
                        
//                      exponential model by Smieszek. 
//                      equation 3.2 is used (simplified equation 3.1 which includes the shedding rate and contact intensity into the calibrationParameter
                        int contactIntensity = 1;
                        if (container instanceof VehicleWrapper) {
                        	contactIntensity = 10;
                        }
                        double infectionProba = 1 - Math.exp( - episimConfig.getCalibrationParameter() * contactIntensity * jointTimeInContainer);
                        if ( rnd.nextDouble() < infectionProba ) {
                            if ( personLeavingContainer.getStatus()==Status.susceptible ) {
                                    infectPerson( personLeavingContainer, otherPerson, now, infectionType );
                                    return;
                            } else {
                                    infectPerson( otherPerson, personLeavingContainer, now, infectionType );
                            }
                        }	

                        if (contactPersons == 10) {
                                break;
                        }

                }

        }
        /** @noinspection BooleanMethodIsAlwaysInverted*/
        private boolean hasStatusRelevantForInfectionDynamics( PersonWrapper personWrapper ){
                switch( personWrapper.getStatus() ) {
                        case susceptible:
                                return true;
                        case infectedButNotContagious:
                                return false;
                        case contagious:
                                return true;
                        case immune:
                                return false;
                        default:
                                throw new IllegalStateException( "Unexpected value: " + personWrapper.getStatus() );
                }
        }
        
        private double lastTimeStep = 0 ;
        private int specificInfectionsCnt = 300;
        
        private void infectPerson( PersonWrapper personWrapper, PersonWrapper infector, double now, String infectionType ){

        		if (personWrapper.getStatus() != Status.susceptible) {
        			throw new RuntimeException("Person to be infected is not susceptible. Status is=" + personWrapper.getStatus());
        		}
        		if (infector.getStatus() != Status.contagious) {
        			throw new RuntimeException("Infector is not contagious. Status is=" + infector.getStatus());
        		}
                personWrapper.setStatus( Status.infectedButNotContagious );
                if ( scenario!=null ){
                        final Person person = PopulationUtils.findPerson( personWrapper.getPersonId(), scenario );
                        if( person != null ){
                                person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                        }
                }
               
                personWrapper.setInfectionDate(iteration);

                noOfInfectedPersons++;

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
                if ( now - lastTimeStep>=300 ){
                		lastTimeStep = now;

                		String[] array = new String[InfectionsWriterFields.values().length];

                		array[InfectionsWriterFields.time.ordinal()] = Double.toString( now );
                		array[InfectionsWriterFields.nInfectedPersons.ordinal()] = Double.toString( noOfInfectedPersons );
                		array[InfectionsWriterFields.nPersonsInQuarantine.ordinal()] = Double.toString( noOfPersonsInQuarantine );
                		array[InfectionsWriterFields.nImmunePersons.ordinal()] = Double.toString( noOfImmunePersons );
                		array[InfectionsWriterFields.nSusceptiblePersons.ordinal()] = Double.toString( populationSize - noOfInfectedPersons - noOfImmunePersons );

                		write( array, infectionsWriter );
                } 
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
        @Override public void reset( int iteration ){
        	for (VehicleWrapper vehicleWrapper : this.vehicleMap.values()) {
        		vehicleWrapper.clearPersons();
        	}
                populationSize = 0;
                for (PersonWrapper person : personMap.values()) {
                		populationSize++;
                        switch ( person.getStatus() ) {
                                case susceptible:
                                        break;
                                case infectedButNotContagious:
                                        if ( iteration - person.getInfectionDate() == 4 ) {
                                                person.setStatus( Status.contagious );
                                        }
                                        break;
                                case contagious:
                                        if (iteration - person.getInfectionDate()  == 6 && rnd.nextDouble() < 0.2 ) {
                                                person.setQuarantineStatus( QuarantineStatus.yes );
                                                person.setQuarantineDate(iteration);
                                                noOfPersonsInQuarantine++;
                                        }
                                        if ( iteration - person.getInfectionDate() == 16 ) {
                                                noOfInfectedPersons--;
                                                person.setStatus( Status.immune);
                                                noOfImmunePersons++;
                                        }
                                        break;
                                case immune:
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getStatus() );
                        }
                        if (person.getQuarantineStatus() == QuarantineStatus.yes) {
                        	if (iteration - person.getQuarantineDate() == 14) {
                        		person.setQuarantineStatus(QuarantineStatus.no);
                        		noOfPersonsInQuarantine--;
                        	}
                        }
                }

                this.iteration = iteration;

                log.warn("===============================");
                log.warn("Beginning day " + this.iteration);
                log.warn("No of susceptible persons=" + (populationSize - noOfInfectedPersons - noOfImmunePersons));
                log.warn( "No of infected persons=" + noOfInfectedPersons );
                log.warn( "No of persons in quarantine=" + noOfPersonsInQuarantine );
                log.warn( "No of immune persons=" + noOfImmunePersons );
                log.warn("===============================");
                lastTimeStep = 0;
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
                Id<Link> getLinkId(){
                        return linkId;
                }
                Set<PseudoFacilityWrapper> getPseudoFacilities() {
                        return pseudoFacilites;
                }
        }
        private static class PersonWrapper {
                private final Id<Person> personId;
                private Status status = Status.susceptible;
                private QuarantineStatus quarantineStatus = QuarantineStatus.no;
                private int infectionDate;
                private int quarantineDate;
                PersonWrapper( Id<Person> personId ) {
                        this.personId = personId;
                }
                void setStatus( Status status ) {
                        this.status = status;
                }
                void setQuarantineStatus( QuarantineStatus quarantineStatus ) {
                    	this.quarantineStatus = quarantineStatus;
                }
                Id<Person> getPersonId(){
                        return personId;
                }
                Status getStatus(){
                        return status;
                }
                QuarantineStatus getQuarantineStatus(){
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
        }
        private static class ContainerWrapper<T> {
                private final Id<T> containerId;
                private Map<Id<Person>,PersonWrapper> persons = new LinkedHashMap<>();
                private Map<Id<Person>,Double> entries = new LinkedHashMap<>();

                ContainerWrapper( Id<T> containerId ) {
                        this.containerId = containerId;
                }
                void addPerson( PersonWrapper person, double now ) {
                        persons.put( person.getPersonId(), person );
                        entries.put(  person.getPersonId(), now );
                }
                PersonWrapper removePerson( Id<Person> personId ) {
                        entries.remove( personId );
                        return persons.remove( personId );
                }
                Id<T> getContainerId(){
                        return containerId;
                }
                Map<Id<Person>,PersonWrapper> getPersons(){
                        return Collections.unmodifiableMap( persons );
                }
                Map<Id<Person>,Double> getContainerEnteringTimes(){
                        return Collections.unmodifiableMap( entries );
                }
                void clearPersons() {
                	this.persons.clear();
                }
        }
        private static class VehicleWrapper extends ContainerWrapper<Vehicle>{
                VehicleWrapper( Id<Vehicle> vehicleId ){
                        super( vehicleId );
                }
        }
        private static class PseudoFacilityWrapper extends ContainerWrapper<Facility>{
                PseudoFacilityWrapper( Id<Facility> facilityId ){
                        super( facilityId );
                }
        }
        enum Status {susceptible, infectedButNotContagious, contagious, immune};
        enum QuarantineStatus {yes, no}

}

