package org.matsim.run.policies;

import org.matsim.application.MATSimApplication;
import org.matsim.run.LeipzigScenario;

/**
 * Main class to run the Leipzig speed reduction scenario.
 */
public final class RunSpeedReduction {

	private RunSpeedReduction() {
	}

	public static void main(String[] args) {
		String[] argsForPolicy = new String[]{
				"--slow-speed-area", "/net/ils/matsim-leipzig/input/shp/leipzig_stadt/Leipzig_stadt.shp",
				"--slow-speed-relative-change", "0.6",
				"--config", "/net/ils/matsim-leipzig/input/v1.3/leipzig-v1.3.1-10pct.config.xml",
				"--parking-cost-area", "/net/ils/matsim-leipzig/input/v1.3/parkingCostArea/Bewohnerparken_2020.shp"
		};
		MATSimApplication.runWithDefaults(LeipzigScenario.class, argsForPolicy);
	}
}
