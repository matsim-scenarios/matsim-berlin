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

package org.matsim.run.drt.intermodalTripFareCompensator;

import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.StringUtils;

import com.google.common.collect.ImmutableSet;

public class IntermodalTripFareCompensatorConfigGroup extends ReflectiveConfigGroup {

	    public static final String GROUP_NAME = "intermodalTripFareCompensator";

	    public static final String COMPENSATION_MONEY_PER_TRIP = "compensationMoneyPerTrip";
	    public static final String COMPENSATION_MONEY_PER_DAY = "compensationMoneyPerDay";
		public static final String COMPENSATION_SCORE_PER_TRIP = "compensationScorePerTrip";
		public static final String COMPENSATION_SCORE_PER_DAY = "compensationScorePerDay";
	    public static final String COMPENSATION_CONDITION= "compensationCondition";
	    public static final String DRT_MODES = "drtModes";
	    public static final String PT_MODES = "ptModes";

	    private double compensationMoneyPerDay = 0.0;
	    private double compensationMoneyPerTrip = 0.0;
		private double compensationScorePerDay = 0.0;
		private double compensationScorePerTrip = 0.0;
	    private CompensationCondition compensationCondition = CompensationCondition.PtModeUsedInSameTrip;
	    
	    /*
	     * initialize, otherwise getDrtModesAsString might return null and thereby writing out a new
	     * IntermodalTripFareCompensatorConfigGroup (e.g. in regular matsim shutdown after last iteration)
	     * leads to NullPointerExceptions :-(
	     */
	    private ImmutableSet<String> drtModes = ImmutableSet.of(TransportMode.drt); 
	    private ImmutableSet<String> ptModes = ImmutableSet.of(TransportMode.pt); // same

	    public enum CompensationCondition {
	    	PtModeUsedInSameTrip, PtModeUsedAnywhereInTheDay
	    }
	    
	    public IntermodalTripFareCompensatorConfigGroup() {
	        super(GROUP_NAME);
	    }

	    @Override
	    public Map<String, String> getComments() {
	        Map<String, String> map = super.getComments();
	        map.put(COMPENSATION_MONEY_PER_TRIP, "Compensation money per Trip (compensation = refund paid to the customer = positive value)");
	        map.put(COMPENSATION_MONEY_PER_DAY, "Compensation money per Day, i.e. only paid once per day no matter the number of trips. Not implemented yet for compensationCondition==PtModeUsedInSameTrip (compensation = refund paid to the customer = positive value)");
			map.put(COMPENSATION_SCORE_PER_TRIP, "Compensation score per Trip (compensation = refund score paid to the customer = positive value)");
			map.put(COMPENSATION_SCORE_PER_DAY, "Compensation score per Day, i.e. only paid once per day no matter the number of trips. Not implemented yet for compensationCondition==PtModeUsedInSameTrip (compensation = refund score paid to the customer = positive value)");
	        map.put(COMPENSATION_CONDITION, "Condition which governs which agents are compensated. Options: "
				+ CompensationCondition.PtModeUsedInSameTrip + ", " + CompensationCondition.PtModeUsedAnywhereInTheDay);
	        map.put(DRT_MODES, "drt modes for which the compensation applies (comma separated list).");
	        map.put(PT_MODES, "pt modes for which the compensation applies (comma separated list).");
	        return map;
	    }

	    @StringGetter(COMPENSATION_MONEY_PER_TRIP)
	    public double getCompensationMoneyPerTrip() {
	        return compensationMoneyPerTrip;
	    }

	    @StringSetter(COMPENSATION_MONEY_PER_TRIP)
	    public void setCompensationMoneyPerTrip(double compensationMoneyPerTrip) {
	        this.compensationMoneyPerTrip = compensationMoneyPerTrip;
	    }
	    
	    @StringGetter(COMPENSATION_MONEY_PER_DAY)
	    public double getCompensationMoneyPerDay() {
	        return compensationMoneyPerDay;
	    }

	    @StringSetter(COMPENSATION_MONEY_PER_DAY)
	    public void setCompensationMoneyPerDay(double compensationMoneyPerDay) {
	        this.compensationMoneyPerDay = compensationMoneyPerDay;
	    }

		@StringGetter(COMPENSATION_SCORE_PER_TRIP)
		public double getCompensationScorePerTrip() {
		return compensationScorePerTrip;
	}

		@StringSetter(COMPENSATION_SCORE_PER_TRIP)
		public void setCompensationScorePerTrip(double compensationScorePerTrip) {
			this.compensationScorePerTrip = compensationScorePerTrip;
		}

		@StringGetter(COMPENSATION_SCORE_PER_DAY)
		public double getCompensationScorePerDay() {
		return compensationScorePerDay;
	}

		@StringSetter(COMPENSATION_SCORE_PER_DAY)
		public void setCompensationScorePerDay(double compensationScorePerDay) {
			this.compensationScorePerDay = compensationScorePerDay;
		}
	    
	    @StringGetter(COMPENSATION_CONDITION)
	    public CompensationCondition getCompensationCondition() {
	        return this.compensationCondition;
	    }

	    @StringSetter(COMPENSATION_CONDITION)
	    public void setCompensationCondition(CompensationCondition compensationCondition) {
	        this.compensationCondition = compensationCondition;
	    }
	    
		@StringGetter(DRT_MODES)
		public String getDrtModesAsString() {
			return String.join(",", drtModes);
		}

		public ImmutableSet<String> getDrtModes() {
			return drtModes;
		}

		@StringSetter(DRT_MODES)
		public void setDrtModesAsString(String drtModesString) {
			this.drtModes = ImmutableSet.copyOf(StringUtils.explode(drtModesString, ','));
		}

		public void setDrtModes(ImmutableSet<String> drtModes) {
			this.drtModes = drtModes;
		}
	
		@StringGetter(PT_MODES)
		public String getPtModesAsString() {
			return String.join(",", ptModes);
		}

		public ImmutableSet<String> getPtModes() {
			return ptModes;
		}

		@StringSetter(PT_MODES)
		public void setPtModesAsString(String ptModesString) {
			this.ptModes = ImmutableSet.copyOf(StringUtils.explode(ptModesString, ','));
		}

		public void setPtModes(ImmutableSet<String> ptModes) {
			this.ptModes = ptModes;
		}
}
