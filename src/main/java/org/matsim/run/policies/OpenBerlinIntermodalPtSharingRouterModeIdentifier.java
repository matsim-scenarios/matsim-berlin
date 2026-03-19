package org.matsim.run.policies;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is an adapted version of class OpenBerlinIntermodalPtDrtRouterModeIdentifier for sharing modes.
 * I do not understand why this class is necessary as -- except some small differences -- is the same as OpenBerlinIntermodalPtSharingRouterAnalysisModeIdentifier.
 * For OpenBerlinDrtScenario such a system of 2 classes seems to be necessary, so we will use it for sharing as well. -sm0226
 */
final class OpenBerlinIntermodalPtSharingRouterModeIdentifier implements AnalysisMainModeIdentifier {
	private final List<String> modeHierarchy = new ArrayList<>() ;
	private final List<String> sharingModes;
	private final Logger log = LogManager.getLogger(OpenBerlinIntermodalPtSharingRouterModeIdentifier.class);

	@Inject
	OpenBerlinIntermodalPtSharingRouterModeIdentifier() {
		sharingModes = Arrays.asList(OpenBerlinScooterSharingScenario.E_SCOOTER, "sharing_" + OpenBerlinScooterSharingScenario.E_SCOOTER);

		modeHierarchy.add( TransportMode.walk ) ;
		modeHierarchy.add( TransportMode.bike );
		modeHierarchy.add( TransportMode.ride ) ;
		modeHierarchy.add( TransportMode.car ) ;
		modeHierarchy.add( "car2" ) ;
		for (String sharingMode: sharingModes) {
			modeHierarchy.add( sharingMode ) ;
		}
		modeHierarchy.add( TransportMode.pt ) ;
		modeHierarchy.add( "freight" );

		// NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
		// in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
		// make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
		// trip as two separate trips. kai, sep'16
	}

	@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
		int mainModeIndex = -1 ;
		for ( PlanElement pe : planElements ) {
			int index;
			String mode;
			if ( pe instanceof Leg leg ) {
				mode = leg.getMode();
			} else {
				continue;
			}
			if (mode.equals(TransportMode.non_network_walk)) {
				// skip, this is only a helper mode in case walk is routed on the network
				continue;
			}
			index = modeHierarchy.indexOf( mode ) ;
			if ( index < 0 ) {
				log.error("unknown mode={}", mode);
				throw new IllegalStateException("") ;
			}
			if ( index > mainModeIndex ) {
				mainModeIndex = index ;
			}
		}
		if (mainModeIndex == -1) {
			log.error("no main mode found for trip {}", planElements);
			throw new IllegalStateException("") ;
		}
		return modeHierarchy.get( mainModeIndex ) ;
	}
}
