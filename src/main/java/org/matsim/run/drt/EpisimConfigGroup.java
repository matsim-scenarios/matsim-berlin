package org.matsim.run.drt;

import org.matsim.core.config.ReflectiveConfigGroup;

public final class EpisimConfigGroup extends ReflectiveConfigGroup {
        private static final String GROUPNAME = "episim";
        public EpisimConfigGroup( ){
                super( GROUPNAME );
        }
        // ---
        private double sample = 0.01;
        public double getSample(){
                return this.sample;
        }
        public void setSample( double sample ){
                this.sample = sample;
        }
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
        // ---
        public enum ClosedActivity { business, educ_higher, educ_secondary, errands, home, leisure, shopping, work }
        private ClosedActivity closedActivity1 = null;
        @StringGetter("closedActivity1")
        public ClosedActivity getClosedActivity1(){
                return this.closedActivity1;
        }
        @StringSetter("closedActivity1")
        public void setClosedActivity1( ClosedActivity closedActivity1 ){
                this.closedActivity1 = closedActivity1;
        }
        // ---
        private double closedActivity1Sample = 1;
        @StringGetter("closedActivity1Sample")
        public double getClosedActivity1Sample(){
                return this.closedActivity1Sample;
        }
        @StringSetter("closedActivity1Sample")
        public void setClosedActivity1Sample( double closedActivity1Sample ){
                this.closedActivity1Sample = closedActivity1Sample;
        }
        // ---
        private ClosedActivity closedActivity2 = null;
        @StringGetter("closedActivity2")
        public ClosedActivity getClosedActivity2(){
                return this.closedActivity2;
        }
        @StringSetter("closedActivity2")
        public void setClosedActivity2( ClosedActivity closedActivity2 ){
                this.closedActivity2 = closedActivity2;
        }
        // ---
        private double closedActivity2Sample = 1;
        @StringGetter("closedActivity2Sample")
        public double getClosedActivity2Sample(){
                return this.closedActivity2Sample;
        }
        @StringSetter("closedActivity2Sample")
        public void setClosedActivity2Sample( double closedActivity2Sample ){
                this.closedActivity2Sample = closedActivity2Sample;
        }
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
        private double calibrationParameter = 0.0000005;
        @StringGetter("calibrationParameter")
        public double getCalibrationParameter(){
                return this.calibrationParameter;
        }
        @StringSetter("calibrationParameter")
        public void setCalibrationParameter( double calibrationParameter ){
                this.calibrationParameter = calibrationParameter;
        }
        // ---
        
        

}
