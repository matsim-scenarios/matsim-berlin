package org.matsim.run.deparking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

@FunctionalInterface
public interface DeParkingApproach {
	// returns the parking costs in €/h
	double newParkingCost(ParkingAnalyzer analyzer, Id<Link> linkId, int iteration, double from, double to);
}
