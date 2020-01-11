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

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author ikaddoura
 */

public class BerlinExperimentalConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "berlinExperimental" ;

	private static final String DRT_INTERMODAL_ACCESS_EGRESS_RANDOMIZATION = "drtIntermodalAccessEgressRandomization";

	public BerlinExperimentalConfigGroup() {
		super(GROUP_NAME);
	}
	
	private double drtIntermodalAccessEgressRandomization = 0.;

	@StringGetter( DRT_INTERMODAL_ACCESS_EGRESS_RANDOMIZATION )
	public double getDrtIntermodalAccessEgressRandomization() {
		return drtIntermodalAccessEgressRandomization;
	}
	
	@StringSetter( DRT_INTERMODAL_ACCESS_EGRESS_RANDOMIZATION )
	public void setDrtIntermodalAccessEgressRandomization(double drtIntermodalAccessEgressRandomization) {
		this.drtIntermodalAccessEgressRandomization = drtIntermodalAccessEgressRandomization;
	}
			
}

