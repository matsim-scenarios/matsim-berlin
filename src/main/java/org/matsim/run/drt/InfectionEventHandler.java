package org.matsim.run.drt;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.Facility;
import org.matsim.run.drt.EpisimConfigGroup.PutTracablePersonsInQuarantine;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.*;

class InfectionEventHandler implements ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler {
        // Some notes:

        // * Especially if we repeat the same events file, then we do not have complete mixing.  So it may happen that only some subpopulations gets infected.

        // * However, if with infection proba=1 almost everybody gets infected, then in our current setup (where infected people remain in the iterations),
        // this will also happen with lower probabilities, albeit slower.  This is presumably the case that we want to investigate.

        // * We seem to be getting two different exponential spreading rates.  With infection proba=1, the crossover is (currently) around 15h.

        // TODO

        // * yyyyyy There are now some things that depend on ID conventions.  We should try to replace them.  This presumably would mean to interpret
        //  additional events.  Those would need to be prepared for the "reduced" files.  kai, mar'20


        private static final Logger log = Logger.getLogger( InfectionEventHandler.class );

        @Inject private Scenario scenario;

        private Map<Id<Person>, EpisimPerson> personMap = new LinkedHashMap<>();
        private Map<Id<Vehicle>, EpisimVehicle> vehicleMap = new LinkedHashMap<>();
        private Map<Id<Facility>, EpisimFacility> pseudoFacilityMap = new LinkedHashMap<>();
        private Map<Id<Link>, EpisimLink> linkMap = new LinkedHashMap<>();

        private int cnt = 10 ;

        private EpisimConfigGroup episimConfig;

        private int iteration=0;

//        private Random rnd = MatsimRandom.getLocalInstance();
        private Random rnd = new Random(1);

        private EpisimReporting reporting ;

        @Inject InfectionEventHandler( Config config ) {
                this.reporting = new EpisimReporting( config );
                this.episimConfig = ConfigUtils.addOrGetModule( config, EpisimConfigGroup.class );
        }

        @Override public void handleEvent( ActivityEndEvent activityEndEvent ) {
                double now = EpisimUtils.getCorrectedTime( activityEndEvent.getTime(), iteration );

                //ignore drt and stage activities
                if( activityEndEvent.getPersonId().toString().startsWith("drt" ) || activityEndEvent.getPersonId().toString().startsWith("rt" ) || TripStructureUtils.isStageActivityType( activityEndEvent.getActType() ) ) {
                        return;
                }

                // find the link
                EpisimLink link = this.linkMap.computeIfAbsent( activityEndEvent.getLinkId() , EpisimLink::new );
                boolean didInfectionDynamics = false;
                // go through all facilities on link to eventually find the person and remove it:
                for( EpisimFacility facilityWrapper : link.getPseudoFacilities() ){
                        EpisimPerson person = facilityWrapper.getPerson( activityEndEvent.getPersonId() );
                        if( person != null ){
                                // run infection dynamics for this person and facility:
                                infectionDynamicsFacility( person, facilityWrapper, now, activityEndEvent.getActType() );
                                facilityWrapper.removePerson( person.getPersonId() );
                                if (person.getLastFacilityId() != null) {
                                	// if person is still in a container from the day before, remove it
	                                if (!person.getLastFacilityId().equals(facilityWrapper.getContainerId().toString())) {
	                                	Id<Facility> id = Id.create(person.getLastFacilityId(), Facility.class);
	                                	pseudoFacilityMap.get(id).removePerson(person.getPersonId());
	                                }
                                }
                                handleInitialInfections( person );
                                didInfectionDynamics = true;
                                break;
                        }
                }
                // quick fix for persons that cannot be found above
                if (!didInfectionDynamics) {
                	EpisimPerson personWrapper = this.personMap.computeIfAbsent( activityEndEvent.getPersonId(), EpisimPerson::new );

                	// create pseudo facility id that includes the activity type:
                    Id<Facility> pseudoFacilityId = createPseudoFacilityId( activityEndEvent );

                    // find the facility
                    EpisimFacility pseudoFacilityWrapper = this.pseudoFacilityMap.computeIfAbsent(pseudoFacilityId, EpisimFacility::new );
                    // it is assumed that these persons start their activity at 00:00
                    pseudoFacilityWrapper.addPerson(personWrapper, iteration * 86400 );
                    infectionDynamicsFacility( personWrapper, pseudoFacilityWrapper, now, activityEndEvent.getActType() );
                    pseudoFacilityWrapper.removePerson(personWrapper.getPersonId());

                }

                handlePersonTrajectory(activityEndEvent.getPersonId(), activityEndEvent.getActType().toString());

        }

