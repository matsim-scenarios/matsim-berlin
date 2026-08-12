package org.matsim.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import static org.matsim.run.policies.MobilityToGridScenariosUtils.RICH;
import static org.matsim.run.policies.OpenBerlinAdditionalCarModeScenario.CAR_EXPENSIVE;

/**
 * Algorithm for differentiation between agents which may use carExpensive only vs agents which may choose between carExpensive and "normal" car.
 */
public final class BerlinCarAlternativePlanAlgorithm implements PlanAlgorithm {

	private final SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	private final MainModeIdentifier mainModeIdentifier;

	@Inject
	BerlinCarAlternativePlanAlgorithm() {
		this.subtourModeChoiceConfigGroup = null;
		this.mainModeIdentifier = null;
	}

	@Override
	public void run(Plan plan) {
//		TODO: change this to SMC without choice to switch to normal car
//		we need a 2nd cfo group for SMC or just a different set of modes.
//		can we give SMC different modes without the cfg group?
//		if not, we need to copy SMC cfg group into berlin and implement another
//		for all other agents normal mode choice set
		if (plan.getPerson().getAttributes().getAttribute("richness") != null &&
		plan.getPerson().getAttributes().getAttribute("richness").equals(RICH)) {
//			we manually change car trips of rich agents to carExpensive, as the network for car = network for carExpensive
//			so no need to reroute the trips
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
				for (Leg leg : trip.getLegsOnly()) {
					if (leg.getMode().equals(TransportMode.car)) {
						leg.setMode(CAR_EXPENSIVE);
					}

					if (leg.getRoutingMode().equals(TransportMode.car)) {
						leg.setRoutingMode(CAR_EXPENSIVE);
					}
				}
			}
		}
	}
}
