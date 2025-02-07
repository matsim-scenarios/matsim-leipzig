package org.matsim.run;


import org.matsim.application.MATSimApplication;


/**
 * Main class to run the Leipzig scenario.
 */
public final class RunLeipzigScenario{

	private RunLeipzigScenario() {
	}

	public static void main(String[] args) {

		MATSimApplication.runWithDefaults(LeipzigScenario.class, args);

	}
}
