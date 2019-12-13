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

package org.matsim.run.drt;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.collections.Tuple;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class PtRoutingModeWrapperProvider implements Provider<RoutingModule> {
	
	private final List<Tuple<String,String>> personAttributes2Values;
	@Inject private @Named(TransportMode.pt) RoutingModule ptRouter;
	
	PtRoutingModeWrapperProvider(final List<Tuple<String,String>> personAttributes2Values) {
		this.personAttributes2Values = personAttributes2Values;
	}

	@Override
	public RoutingModule get() {
		return new PtRoutingModeWrapper(personAttributes2Values, ptRouter);
	}

}