		@Override public void handleEvent( PersonEntersVehicleEvent entersVehicleEvent ) {
                double now = EpisimUtils.getCorrectedTime( entersVehicleEvent.getTime(), iteration );

                // ignore pt drivers and drt
                if ( entersVehicleEvent.getPersonId().toString().startsWith("pt_pt" ) || entersVehicleEvent.getPersonId().toString().startsWith("pt_tr" ) || entersVehicleEvent.getPersonId().toString().startsWith("drt" ) || entersVehicleEvent.getPersonId().toString().startsWith("rt" )) {
                        return;
                }

                // if pt is shut down nothing happens here
                if (episimConfig.getUsePt() == EpisimConfigGroup.UsePt.no && episimConfig.getUsePtDate() <= iteration) {
                		return;
                }

                // find the person:
                EpisimPerson personWrapper = this.personMap.computeIfAbsent( entersVehicleEvent.getPersonId(), EpisimPerson::new );

                // find the vehicle:
                EpisimVehicle vehicleWrapper = this.vehicleMap.computeIfAbsent( entersVehicleEvent.getVehicleId(), EpisimVehicle::new );

                // add person to vehicle and memorize entering time:
                vehicleWrapper.addPerson( personWrapper, now );

        }

        @Override public void handleEvent( PersonLeavesVehicleEvent leavesVehicleEvent ) {
                double now = EpisimUtils.getCorrectedTime( leavesVehicleEvent.getTime(), iteration );

                // ignore pt drivers and drt
                if ( leavesVehicleEvent.getPersonId().toString().startsWith("pt_pt" ) || leavesVehicleEvent.getPersonId().toString().startsWith("pt_tr" ) || leavesVehicleEvent.getPersonId().toString().startsWith("drt" ) || leavesVehicleEvent.getPersonId().toString().startsWith("rt" )){
                        return;
                }

                // if pt is shut down nothing happens here
                if (episimConfig.getUsePt() == EpisimConfigGroup.UsePt.no && episimConfig.getUsePtDate() <= iteration) {
                		return;
                }
                // find vehicle:
                EpisimVehicle vehicle = this.vehicleMap.get( leavesVehicleEvent.getVehicleId() );


                EpisimPerson personWrapper = vehicle.getPerson( leavesVehicleEvent.getPersonId() );

                infectionDynamicsVehicle( personWrapper, vehicle, now );


                // remove person from vehicle:
                vehicle.removePerson( personWrapper.getPersonId() );
        }

        @Override public void handleEvent( ActivityStartEvent activityStartEvent ) {
                double now = EpisimUtils.getCorrectedTime( activityStartEvent.getTime(), iteration );

                //ignore drt and stage activities
                if( activityStartEvent.getPersonId().toString().startsWith("drt" ) || activityStartEvent.getPersonId().toString().startsWith("rt" ) || TripStructureUtils.isStageActivityType( activityStartEvent.getActType() ) ) {
                        return;
                }

                // find the person:
                EpisimPerson personWrapper = this.personMap.computeIfAbsent( activityStartEvent.getPersonId(), EpisimPerson::new );

                // find the link:
                EpisimLink linkWrapper = this.linkMap.computeIfAbsent( activityStartEvent.getLinkId(), EpisimLink::new );

                // create pseudo facility id that includes the activity type:
                Id<Facility> pseudoFacilityId = createPseudoFacilityId( activityStartEvent );

                // find the facility
                EpisimFacility pseudoFacilityWrapper = this.pseudoFacilityMap.computeIfAbsent(pseudoFacilityId, EpisimFacility::new );

                // add facility to link
                linkWrapper.addPseudoFacility(pseudoFacilityWrapper);

                // add person to facility
                pseudoFacilityWrapper.addPerson(personWrapper, now );

                personWrapper.setLastFacilityId(pseudoFacilityId.toString());

                handlePersonTrajectory(activityStartEvent.getPersonId(), activityStartEvent.getActType().toString());

        }

