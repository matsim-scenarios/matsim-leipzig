package org.matsim.run.policies;

import org.matsim.application.MATSimApplication;
import org.matsim.run.LeipzigScenario;

/**
 * Main class to run the Leipzig car free medium scenario.
 */
public final class RunCarFreeMedium {

	private RunCarFreeMedium() {
	}

	public static void main(String[] args) {
		String[] argsForPolicy = new String[]{
				"--car-free-area", "/net/ils/matsim-leipzig/input/shp/leipzig_carfree_area_medium/Zonen95_update.shp",
				"--config", "/net/ils/matsim-leipzig/input/v1.3/leipzig-v1.3.1-10pct.config.xml",
				"--parking-cost-area", "/net/ils/matsim-leipzig/input/v1.3/parkingCostArea/Bewohnerparken_2020.shp"
		};
		MATSimApplication.runWithDefaults(LeipzigScenario.class, argsForPolicy);
	}
}
