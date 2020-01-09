/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.run;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;

import java.util.List;

/**
 * A default implementation of {@link RaptorIntermodalAccessEgress} returning a new RIntermodalAccessEgress,
 * which contains a list of legs (same as in the input), the associated travel time as well as the disutility.
 *
 * @author pmanser / SBB
 */
public class BerlinRaptorIntermodalAccessEgress implements RaptorIntermodalAccessEgress {
	
	@Inject Config config;

    @Override
    public RIntermodalAccessEgress calcIntermodalAccessEgress(final List<? extends PlanElement> legs, RaptorParameters params, Person person) {
        double utility = 0.0;
        double tTime = 0.0;
        for (PlanElement pe : legs) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();
                double travelTime = ((Leg) pe).getTravelTime();
                if (Time.getUndefinedTime() != travelTime) {
                    tTime += travelTime;
                    // overrides individual parameters per person
                    utility += travelTime * config.planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling() / 3600;
                }
                utility = config.planCalcScore().getModes().get(mode).getConstant();
            }
        }
        return new RIntermodalAccessEgress(legs, -utility, tTime);
    }
}
