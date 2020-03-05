package org.matsim.run.drt;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

class KNEventsInfection{

        public static void main( String[] args ){
//              String filename = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_wo_linkEnterLeave.xml.gz";
                
        	String filename;
                if ( args.length==0 || args[0]=="" ) {
//                	filename = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_wo_linkEnterLeave.xml.gz";
                	filename = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/output-berlin-v5.4-10pct/berlin-v5.4-10pct.output_events_reduced.xml.gz";
                } else {
                	filename = args[0] ;
                }
                
                
                EventsManager events = EventsUtils.createEventsManager();
                
                events.addHandler( new InfectionEventHandler() );

                for ( int iteration=0 ; iteration<=1000 ; iteration++ ){
                        events.resetHandlers( iteration );
                        EventsUtils.readEvents( events, filename );
                }

        }

}
