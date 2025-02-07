package org.matsim.project.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

public class AddScoringFunctionFactories implements ScoringFunctionFactory {
	private final Scenario scenario;

	public AddScoringFunctionFactories(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sumScoringFunction = new SumScoringFunction();

		// Score activities, legs, payments and being stuck
		// with the default MATSim scoring based on utility parameters in the config file.
		final ScoringParameters params = new ScoringParameters.Builder(scenario, person).build();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		//own functions:

		sumScoringFunction.addScoringFunction(new LateArrivalPenaltyScoring());

		//edit for change from baseCase to scenarioCase
		boolean baseCase = false;
		if(!baseCase) {
			sumScoringFunction.addScoringFunction(new CarUsePenaltyScoring());
		}

		return sumScoringFunction;
	}
}
