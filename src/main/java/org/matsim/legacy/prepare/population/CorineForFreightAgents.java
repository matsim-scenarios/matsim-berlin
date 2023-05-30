/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.legacy.prepare.population;

import java.util.HashMap;
import java.util.Map;

import playground.vsp.corineLandcover.CORINELandCoverCoordsModifier;

/**
* @author ikaddoura
*/

public class CorineForFreightAgents {

	public static void main(String[] args) {
    		String corineLandCoverFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/corine_landcover_berlin-brandenburg/corine_lancover_berlin-brandenburg_GK4.shp";

	    String zoneFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
	    String zoneIdTag = "NR";

	    String matsimPlans = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/freight/freight-agents-berlin4.1_sampleSize0.1.xml.gz";
	    String outPlans = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/freight/freight-agents-berlin4.1_sampleSize0.1_corine-landcover_1.xml.gz";

	    boolean simplifyGeom = false;
	    boolean combiningGeoms = false;
	    boolean sameHomeActivity = false;
	    String homeActivityPrefix = "home";

	    Map<String, String> shapeFileToFeatureKey = new HashMap<>();
	    shapeFileToFeatureKey.put(zoneFile, zoneIdTag);

	    CORINELandCoverCoordsModifier plansFilterForCORINELandCover = new CORINELandCoverCoordsModifier(matsimPlans,
	            shapeFileToFeatureKey,
	            corineLandCoverFile,
	            simplifyGeom,
	            combiningGeoms,
	            sameHomeActivity,
	            homeActivityPrefix);
	    plansFilterForCORINELandCover.process();
	    plansFilterForCORINELandCover.writePlans(outPlans);
    }
}

