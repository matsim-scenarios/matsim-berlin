package org.matsim.run.drt;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
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
//        private long noOfInfectedPersons = cnt;
//        private long noOfPersonsInQuarantine = 0;
//        private long noOfImmunePersons = 0;
//        private long noOfContagious = 0;
        // the above is too error-prone.  Rather just count.  kai, mar'20

        private BufferedWriter infectionsWriter;
        private BufferedWriter infectionEventsWriter;

        private enum InfectionsWriterFields{ time, nInfected, nInQuarantine, nRecovered, nSusceptible, nContagious, nInfectedButNotContagious,
                nInfectedCumulative }
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

//                if (event.getTime() >= 86400) {
//                        // yyyyyy I am still not convinced that this is a good idea.  Persons who return later will now not return at all, i.e. they are also
//                        // not able to participate in the overnight infection dynamics.
//                        return;
//                }

                double now = getCorrectedTime( event.getTime(), iteration  );

                // the events follow the matsim sequence, i.e. all agents start at activities.  kai, mar'20

                if ( event instanceof ActivityEndEvent ) {
                        final ActivityEndEvent activityEndEvent = (ActivityEndEvent) event;

                        //ignore drt and stage activities
                        if( activityEndEvent.getPersonId().toString().startsWith("drt" ) || TripStructureUtils.isStageActivityType( activityEndEvent.getActType() ) ) {
                                return;
                        }

                        // if configured, there will be no infections at activities of a certain type
                        // the probability that a there is no infection at a certain activity can be set via episimConfig.getClosedActivity1Sample()
                        // if closedActivity1() is set to "work" and activity1Sample() is set to "0.5" this essentially means that 50% of working people do home office
                        // to do: also switch off infections in pt to / from closed activity.		SM, mar'20
                        if (episimConfig.getClosedActivity1() != null) {
                                if (rnd.nextDouble() < episimConfig.getClosedActivity1Sample() && episimConfig.getClosedActivity1().toString().equals( activityEndEvent.getActType() )) {
                                        return;
                                }
                        }
                        if (episimConfig.getClosedActivity2() != null) {
                                if (rnd.nextDouble() < episimConfig.getClosedActivity2Sample() && episimConfig.getClosedActivity2().toString().equals( activityEndEvent.getActType() )) {
                                        return;
                                }
                        }

                        // find the link
                        LinkWrapper link = this.linkMap.computeIfAbsent( activityEndEvent.getLinkId() , LinkWrapper::new );

                        // go through all facilities on link to eventually find the person and remove it:
                        for( PseudoFacilityWrapper facilityWrapper : link.getPseudoFacilities() ){
                                PersonWrapper person = facilityWrapper.getPerson( activityEndEvent.getPersonId() );
                                if( person != null ){

                                        // run infection dynamics for this person and facility:
                                        infectionDynamicsFacility( person, facilityWrapper, now, activityEndEvent.getActType() );
                                        facilityWrapper.removePerson( person.getPersonId() );
                                        handleInitialInfections( person );
                                        break;
                                }
                        }

                }  else if ( event instanceof PersonEntersVehicleEvent ) {
                        final PersonEntersVehicleEvent entersVehicleEvent = (PersonEntersVehicleEvent) event;

                        // if pt is shut down nothing happens here
                        if (episimConfig.getUsePt() == EpisimConfigGroup.UsePt.no) {
                                return;
                        }
                        // ignore pt drivers
                        if ( entersVehicleEvent.getPersonId().toString().startsWith("pt_pt" ) || entersVehicleEvent.getPersonId().toString().startsWith("pt_tr" )) {
                                return;
                        }

                        // find the person:
                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( entersVehicleEvent.getPersonId(), PersonWrapper::new );

                        // find the vehicle:
                        VehicleWrapper vehicleWrapper = this.vehicleMap.computeIfAbsent( entersVehicleEvent.getVehicleId(), VehicleWrapper::new );

                        // add person to vehicle and memorize entering time:
                        vehicleWrapper.addPerson( personWrapper, now );

                }  else if (event instanceof PersonLeavesVehicleEvent ) {
                        final PersonLeavesVehicleEvent leavesVehicleEvent = (PersonLeavesVehicleEvent) event;

                        // if pt is shut down nothing happens here
                        if (episimConfig.getUsePt() == EpisimConfigGroup.UsePt.no) {
                                return;
                        }

                        // ignore pt drivers
                        if ( leavesVehicleEvent.getPersonId().toString().startsWith("pt_pt" ) || leavesVehicleEvent.getPersonId().toString().startsWith("pt_tr" )) {
                                return;
                        }

                        // find vehicle:
                        VehicleWrapper vehicle = this.vehicleMap.get( leavesVehicleEvent.getVehicleId() );

                        // remove person from vehicle:
                        PersonWrapper personWrapper = vehicle.getPerson( leavesVehicleEvent.getPersonId() );

                        infectionDynamicsVehicle( personWrapper, vehicle, now );

                        vehicle.removePerson( personWrapper.getPersonId() );

                }  else if (event instanceof ActivityStartEvent) {
                        final ActivityStartEvent activityStartEvent = (ActivityStartEvent) event;

                        //see ActivityEndEvent for explanation
                        if (episimConfig.getClosedActivity1() != null) {
                                if (rnd.nextDouble() < episimConfig.getClosedActivity1Sample() && episimConfig.getClosedActivity1().toString().equals( activityStartEvent.getActType() )) {
                                        return;
                                }
                        }
                        if (episimConfig.getClosedActivity2() != null) {
                                if (rnd.nextDouble() < episimConfig.getClosedActivity2Sample() && episimConfig.getClosedActivity2().toString().equals( activityStartEvent.getActType() )) {
                                        return;
                                }
                        }
                        //ignore drt and stage activities
                        if( activityStartEvent.getPersonId().toString().startsWith("drt" ) || TripStructureUtils.isStageActivityType( activityStartEvent.getActType() ) ) {
                                return;
                        }

                        // find the person:
                        PersonWrapper personWrapper = this.personMap.computeIfAbsent( activityStartEvent.getPersonId(), PersonWrapper::new );

                        // find the link:
                        LinkWrapper linkWrapper = this.linkMap.computeIfAbsent( activityStartEvent.getLinkId(), LinkWrapper::new );

                        // create pseudo facility id that includes the activity type:
                        Id<Facility> pseudoFacilityId = createPseudoFacilityId( activityStartEvent );

                        // find the facility
                        PseudoFacilityWrapper pseudoFacilityWrapper = this.pseudoFacilityMap.computeIfAbsent(pseudoFacilityId, PseudoFacilityWrapper::new);

                        // add facility to link
                        linkWrapper.addPseudoFacility(pseudoFacilityWrapper);

                        // add person to facility
                        pseudoFacilityWrapper.addPerson(personWrapper, now );

                }

        }
        private Id<Facility> createPseudoFacilityId( HasFacilityId event ) {
                if (scenarioWithFacilites ) {
                        return Id.create( event.getFacilityId(), Facility.class );
                }
                else {
                        if ( event instanceof ActivityStartEvent ){
                                ActivityStartEvent theEvent = (ActivityStartEvent) event;
                                return Id.create( theEvent.getActType().split( "_" )[0] + "_" + theEvent.getLinkId().toString(), Facility.class );
                        } else if ( event instanceof ActivityEndEvent ) {
                                ActivityEndEvent theEvent = (ActivityEndEvent) event;
                                return Id.create( theEvent.getActType().split( "_" )[0] + "_" + theEvent.getLinkId().toString(), Facility.class );
                        } else {
                                throw new RuntimeException( "unexpected event type=" + ((Event)event).getEventType() ) ;
                        }
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

                final boolean shuffleAll = true ;
                Collection<PersonWrapper> persons;
                if ( shuffleAll ){
                        List<PersonWrapper> list = new ArrayList<>( container.getPersons() );
                        Collections.shuffle( list, rnd ); // so we do not always see the persons in the same sequence in each iteration
                        persons = list;
                } else {
                        persons = container.getPersons();
                }

                int contactPersons = 0 ;
                List<PersonWrapper> personsAlreadySeen = new ArrayList<>( Collections.singletonList( personLeavingContainer ));
                for (PersonWrapper somePerson : persons) {

                        // we are essentially looking at the situation when the person leaves the container.  Interactions with other persons who have
                        // already left the container were treated then.  In consequence, we have some "circle of persons around us" (yyyy which should
                        //  depend on the density), and then a probability of infection in either direction.

                        // if we have seen enough, then break, no matter what:
                        if (contactPersons >= 10) {
                                break;
                        }
                        // For the time being, will just assume that the first 10 persons are the ones we interact with.  Note that because of
                        // shuffle, those are 10 different persons every day.

                        PersonWrapper otherPerson = null;
                        if ( shuffleAll ) {
                                otherPerson = somePerson;
                                if ( personLeavingContainer.getPersonId()==otherPerson.getPersonId() ) {
                                        // if person find itself nothing happens
                                        continue;
                                }
                        } else {
                                if ( personsAlreadySeen.size() == container.getPersons().size() ) {
                                        return ;
                                        // in principle, should be possible to hedge against this otherwise, but because of "self" it is not totally
                                        // straightforward.  kai, mar'20
                                }
                                do{
                                        int idx = rnd.nextInt( container.getPersons().size() );
                                        otherPerson = container.getPersons().get( idx );
                                } while( personsAlreadySeen.contains( otherPerson ) );
                        }
                        personsAlreadySeen.add( otherPerson );

                        contactPersons++;

                        // (we count "quarantine" as well since they essentially represent "holes", i.e. persons who are no longer there and thus the
                        // density in the transit container goes down.  kai, mar'20)

                        if ( !hasStatusRelevantForInfectionDynamics( otherPerson ) || otherPerson.getQuarantineStatus() == QuarantineStatus.yes) {
                                continue;
                        }

                        if(infectionType.equals("home") || infectionType.equals("work") || (infectionType.equals("leisure") && rnd.nextDouble() < 0.8)) {
                                if (!personLeavingContainer.getTracableContactPersons().contains(otherPerson)) {
                                        personLeavingContainer.addTracableContactPerson(otherPerson);
                                }
                                if (!otherPerson.getTracableContactPersons().contains(personLeavingContainer)) {
                                        otherPerson.addTracableContactPerson(personLeavingContainer);
                                }
                        }

                        if ( personLeavingContainer.getStatus()==otherPerson.getStatus() ) {
                                // (if they have the same status, then nothing can happen between them)
                                continue;
                        }

                        Double containerEnterTimeOfPersonLeaving = container.getContainerEnteringTime( personLeavingContainer.getPersonId() );
                        Double containerEnterTimeOfOtherPerson = container.getContainerEnteringTime( otherPerson.getPersonId() );

                        // persons leaving their first-ever activity have no starting time for that activity.  Need to hedge against that.  Since all persons
                        // start healthy (the first seeds are set at enterVehicle), we can make some assumptions.
                        if ( containerEnterTimeOfPersonLeaving==null && containerEnterTimeOfOtherPerson==null ) {
                                throw new RuntimeException( "should not happen" );
                                // null should only happen at first activity.  However, at first activity all persons are susceptible.  So the only way we
                                // can get here is if an infected person entered the container and is now leaving again, while the other person has been in the
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
                        case recovered:
                                return false;
                        default:
                                throw new IllegalStateException( "Unexpected value: " + personWrapper.getStatus() );
                }
        }

        private int specificInfectionsCnt = 300;

        private void infectPerson( PersonWrapper personWrapper, PersonWrapper infector, double now, String infectionType ){

                if (personWrapper.getStatus() != Status.susceptible) {
                        throw new RuntimeException("Person to be infected is not susceptible. Status is=" + personWrapper.getStatus());
                }
                if (infector.getStatus() != Status.contagious) {
                        throw new RuntimeException("Infector is not contagious. Status is=" + infector.getStatus());
                }
                if (personWrapper.getQuarantineStatus() == QuarantineStatus.yes) {
                        throw new RuntimeException("Person to be infected is in quarantine.");
                }
                if (infector.getQuarantineStatus() == QuarantineStatus.yes) {
                        throw new RuntimeException("Infector is in quarantine.");
                }
                personWrapper.setStatus( Status.infectedButNotContagious );
                if ( scenario!=null ){
                        final Person person = PopulationUtils.findPerson( personWrapper.getPersonId(), scenario );
                        if( person != null ){
                                person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                        }
                }

                personWrapper.setInfectionDate(iteration);

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
                // why not just vehicleMap.clear()? kai, mar'20


                for (PersonWrapper person : personMap.values()) {
                        switch ( person.getStatus() ) {
                                case susceptible:
                                        break;
                                case infectedButNotContagious:
                                        if ( iteration - person.getInfectionDate() >= 4 ) {
                                                person.setStatus( Status.contagious );
                                        }
                                        break;
                                case contagious:
                                        if (iteration - person.getInfectionDate()  == 6 && rnd.nextDouble() < 0.2 ) {
                                                person.setQuarantineStatus( QuarantineStatus.yes );
                                                person.setQuarantineDate(iteration);
                                                if (episimConfig.getPutTracablePersonsInQuarantine()==EpisimConfigGroup.PutTracablePersonsInQuarantine.yes) {
                                                        for (PersonWrapper pw : person.getTracableContactPersons()) {
                                                                if (pw.getQuarantineStatus() == QuarantineStatus.no) {
                                                                        pw.setQuarantineStatus(QuarantineStatus.yes);
                                                                        pw.setQuarantineDate(iteration);
                                                                }
                                                        }
                                                }
                                        }
                                        if ( iteration - person.getInfectionDate() >= 16 ) {
                                                person.setStatus( Status.recovered );
                                        }
                                        break;
                                case recovered:
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getStatus() );
                        }
                        if (person.getQuarantineStatus() == QuarantineStatus.yes) {
                                if (iteration - person.getQuarantineDate() >= 14) {
                                        person.setQuarantineStatus(QuarantineStatus.no);
                                }
                        }
                        person.getTracableContactPersons().clear();
                }

                this.iteration = iteration;

                long nSusceptible = 0;
                long nInfectedButNotContagious = 0;
                long nContagious = 0;
                long nRecovered = 0;
                long nQuarantined = 0;
                for( PersonWrapper person : personMap.values() ){
                        switch( person.getStatus() ) {
                                case susceptible:
                                        nSusceptible++;
                                        break;
                                case infectedButNotContagious:
                                        nInfectedButNotContagious++;
                                        break;
                                case contagious:
                                        nContagious++;
                                        break;
                                case recovered:
                                        nRecovered++;
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getStatus() );
                        }
                        switch( person.getQuarantineStatus() ) {
                                case yes:
                                        nQuarantined++;
                                        break;
                                case no:
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getQuarantineStatus() );
                        }
                }

                log.warn("===============================");
                log.warn("Beginning day " + this.iteration);
                log.warn("No of susceptible persons=" + nSusceptible );
                log.warn( "No of infected but not contagious persons=" + nInfectedButNotContagious );
                log.warn( "No of contagious persons=" + nContagious );
                log.warn( "No of recovered persons=" + nRecovered );
                log.warn( "---" );
                log.warn( "No of persons in quarantaine=" + nQuarantined );
                log.warn("===============================");

                String[] array = new String[InfectionsWriterFields.values().length];

                array[InfectionsWriterFields.time.ordinal()] = Double.toString( getCorrectedTime( 0.,iteration ) );
                array[InfectionsWriterFields.nSusceptible.ordinal()] = Long.toString( nSusceptible );
                array[InfectionsWriterFields.nInfectedButNotContagious.ordinal()] = Long.toString( nInfectedButNotContagious );
                array[InfectionsWriterFields.nContagious.ordinal()] = Long.toString( nContagious );
                array[InfectionsWriterFields.nRecovered.ordinal()] = Long.toString( nRecovered );

                array[InfectionsWriterFields.nInfected.ordinal()] = Long.toString( (nInfectedButNotContagious + nContagious) ) ;
                array[InfectionsWriterFields.nInfectedCumulative.ordinal()] = Long.toString( (nInfectedButNotContagious + nContagious + nRecovered) );

                array[InfectionsWriterFields.nInQuarantine.ordinal()] = Long.toString( nQuarantined );

                write( array, infectionsWriter );

        }
        private static double lastNow = -1 ;
        private double getCorrectedTime( double time, long iteration ) {
                final double now = Math.min( time, 3600. * 24 ) + iteration * 24. * 3600;
                if ( now < lastNow ) {
                        throw new RuntimeException( "we are going backwards in time; something is wrong" );
                }
                lastNow = now;
                return now;
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
                private Set<PersonWrapper> tracableContactPersons = new HashSet<>();
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
                void addTracableContactPerson( PersonWrapper personWrapper ) {
                        tracableContactPersons.add( personWrapper );
                }
                Set<PersonWrapper> getTracableContactPersons() {
                        return tracableContactPersons;
                }
        }
        private static class ContainerWrapper<T> {
                private final Id<T> containerId;

                private Map<Id<Person>,PersonWrapper> persons = new LinkedHashMap<>();
                private List<PersonWrapper> personsAsList = new ArrayList<>();
                // I need them as list, to make the "randomly draw persons within container" a bit cheaper.  kai, mar'20
                // one might be able to combine these two, e.g. by using binarySearch.  kai, mar'20

                private Map<Id<Person>,Double> containerEnterTimes = new LinkedHashMap<>();

                ContainerWrapper( Id<T> containerId ) {
                        this.containerId = containerId;
                }
                void addPerson( PersonWrapper person, double now ) {
                        persons.put( person.getPersonId(), person );
                        personsAsList.add( person );
                        containerEnterTimes.put(  person.getPersonId(), now );
                }
                /** @noinspection UnusedReturnValue*/
                PersonWrapper removePerson( Id<Person> personId ) {
                        containerEnterTimes.remove( personId );
                        PersonWrapper personWrapper = persons.remove( personId );
                        boolean wasRemoved = personsAsList.remove( personWrapper );
                        Gbl.assertIf( wasRemoved );
                        return personWrapper;
                }
                Id<T> getContainerId(){
                        return containerId;
                }
//                Map<Id<Person>,PersonWrapper> getPersons(){
//                        return Collections.unmodifiableMap( persons );
//                }
//                Map<Id<Person>,Double> getContainerEnteringTimes(){
//                        return Collections.unmodifiableMap( containerEnterTimes );
//                }
                // we are not giving out internals of data structures! kai, mar'20

                void clearPersons() {
                        this.persons.clear();
                        this.personsAsList.clear();
                        this.containerEnterTimes.clear();
                }
                Double getContainerEnteringTime( Id<Person> personId ){
                        return containerEnterTimes.get( personId );
                }
                PersonWrapper getPerson( Id<Person> personId ){
                        return persons.get( personId );
                }
                public List<PersonWrapper> getPersons(){
                        return Collections.unmodifiableList( personsAsList );
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
        enum Status {susceptible, infectedButNotContagious, contagious, recovered};
        enum QuarantineStatus {yes, no}
}

