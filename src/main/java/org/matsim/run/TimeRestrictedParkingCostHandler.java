package org.matsim.run;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.StageActivityTypeIdentifier;

import com.google.inject.Inject;

/**
 * Implementation of ParkingCostHandler with an additional check for time restriction when including parking cost into a simulation.
 *
 * @author ikaddoura, simei94
 */
final class TimeRestrictedParkingCostHandler implements TransitDriverStartsEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler {

	private final Map<Id<Person>, Double> personId2lastLeaveVehicleTime = new HashMap<>();
	private final Map<Id<Person>, String> personId2previousActivity = new HashMap<>();
	private final Map<Id<Person>, Id<Link>> personId2relevantModeLinkId = new HashMap<>();
	private final Set<Id<Person>> ptDrivers = new HashSet<>();
	private final Set<Id<Person>> hasAlreadyPaidDailyResidentialParkingCosts = new HashSet<>();
	private boolean isInRestrictedParkingPeriod;
	private final double parkingCostTimePeriodStart;
	private final double parkingCostTimePeriodEnd;

	@Inject
	private EventsManager events;

	@Inject
	private Scenario scenario;

	@Inject
	private QSimConfigGroup qSimConfigGroup;

	TimeRestrictedParkingCostHandler(double parkingCostTimePeriodStart, double parkingCostTimePeriodEnd) {
		this.parkingCostTimePeriodStart = parkingCostTimePeriodStart;
		this.parkingCostTimePeriodEnd = parkingCostTimePeriodEnd;
	}


	@Override
	public void reset(int iteration) {
		this.personId2lastLeaveVehicleTime.clear();
		this.personId2previousActivity.clear();
		this.personId2relevantModeLinkId.clear();
		this.hasAlreadyPaidDailyResidentialParkingCosts.clear();
		this.ptDrivers.clear();
	}

