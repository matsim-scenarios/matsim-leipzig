package org.matsim.project.scoring;


import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.project.utils.PersonAttributeChecker;

import java.util.Objects;


public class CarUsePenaltyScoring implements SumScoringFunction.ArbitraryEventScoring {

	private double score;

	@Override
	public void handleEvent(Event event) {

		if (event instanceof PersonArrivalEvent arrivalEvent) {

			if (Objects.equals(arrivalEvent.getLinkId().toString(), "911246612")) {

				if (arrivalEvent.getLegMode().equals("car")){

					if(!PersonAttributeChecker.hasPersonAttribute(arrivalEvent.getPersonId().toString(), "hasReducedMobility")) {

						if (PersonAttributeChecker.hasPersonAttribute(arrivalEvent.getPersonId().toString(), "goesToStadium")) {

									score -= 100;

						}
					}
				}
			}
		}
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return score;
	}

}
