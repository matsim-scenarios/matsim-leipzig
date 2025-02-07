package org.matsim.run;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
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
 * Standard subtour mode choice, but replaced the re-routing.
 */
public class LeipzigSubtourModeChoice implements Provider<PlanStrategy> {

	public static final String STRATEGY_NAME = "SubTourModeChoiceLeipzig";

	@Inject
	private Provider<TripRouter> tripRouterProvider;
	@Inject
	private GlobalConfigGroup globalConfigGroup;
	@Inject
	private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	@Inject
	private ActivityFacilities facilities;
	@Inject
	private PermissibleModesCalculator permissibleModesCalculator;
	@Inject
	private TimeInterpretation timeInterpretation;
	@Inject
	private SingleModeNetworksCache singleModeNetworksCache;
	@Inject
	private Scenario scenario;
	@Inject
	private MultimodalLinkChooser linkChooser;

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
		builder.addStrategyModule(new org.matsim.core.replanning.modules.SubtourModeChoice(globalConfigGroup, subtourModeChoiceConfigGroup, permissibleModesCalculator));

		// Re-routing
		builder.addStrategyModule(new AbstractMultithreadedModule(globalConfigGroup) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return new LeipzigRouterPlanAlgorithm(tripRouterProvider.get(), facilities, timeInterpretation, singleModeNetworksCache, scenario, linkChooser);
			}
		});

		return builder.build();
	}

}
