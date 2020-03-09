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
        private static final double calibrationParameter = 0.000002;
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
        private int noOfInfectedDrivers = 0;
        private int populationSize = 0;

        private BufferedWriter infectionsWriter;
        private BufferedWriter infectionEventsWriter;

        private enum InfectionsWriterFields{ time, nInfected, nInfectedDrivers, nInfectedPersons, nPersonsInQuarantine, nImmunePersons, nSusceptiblePersons }
        private enum InfectionEventsWriterFields{ time, infector, infected, infectionType }

        private int iteration=0;

        private Random rnd = MatsimRandom.getLocalInstance();

        @Inject InfectionEventHandler() {
                infectionsWriter = prepareWriter( "infections.txt", InfectionsWriterFields.class );
                infectionEventsWriter = prepareWriter( "infectionEvents.txt" , InfectionEventsWriterFields.class );
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
                // @Sebastian: Es gibt in PopulationUtils, FaciitiesUtils einige helper methods, die mit diesen Unklarheiten bzgl. linkIds, facilityIds
                // umgehen.  Schaust Du bitte mal rein?  Im Grunde muesste es reichen, sich fuer die Dynamik in Gebaeuden nur die Activity events
                // anzuschauen, und fuer die Dynamik in Vehicles nur die Vehicle enter/leave events.  Danke.  kai, mar'20

                // the events follow the matsim sequence, i.e. all agents start at activities.  kai, mar'20

                if ( event instanceof ActivityEndEvent ) {

                        // find the link
                        LinkWrapper link = this.linkMap.computeIfAbsent( ((ActivityEndEvent) event).getLinkId() , LinkWrapper::new );

                        // go through all facilities on link to eventually find the person and remove it:
                        for( PseudoFacilityWrapper facilityWrapper : link.getPseudoFacilities() ){
                                PersonWrapper person = facilityWrapper.getPersons().get( ((ActivityEndEvent) event).getPersonId() );
                                if( person != null ){

                                        // run infection dynamics for this person and facility:
                                        infectionDynamicsFacility( person, facilityWrapper, event.getTime() );
                                        facilityWrapper.removePerson( person.getPersonId() );
                                        break;
                                }
                        }

                }  else if ( event instanceof PersonEntersVehicleEvent ) {

                        // find the person:
                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getPersonId(), PersonWrapper::new );

                        // find the vehicle:
                        VehicleWrapper vehicleWrapper = this.vehicleMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getVehicleId(), VehicleWrapper::new );

                        // add person to vehicle and memorize entering time (yy this should rather be one method so):
                        vehicleWrapper.addPerson( personWrapper, event.getTime() );

                        handleInitialInfections( personWrapper );

                }  else if (event instanceof PersonLeavesVehicleEvent ) {

                        // find vehicle:
                        VehicleWrapper vehicle = this.vehicleMap.get( ((PersonLeavesVehicleEvent) event).getVehicleId() );

                        // remove person from vehicle:
                        PersonWrapper personWrapper = vehicle.getPersons().get( ((PersonLeavesVehicleEvent) event).getPersonId() );

                        infectionDynamicsVehicle( personWrapper, vehicle, event.getTime() );
                        
                        vehicle.removePerson( personWrapper.getPersonId() );

                }  else if (event instanceof ActivityStartEvent) {

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
                        pseudoFacilityWrapper.addPerson(personWrapper, event.getTime() );

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
                        if ( !personWrapper.getPersonId().toString().startsWith( "pt_pt" ) && !personWrapper.getPersonId().toString().startsWith( "pt_tr" ) ) {
                                personWrapper.setStatus( Status.infectedButNotContagious );
                                personWrapper.setInfectionDate(iteration);
                                log.warn(" person " + personWrapper.personId +" has initial infection");
                                cnt--;
                        }
                        if ( scenario!=null ){
                                final Person person = PopulationUtils.findPerson( personWrapper.personId, scenario );
                                if( person != null ){
                                        person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                                }
                        }
                }
        }
        private void infectionDynamicsVehicle( PersonWrapper personLeavingVehicle, VehicleWrapper vehicle, double now ){
                final double infectionProbaPerHour = 0.1;
                infectionDynamicsGeneralized( personLeavingVehicle, vehicle, infectionProbaPerHour, now, "Vehicle" );
        }
        private void infectionDynamicsFacility( PersonWrapper personLeavingFacility, PseudoFacilityWrapper facility,  double now ) {
                final double infectionProbaPerHour = 0.01;
                infectionDynamicsGeneralized( personLeavingFacility, facility, infectionProbaPerHour, now, "Activity" );
        }
        private void infectionDynamicsGeneralized( PersonWrapper personLeavingContainer, ContainerWrapper<?> container, double infectionProbaPerHour, double now, String infectionType ) {

//        	Every time an agent leaves a container, a group of contact persons is determined. The more time agents spend together in a container the higher the proba that they become contact persons.
//        	If the agent is infected, it can infect susceptible agents in its contact group.
//        	If the agent is susceptible, it can be infected by agents in its contact group. SM, mar'20

                if ( !hasStatusRelevantForInfectionDynamics( personLeavingContainer ) ) {
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
                			continue;
                		}
                		contactPersons++;
 
                        // (we count "quarantine" as well since they essentially represent "holes", i.e. persons who are no longer there and thus the
                        // density in the transit container goes down.  kai, mar'20)

                        if ( !hasStatusRelevantForInfectionDynamics( otherPerson ) ) {
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
                        
//                      exponential model by Smieszek. 
//                      equation 3.2 is used (simplified equation 3.1 which includes the shedding rate and contact intensity into the calibrationParameter
                        double infectionProba = 1 - Math.exp( - calibrationParameter * jointTimeInContainer);
                        if ( rnd.nextDouble() < infectionProba ) {
                            if ( personLeavingContainer.getStatus()==Status.susceptible ) {
                                    infectPerson( personLeavingContainer, otherPerson, now, infectionType );
                            } else {
                                    infectPerson( otherPerson, personLeavingContainer, now, infectionType );
                            }
                        }	

                        // yyyyyy replace with exponential model by Smieszek.  Note that the equation below is an approximation, which is acceptable for
                        // small probabilities.  For large probabilities, the equation below results in values that are too high: even with high infection
                        // probabilities, there is always _some_ proba that one does not get infected.

//                        if ( rnd.nextDouble() < jointTimeInVeh/3600. * infectionProbaPerHour ) {
//                                if ( personLeavingContainer.getStatus()==Status.susceptible ) {
//                                        infectPerson( personLeavingContainer, otherPerson, now );
//                                } else {
//                                        infectPerson( otherPerson, personLeavingContainer, now );
//                                }
//                        }

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
                        case quarantine:
                                return false;
                        case immune:
                                return false;
                        default:
                                throw new IllegalStateException( "Unexpected value: " + personWrapper.getStatus() );
                }
        }
        private double lastTimeStep = 0 ;
        private int specificInfectionsCnt = 300;
        private void infectPerson( PersonWrapper personWrapper, PersonWrapper infector, double now, String infectionType ){
                if ( personWrapper.getPersonId().toString().startsWith( "pt_pt" ) || personWrapper.getPersonId().toString().startsWith( "pt_tr" ) ) {
                        return;
                }
                Status prevStatus = personWrapper.getStatus();
                personWrapper.setStatus( Status.infectedButNotContagious );
                if ( scenario!=null ){
                        final Person person = PopulationUtils.findPerson( personWrapper.getPersonId(), scenario );
                        if( person != null ){
                                person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                        }
                }
                if ( prevStatus!= Status.infectedButNotContagious ) {
                        personWrapper.setInfectionDate(iteration);
                        if (personWrapper.getPersonId().toString().startsWith("pt_pt")) {
                                noOfInfectedDrivers++;
                        }
                        else {
                                noOfInfectedPersons++;
                        }
                        if ( specificInfectionsCnt-- > 0 ){
                                log.warn( "infection of personId=" + personWrapper.getPersonId() + " by person=" + infector.getPersonId() + " at/in " + infectionType );
                        }
                        {
                                String[] array = new String[InfectionEventsWriterFields.values().length];
                                array[InfectionEventsWriterFields.time.ordinal()] = Double.toString( now + iteration * 3600. * 24. );
                                array[InfectionEventsWriterFields.infector.ordinal()] = infector.getPersonId().toString();
                                array[InfectionEventsWriterFields.infected.ordinal()] = personWrapper.getPersonId().toString();
                                array[InfectionEventsWriterFields.infectionType.ordinal()] = infectionType;

                                write( array, infectionEventsWriter );

                        }
                        if ( now - lastTimeStep>=300 ){
                                lastTimeStep = now;
//                                log.warn( "No of infected persons=" + noOfInfectedPersons );
//                                log.warn( "No of infected drivers=" + noOfInfectedDrivers );

                                String[] array = new String[InfectionsWriterFields.values().length];

                                array[InfectionsWriterFields.time.ordinal()] = Double.toString( now + iteration * 3600. * 24. );
                                array[InfectionsWriterFields.nInfectedDrivers.ordinal()] = Double.toString( noOfInfectedDrivers );
                                array[InfectionsWriterFields.nInfectedPersons.ordinal()] = Double.toString( noOfInfectedPersons );
                                array[InfectionsWriterFields.nInfected.ordinal()] = Double.toString( noOfInfectedDrivers + noOfInfectedPersons );
                                array[InfectionsWriterFields.nPersonsInQuarantine.ordinal()] = Double.toString( noOfPersonsInQuarantine );
                                array[InfectionsWriterFields.nImmunePersons.ordinal()] = Double.toString( noOfImmunePersons );
                                array[InfectionsWriterFields.nSusceptiblePersons.ordinal()] = Double.toString( populationSize - noOfInfectedPersons - noOfPersonsInQuarantine - noOfImmunePersons );

                                write( array, infectionsWriter );
                        }
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
        	for (PseudoFacilityWrapper facility : this.pseudoFacilityMap.values()) {
        		facility.clearPersons();
        	}
                populationSize = 0;
                for (PersonWrapper person : personMap.values()) {
                        if (!person.getPersonId().toString().startsWith("pt_pt") && !person.getPersonId().toString().startsWith("pt_tr")) {
                                populationSize++;
                        }
                        switch ( person.getStatus() ) {
                                case susceptible:
                                        break;
                                case infectedButNotContagious:
                                        if ( iteration - person.getInfectionDate() ==1 ) {
                                                person.setStatus( Status.contagious );
                                        }
                                        break;
                                case contagious:
                                        if (iteration - person.getInfectionDate()  >= 7 && rnd.nextDouble() < 0.5 ) {
                                                noOfInfectedPersons--;
                                                person.setStatus(Status.quarantine);
                                                noOfPersonsInQuarantine++;
                                        }
                                        if ( iteration - person.getInfectionDate() >= 14 ) {
                                                noOfInfectedPersons--;
                                                person.setStatus( Status.immune);
                                                noOfImmunePersons++;
                                        }
                                        break;
                                case quarantine:
                                        if (iteration - person.getInfectionDate()  >= 14 ) {
                                                noOfPersonsInQuarantine--;
                                                person.setStatus(Status.immune);
                                                noOfImmunePersons++;
                                        }
                                        break;
                                case immune:
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getStatus() );
                        }
                }

                this.iteration = iteration;

                log.warn("===============================");
                log.warn("Beginning day " + this.iteration);
                log.warn("No of susceptible persons=" + (populationSize - noOfInfectedPersons - noOfPersonsInQuarantine - noOfImmunePersons));
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
                private int infectionDate;
                PersonWrapper( Id<Person> personId ) {
                        this.personId = personId;
                }
                void setStatus( Status status ) {
                        this.status = status;
                }
                Id<Person> getPersonId(){
                        return personId;
                }
                Status getStatus(){
                        return status;
                }
                void setInfectionDate (int date) {
                        this.infectionDate = date;
                }
                int getInfectionDate () {
                        return this.infectionDate;
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
        enum Status {susceptible, infectedButNotContagious, contagious, quarantine, immune};

}

