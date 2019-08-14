/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

/**
 * @author ikaddoura based on thibaut
 */
public final class BerlinMainModeIdentifier implements MainModeIdentifier {
	@Override
	public String identifyMainMode( final List<? extends PlanElement> tripElements) {
		
		if (tripElements.get(0) instanceof Activity) {
			throw new RuntimeException("This main mode identifier processes trip elements only. The first plan element must not be an instance of Activity. " + tripElements);
		}
		
		String mode = ((Leg) tripElements.get( 0 )).getMode();
		if ( mode.equals( TransportMode.transit_walk ) ) {
			return TransportMode.pt ;
			// in case no transit stop is found and for backward compatibility because earlier versions of the SBB raptor
			// returned transit_walk for direct walk trips between two activities. ihab aug'19
		}
		
		for ( PlanElement pe : tripElements ) {
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
				String mode2 = leg.getMode() ;
				if ( !mode2.contains( TransportMode.non_network_walk ) &&
						!mode2.contains( TransportMode.non_network_walk ) &&
						!mode2.contains( TransportMode.transit_walk ) ) {
					return mode2 ;
				}
			}
		}
		
		// There might be the case that no pt route is found between two activity locations. In such cases, the fork of the SBB raptor router which we use
		// returns the following: *non_network_walk* --> *pt interaction* --> *non_network_walk*.
		// The following checks this specific case and then returns pt as the main mode. ihab aug'19
		if (tripElements.size() == 3 && tripElements.get(0) instanceof Leg && tripElements.get(1) instanceof Activity && tripElements.get(2) instanceof Leg) {
			Leg leg0 = (Leg) tripElements.get(0);
			Activity act = (Activity) tripElements.get(1);
			Leg leg1 = (Leg) tripElements.get(2);
			
			if (leg0.getMode().equals(TransportMode.non_network_walk) &&
					act.getType().contains("interaction") &&
					leg1.getMode().equals(TransportMode.non_network_walk)) {
				String firstPart = act.getType().split("interaction")[0];
				String mainMode = firstPart.trim();
				return mainMode;
			}
		}
		
		throw new RuntimeException( "could not identify main mode "+ tripElements) ;
		
	}
}
