/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.run.scoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.PtConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a copy of {@link org.matsim.core.scoring.functions.CharyparNagelLegScoring}.
 * Distance utilities are scored with different linear functions per distance group.
 */
@SuppressWarnings("checkstyle")
public final class PiecewiseLinearlLegScoring implements org.matsim.core.scoring.SumScoringFunction.LegScoring, org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring {
	// yyyy URL in above javadoc is broken.  kai, feb'17

	private static final Logger log = LogManager.getLogger(PiecewiseLinearlLegScoring.class);
	private static int ccc = 0;
	/**
	 * The parameters used for scoring.
	 */
	private final ScoringParameters params;
	private final Set<String> ptModes;
	private final double marginalUtilityOfMoney;
	private final Set<String> modesAlreadyConsideredForDailyConstants;
	private double score;
	private Network network;
	private boolean nextEnterVehicleIsFirstOfTrip = true;
	private boolean nextStartPtLegIsFirstOfTrip = true;
	private boolean currentLegIsPtLeg = false;
	private double lastActivityEndTime = Double.NaN;

	public PiecewiseLinearlLegScoring(final ScoringParameters params, Network network, Set<String> ptModes) {
		this.params = params;
		this.network = network;
		this.ptModes = ptModes;
		this.modesAlreadyConsideredForDailyConstants = new HashSet<>();
		this.marginalUtilityOfMoney = this.params.marginalUtilityOfMoney;
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

	/**
	 * Calculate the score for a leg.
	 */
	private double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		// travel time in seconds
		double travelTime = arrivalTime - departureTime;
		ModeUtilityParameters modeParams = this.params.modeParams.get(leg.getMode());

		if (modeParams == null) {
			if (leg.getMode().equals(TransportMode.transit_walk) || leg.getMode().equals(TransportMode.non_network_walk)) {
				modeParams = this.params.modeParams.get(TransportMode.walk);
			} else {
//				modeParams = this.params.modeParams.get(TransportMode.other);
				throw new RuntimeException("just encountered mode for which no scoring parameters are defined: " + leg.getMode());
			}
		}

		tmpScore += travelTime * modeParams.marginalUtilityOfTraveling_s;

		if (modeParams instanceof DistanceGroupModeUtilityParameters distParams) {

			if (modeParams.monetaryDistanceCostRate != 0.0) {
				Route route = leg.getRoute();
				// distance in meters
				double dist = route.getDistance();
				if (Double.isNaN(dist)) {
					if (ccc < 10) {
						ccc++;
						LogManager.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
							"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
							"write distance into plan.  Needs to be fixed or these plans will die out.");
						if (ccc == 10) {
							LogManager.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
				}
				tmpScore += modeParams.monetaryDistanceCostRate * this.marginalUtilityOfMoney * dist;
			}

			Route route = leg.getRoute();
			double dist = route.getDistance();

			// Apply the default distance scoring parameter.
			if (modeParams.marginalUtilityOfDistance_m != 0) {
				tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
			}

			tmpScore += distParams.calcUtilityDistDelta(dist);

		} else {

			// standard leg scoring
			if (modeParams.marginalUtilityOfDistance_m != 0.0
				|| modeParams.monetaryDistanceCostRate != 0.0) {
				Route route = leg.getRoute();
				// distance in meters
				double dist = route.getDistance();
				if (Double.isNaN(dist)) {
					if (ccc < 10) {
						ccc++;
						LogManager.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
							"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
							"write distance into plan.  Needs to be fixed or these plans will die out.");
						if (ccc == 10) {
							LogManager.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
				}
				tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
				tmpScore += modeParams.monetaryDistanceCostRate * this.marginalUtilityOfMoney * dist;
			}
		}

		tmpScore += modeParams.constant;
		// (yyyy once we have multiple legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		// (yy NOTE: the constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)

		// account for the daily constants
		if (!modesAlreadyConsideredForDailyConstants.contains(leg.getMode())) {
			tmpScore += modeParams.dailyUtilityConstant + modeParams.dailyMoneyConstant * this.marginalUtilityOfMoney;
			modesAlreadyConsideredForDailyConstants.add(leg.getMode());
		}
		// yyyy the above will cause problems if we ever decide to differentiate pt mode into bus, tram, train, ...
		// Might have to move the MainModeIdentifier then.  kai, sep'18

		return tmpScore;
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof ActivityEndEvent) {
			// When there is a "real" activity, flags are reset:
			if (!PtConstants.TRANSIT_ACTIVITY_TYPE.equals(((ActivityEndEvent) event).getActType())) {
				this.nextEnterVehicleIsFirstOfTrip = true;
				this.nextStartPtLegIsFirstOfTrip = true;
			}
			this.lastActivityEndTime = event.getTime();
		}

		if (event instanceof PersonEntersVehicleEvent && currentLegIsPtLeg) {
			if (!this.nextEnterVehicleIsFirstOfTrip) {
				// all vehicle entering after the first triggers the disutility of line switch:
				this.score += params.utilityOfLineSwitch;
			}
			this.nextEnterVehicleIsFirstOfTrip = false;
			// add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
			this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaitingPt_s - this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s);
		}

		if (event instanceof PersonDepartureEvent) {
			String mode = ((PersonDepartureEvent) event).getLegMode();

			this.currentLegIsPtLeg = this.ptModes.contains(mode);
			if (currentLegIsPtLeg) {
				if (!this.nextStartPtLegIsFirstOfTrip) {
					this.score -= params.modeParams.get(mode).constant;
					// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
					// modification is minimally invasive.  kai, dec'12)
				}
				this.nextStartPtLegIsFirstOfTrip = false;
			}
		}
	}

	@Override
	public void handleLeg(Leg leg) {
		Gbl.assertIf(leg.getDepartureTime().isDefined());
		Gbl.assertIf(leg.getTravelTime().isDefined());

		double legScore = calcLegScore(
			leg.getDepartureTime().seconds(), leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), leg);
		if (Double.isNaN(legScore)) {
			log.error("dpTime=" + leg.getDepartureTime().seconds()
				+ "; ttime=" + leg.getTravelTime().seconds() + "; leg=" + leg);
			throw new RuntimeException("score is NaN");
		}
		this.score += legScore;
	}
}
