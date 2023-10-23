package org.matsim.run.prepare.network;

import org.matsim.application.prepare.network.opt.FeatureRegressor;
import org.matsim.application.prepare.network.opt.NetworkModel;

public class LeipzigNetworkParams implements NetworkModel {
	@Override
	public FeatureRegressor capacity(String junctionType) {
		return switch (junctionType) {
			case "traffic_light" -> LeipzigNetworkParams_capacity_traffic_light.INSTANCE;
			case "right_before_left" -> LeipzigNetworkParams_capacity_right_before_left.INSTANCE;
			case "priority" -> LeipzigNetworkParams_capacity_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}

	@Override
	public FeatureRegressor speedFactor(String junctionType) {
		return switch (junctionType) {
			case "traffic_light" -> LeipzigNetworkParams_speedRelative_traffic_light.INSTANCE;
			case "right_before_left" -> LeipzigNetworkParams_speedRelative_right_before_left.INSTANCE;
			case "priority" -> LeipzigNetworkParams_speedRelative_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}
}
