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

import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class IntermodalTripFareCompensatorsConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "intermodalTripFareCompensators";

    public IntermodalTripFareCompensatorsConfigGroup() {
        super(GROUP_NAME);
    }

    public static IntermodalTripFareCompensatorsConfigGroup get(Config config) {
        return (IntermodalTripFareCompensatorsConfigGroup) config.getModules().get(GROUP_NAME);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        if (type.equals(IntermodalTripFareCompensatorConfigGroup.GROUP_NAME)) {
            return new IntermodalTripFareCompensatorConfigGroup();
        }
        throw new IllegalArgumentException(type);
    }

    @SuppressWarnings("unchecked")
    public Collection<IntermodalTripFareCompensatorConfigGroup> getIntermodalTripFareCompensatorConfigGroups() {
        return (Collection<IntermodalTripFareCompensatorConfigGroup>) getParameterSets(IntermodalTripFareCompensatorConfigGroup.GROUP_NAME);
    }

}