        private Id<Facility> createPseudoFacilityId( HasFacilityId event ) {
                if (episimConfig.getFacilitiesHandling()== EpisimConfigGroup.FacilitiesHandling.snz ) {
                        return Id.create( event.getFacilityId(), Facility.class );
                } else if ( episimConfig.getFacilitiesHandling() == EpisimConfigGroup.FacilitiesHandling.bln ){
                        if ( event instanceof ActivityStartEvent ){
                                ActivityStartEvent theEvent = (ActivityStartEvent) event;
                                return Id.create( theEvent.getActType().split( "_" )[0] + "_" + theEvent.getLinkId().toString(), Facility.class );
                        } else if ( event instanceof ActivityEndEvent ) {
                                ActivityEndEvent theEvent = (ActivityEndEvent) event;
                                return Id.create( theEvent.getActType().split( "_" )[0] + "_" + theEvent.getLinkId().toString(), Facility.class );
                        } else {
                                throw new RuntimeException( "unexpected event type=" + ((Event)event).getEventType() ) ;
                        }
                } else {
                        throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
                }

        }
		private void handlePersonTrajectory(Id<Person> personId, String trajectoryElement) {
			EpisimPerson person = personMap.get(personId);
			if (person.getCurrentPositionInTrajectory() + 1 == person.getTrajectory().size()) {
				return;
			}
			person.setCurrentPositionInTrajectory(person.getCurrentPositionInTrajectory() + 1);
			if (iteration > 0) {
				return;
			}
			person.addToTrajectory(trajectoryElement);
		}
        private void handleInitialInfections( EpisimPerson personWrapper ){
                // initial infections:
                if( cnt > 0 ){
                        personWrapper.setDiseaseStatus( DiseaseStatus.infectedButNotContagious );
                        personWrapper.setInfectionDate(iteration);
                        log.warn(" person " + personWrapper.getPersonId() +" has initial infection");
                        cnt--;
                        if ( scenario!=null ){
                                final Person person = PopulationUtils.findPerson( personWrapper.getPersonId(), scenario );
                                if( person != null ){
                                        person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                                }
                        }
                }
        }
        private void infectionDynamicsVehicle( EpisimPerson personLeavingVehicle, EpisimVehicle vehicle, double now ){
                infectionDynamicsGeneralized( personLeavingVehicle, vehicle, now, vehicle.getContainerId().toString() );
        }
        private void infectionDynamicsFacility( EpisimPerson personLeavingFacility, EpisimFacility facility, double now, String actType ) {
                infectionDynamicsGeneralized( personLeavingFacility, facility, now, actType );
        }
        private void infectionDynamicsGeneralized( EpisimPerson personLeavingContainer, EpisimContainer<?> container, double now, String infectionType ) {
        		
                if (iteration == 0) {
                        return;
                }

                if( EpisimUtils.isRelevantForInfectionDynamics( personLeavingContainer, container, episimConfig, iteration, rnd ) ) {
                        return;
                }

                int contactPersons = 0 ;

                ArrayList<EpisimPerson> personsToInteractWith = new ArrayList<>( container.getPersons() );
                personsToInteractWith.remove( personLeavingContainer );

                for ( int ii = 0 ; ii<personsToInteractWith.size(); ii++ ) {
                        // (this is "-1" because we can't interact with "self")

                        // we are essentially looking at the situation when the person leaves the container.  Interactions with other persons who have
                        // already left the container were treated then.  In consequence, we have some "circle of persons around us" (yyyy which should
                        //  depend on the density), and then a probability of infection in either direction.

                        // if we have seen enough, then break, no matter what:
                        if (contactPersons >= 3) {
                                break;
                        }
                        // For the time being, will just assume that the first 10 persons are the ones we interact with.  Note that because of
                        // shuffle, those are 10 different persons every day.

                        int idx = rnd.nextInt( container.getPersons().size() );
                        EpisimPerson otherPerson = container.getPersons().get( idx );

                        contactPersons++;

                        // (we count "quarantine" as well since they essentially represent "holes", i.e. persons who are no longer there and thus the
                        // density in the transit container goes down.  kai, mar'20)

                        if ( personLeavingContainer.getDiseaseStatus()==otherPerson.getDiseaseStatus() ) {
                                // (if they have the same status, then nothing can happen between them)
                                continue;
                        }

                        if( EpisimUtils.isRelevantForInfectionDynamics( personLeavingContainer, container, episimConfig, iteration, rnd ) ) {
                                return;
                        }

                        // keep track of contacts:
                        if(infectionType.equals("home") || infectionType.equals("work") || (infectionType.equals("leisure") && rnd.nextDouble() < 0.8)) {
                                if (!personLeavingContainer.getTracableContactPersons().contains(otherPerson)) {
                                        personLeavingContainer.addTracableContactPerson(otherPerson);
                                }
                                if (!otherPerson.getTracableContactPersons().contains(personLeavingContainer)) {
                                        otherPerson.addTracableContactPerson(personLeavingContainer);
                                }
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
                        if (container instanceof EpisimVehicle ) {
                                contactIntensity = 2;
                        }
                        double infectionProba = 1 - Math.exp( - episimConfig.getCalibrationParameter() * contactIntensity * jointTimeInContainer);
                        if ( rnd.nextDouble() < infectionProba ) {
                                if ( personLeavingContainer.getDiseaseStatus()== DiseaseStatus.susceptible ) {
                                        infectPerson( personLeavingContainer, otherPerson, now, infectionType );
                                        return;
                                } else {
                                        infectPerson( otherPerson, personLeavingContainer, now, infectionType );
                                }
                        }

                }

        }

        private void infectPerson( EpisimPerson personWrapper, EpisimPerson infector, double now, String infectionType ){

                if (personWrapper.getDiseaseStatus() != DiseaseStatus.susceptible) {
                        throw new RuntimeException("Person to be infected is not susceptible. Status is=" + personWrapper.getDiseaseStatus());
                }
                if (infector.getDiseaseStatus() != DiseaseStatus.contagious) {
                        throw new RuntimeException("Infector is not contagious. Status is=" + infector.getDiseaseStatus());
                }
                if (personWrapper.getQuarantineStatus() == QuarantineStatus.full ) {
                        throw new RuntimeException("Person to be infected is in quarantine.");
                }
                if (infector.getQuarantineStatus() == QuarantineStatus.full ) {
                        throw new RuntimeException("Infector is in quarantine.");
                }
                personWrapper.setDiseaseStatus( DiseaseStatus.infectedButNotContagious );
                if ( scenario!=null ){
                        final Person person = PopulationUtils.findPerson( personWrapper.getPersonId(), scenario );
                        if( person != null ){
                                person.getAttributes().putAttribute( AgentSnapshotInfo.marker, true );
                        }
                }

                personWrapper.setInfectionDate(iteration);

                reporting.reportInfection( personWrapper, infector, now, infectionType );
        }
        @Override public void reset( int iteration ){

                for ( EpisimVehicle vehicleWrapper : this.vehicleMap.values()) {
                        vehicleWrapper.clearPersons();
                }
                // why not just vehicleMap.clear()? kai, mar'20


                for ( EpisimPerson person : personMap.values()) {
                	person.setCurrentPositionInTrajectory(0);
                        switch ( person.getDiseaseStatus() ) {
                                case susceptible:
                                        break;
                                case infectedButNotContagious:
                                        if ( iteration - person.getInfectionDate() >= 4 ) {
                                                person.setDiseaseStatus( DiseaseStatus.contagious );
                                        }
                                        break;
                                case contagious:
                                        if (iteration - person.getInfectionDate() == 6 ){
                                                final double nextDouble = rnd.nextDouble();
                                                if( nextDouble < 0.2 ){
                                                        // 20% recognize that they are sick and go into quarantaine:

                                                        person.setQuarantineDate( iteration );
                                                        // yyyy date needs to be qualified by status (or better, add iteration into quarantine status setter)

                                                        person.setQuarantineStatus( QuarantineStatus.full );
                                                        // yyyy this should become "home"!  kai, mar'20

                                                        if( episimConfig.getPutTracablePersonsInQuarantine() == PutTracablePersonsInQuarantine.yes ){
                                                                for( EpisimPerson pw : person.getTracableContactPersons() ){
                                                                        if( pw.getQuarantineStatus() == QuarantineStatus.no ){

                                                                                pw.setQuarantineStatus( QuarantineStatus.full );
                                                                                // yyyy this should become "home"!  kai, mar'20

                                                                                pw.setQuarantineDate( iteration );
                                                                                // yyyy date needs to be qualified by status (or better, add iteration into
                                                                                // quarantine status setter)

                                                                        }
                                                                }
                                                        }

                                                }
                                        } else if ( iteration - person.getInfectionDate() == 10 ) {
                                                if ( rnd.nextDouble() < 0.045 ){
                                                        person.setDiseaseStatus( DiseaseStatus.seriouslySick );
                                                }
                                        } else if ( iteration - person.getInfectionDate() >= 16 ) {
                                                person.setDiseaseStatus( DiseaseStatus.recovered );
                                        }
                                        break;
                                case seriouslySick:
                                        if ( iteration - person.getInfectionDate() == 11 ) {
                                                if ( rnd.nextDouble() < 0.25 ){
                                                        person.setDiseaseStatus( DiseaseStatus.critical );
                                                }
                                        } else if ( iteration - person.getInfectionDate() >= 23 ) {
                                                person.setDiseaseStatus( DiseaseStatus.recovered );
                                        }
                                        break;
                                case critical:
                                        if ( iteration - person.getInfectionDate() == 20 ) {
                                                person.setDiseaseStatus( DiseaseStatus.seriouslySick );
                                        }
                                        break;
                                case recovered:
                                        break;
                                default:
                                        throw new IllegalStateException( "Unexpected value: " + person.getDiseaseStatus() );
                        }
                        if (person.getQuarantineStatus() == QuarantineStatus.full ) {
                                if (iteration - person.getQuarantineDate() >= 14) {
                                        person.setQuarantineStatus( QuarantineStatus.no );
                                }
                        }
                        person.getTracableContactPersons().clear();
                }

                this.iteration = iteration;

                reporting.reporting( personMap, iteration );

        }
        private static class EpisimLink{
                private final Id<Link> linkId;
                private Set<EpisimFacility> pseudoFacilites = new LinkedHashSet<>();
                EpisimLink( Id<Link> vehicleId ) {
                        this.linkId = vehicleId;
                }
                void addPseudoFacility( EpisimFacility pseudoFacility ) {
                        pseudoFacilites.add( pseudoFacility );
                }
                Id<Link> getLinkId(){
                        return linkId;
                }
                Set<EpisimFacility> getPseudoFacilities() {
                        return pseudoFacilites;
                }
        }

        static class EpisimVehicle extends EpisimContainer<Vehicle>{
                EpisimVehicle( Id<Vehicle> vehicleId ){
                        super( vehicleId );
                }
        }
        static class EpisimFacility extends EpisimContainer<Facility>{
                EpisimFacility( Id<Facility> facilityId ){
                        super( facilityId );
                }
        }
        enum DiseaseStatus{susceptible, infectedButNotContagious, contagious, seriouslySick, critical, recovered};
        enum QuarantineStatus {full, atHome, no}
}

