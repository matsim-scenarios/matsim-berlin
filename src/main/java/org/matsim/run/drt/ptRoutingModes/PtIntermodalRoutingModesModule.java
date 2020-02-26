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

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
 * 
 * @author vsp-gleich
 *
 */
public class PtIntermodalRoutingModesModule extends AbstractModule {
	@Override
	public void install() {
		PtIntermodalRoutingModesConfigGroup ptIntermodalRoutingModesConfigGroup = ConfigUtils
				.addOrGetModule(getConfig(), PtIntermodalRoutingModesConfigGroup.class);
		ptIntermodalRoutingModesConfigGroup.getPtIntermodalRoutingModeParameterSets()
				.forEach(ptIntermodalRoutingModeConfigGroup -> addRoutingModuleBinding(
						ptIntermodalRoutingModeConfigGroup.getRoutingMode())
								.toProvider(new PtRoutingModeWrapperProvider(ptIntermodalRoutingModeConfigGroup)));
	}
}
