package org.matsim.prepare.pt;

import com.conveyal.gtfs.model.Route;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Apply better tags to the route types to have more differentiated vehicle types.
 */
@SuppressWarnings("unused")
public class CorrectRouteTypes implements Consumer<Route> {

	private static final Pattern ICE = Pattern.compile("ICE [0-9]+");
	private static final Pattern IC = Pattern.compile("(IC|EC) [0-9]+");
	private static final Pattern RE = Pattern.compile("((RE|RB[0-9]+)|FEX|^RE)");

	private static final Pattern S_BAHN = Pattern.compile("S[0-9]+");
	private static final Pattern U_BAHN = Pattern.compile("U[0-9]+");

	@Override
	public void accept(Route route) {
		// Check for name and the initial simple route type
		if (S_BAHN.matcher(route.route_short_name).matches() && route.route_type == 2) {
			route.route_type = 109;
		} else if (U_BAHN.matcher(route.route_short_name).matches() && route.route_type == 1) {
			route.route_type = 402;
		}

		// 101 ICE
		if (ICE.matcher(route.route_short_name).matches() && route.route_type == 2) {
			route.route_type = 101;
		}

		// 102 InterCity/EuroCity
		if (IC.matcher(route.route_short_name).matches() && route.route_type == 2) {
			route.route_type = 102;
		}

		// 106 Regionalzug
		if (RE.matcher(route.route_short_name).matches() && route.route_type == 2) {
			route.route_type = 106;
		}

	}

}
