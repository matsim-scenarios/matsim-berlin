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
        // Some notes:

        // * Especially if we repeat the same events file, then we do not have complete mixing.  So it may happen that only some subpopulation gets infected.

        // * However, if with infection proba=1 almost everybody gets infected, then in our current setup (where infected people remain in the iterations),
        // this will also happen with lower probabilities, albeit slower.  This is presumably the case that we want to investigate.

        // * We seem to be getting two different exponential spreading rates.  With infection proba=1, the crossover is (currently) around 15h.


        private static final Logger log = Logger.getLogger( InfectionEventHandler.class );

        @Inject private Scenario scenario;

        private Map<Id<Person>,PersonWrapper> personMap = new HashMap<>();
        private Map<Id<Vehicle>,VehicleWrapper> vehicleMap = new HashMap<>();
        private Map<String,PseudoFacilityWrapper> pseudoFacilityMap = new HashMap<>();
        private Map<Id<Link>,LinkWrapper> linkMap = new HashMap<>();

        private int cnt = 10 ;
        private int noOfInfectedPersons = cnt;
        private int noOfInfectedDrivers = 0;

        private BufferedWriter infectionsWriter;
        private BufferedWriter infectionEventsWriter;

        private enum InfectionsWriterFields{ time, nInfected, nInfectedDrivers, nInfectedPersons }
        private enum InfectionEventsWriterFields{ time, infector, infected }

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
                // @Sebatian: Es gibt in PopulationUtils, FaciitiesUtils einige helper methods, die mit diesen Unklarheiten bzgl. linkIds, facilityIds
                // umgehen.  Schaust Du bitte mal rein?  Im Grunde muesste es reichen, sich fuer die Dynamik in Gebaeuden nur die Activity events
                // anzuschauen, und fuer die Dynamik in Vehicles nur die Vehicle enter/leave events.  Danke.  kai, mar'20


                // the events follow the matsim sequence, i.e. all agents start at activities.  kai, mar'20

                if ( event instanceof ActivityEndEvent ) {

                       // nothing to do

                } else if ( event instanceof PersonDepartureEvent ) {
                        // it should, in principle, be possible to use the AcitivityEndEvent, and to remove the person directly from the activity.  However,
                        // we cannot rely on the activity having a facilityId.  kai, mar'20

                        // find the link ...
                        LinkWrapper link = this.linkMap.computeIfAbsent( ((PersonDepartureEvent) event).getLinkId() , LinkWrapper::new );

                        // ... and go through all facilities on link to eventually find the person and remove it:
                        for( PseudoFacilityWrapper facilityWrapper : link.getPseudoFacilities() ){
                                PersonWrapper result = facilityWrapper.removePerson( ((PersonDepartureEvent) event).getPersonId() );
                                if( result != null ){
                                        break;
                                }
                        }

                        // note that persons are not found on those facilities where they started.  Maybe fix. kai, mar'20

                } else if ( event instanceof PersonEntersVehicleEvent ) {

                        // find the vehicle:
                        VehicleWrapper vehicleWrapper = this.vehicleMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getVehicleId(), VehicleWrapper::new );

                        // find the person:
                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonEntersVehicleEvent) event).getPersonId(), PersonWrapper::new );

                        // add person to vehicle:
                        vehicleWrapper.addPerson( personWrapper );

                        handleInitialInfections( personWrapper );

                        infectionDynamics( vehicleWrapper.getPersons(), event.getTime() );

                } else if (event instanceof PersonLeavesVehicleEvent ) {

                        VehicleWrapper vehicle = this.vehicleMap.get( ((PersonLeavesVehicleEvent) event).getVehicleId() );
                        vehicle.removePerson( ((PersonLeavesVehicleEvent) event).getPersonId() );

                } else if ( event instanceof PersonArrivalEvent ) {
                        // it should, in principle, be possible to only look at the ActivityStartEvent.  Unfortunately, we cannot rely on the
                        // ActivityStartEvent always having linkId.  (Is that statement really correct?)  kai, mar'20

//                        this.linkMap.computeIfAbsent( ((PersonArrivalEvent) event).getLinkId(), LinkWrapper::new );

                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( ((PersonArrivalEvent) event).getPersonId(), PersonWrapper::new );

                        personWrapper.setLinkId(((PersonArrivalEvent) event).getLinkId() );

//                        linkWrapper.addPerson( personWrapper );
//                       
//                        infectionDynamics( linkWrapper.getPersons() );

                } else if (event instanceof ActivityStartEvent) {
                        // it should, in principle, be possible to only look at the ActivityStartEvent.  Unfortunately, we cannot rely on the
                        // ActivityStartEvent always having linkId.  (Is that statement really correct?)  kai, mar'20

                        if(((ActivityStartEvent) event).getPersonId().toString().startsWith("drt")){
                                return;
                        }

                        PersonWrapper personWrapper = this.personMap.get(((ActivityStartEvent) event).getPersonId());

                        LinkWrapper linkWrapper = this.linkMap.computeIfAbsent(personWrapper.getLastLink(), LinkWrapper::new );

                        String pseudoFacilityId = getPseudoFacilityId( ((ActivityStartEvent) event).getActType(), linkWrapper.getLinkId() );

                        PseudoFacilityWrapper pseudoFacilityWrapper = this.pseudoFacilityMap.computeIfAbsent(pseudoFacilityId, PseudoFacilityWrapper::new);

                        pseudoFacilityWrapper.addPerson(personWrapper);

                        linkWrapper.addPseudoFacility(pseudoFacilityWrapper);


                        infectionDynamics( pseudoFacilityWrapper.getPersons(), event.getTime());

                }

        }
        private String getPseudoFacilityId( String activityType, Id<Link> linkId) {
                return activityType.split("_" )[0] + "_" + linkId.toString();
        }
        private void handleInitialInfections( PersonWrapper personWrapper ){
                // initial infections:
                if( cnt > 0 ){
                        if ( !personWrapper.getPersonId().toString().startsWith( "pt_pt" ) ) {
                                personWrapper.setStatus( Status.infected );
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
        private void infectionDynamics( Map<Id<Person>,PersonWrapper> persons, double now ){

                for( PersonWrapper infector : persons.values() ){
                        if( infector.getStatus() == Status.infected ){
                                for( PersonWrapper person : persons.values() ){
                                        if ( rnd.nextDouble() < 0.01 ){
                                                infectPerson( person, infector, now );
                                        }
                                }
                        }
                }

        }
        private double lastTimeStep = 0 ;
        private int specificInfectionsCnt = 300;
        private void infectPerson( PersonWrapper personWrapper, PersonWrapper infector, double now ){
                if ( personWrapper.getPersonId().toString().startsWith( "pt_pt" ) ) {
                        return;
                }
                Status prevStatus = personWrapper.getStatus();
                personWrapper.setStatus( Status.infected );
                if ( scenario!=null ){
                        final Person person = PopulationUtils.findPerson( personWrapper.getPersonId(), scenario );
                        if( person != null ){
                                person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                        }
                }
                if ( prevStatus!= Status.infected ) {
                        if (personWrapper.getPersonId().toString().startsWith("pt_pt")) {
                                noOfInfectedDrivers++;
                        }
                        else {
                                noOfInfectedPersons++;
                        }
                        if ( specificInfectionsCnt-- > 0 ){
                                log.warn( "infection of personId=" + personWrapper.getPersonId() + " by person=" + infector.getPersonId() );
                        }
                        {
                                String[] array = new String[InfectionEventsWriterFields.values().length];
                                array[InfectionEventsWriterFields.time.ordinal()] = Double.toString( now + iteration * 3600. * 24. );
                                array[InfectionEventsWriterFields.infector.ordinal()] = infector.getPersonId().toString();
                                array[InfectionEventsWriterFields.infected.ordinal()] = personWrapper.getPersonId().toString();

                                write( array, infectionEventsWriter );

                        }
                        if ( now - lastTimeStep>=300 ){
                                lastTimeStep = now;
                                log.warn( "No of infected persons=" + noOfInfectedPersons );
                                log.warn( "No of infected drivers=" + noOfInfectedDrivers );

                                String[] array = new String[InfectionsWriterFields.values().length];

                                array[InfectionsWriterFields.time.ordinal()] = Double.toString( now + iteration * 3600. * 24. );
                                array[InfectionsWriterFields.nInfectedDrivers.ordinal()] = Double.toString( noOfInfectedDrivers );
                                array[InfectionsWriterFields.nInfectedPersons.ordinal()] = Double.toString( noOfInfectedPersons );
                                array[InfectionsWriterFields.nInfected.ordinal()] = Double.toString( noOfInfectedDrivers + noOfInfectedPersons );

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
                this.iteration = iteration;
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
        private static class VehicleWrapper {
                private final Id<Vehicle> vehicleId;
                private Map<Id<Person>,PersonWrapper> persons = new LinkedHashMap<>();
                VehicleWrapper( Id<Vehicle> vehicleId ) {
                        this.vehicleId = vehicleId;
                }
                void addPerson( PersonWrapper person ) {
                        persons.put( person.getPersonId(), person );
                }
                void removePerson( Id<Person> personId ) {
                        persons.remove( personId );
                }
                Id<Vehicle> getVehicleId(){
                        return vehicleId;
                }
                Map<Id<Person>,PersonWrapper> getPersons(){
                        return Collections.unmodifiableMap( persons );
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
                Id<Person> getPersonId(){
                        return personId;
                }
                Status getStatus(){
                        return status;
                }
                Id<Link> getLastLink(){
                        return lastLink;
                }
                void setLinkId( Id<Link> lastLink ){
                        this.lastLink = lastLink;
                }
        }
        private static class PseudoFacilityWrapper {
                private final String pseudoFacilityId;
                private Map<Id<Person>,PersonWrapper> persons = new LinkedHashMap<>();
                PseudoFacilityWrapper(String pseudoFacilityId) {
                        this.pseudoFacilityId = pseudoFacilityId;
                }
                void addPerson( PersonWrapper person ) {
                        persons.put( person.getPersonId(), person );
                }
                PersonWrapper removePerson( Id<Person> personId ) {
                        return persons.remove( personId );
                }
                Map<Id<Person>,PersonWrapper> getPersons(){
                        return Collections.unmodifiableMap( persons );
                }
        }
        enum Status {susceptible, infected, immune};

}

