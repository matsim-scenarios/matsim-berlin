package org.matsim.prepare;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.Map;

/**
 * This class attached information to the experienced plans needed for calibration.
 */
class ExtendExperiencedPlansListener implements ScoringListener {

	private final ExperiencedPlansService service;

	@Inject
	ExtendExperiencedPlansListener(ExperiencedPlansService service) {
		this.service = service;
	}

	@Override
	public void notifyScoring(ScoringEvent event) {

		// Run before ExperiencedPlansServiceImpl

		Vehicles vehicles = event.getServices().getScenario().getVehicles();

		for (Map.Entry<Id<Person>, Plan> e : service.getExperiencedPlans().entrySet()) {

			for (Leg leg : TripStructureUtils.getLegs(e.getValue())) {

				Map<String, Object> attr = leg.getAttributes().getAsMap();
				if (attr.containsKey("vehicleId") && attr.get("vehicleId") != null) {

					Id<Vehicle> id = (Id<Vehicle>) attr.get("vehicleId");

					Vehicle veh = vehicles.getVehicles().get(id);

					if (veh == null)
						continue;

					leg.getAttributes().putAttribute("vehicleType", veh.getType().getId().toString());
					leg.getAttributes().putAttribute("networkMode", veh.getType().getNetworkMode());
				}
			}
		}
	}
}
