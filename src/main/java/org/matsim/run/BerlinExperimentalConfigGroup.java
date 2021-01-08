/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.run;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author ikaddoura
 */

public class BerlinExperimentalConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "berlinExperimental" ;

    private static final String POPULATION_DOWNSAMPLE_FACTOR = "populationDownsampleFactor";
    private static final String TAG_DRT_LINKS_BUFFER_AROUND_SERVICE_AREA_SHP = "tagDrtLinksBufferAroundServiceAreaShp";
	
	public BerlinExperimentalConfigGroup() {
		super(GROUP_NAME);
	}

	public BerlinExperimentalConfigGroup( boolean storeUnknownParametersAsStrings ){
            super( GROUP_NAME, storeUnknownParametersAsStrings ) ;
        }
	
	private double populationDownsampleFactor = 1.0;
    private double tagDrtLinksBufferAroundServiceAreaShp = 2000.0;
	
    @StringGetter(POPULATION_DOWNSAMPLE_FACTOR)
    public double getPopulationDownsampleFactor() {
        return populationDownsampleFactor;
    }

    @StringSetter(POPULATION_DOWNSAMPLE_FACTOR)
    public void setPopulationDownsampleFactor(double populationDownsampleFactor) {
        this.populationDownsampleFactor = populationDownsampleFactor;
    }

    @StringGetter(TAG_DRT_LINKS_BUFFER_AROUND_SERVICE_AREA_SHP)
    public double getTagDrtLinksBufferAroundServiceAreaShp() {
        return tagDrtLinksBufferAroundServiceAreaShp;
    }

    @StringSetter(TAG_DRT_LINKS_BUFFER_AROUND_SERVICE_AREA_SHP)
    public void setTagDrtLinksBufferAroundServiceAreaShp(double tagDrtLinksBufferAroundServiceAreaShp) {
        this.tagDrtLinksBufferAroundServiceAreaShp = tagDrtLinksBufferAroundServiceAreaShp;
    }
			
}

