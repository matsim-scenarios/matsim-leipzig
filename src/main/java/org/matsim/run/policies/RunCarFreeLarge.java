package org.matsim.run.policies;

import org.matsim.application.MATSimApplication;
import org.matsim.run.LeipzigScenario;

public class RunCarFreeLarge {

		private RunCarFreeLarge() {
		}

		public static void main(String[] args) {
			String[] argsForPolicy = new String[]{
					"--car-free-area", "net/ils/matsim-leipzig/input/shp/leipzig_carfree_area_large"
			};
			MATSimApplication.runWithDefaults(LeipzigScenario.class, argsForPolicy);
		}

}
