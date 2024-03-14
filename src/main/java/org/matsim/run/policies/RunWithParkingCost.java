package org.matsim.run.policies;

import org.matsim.application.MATSimApplication;
import org.matsim.run.LeipzigScenario;

/**
 * Main class to run the Leipzig with parking cost scenario.
 */
public final class RunWithParkingCost {

	private RunWithParkingCost() {
	}

	public static void main(String[] args) {
		String[] argsForPolicy = new String[]{
				"--config", "/net/ils/matsim-leipzig/input/v1.3/leipzig-v1.3.1-10pct.config.xml",
				"--parking",
				"--parking-cost-area", "/net/ils/matsim-leipzig/input/v1.3/parkingCostArea/Bewohnerparken_2020.shp"
		};
		MATSimApplication.runWithDefaults(LeipzigScenario.class, argsForPolicy);
	}

}
