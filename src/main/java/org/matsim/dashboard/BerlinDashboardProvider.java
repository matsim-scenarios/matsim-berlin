package org.matsim.dashboard;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.*;

import java.util.List;
import java.util.Set;

/**
 * Provider for default dashboards in the scenario.
 */
public class BerlinDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		// The trip dashboard against the SrV reference data, which is what the ASC calibration calibrates against.
		// SimWrapper adds the default provider's TripDashboard (without reference data) before this provider is asked,
		// and skips a second dashboard of the same class and context, so this one needs a context of its own. It then
		// appears next to the default one, as in the Dresden scenario, and its analysis goes to
		// analysis/population-calibration.
		Dashboard trips = Dashboard.customize(new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv")
				.setAnalysisArgs("--match-id", "^berlin.+", "--shp-filter", "none")
				.withChoiceEvaluation(true)
				.withDistanceDistribution("mode_share_distance_distribution.csv")
				.withGroupedRefData("mode_share_per_group_dist_ref.csv", "age", "income", "employment", "economic_status", "zone"))
			.context("calibration")
			.title("Trips (calibration)");

		ActivityDashboard activities = new ActivityDashboard("berlin_inspire_grid_1km.gpkg")
			.addActivityType("work", List.of("work"), List.of(ActivityDashboard.Indicator.COUNTS), false, null)
			.addActivityType("leisure", List.of("leisure"), List.of(ActivityDashboard.Indicator.COUNTS), true, null)
			.addActivityType("shopping", List.of("shop", "shop_daily"), List.of(ActivityDashboard.Indicator.COUNTS), true, null);

		return List.of(
			trips,
			activities,
			new TravelTimeComparisonDashboard(ApplicationUtils.resolve(config.getContext(), "berlin-v" + OpenBerlinScenario.VERSION + "-routes-ref.csv.gz")),
			new EmissionsDashboard(config.global().getCoordinateSystem()),
			new NoiseDashboard(config.global().getCoordinateSystem()),
			new TrafficCountsDashboard()
				.withModes(TransportMode.car, Set.of(TransportMode.car))
				.withModes(TransportMode.truck, Set.of(TransportMode.truck, "freight"))
		);
	}

}
