package org.matsim.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.PersonPrepareForSimAlgorithm;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.core.router.PlanRouter.putVehicleFromOldTripIntoNewTripIfMeaningful;
import static org.matsim.run.LeipzigUtils.isLinkParkingTypeInsideResidentialArea;

final class LeipzigRouterPlanAlgorithm implements PlanAlgorithm, PersonPrepareForSimAlgorithm {
	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;
	private final TimeInterpretation timeInterpretation;
	private final Network fullModalNetwork;
	private final Network reducedNetwork;
	private final MultimodalLinkChooser linkChooser;
	private final Scenario scenario;

	private final XY2Links xy2Links;

	@Inject
	LeipzigRouterPlanAlgorithm(final TripRouter tripRouter, final ActivityFacilities facilities, final TimeInterpretation timeInterpretation,
							   SingleModeNetworksCache singleModeNetworksCache, Scenario scenario, MultimodalLinkChooser linkChooser) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
		this.timeInterpretation = timeInterpretation;
		this.scenario = scenario;
		this.fullModalNetwork = singleModeNetworksCache.getSingleModeNetworksCache().get(TransportMode.car);

		// yyyy one should look at the networks cache and see how the following is done.  And maybe even register it there.
		this.reducedNetwork = NetworkUtils.createNetwork(scenario.getConfig().network());
		this.linkChooser = linkChooser;
		for (Node node : this.fullModalNetwork.getNodes().values()) {
			reducedNetwork.addNode(node);
		}
		for (Link link : this.fullModalNetwork.getLinks().values()) {
			if (!LeipzigUtils.isLinkParkingTypeInsideResidentialArea(link)) {
				reducedNetwork.addLink(link);
			}
		}

