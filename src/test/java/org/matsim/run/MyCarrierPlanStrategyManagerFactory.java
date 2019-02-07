package org.matsim.run;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.core.replanning.GenericStrategyManager;

public class MyCarrierPlanStrategyManagerFactory implements CarrierPlanStrategyManagerFactory {

	@Override
	public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
		return null;
	}

}