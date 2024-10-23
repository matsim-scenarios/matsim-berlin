package org.matsim.prepare.pt;

import com.conveyal.gtfs.model.Stop;

import java.util.function.Consumer;

/**
 * Correct stops that are wrong in the input data.
 */
@SuppressWarnings("unused")
public class CorrectStopLocations implements Consumer<Stop> {

	@Override
	public void accept(Stop stop) {

		// Harpe, Ort
		if (stop.stop_id.equals("240019")) {
			stop.stop_lat = 52.8519613;
			stop.stop_lon = 10.8828626;
		}

		// Quastenberger Damm
		if (stop.stop_id.equals("490866")) {
			stop.stop_lat = 53.4978109;
			stop.stop_lon = 13.3297704;
		}
	}

}
