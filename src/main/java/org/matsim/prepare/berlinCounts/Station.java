package org.matsim.prepare.berlinCounts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Record class to hold VIZ count station data, e.g. coords and aggregated volumes.
 */
public record Station(String id, String name, String direction, Coord coord, AtomicReference<Link> linkAtomicReference) {

	public Station(String id, String name, String direction, Coord coord) {
		this(id, name, direction, coord, new AtomicReference<>());
	}
}
