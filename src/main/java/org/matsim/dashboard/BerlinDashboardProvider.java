package org.matsim.dashboard;

import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.TripDashboard;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Provider for default dashboards in the scenario.
 */
public class BerlinDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		TripDashboard trips = new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv");
		trips.setAnalysisArgs("--match-id", "^berlin.+", "--shp-filter", "none");

		URL refURL = IOUtils.extendUrl(config.getContext(), "berlin-v6.0-routes-ref.csv.gz");
		String refData;
		try {
			refData = new File(refURL.toURI()).getAbsolutePath();
		} catch (URISyntaxException e) {
			refData = refURL.toString();
		}

		return List.of(trips, new DTVComparisonDashboard(), new TravelTimeComparisonDashboard(refData));
	}

}
