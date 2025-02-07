package org.matsim.project.scoring;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.project.plans.EditPlans;

public final class LateArrivalPenaltyScoring implements SumScoringFunction.ArbitraryEventScoring {

	private double score;

	@Override
	public void handleEvent(Event event) {
		if (event instanceof ActivityStartEvent) {
			if(((ActivityStartEvent) event).getActType().equals("stadium")){
				if(event.getTime() > EditPlans.getStartTime()){

					//minus 0.1 score for every second late
					score -= (event.getTime() - EditPlans.getStartTime()) / 10;

				}
			}
		}
	}

	@Override public void finish() {}

	@Override
	public double getScore() {
		return score;
	}
}
