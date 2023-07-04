package org.matsim.run;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

/**
 * This class installs the specific routing algorithm, which is implemented in the Leipzig scenario.
 * The algorithm includes a logic for parking vehicles in a specific area.
 */
public class LeipzigRoutingStrategyProvider implements Provider<PlanStrategy> {
	public static final String STRATEGY_NAME = "ReRouteLeipzig";
	// is a provider in matsim core.  maybe try without.  kai, apr'23
	@Inject
	private GlobalConfigGroup globalConfigGroup;
	@Inject
	private ActivityFacilities facilities;
	@Inject
	private Provider<TripRouter> tripRouterProvider;
	@Inject
	private SingleModeNetworksCache singleModeNetworksCache;
	@Inject
	private Scenario scenario;
	@Inject
	private TimeInterpretation timeInterpretation;
	@Inject
	private MultimodalLinkChooser linkChooser;

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
		builder.addStrategyModule(new AbstractMultithreadedModule(globalConfigGroup) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return new LeipzigRouterPlanAlgorithm(tripRouterProvider.get(), facilities, timeInterpretation, singleModeNetworksCache, scenario, linkChooser);
			}
		});
		return builder.build();
	}
}
