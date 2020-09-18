/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.run.ev;

import com.google.common.collect.ImmutableList;
import org.matsim.vehicles.EngineInformation;

import java.util.Collection;

class EVUtils {

	private static final String INITIALENERGY = "initialEnergy";
	private static final String CHARGERTYPES = "chargerTypes";

	/**
	 *
	 * @param engineInformation
	 * @return the initial energy in Joule
	 */
	static Double getInitialEnergy(EngineInformation engineInformation ){
		return (Double) engineInformation.getAttributes().getAttribute( INITIALENERGY );
	}

	/**
	 *
	 * @param engineInformation
	 * @param initialEnergy in Joule
	 */
	static void setInitialEnergy(EngineInformation engineInformation, double initialEnergy ){
		engineInformation.getAttributes().putAttribute(INITIALENERGY,  initialEnergy);
	}

	static ImmutableList<String> getChargerTypes(EngineInformation engineInformation ){
		return ImmutableList.copyOf((Collection<String>) engineInformation.getAttributes().getAttribute( CHARGERTYPES));
	}

	static void setChargerTypes(EngineInformation engineInformation, Collection<String> chargerTypes ){
		engineInformation.getAttributes().putAttribute(CHARGERTYPES,  chargerTypes);
	}

}
