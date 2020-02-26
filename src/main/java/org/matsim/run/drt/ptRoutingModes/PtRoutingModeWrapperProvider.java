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

package org.matsim.run.drt.ptRoutingModes;

import java.util.Map;

import org.matsim.core.router.RoutingModule;
import org.matsim.run.drt.ptRoutingModes.PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * 
 * @author vsp-gleich
 *
 */
class PtRoutingModeWrapperProvider implements Provider<RoutingModule> {
	
	private final PtIntermodalRoutingModeParameterSet routingModeParams;
	@Inject private Map<String, Provider<RoutingModule>> routingModuleProviders;
	
	PtRoutingModeWrapperProvider(final PtIntermodalRoutingModeParameterSet routingModeParams) {
		this.routingModeParams = routingModeParams;
	}

	@Override
	public RoutingModule get() {
		return new PtRoutingModeWrapper(routingModeParams, routingModuleProviders.get(routingModeParams.getDelegateMode()).get());
	}

}
