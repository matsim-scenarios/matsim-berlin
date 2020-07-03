
/* *********************************************************************** *
 * project: org.matsim.*
 * OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.run.drt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;

import com.google.inject.Inject;

/**
 * Based on {@link TransportPlanningMainModeIdentifier}
 *
 * ModeStatsControlerListener takes modes from scoreConfig.getAllModes() and ignores everything else.
 * So keep this in mind before using this class.
 * 
 * @author nagel / gleich
 *
 */
public final class OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier implements AnalysisMainModeIdentifier {
	private final List<String> modeHierarchy = new ArrayList<>() ;
	private final List<String> drtModes;
	private static final Logger log = Logger.getLogger(OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.class);
	public static final String ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS = "pt_w_drt_used";

	@Inject
	public OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier() {
		drtModes = Arrays.asList(TransportMode.drt, "drt2", "drt_teleportation");
		
		modeHierarchy.add( TransportMode.walk ) ;
		modeHierarchy.add( "bicycle" ); // TransportMode.bike is not registered as main mode, only "bicycle" ;
		modeHierarchy.add( TransportMode.ride ) ;
		modeHierarchy.add( TransportMode.car ) ;
		for (String drtMode: drtModes) {
			modeHierarchy.add( drtMode ) ;
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
		List<String> modesFound = new ArrayList<>();
		for ( PlanElement pe : planElements ) {
			int index;
			String mode;
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
				mode = leg.getMode();
			} else {
				continue;
			}
			if (mode.equals(TransportMode.non_network_walk)) {
				// skip, this is only a helper mode for access, egress and pt transfers
				continue;
			}
			if (mode.equals(TransportMode.transit_walk)) {
				mode = TransportMode.walk;
			} else {
				for (String drtMode: drtModes) {
					if (mode.equals(drtMode + "_fallback")) {// transit_walk / drt_walk / ... to be replaced by _fallback soon
						mode = TransportMode.walk;
					}
				}
			}
			modesFound.add(mode);
			index = modeHierarchy.indexOf( mode ) ;
			if ( index < 0 ) {
				throw new RuntimeException("unknown mode=" + mode ) ;
			}
			if ( index > mainModeIndex ) {
				mainModeIndex = index ;
			}
		}
		if (mainModeIndex == -1) {
			throw new RuntimeException("no main mode found for trip " + planElements.toString() ) ;
		}
		
		String mainMode = modeHierarchy.get( mainModeIndex ) ;
		// differentiate pt monomodal/intermodal
		if (mainMode.equals(TransportMode.pt)) {
			boolean isDrtPt = false;
			for (String modeFound: modesFound) {
				if (modeFound.equals(TransportMode.pt)) {
					continue;
				} else if (modeFound.equals(TransportMode.walk)) {
					continue;
				} else if (drtModes.contains(modeFound)) {
					isDrtPt = true;
				} else {
					log.error("unknown intermodal pt trip: " + planElements.toString());
					throw new RuntimeException("unknown intermodal pt trip");
				}
			}
			
			if (isDrtPt) {
				return OpenBerlinIntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS;
			} else {
				return TransportMode.pt;
			}
			
		} else {
			return mainMode;
		}
	}
}
