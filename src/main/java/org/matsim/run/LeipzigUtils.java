package org.matsim.run;

import org.matsim.api.core.v01.network.Link;

import java.util.HashSet;
import java.util.Set;

/**
 * Utils class to adapt scenario-related person / link attributes.
 */
public final class LeipzigUtils{

	private static final String MODE = "car";
	private static final String DAILY_PARKING_COST_LINK_ATTRIBUTE_NAME = "dailyPCost";
	private static final String FIRST_HOUR_PARKING_COST_LINK_ATTRIBUTE_NAME = "oneHourPCost";
	private static final String EXTRA_HOUR_PARKING_COST_LINK_ATTRIBUTE_NAME = "extraHourPCost";
	private static final String MAX_DAILY_PARKING_COST_LINK_ATTRIBUTE_NAME = "maxDailyPCost";
	private static final String MAX_PARKING_DURATION_ATTRIBUTE_NAME = "maxPDuration";
	private static final String PARKING_PENALTY_ATTRIBUTE_NAME = "penalty";
	private static final String RESIDENTIAL_PARKING_FEE_PER_DAY = "residentialPFee";
	private static final String ACTIVITY_PREFIX_FOR_DAILY_PARKING_COSTS = "home";
	private static final Set<String> ACTIVITY_PREFIX_TO_BE_EXCLUDED_FROM_PARKING_COST = new HashSet<>();

	// do not instantiate
	private LeipzigUtils(){}

	/**
	 * Defines the parkingBehaviour of a person.
	 */
	enum PersonParkingBehaviour {defaultLogic, parkingSearchLogicLeipzig}

	/**
	 * Defines the parkingType of a network link.
	 */
	private enum LinkParkingType{linkOutsideResidentialArea, linkInResidentialArea}

	/**
	 * Check if link is inside residential area or not (parking on link is restricted or not).
	 */
	public static boolean isLinkParkingTypeInsideResidentialArea(Link link ) {

		 // If a link is null he is not in the modal network anymore. The default logic is as in the car free area all agents can´t park at the closest link as well
		String result;
		if (link != null) {
			result = (String) link.getAttributes().getAttribute("linkParkingType");
		} else {
			result = null;
		}

		boolean insideResidentialArea = true;

		if (result == null) {
			insideResidentialArea = false;
		}
		return insideResidentialArea;
	}

	static String getMode() {
		return MODE;
	}

	static String getDailyParkingCostLinkAttributeName() {
		return DAILY_PARKING_COST_LINK_ATTRIBUTE_NAME;
	}

	static String getFirstHourParkingCostLinkAttributeName() {
		return FIRST_HOUR_PARKING_COST_LINK_ATTRIBUTE_NAME;
	}

	static String getExtraHourParkingCostLinkAttributeName() {
		return EXTRA_HOUR_PARKING_COST_LINK_ATTRIBUTE_NAME;
	}

	static String getMaxDailyParkingCostLinkAttributeName() {
		return MAX_DAILY_PARKING_COST_LINK_ATTRIBUTE_NAME;
	}

	static String getMaxParkingDurationAttributeName() {
		return MAX_PARKING_DURATION_ATTRIBUTE_NAME;
	}

	static String getParkingPenaltyAttributeName() {
		return PARKING_PENALTY_ATTRIBUTE_NAME;
	}

	static String getResidentialParkingFeeAttributeName() {
		return RESIDENTIAL_PARKING_FEE_PER_DAY;
	}

	static String getActivityPrefixForDailyParkingCosts() {
		return ACTIVITY_PREFIX_FOR_DAILY_PARKING_COSTS;
	}

	static Set<String> getActivityPrefixesToBeExcludedFromParkingCost() {
		return ACTIVITY_PREFIX_TO_BE_EXCLUDED_FROM_PARKING_COST;
	}

	public static void setLinkParkingTypeToInsideResidentialArea(Link link ){
		link.getAttributes().putAttribute( "linkParkingType", LinkParkingType.linkInResidentialArea.toString() );
	}

	public static void setFirstHourParkingCost(Link link, double parkingCost) {
		link.getAttributes().putAttribute(getFirstHourParkingCostLinkAttributeName(), parkingCost);
	}

	public static void setExtraHourParkingCost(Link link, double parkingCost) {
		link.getAttributes().putAttribute(getExtraHourParkingCostLinkAttributeName(), parkingCost);
	}

	public static void setResidentialParkingCost(Link link, double parkingCost) {
		link.getAttributes().putAttribute(getResidentialParkingFeeAttributeName(), parkingCost);
	}

	public static void setParkingCapacity(Link link, double parkingCapacity) {
		link.getAttributes().putAttribute("parkingCapacity", parkingCapacity);
	}



}
