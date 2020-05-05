/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.run;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.run.BerlinExperimentalConfigGroup.IntermodalAccessEgressModeUtilityRandomization;
import org.matsim.run.drt.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.run.drt.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;

/**
 * A default implementation of {@link RaptorIntermodalAccessEgress} returning a new RIntermodalAccessEgress,
 * which contains a list of legs (same as in the input), the associated travel time as well as the disutility.
 *
 * @author vsp-gleich / ikaddoura
 */
public class BerlinRaptorIntermodalAccessEgress implements RaptorIntermodalAccessEgress {
	
	Config config;	
	BerlinExperimentalConfigGroup berlinCfg;
	DrtFaresConfigGroup drtFaresConfigGroup;
	IntermodalTripFareCompensatorsConfigGroup interModalTripFareCompensatorsCfg;

	// for randomization per person, per mode, per direction (but same random value for one combination of this per routing request)
    Id<Person> lastPersonId = Id.createPersonId("");
    RaptorStopFinder.Direction lastDirection = RaptorStopFinder.Direction.EGRESS;
    Map<String, Double> lastModes2Randomization = new HashMap<>();
	
	Random random = MatsimRandom.getLocalInstance();
	
	@Inject
    BerlinRaptorIntermodalAccessEgress(Config config) {
		this.config = config;
		this.berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
		this.drtFaresConfigGroup = ConfigUtils.addOrGetModule(config, DrtFaresConfigGroup.class);
		this.interModalTripFareCompensatorsCfg = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);
	}

	@Override
    public RIntermodalAccessEgress calcIntermodalAccessEgress( final List<? extends PlanElement> legs, RaptorParameters params, Person person,
                                                               RaptorStopFinder.Direction direction) {
		// maybe nicer using raptor parameters per person ?
		String subpopulationName = null;
		if (person.getAttributes() != null) {
			Object attr = person.getAttributes().getAttribute("subpopulation") ;
			subpopulationName = attr == null ? null : attr.toString();
		}
		
		ScoringParameterSet scoringParams = config.planCalcScore().getScoringParameters(subpopulationName);
		
        double utility = 0.0;
        double tTime = 0.0;
        for (PlanElement pe : legs) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();
				OptionalTime travelTime = ((Leg) pe).getTravelTime();

                // overrides individual parameters per person; use default scoring parameters
                if (travelTime.isDefined()) {
                    tTime += travelTime.seconds();
                    utility += travelTime.seconds() * (scoringParams.getModes().get(mode).getMarginalUtilityOfTraveling() + (-1) * scoringParams.getPerforming_utils_hr()) / 3600;
                }
                Double distance = ((Leg) pe).getRoute().getDistance();
                if (distance != null && distance != 0.) {
                	utility += distance * scoringParams.getModes().get(mode).getMarginalUtilityOfDistance();
                	utility += distance * scoringParams.getModes().get(mode).getMonetaryDistanceRate() * scoringParams.getMarginalUtilityOfMoney();
                }
                utility += scoringParams.getModes().get(mode).getConstant();
                
                // account for drt fares
                for (DrtFareConfigGroup drtFareConfigGroup : drtFaresConfigGroup.getDrtFareConfigGroups()) {
                	if (drtFareConfigGroup.getMode().equals(mode)) {
                        double fare = 0.;
                		if (distance != null && distance != 0.) {
                        	fare += drtFareConfigGroup.getDistanceFare_m() * distance;
                        }
                                                
                        if (travelTime.isDefined()) {
                            fare += drtFareConfigGroup.getTimeFare_h() * travelTime.seconds() / 3600.;

                        }
                        
                        fare += drtFareConfigGroup.getBasefare(); 
                        fare = Math.max(fare, drtFareConfigGroup.getMinFarePerTrip());
                        utility += -1. * fare * scoringParams.getMarginalUtilityOfMoney();
                	}
                }
                
                // account for intermodal trip fare compensations
                for (IntermodalTripFareCompensatorConfigGroup compensatorCfg : interModalTripFareCompensatorsCfg.getIntermodalTripFareCompensatorConfigGroups()) {
                	if (compensatorCfg.getDrtModes().contains(mode) && compensatorCfg.getPtModes().contains(TransportMode.pt)) {
                		// the following is a compensation, thus positive!
                		utility += compensatorCfg.getCompensationPerTrip() * scoringParams.getMarginalUtilityOfMoney();
                	}
                }

                //check whether the same agente was already handled for the same direction (for each trip it should always first handle all access stops and then all egress stops)
                // assumes that the RaptorStopFinder handles by person, then by direction, then by mode for each routing request (what DefaultRaptorStopFinder does)
                // -> same person, same direction should be all in one row without other agents in between (otherwise will not work as expected)
                if(!(lastPersonId.equals(person.getId()) && lastDirection.equals(direction))) {
                    lastModes2Randomization.clear();
                    lastPersonId = person.getId();
                    lastDirection = direction;
                }

                // apply randomization to utility if applicable;
                IntermodalAccessEgressModeUtilityRandomization randomization = berlinCfg.getIntermodalAccessEgressModeUtilityRandomization(mode);
                if (randomization != null) {
                	double utilityRandomizationSigma = randomization.getAdditiveRandomizationWidth();
                	if (utilityRandomizationSigma != 0.0) {
                        Double additiveRandomization = lastModes2Randomization.get(mode);
                        if (additiveRandomization == null) {
                            /**
                             * logNormal distribution in {@link org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory}
                             */
//                            double normalization = 1. / Math.exp(utilityRandomizationSigma * utilityRandomizationSigma / 2);
//                            lastModes2Randomization.put(mode, Math.exp( utilityRandomizationSigma * random.nextGaussian() ) * normalization);
//                            Does log normal distribution really make sense for a term we add (instead of multiply)?
//                            Maybe rather use log normal distribution to multiply with the estimated travel time?
//                            (fare and distance seem more predictable, but travel time fluctuates)-gl mar'20
                            additiveRandomization = (random.nextDouble() - 0.5) * utilityRandomizationSigma;
                            lastModes2Randomization.put(mode, additiveRandomization);
                        }
//                        System.err.println(person.getId().toString() + ";" + direction.toString() + ";" + additiveRandomization);
//                        utility *= modeRandom; // analogue beta factor (taste variations)

                        utility += additiveRandomization;
//                        positive utility for a leg is hard to interpret and inh theory should not happen, but it can happen with high intermodal compensations. So do not exclude it.
//                        if (utility > 0) {
//                            utility = 0;
//                        }
                    }
                }
            }
        }
        return new RIntermodalAccessEgress(legs, -utility, tTime, direction);
    }
}
