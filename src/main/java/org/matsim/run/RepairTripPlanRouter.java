/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.vehicles.Vehicle;

/**
 * {@link PlanAlgorithm} responsible for routing only those trips which don't have a route in at least one leg.
 * Activity times are not updated, even if the previous trip arrival time
 * is after the activity end time.
 *
 * @author thibautd, ikaddoura
 */
public class RepairTripPlanRouter implements PlanAlgorithm, PersonAlgorithm {
	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;

	/**
	 * Initialises an instance.
	 * @param tripRouter the {@link TripRouter} to use to route individual trips
	 * @param facilities the {@link ActivityFacilities} to which activities are refering.
	 * May be <tt>null</tt>: in this case, the router will be given facilities wrapping the
	 * origin and destination activity.
	 */
	public RepairTripPlanRouter(
			final TripRouter tripRouter,
			final ActivityFacilities facilities) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
	}

	/**
	 * Short for initialising without facilities.
	 */
	public RepairTripPlanRouter(
			final TripRouter routingHandler) {
		this( routingHandler, null);
	}

	@Override
	public void run(final Plan plan) {
		final List<Trip> trips = TripStructureUtils.getTrips( plan );

		for (Trip oldTrip : trips) {
			boolean legWithInvalidRoute = false;
			
			for (Leg leg : oldTrip.getLegsOnly()) {
				if (leg.getRoute() == null) {
					legWithInvalidRoute = true;
					break;
				}
			}
			
			if (legWithInvalidRoute) {
				
				// emulate a routing mode ROUTING_MODE_PT_WITH_DRT_ENABLED_FOR_ACCESS_EGRESS = "pt_w_drt" using person attributes
				String originalRoutingMode = TripStructureUtils.identifyMainMode( oldTrip.getTripElements() );
				String routingMode;
				if (originalRoutingMode.equals(RunDrtOpenBerlinScenario.ROUTING_MODE_PT_WITH_DRT_ENABLED_FOR_ACCESS_EGRESS)) {
					plan.getPerson().getAttributes().putAttribute(
							RunDrtOpenBerlinScenario.DRT_ACCESS_EGRESS_TO_PT_PERSON_FILTER_ATTRIBUTE, 
							RunDrtOpenBerlinScenario.DRT_ACCESS_EGRESS_TO_PT_PERSON_FILTER_VALUE);
					routingMode = TransportMode.pt;
				} else {
					routingMode = originalRoutingMode;
				}
				
				final List<? extends PlanElement> newTrip =
						tripRouter.calcRoute(
								routingMode,
							  FacilitiesUtils.toFacility( oldTrip.getOriginActivity(), facilities ),
							  FacilitiesUtils.toFacility( oldTrip.getDestinationActivity(), facilities ),
								calcEndOfActivity( oldTrip.getOriginActivity() , plan, tripRouter.getConfig() ),
								plan.getPerson() );
				
				if (originalRoutingMode.equals(RunDrtOpenBerlinScenario.ROUTING_MODE_PT_WITH_DRT_ENABLED_FOR_ACCESS_EGRESS)) {
					plan.getPerson().getAttributes().removeAttribute(RunDrtOpenBerlinScenario.DRT_ACCESS_EGRESS_TO_PT_PERSON_FILTER_ATTRIBUTE);
					for (PlanElement pe: newTrip) {
						if (pe instanceof Leg) {
							TripStructureUtils.setRoutingMode((Leg) pe, originalRoutingMode);
						}
					}
				}
				
				putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTrip);
				TripRouter.insertTrip(
						plan, 
						oldTrip.getOriginActivity(),
						newTrip,
						oldTrip.getDestinationActivity());
			}
			
		}
	}

	/**
	 * If the old trip had vehicles set in its network routes, and it used a single vehicle,
	 * and if the new trip does not come with vehicles set in its network routes,
	 * then put the vehicle of the old trip into the network routes of the new trip.
	 * @param oldTrip The old trip
	 * @param newTrip The new trip
	 */
	private static void putVehicleFromOldTripIntoNewTripIfMeaningful(Trip oldTrip, List<? extends PlanElement> newTrip) {
		Id<Vehicle> oldVehicleId = getUniqueVehicleId(oldTrip);
		if (oldVehicleId != null) {
			for (Leg leg : TripStructureUtils.getLegs(newTrip)) {
				if (leg.getRoute() instanceof NetworkRoute) {
					if (((NetworkRoute) leg.getRoute()).getVehicleId() == null) {
						((NetworkRoute) leg.getRoute()).setVehicleId(oldVehicleId);
					}
				}
			}
		}
	}

	private static Id<Vehicle> getUniqueVehicleId(Trip trip) {
		Id<Vehicle> vehicleId = null;
		for (Leg leg : trip.getLegsOnly()) {
			if (leg.getRoute() instanceof NetworkRoute) {
				if (vehicleId != null && (!vehicleId.equals(((NetworkRoute) leg.getRoute()).getVehicleId()))) {
					return null; // The trip uses several vehicles.
				}
				vehicleId = ((NetworkRoute) leg.getRoute()).getVehicleId();
			}
		}
		return vehicleId;
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run( plan );
		}
	}

	public static double calcEndOfActivity(
			final Activity activity,
			final Plan plan,
			final Config config ) {
		// yyyy similar method in PopulationUtils.  TripRouter.calcEndOfPlanElement in fact uses it.  However, this seems doubly inefficient; calling the
		// method in PopulationUtils directly would probably be faster.  kai, jul'19

		if (!Time.isUndefinedTime(activity.getEndTime())) return activity.getEndTime();

		// no sufficient information in the activity...
		// do it the long way.
		// XXX This is inefficient! Using a cache for each plan may be an option
		// (knowing that plan elements are iterated in proper sequence,
		// no need to re-examine the parts of the plan already known)
		double now = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			now = TripRouter.calcEndOfPlanElement( now, pe, config );
			if (pe == activity) return now;
		}

		throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
	}

}