	private boolean checkTimeRestriction(double time) {
		boolean isRelevant = true;

		if (parkingCostTimePeriodStart > 0. && parkingCostTimePeriodEnd > 0.) {
			//start + end of parking period are defined
			if (!(parkingCostTimePeriodStart <= time
					&& time <= parkingCostTimePeriodEnd)) {

				//event time is outside of parking period
				isRelevant = false;
			}
		} else if (parkingCostTimePeriodStart == 0. && parkingCostTimePeriodEnd == 0.) {
			//start + end of parking period are not defined = no time restriction for parking = parking cost is charged at any time -> default!
		} else if (parkingCostTimePeriodStart == 0. && parkingCostTimePeriodEnd > 0.) {
			//start of parking period is not defined, the end is defined
			if (time > parkingCostTimePeriodEnd) {
				//parking period is already over
				isRelevant = false;
			}
		} else if (parkingCostTimePeriodStart > 0. && parkingCostTimePeriodEnd == 0.) {
			//start of parking period is defined, the end is not defined
			if (parkingCostTimePeriodStart > time) {
				//parking period has yet to begin
				isRelevant = false;
			}
		}
		return isRelevant;
	}


	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptDrivers.add(event.getDriverId());
	}


	@Override
	public void handleEvent(ActivityEndEvent event) {

		if (ptDrivers.contains(event.getPersonId())) {
			// skip pt drivers
		} else {
			if (!(StageActivityTypeIdentifier.isStageActivity(event.getActType()))) {

				personId2previousActivity.put(event.getPersonId(), event.getActType());

				if (personId2relevantModeLinkId.get(event.getPersonId()) != null) {
					personId2relevantModeLinkId.remove(event.getPersonId());
				}
			}
		}
	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {

		isInRestrictedParkingPeriod = checkTimeRestriction(event.getTime());

		if (isInRestrictedParkingPeriod) {
			if (! ptDrivers.contains(event.getPersonId()) && event.getLegMode().equals(LeipzigUtils.getMode())) {
				// There might be several departures during a single trip.
				personId2relevantModeLinkId.put(event.getPersonId(), event.getLinkId());
			}
		}
	}


	@Override
	@SuppressWarnings("CyclomaticComplexity")
	public void handleEvent(PersonEntersVehicleEvent event) {

		double amount;

		isInRestrictedParkingPeriod = checkTimeRestriction(event.getTime());

		if (isInRestrictedParkingPeriod && !ptDrivers.contains(event.getPersonId()) && personId2relevantModeLinkId.get(event.getPersonId()) != null) {

			Link link = scenario.getNetwork().getLinks().get(personId2relevantModeLinkId.get(event.getPersonId()));

			if (LeipzigUtils.getActivityPrefixesToBeExcludedFromParkingCost().stream()
					.noneMatch(s -> personId2previousActivity.get(event.getPersonId()).startsWith(s))) {

				if (personId2previousActivity.get(event.getPersonId()).startsWith(LeipzigUtils.getActivityPrefixForDailyParkingCosts())
				&& !hasAlreadyPaidDailyResidentialParkingCosts.contains(event.getPersonId())) {
					// daily residential parking costs

					hasAlreadyPaidDailyResidentialParkingCosts.add(event.getPersonId());

					double residentialParkingFeePerDay = 0.;
					if (link.getAttributes().getAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName()) != null) {
						residentialParkingFeePerDay = (double) link.getAttributes().getAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName());
					}

					if (residentialParkingFeePerDay > 0.) {
						amount = -1. * residentialParkingFeePerDay;
						events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount, "residential parking", "city", "link " + link.getId().toString()));
					}

				} else {
					// other parking cost types

					double parkingStartTime = 0.;
					if (personId2lastLeaveVehicleTime.get(event.getPersonId()) != null) {
						parkingStartTime = personId2lastLeaveVehicleTime.get(event.getPersonId());
					}
					int parkingDurationHrs = (int) Math.ceil((event.getTime() - parkingStartTime) / 3600.);

					double extraHourParkingCosts = 0.;
					if (link.getAttributes().getAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName()) != null) {
						extraHourParkingCosts = (double) link.getAttributes().getAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName());
					}

					double firstHourParkingCosts = 0.;
					if (link.getAttributes().getAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName()) != null) {
						firstHourParkingCosts = (double) link.getAttributes().getAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName());
					}

					double dailyParkingCosts = firstHourParkingCosts + 29 * extraHourParkingCosts;
					if (link.getAttributes().getAttribute(LeipzigUtils.getDailyParkingCostLinkAttributeName()) != null) {
						dailyParkingCosts = (double) link.getAttributes().getAttribute(LeipzigUtils.getDailyParkingCostLinkAttributeName());
					}

					double maxDailyParkingCosts = dailyParkingCosts;
					if (link.getAttributes().getAttribute(LeipzigUtils.getMaxDailyParkingCostLinkAttributeName()) != null) {
						maxDailyParkingCosts = (double) link.getAttributes().getAttribute(LeipzigUtils.getMaxDailyParkingCostLinkAttributeName());
					}

					double maxParkingDurationHrs = 30;
					if (link.getAttributes().getAttribute(LeipzigUtils.getMaxParkingDurationAttributeName()) != null) {
						maxParkingDurationHrs = (double) link.getAttributes().getAttribute(LeipzigUtils.getMaxParkingDurationAttributeName());
					}

					double parkingPenalty = 0.;
					if (link.getAttributes().getAttribute(LeipzigUtils.getParkingPenaltyAttributeName()) != null) {
						parkingPenalty = (double) link.getAttributes().getAttribute(LeipzigUtils.getParkingPenaltyAttributeName());
					}

					double costs = 0.;
					if (parkingDurationHrs > 0) {
						costs += firstHourParkingCosts;
						costs += (parkingDurationHrs - 1) * extraHourParkingCosts;
					}
					if (costs > dailyParkingCosts) {
						costs = dailyParkingCosts;
					}
					if (costs > maxDailyParkingCosts) {
						costs = maxDailyParkingCosts;
					}
					if ((parkingDurationHrs > maxParkingDurationHrs) && (costs < parkingPenalty)) {
						costs = parkingPenalty;
					}

					if (costs > 0.) {
						amount = -1. * costs;
						events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount, "non-residential parking", "city", "link " + link.getId().toString()));
					}
				}
			}
		}
	}


	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {

		isInRestrictedParkingPeriod = checkTimeRestriction(event.getTime());

		if (isInRestrictedParkingPeriod && !ptDrivers.contains(event.getPersonId())) {
			personId2lastLeaveVehicleTime.put(event.getPersonId(), event.getTime());
		}
	}
}
