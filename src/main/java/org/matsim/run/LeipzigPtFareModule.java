package org.matsim.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import playground.vsp.pt.fare.DistanceBasedPtFareHandler;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtFareUpperBoundHandler;

/**
 * Provides pt fare calculation for leipzig.
 */
public class LeipzigPtFareModule extends AbstractModule {

	final double minFare = 2.0;
	final int longDistanceThreshold = 50000;
	final double normalTripSlope = 0.00017987993018495408;
	final double longTripSlope = 0.000;
	final double normalBaseFare = 2.4710702921120262;
	final double longBaseFare = 18.90;

	@Override
	public void install() {
		// Set the money related thing in the config (planCalcScore) file to 0.
		getConfig().planCalcScore().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0);
		getConfig().planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(0);

		// Initialize config group (and also write in the output config)
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(this.getConfig(), PtFareConfigGroup.class);
		DistanceBasedPtFareParams distanceBasedPtFareParams = ConfigUtils.addOrGetModule(this.getConfig(), DistanceBasedPtFareParams.class);

		// Set parameters
		ptFareConfigGroup.setApplyUpperBound(true);
		ptFareConfigGroup.setUpperBoundFactor(1.5);

		// https://www.l.de/verkehrsbetriebe/produkte/tarife-auf-einen-blick/
		// https://www.mdv.de/site/uploads/tarifzonenplan.pdf

		// Minimum fare (e.g. short trip or 1 zone ticket)
		distanceBasedPtFareParams.setMinFare(minFare);
		// Division between long trip and short trip (unit: m)
		distanceBasedPtFareParams.setLongDistanceTripThreshold(longDistanceThreshold);

		// y = ax + b --> a value, for short trips
		distanceBasedPtFareParams.setNormalTripSlope(normalTripSlope);
		// y = ax + b --> b value, for short trips
		distanceBasedPtFareParams.setNormalTripIntercept(normalBaseFare);

		// Base price is the daily ticket for long trips
		// y = ax + b --> a value, for long trips
		distanceBasedPtFareParams.setLongDistanceTripSlope(longTripSlope);
		// y = ax + b --> b value, for long trips
		distanceBasedPtFareParams.setLongDistanceTripIntercept(longBaseFare);


		// Add bindings
		addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(distanceBasedPtFareParams));
		if (ptFareConfigGroup.getApplyUpperBound()) {
			PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(ptFareConfigGroup.getUpperBoundFactor());
			addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
			addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
		}
	}

	/**
	 * base fare for pt ride.
	 */
	public Double getNormalPtBaseFare() {
		return normalBaseFare;
	}

	/**
	 * distance based fare for pt ride.
	 */
	public Double getNormalDistanceBasedFare() {
		return normalTripSlope;
	}
}
