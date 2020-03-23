package org.matsim.run.drt;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EpisimConfigGroup extends ReflectiveConfigGroup {
        private static final Logger log = Logger.getLogger( EpisimConfigGroup.class );
        private static final String GROUPNAME = "episim";
        public EpisimConfigGroup( ){
                super( GROUPNAME );
        }
        // ---
//        private double sample = 0.01;
//        public double getSample(){
//                return this.sample;
//        }
//        public void setSample( double sample ){
//                this.sample = sample;
//        }
        // currently nowhere used.  kai, mar'20
        // ---
//        public enum Case { berlin1pct, berlin10pct, snz25pct }
//        private Case theCase = Case.berlin1pct;
//        @StringGetter("case")
//        public Case getCase(){
//                return this.theCase;
//        }
//        @StringSetter("case")
//        public void setCase( Case theCase ){
//                this.theCase = theCase;
//        }
        // yyyyyy I woud actually like to have the "cases" back.  Reason is that I would expect that "optimal" calibration parameters to not only depend on
        // the sample sizes.  kai, mar'20
        // ---
//        public enum ClosedActivity { business, educ_higher, educ_secondary, errands, home, leisure, shopping, work }
        // these are better Strings.  Let's discussion in person.  kai, mar'20

//        private String closedActivity1 = null;
//        @StringGetter("closedActivity1")
//        public String getClosedActivity1(){
//                return this.closedActivity1;
//        }
//        @StringSetter("closedActivity1")
//        public void setClosedActivity1( String closedActivity1 ){
//                this.closedActivity1 = closedActivity1;
//        }
        // ---
//        private double closedActivity1Sample = 1;
//        @StringGetter("closedActivity1Sample")
//        public double getClosedActivity1Sample(){
//                return this.closedActivity1Sample;
//        }
//        @StringSetter("closedActivity1Sample")
//        public void setClosedActivity1Sample( double closedActivity1Sample ){
//                this.closedActivity1Sample = closedActivity1Sample;
//        }
        // ---
//        private double closedActivity1Date = 0;
//        @StringGetter("closedActivity1Date")
//        public double getClosedActivity1Date(){
//                return this.closedActivity1Date;
//        }
//        @StringSetter("closedActivity1Date")
//        public void setClosedActivity1Date( double closedActivity1Date ){
//                this.closedActivity1Date = closedActivity1Date;
//        }
//        // ---
//        private String closedActivity2 = null;
//        @StringGetter("closedActivity2")
//        public String getClosedActivity2(){
//                return this.closedActivity2;
//        }
//        @StringSetter("closedActivity2")
//        public void setClosedActivity2( String closedActivity2 ){
//                this.closedActivity2 = closedActivity2;
//        }
//        // ---
//        private double closedActivity2Date = 0;
//        @StringGetter("closedActivity2Date")
//        public double getClosedActivity2Date(){
//                return this.closedActivity2Date;
//        }
//        @StringSetter("closedActivity2Date")
//        public void setClosedActivity2Date( double closedActivity2Date ){
//                this.closedActivity2Date = closedActivity2Date;
//        }
        // ---
//        private double closedActivity2Sample = 1;
//        @StringGetter("closedActivity2Sample")
//        public double getClosedActivity2Sample(){
//                return this.closedActivity2Sample;
//        }
//        @StringSetter("closedActivity2Sample")
//        public void setClosedActivity2Sample( double closedActivity2Sample ){
//                this.closedActivity2Sample = closedActivity2Sample;
//        }
        // ---
        public enum UsePt { yes, no }
        private UsePt usePt = UsePt.yes;
        @StringGetter("usePt")
        public UsePt getUsePt(){
                return this.usePt;
        }
        @StringSetter("usePt")
        public void setUsePt( UsePt usePt ){
                this.usePt = usePt;
        }
        // ---
        private double usePtDate = 0;
        @StringGetter("usePtDate")
        public double getUsePtDate(){
                return this.usePtDate;
        }
        @StringSetter("usePtDate")
        public void setUsePtDate( double usePtDate ){
                this.usePtDate = usePtDate;
        }
        // ---
//        private double shutdownDate = 1000;
//        @StringGetter("shutdownDate")
//        public double getShutdownDate(){
//                return this.shutdownDate;
//        }
//        @StringSetter("shutdownDate")
//        public void setShutdownDate( double shutdownDate ){
//                this.shutdownDate = shutdownDate;
//        }
        // ---
//        private double quarantineSample = 0.2;
//        @StringGetter("quarantineSample")
//        public double getQuarantineSample(){
//                return this.quarantineSample;
//        }
//        @StringSetter("quarantineSample")
//        public void setQuarantineSample( double quarantineSample ){
//                this.quarantineSample = quarantineSample;
//        }
        // alle anderen Bestandteile des "disease progression models" sind auch "in code"!  kai, mar'20
        // ---
        private String inputEventsFile = null;
        @StringGetter("inputEventsFile")
        public String getInputEventsFile(){
                return this.inputEventsFile;
        }
        @StringSetter("inputEventsFile")
        public void setInputEventsFile( String inputEventsFile ){
                this.inputEventsFile = inputEventsFile;
        }
        // ---
        private double calibrationParameter = 0.0000012;
        @StringGetter("calibrationParameter")
        public double getCalibrationParameter(){
                return this.calibrationParameter;
        }
        @StringSetter("calibrationParameter")
        public void setCalibrationParameter( double calibrationParameter ){
                this.calibrationParameter = calibrationParameter;
        }
        // ---
        public enum PutTracablePersonsInQuarantine { yes, no }
        private PutTracablePersonsInQuarantine putTracablePersonsInQuarantine = PutTracablePersonsInQuarantine.no;
        @StringGetter("putTracablePersonsInQuarantine")
        public PutTracablePersonsInQuarantine getPutTracablePersonsInQuarantine(){
                return this.putTracablePersonsInQuarantine;
        }
        @StringSetter("putTracablePersonsInQuarantine")
        public void setUsePt( PutTracablePersonsInQuarantine putTracablePersonsInQuarantine ){
                this.putTracablePersonsInQuarantine = putTracablePersonsInQuarantine;
        }
        // ---
        public enum FacilitiesHandling {bln, snz}
        private FacilitiesHandling facilitiesHandling = FacilitiesHandling.snz;
        @StringGetter( "facilitiesHandling" )
        public FacilitiesHandling getFacilitiesHandling(){
                return facilitiesHandling;
        }
        @StringSetter( "facilitiesHandling" )
        public void setFacilitiesHandling( FacilitiesHandling facilitiesHandling ){
                this.facilitiesHandling = facilitiesHandling;
        }
        public static class InfectionParams extends ReflectiveConfigGroup {
                static final String SET_TYPE = "xyz";
                private String containerName;

                InfectionParams( final String containerName ) {
                        this();
                        this.containerName = containerName;
                }

                private InfectionParams() {
                        super(SET_TYPE);
                }
                //                @StringGetter( "activityType" )
                String getContainerName(){
                        return containerName;
                }
                //                @StringSetter( "activityType" )
//                public void setContainerName( String actType ){
//                        this.containerName = actType;
//                }
                // ---
                private long shutdownDay = Long.MAX_VALUE;
                /** @noinspection WeakerAccess*/
                public long getShutdownDay(){
                        return shutdownDay;
                }
                /** @noinspection WeakerAccess
                 * @return*/
                public InfectionParams setShutdownDay( long shutdownDay ){
                        this.shutdownDay = shutdownDay;
                        return this;
                }
                // ---
                private double remainingFraction = 0.;
                /** @noinspection WeakerAccess*/
                public double getRemainingFraction(){
                        return remainingFraction;
                }
                /** @noinspection WeakerAccess
                 * @return*/
                public InfectionParams setRemainingFraction( double remainingFraction ){
                        this.remainingFraction = remainingFraction;
                        return this;
                }
                // ---
                private double contactIntensity = 1.;
                /**
                 * this is from iteration 0!
                 */
                public double getContactIntensity(){
                        return contactIntensity;
                }
                /**
                 * this is from iteration 0!
                 * @return
                 */
                public InfectionParams setContactIntensity( double contactIntensity ){
                        this.contactIntensity = contactIntensity;
                        return this;
                }
        }
        @Override
        public void addParameterSet(final ConfigGroup set ) {
                // this is, I think, necessary for the automatic reading from file, and possibly for the commandline stuff.
                switch (set.getName()) {
                        case InfectionParams.SET_TYPE:
                                addContainerParams( (InfectionParams) set );
                                break;
                        default:
                                throw new IllegalArgumentException(set.getName());
                }
        }
        void addContainerParams( final InfectionParams params ) {
                final InfectionParams previous = this.getContainerParams().get( params.getContainerName() );

                if (previous != null) {
                        log.info("scoring parameters for activityType=" + previous.getContainerName() + " were just replaced." );

                        final boolean removed = removeParameterSet(previous);
                        if (!removed)
                                throw new RuntimeException("problem replacing params ");
                }

                super.addParameterSet(params);
        }

        Map<String, InfectionParams> getContainerParams() {
                @SuppressWarnings("unchecked")
                final Collection<InfectionParams> parameters = (Collection<InfectionParams>) getParameterSets( InfectionParams.SET_TYPE );
                final Map<String, InfectionParams> map = new LinkedHashMap<>();

                for ( InfectionParams pars : parameters) {
                        if (this.isLocked()) {
                                pars.setLocked();
                        }
                        map.put(pars.getContainerName(), pars );
                }

                return Collections.unmodifiableMap( map );
        }


}
