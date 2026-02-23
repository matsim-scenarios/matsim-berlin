package org.matsim.run.policies.autofrei;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;

import java.util.Set;

public class RunAutofreiPolicy extends RunAutofreiBaseCaseCtdExtended {
	private static final Set<String> RESTRICTED_MODES = Set.of(TransportMode.car, TransportMode.ride);
	public static final String NEW_MODE_SMALL_SCALE_COMMERCIAL_AND_GOODS_TRAFFIC = "commercial_goods_car";

	public static void main(String[] args) {
		MATSimApplication.run(RunAutofreiPolicy.class, args);
	}

//	private static void readWriteNetwork() {
//		Network network = NetworkUtils.readNetwork(ConfigUtils.loadConfig("input/v6.4/berlin-v6.4.config.xml").network().getInputFile());
//		AutofreiUtils.restrictHighwayLinks(network, RESTRICTED_MODES);
//		NetworkUtils.writeNetwork(network, "berlin-v6.4.network-restricted.xml.gz");
//	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		Network network = scenario.getNetwork();
		AutofreiUtils.restrictInnerRingLinks(network, RESTRICTED_MODES);
	}
}
