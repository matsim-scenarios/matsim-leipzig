package org.matsim.dashboard;

import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.run.LeipzigScenario;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.TravelTimeComparisonDashboard;
import org.matsim.simwrapper.dashboard.TripDashboard;

import java.util.List;

/**
 * Provider for default dashboards in the scenario.
 * Declared in META-INF/services
 */
public class LeipzigDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {

		TripDashboard trips = new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv");

		return List.of(trips,
			new TravelTimeComparisonDashboard(ApplicationUtils.resolve(config.getContext(), "leipzig-v" + LeipzigScenario.VERSION + "-routes-ref.csv.gz"))
		);
	}

}
