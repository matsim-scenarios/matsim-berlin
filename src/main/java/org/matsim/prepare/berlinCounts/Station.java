package org.matsim.prepare.berlinCounts;

import org.matsim.api.core.v01.Coord;

import java.util.HashMap;
import java.util.Map;

/**
 * Record class to hold VIZ count station data, e.g. coords and aggregated volumes.
 */
public record Station(String id, String name, String direction, Coord coord, Map<Integer, Double> miv, Map<Integer, Double> freight) {

	public Station(String id, String name, String direction, Coord coord) {
		this(id, name, direction, coord, new HashMap<>(), new HashMap<>());
	}
}
