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

package org.matsim.legacy.run;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.StringUtils;

/**
 *
 * @author ikaddoura
 */

public class BerlinExperimentalConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "berlinExperimental" ;

    private static final String POPULATION_DOWNSAMPLE_FACTOR = "populationDownsampleFactor";
    private static final String TAG_DRT_LINKS_BUFFER_AROUND_SERVICE_AREA_SHP = "tagDrtLinksBufferAroundServiceAreaShp";
    private static final String PLAN_TYPE_OVERWRITING = "planTypeOverwriting";
    private static final String NETWORK_MODES_TO_ADD_TO_ALL_CAR_LINKS = "networkModesToAddToAllCarLinks";

    private static final String ANALYSIS_LEVEL = "analysisLevel";

    enum PlanTypeOverwriting {NO_OVERWRITE, TAG_INITIAL_SELECTED_PLAN_AND_MODIFIED_PLANS_DIFFERENTLY};
    enum AnalysisLevel {MINIMAL, FULL}

	public BerlinExperimentalConfigGroup() {
		super(GROUP_NAME);
	}

	public BerlinExperimentalConfigGroup( boolean storeUnknownParametersAsStrings ){
            super( GROUP_NAME, storeUnknownParametersAsStrings ) ;
        }

	private double populationDownsampleFactor = 1.0;
    private double tagDrtLinksBufferAroundServiceAreaShp = 2000.0;
    private PlanTypeOverwriting planTypeOverwriting = PlanTypeOverwriting.NO_OVERWRITE;
    private Collection<String> networkModesToAddToAllCarLinks = Collections.emptyList();
    private AnalysisLevel analysisLevel = AnalysisLevel.MINIMAL;

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

    @StringGetter(PLAN_TYPE_OVERWRITING)
    public PlanTypeOverwriting getPlanTypeOverwriting () {
        return planTypeOverwriting;
    }

    @StringSetter(PLAN_TYPE_OVERWRITING)
    public void setPlanTypeOverwriting (PlanTypeOverwriting planTypeOverwriting) {
        this.planTypeOverwriting = planTypeOverwriting;
    }

    public Collection<String> getNetworkModesToAddToAllCarLinks() {
        return this.networkModesToAddToAllCarLinks;
    }

    @StringGetter(NETWORK_MODES_TO_ADD_TO_ALL_CAR_LINKS)
    public String getNetworkModesToAddToAllCarLinksAsString() {
        return String.join(",", networkModesToAddToAllCarLinks);
    }

    public void setNetworkModesToAddToAllCarLinks(Collection<String> networkModesToAddToAllCarLinks) {
        this.networkModesToAddToAllCarLinks = networkModesToAddToAllCarLinks;
    }

    @StringSetter(NETWORK_MODES_TO_ADD_TO_ALL_CAR_LINKS)
    public void setNetworkModesToAddToAllCarLinksAsString(String networkModesToAddToAllCarLinksString) {
        this.networkModesToAddToAllCarLinks = ImmutableSet.copyOf(StringUtils.explode(networkModesToAddToAllCarLinksString, ','));
    }

    @StringGetter(ANALYSIS_LEVEL)
    public AnalysisLevel getAnalysisLevel () {
        return analysisLevel;
    }

    @StringSetter(ANALYSIS_LEVEL)
    public void setAnalysisLevel (AnalysisLevel analysisLevel) {
        this.analysisLevel = analysisLevel;
    }

}

