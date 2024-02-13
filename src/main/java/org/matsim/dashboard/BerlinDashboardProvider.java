package org.matsim.dashboard;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.TrafficCountsDashboard;
import org.matsim.simwrapper.dashboard.TravelTimeComparisonDashboard;
import org.matsim.simwrapper.dashboard.TripDashboard;

import java.util.List;
import java.util.Set;

/**
 * Provider for default dashboards in the scenario.
 */
public class BerlinDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		TripDashboard trips = new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv");
		trips.setAnalysisArgs("--match-id", "^berlin.+", "--shp-filter", "none");

		return List.of(
			trips,
			new TravelTimeComparisonDashboard(ApplicationUtils.resolve(config.getContext(), "berlin-v" + OpenBerlinScenario.VERSION + "-routes-ref.csv.gz")),
			new TrafficCountsDashboard()
				.withModes(TransportMode.car, Set.of(TransportMode.car))
				.withModes(TransportMode.truck, Set.of(TransportMode.truck, "freight"))
		);
	}

}