		xy2Links = new XY2Links(fullModalNetwork, scenario.getActivityFacilities());
	}

	private static LeipzigUtils.PersonParkingBehaviour getParkingBehaviour(Network fullModalNetwork, Activity originActivity, String routingMode) {
		LeipzigUtils.PersonParkingBehaviour parkingBehaviourAtOrigin = LeipzigUtils.PersonParkingBehaviour.defaultLogic;

		// if we find out that there are time restrictions on all the links
		if (routingMode.equals(TransportMode.car)) {

			// an dieser stelle waere es besser abzufragen, ob die person in der naehe wohnt anstatt nur die home act -> residential parking zuzuordnen
			// check if non-home activity (since otherwise we assume that there is no parking restriction):
			if (!originActivity.getType().equals(ActivityTypes.HOME)) {

				Link link = fullModalNetwork.getLinks().get(originActivity.getLinkId());
				if (isLinkParkingTypeInsideResidentialArea(link)) {
					parkingBehaviourAtOrigin = LeipzigUtils.PersonParkingBehaviour.parkingSearchLogicLeipzig;
				}
			}
		}
		return parkingBehaviourAtOrigin;
	}

	@Override
	public void run(final Plan plan) {
		final List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);


		for (TripStructureUtils.Trip oldTrip : trips) {
			final String routingMode = TripStructureUtils.identifyMainMode(oldTrip.getTripElements());
			timeTracker.addActivity(oldTrip.getOriginActivity());

			final Facility fromFacility = FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities);
			final Facility toFacility = FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities);


			// At this point, I only want to deal with residential parking.  Shopping comes later (and is simpler).

			LeipzigUtils.PersonParkingBehaviour parkingBehaviourAtOrigin = getParkingBehaviour(fullModalNetwork, oldTrip.getOriginActivity(), routingMode);
			LeipzigUtils.PersonParkingBehaviour parkingBehaviourAtDestination = getParkingBehaviour(fullModalNetwork, oldTrip.getDestinationActivity(), routingMode);

			if (parkingBehaviourAtOrigin == LeipzigUtils.PersonParkingBehaviour.defaultLogic
				&& parkingBehaviourAtDestination == LeipzigUtils.PersonParkingBehaviour.defaultLogic) {
				// standard case:
				final List<? extends PlanElement> newTripElements = tripRouter.calcRoute(routingMode, fromFacility, toFacility,
					timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
				putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTripElements);
				TripRouter.insertTrip(plan, oldTrip.getOriginActivity(), newTripElements, oldTrip.getDestinationActivity());
				timeTracker.addElements(newTripElements);

			} else if (parkingBehaviourAtOrigin == LeipzigUtils.PersonParkingBehaviour.parkingSearchLogicLeipzig
				&& parkingBehaviourAtDestination == LeipzigUtils.PersonParkingBehaviour.defaultLogic) {

				TripRouter.insertTrip(plan, oldTrip.getOriginActivity(),
					createTripForParkingAtOrigin(oldTrip, routingMode, fromFacility, toFacility, plan, timeTracker), oldTrip.getDestinationActivity());

			} else if (parkingBehaviourAtOrigin == LeipzigUtils.PersonParkingBehaviour.defaultLogic
				&& parkingBehaviourAtDestination == LeipzigUtils.PersonParkingBehaviour.parkingSearchLogicLeipzig) {

				TripRouter.insertTrip(plan, oldTrip.getOriginActivity(),
					createTripForParkingAtDestination(oldTrip, routingMode, fromFacility, toFacility, plan, timeTracker), oldTrip.getDestinationActivity());


			} else if (parkingBehaviourAtOrigin == LeipzigUtils.PersonParkingBehaviour.parkingSearchLogicLeipzig
				&& parkingBehaviourAtDestination == LeipzigUtils.PersonParkingBehaviour.parkingSearchLogicLeipzig) {

				// restricted parking at origin:
				// first find parking:
				final Link originParkingLink = linkChooser.decideOnLink(fromFacility, reducedNetwork);

				final Activity originParkingActivity = scenario.getPopulation().getFactory().createInteractionActivityFromLinkId(
					TripStructureUtils.createStageActivityType("parking"), originParkingLink.getId());
				final Facility originParkingFacility = FacilitiesUtils.toFacility(originParkingActivity, facilities);

				//parking at destination
				final Link destinationParkingLink = linkChooser.decideOnLink(toFacility, reducedNetwork);

				final Activity destinationParkingActivity = scenario.getPopulation().getFactory().createInteractionActivityFromLinkId(
					TripStructureUtils.createStageActivityType("parking"), destinationParkingLink.getId());
				final Facility destinationParkingFacility = FacilitiesUtils.toFacility(destinationParkingActivity, facilities);

				// trip from origin to originParking:
				final List<? extends PlanElement> originWalkTripElements = tripRouter.calcRoute(TransportMode.walk, fromFacility, originParkingFacility,
					timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
				for (PlanElement tripElement : originWalkTripElements) {
					if (tripElement instanceof Leg leg) {
						TripStructureUtils.setRoutingMode(leg, TransportMode.car);
					}
				}

				List<PlanElement> newTripElements = new ArrayList<>(originWalkTripElements);
				// originParking interaction:
				newTripElements.add(originParkingActivity);
				// trip from originParking to destinationParking:
				final List<? extends PlanElement> carTripElements = tripRouter.calcRoute(routingMode, originParkingFacility, destinationParkingFacility,
					timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
				newTripElements.addAll(carTripElements);

				newTripElements.add(destinationParkingActivity);

				// trip from destinationParking to destination:
				final List<? extends PlanElement> destinationWalkTripElements = tripRouter.calcRoute(TransportMode.walk, destinationParkingFacility, toFacility,
					timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
				for (PlanElement tripElement : destinationWalkTripElements) {
					if (tripElement instanceof Leg leg) {
						TripStructureUtils.setRoutingMode(leg, TransportMode.car);
					}
				}
				newTripElements.addAll(destinationWalkTripElements);


				putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTripElements);
				timeTracker.addElements(newTripElements);

				TripRouter.insertTrip(plan, oldTrip.getOriginActivity(), newTripElements, oldTrip.getDestinationActivity());

			} else {
				throw new IllegalStateException("Unhandled parking situation");
			}

		}

	}

	@Override
	public void run(Person person) {

		for (Plan plan : person.getPlans()) {
			xy2Links.run(plan);
			run(plan);
		}
	}

	private List<PlanElement> createTripForParkingAtOrigin(TripStructureUtils.Trip oldTrip, String routingMode, Facility fromFacility, Facility toFacility, Plan plan, TimeTracker timeTracker) {

		// restricted parking at origin:
		// first find parking:
		final Link parkingLink = linkChooser.decideOnLink(fromFacility, reducedNetwork);

		final Activity parkingActivity = scenario.getPopulation().getFactory().createInteractionActivityFromLinkId(
			TripStructureUtils.createStageActivityType("parking"), parkingLink.getId());
		final Facility parkingFacility = FacilitiesUtils.toFacility(parkingActivity, facilities);

		// trip from origin to parking:
		final List<? extends PlanElement> walkTripElements = tripRouter.calcRoute(TransportMode.walk, fromFacility, parkingFacility,
			timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
		for (PlanElement tripElement : walkTripElements) {
			if (tripElement instanceof Leg leg) {
				TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			}
		}

		List<PlanElement> newTripElements = new ArrayList<>(walkTripElements);
		// parking interaction:
		newTripElements.add(parkingActivity);
		// trip from parking to final destination:
		final List<? extends PlanElement> carTripElements = tripRouter.calcRoute(routingMode, parkingFacility, toFacility,
			timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
		newTripElements.addAll(carTripElements);

		putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTripElements);
		timeTracker.addElements(newTripElements);

		return newTripElements;
	}

	private List<PlanElement> createTripForParkingAtDestination(TripStructureUtils.Trip oldTrip, String routingMode, Facility fromFacility, Facility toFacility, Plan plan, TimeTracker timeTracker) {

		//parking at destination
		final Link parkingLink = linkChooser.decideOnLink(toFacility, reducedNetwork);
		final Activity parkingActivity = scenario.getPopulation().getFactory().createInteractionActivityFromLinkId(
			TripStructureUtils.createStageActivityType("parking"), parkingLink.getId());
		final Facility parkingFacility = FacilitiesUtils.toFacility(parkingActivity, facilities);

		// trip from origin to parking:
		final List<? extends PlanElement> carTripElements = tripRouter.calcRoute(routingMode, fromFacility, parkingFacility,
			timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
		// parking interaction:

		List<PlanElement> newTripElements = new ArrayList<>(carTripElements);
		newTripElements.add(parkingActivity);

		// trip from parking to destination:
		final List<? extends PlanElement> walkTripElements = tripRouter.calcRoute(TransportMode.walk, parkingFacility, toFacility,
			timeTracker.getTime().seconds(), plan.getPerson(), oldTrip.getTripAttributes());
		for (PlanElement tripElement : walkTripElements) {
			if (tripElement instanceof Leg leg) {
				TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			}
		}
		newTripElements.addAll(walkTripElements);

		putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTripElements);

		timeTracker.addElements(newTripElements);

		return newTripElements;
	}

}
