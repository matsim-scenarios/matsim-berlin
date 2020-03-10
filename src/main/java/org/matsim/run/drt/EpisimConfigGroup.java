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
                return sample;
        }
        public void setSample( double sample ){
                this.sample = sample;
        }
        // ---
        public enum Case { berlin1pct, berlin10pct, snz25pct }
        private Case theCase = Case.berlin1pct;
        @StringGetter("case")
        public Case getCase(){
                return theCase;
        }
        @StringSetter("case")
        public void setCase( Case theCase ){
                this.theCase = theCase;
        }
        // ---

}
