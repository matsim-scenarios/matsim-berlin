package org.matsim.prepare.network;

import org.matsim.application.prepare.Predictor;
import org.matsim.application.prepare.network.params.NetworkModel;

@SuppressWarnings("unused")
public class BerlinNetworkParams implements NetworkModel {
	@Override
	public Predictor capacity(String junctionType, String highwayType) {
		return switch (junctionType) {
			case "traffic_light" -> BerlinNetworkParams_capacity_traffic_light.INSTANCE;
			case "right_before_left" -> BerlinNetworkParams_capacity_right_before_left.INSTANCE;
			case "priority" -> BerlinNetworkParams_capacity_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}

	@Override
	public Predictor speedFactor(String junctionType, String highwayType) {
		return switch (junctionType) {
			case "traffic_light" -> BerlinNetworkParams_speedRelative_traffic_light.INSTANCE;
			case "right_before_left" -> BerlinNetworkParams_speedRelative_right_before_left.INSTANCE;
			case "priority" -> BerlinNetworkParams_speedRelative_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}
}
