package org.matsim.dashboard;

import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.run.RunOpenBerlinScenario;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.TrafficCountsDashboard;
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

		// TODO: the freight mode is not separated correctly from car yet
		return List.of(
			trips,
			new TravelTimeComparisonDashboard(ApplicationUtils.resolve(config.getContext(), "berlin-v" + RunOpenBerlinScenario.VERSION + "-routes-ref.csv.gz")),
			Dashboard.customize(new TrafficCountsDashboard(ApplicationUtils.resolve(config.getContext(), "berlin-v" + RunOpenBerlinScenario.VERSION + "-counts-hgv-vmz.xml.gz"), Set.of("freight")))
				.title("Truck counts")
				.context("freight")
		);
	}

}
