package org.matsim.run.policies.autofrei;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.MultimodalLinkChooserDefaultImpl;

import java.util.Set;

import static org.matsim.run.policies.autofrei.RunAutofreiPolicy.NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC;

/**
 * This class is an extended version of the base case ctd. It adds:
 * - a new mode for small-scale commercial and goods traffic, which is allowed on all links.
 * - use custom multimodal link chooser for better bike routing.
 *
 */
public class RunAutofreiBaseCaseCtdExtended extends RunAutofreiBaseCaseCtd {
	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiBaseCaseCtdExtended.class, args);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		Network network = scenario.getNetwork();

		// add new mode to links
		AutofreiUtils.addNewModeBasedOnCarToLinks(network, NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC);

		// add new vehicle type
		AutofreiUtils.addNewVehicleTypeBasedOnCar(scenario, NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC);

		// change network mode of commercial traffic vehicles
		AutofreiUtils.changeNetworkModeOfCommercialVehicleTypes(scenario, NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC, Set.of("golf1.4", "vwCaddy", "mercedes313"));

		// change the mode in the plans for commercial traffic to the new mode
		AutofreiUtils.replaceCarTripsByNewMode(scenario, NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC, Set.of("commercialPersonTraffic", "commercialPersonTraffic_service", "goodsTraffic"));

		// add hbefa information for emissions dashboard
		AutofreiUtils.addHbefaCategories(scenario);
	}

	@Override
	protected Config prepareConfig(Config config) {
		Config newConfig = super.prepareConfig(config);

		// add new network mode for small-scale commercial traffic. Currently, this as network mode car, but this mode is allowed on all links.
		// for easier implementation of the autofrei policy, we introduce a new mode for small-scale commercial traffic.
		AutofreiUtils.addNewModeBasedOnCarToLinks(newConfig, NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC);

		return newConfig;
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MultimodalLinkChooserDefaultImpl.class);
				bind(MultimodalLinkChooser.class).to(MyMultimodalLinkChooser.class);
			}
		});
	}
}
