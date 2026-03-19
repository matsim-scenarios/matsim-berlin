package org.matsim.run.policies;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.legacy.run.drt.OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is an adapted version of class OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier for sharing modes.
 */
final class OpenBerlinIntermodalPtDrtAndSharingRouterAnalysisModeIdentifier implements AnalysisMainModeIdentifier {
	public static final String ANALYSIS_MAIN_MODE_PT_WITH_SHARING_USED_FOR_ACCESS_OR_EGRESS = "pt_w_sharing_used";
	private static final Logger log = LogManager.getLogger(OpenBerlinIntermodalPtDrtAndSharingRouterAnalysisModeIdentifier.class);
	private final List<String> modeHierarchy = new ArrayList<>() ;
	private final List<String> sharingModes;
	private final List<String> drtModes;

	@Inject
	OpenBerlinIntermodalPtDrtAndSharingRouterAnalysisModeIdentifier() {
		sharingModes = Arrays.asList(OpenBerlinScooterSharingScenario.E_SCOOTER, "sharing_" + OpenBerlinScooterSharingScenario.E_SCOOTER);
		drtModes = Arrays.asList(TransportMode.drt, "drt2", "drt_teleportation");

		modeHierarchy.add( TransportMode.walk ) ;
		// TransportMode.bike is not registered as main mode, only "bicycle" ;
		modeHierarchy.add( TransportMode.bike );
		modeHierarchy.add( TransportMode.ride ) ;
		modeHierarchy.add( TransportMode.car ) ;
		modeHierarchy.add( "car2" ) ;
		for (String drtMode: drtModes) {
			modeHierarchy.add( drtMode ) ;
		}

		for (String sharingMode: sharingModes) {
			modeHierarchy.add( sharingMode ) ;
		}
		modeHierarchy.add( TransportMode.pt ) ;
		modeHierarchy.add( "freight" );
		modeHierarchy.add( "truck" );

		// NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
		// in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
		// make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
		// trip as two separate trips. kai, sep'16
	}

	@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
		int mainModeIndex = -1 ;
		List<String> modesFound = new ArrayList<>();
		for ( PlanElement pe : planElements ) {
			int index;
			String mode;
			if ( pe instanceof Leg leg) {
				mode = leg.getMode();
			} else {
				continue;
			}
			if (mode.equals(TransportMode.non_network_walk)) {
				// skip, this is only a helper mode in case walk is routed on the network
				continue;
			}
			modesFound.add(mode);
			index = modeHierarchy.indexOf( mode ) ;
			if ( index < 0 ) {
				log.error("unknown mode={}", mode );
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

		String mainMode = modeHierarchy.get( mainModeIndex ) ;
		// differentiate pt monomodal/intermodal
		if (mainMode.equals(TransportMode.pt)) {
			boolean isSharingPt = false;
			boolean isDrtPt = false;
			for (String modeFound: modesFound) {
				if (modeFound.equals(TransportMode.pt)) {
					continue;
				} else if (modeFound.equals(TransportMode.walk)) {
					continue;
				} else if (drtModes.contains(modeFound)) {
					isDrtPt = true;
				} else if (sharingModes.contains(modeFound)) {
					isSharingPt = true;
				} else {
					log.error("unknown intermodal pt trip: {}", planElements);
					throw new IllegalStateException("unknown intermodal pt trip");
				}
			}

			if (isDrtPt && isSharingPt) {
				log.error("Trip is a {} and {} trip. This is not possible. Aborting!", OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS,
					ANALYSIS_MAIN_MODE_PT_WITH_SHARING_USED_FOR_ACCESS_OR_EGRESS);
				throw new IllegalStateException("");
			}

			if (isSharingPt) {
				return ANALYSIS_MAIN_MODE_PT_WITH_SHARING_USED_FOR_ACCESS_OR_EGRESS;
			} else if (isDrtPt) {
				return OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS;
			} else {
				return TransportMode.pt;
			}

		} else {
			return mainMode;
		}
	}
}
