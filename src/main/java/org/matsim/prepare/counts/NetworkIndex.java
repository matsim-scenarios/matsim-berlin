package org.matsim.prepare.counts;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.List;

/**
 * Class to match points or linestrings to MATSim network links
 * can be used to create counts
 */
public class NetworkIndex<T> {

	/*
		TODO: use network index from matsim-libs

		features matching with detailed geometries
		IDEA: read link geometries and use detailed linestring, instead of simple ones

	 */

	private STRtree index = new STRtree();
	private final double range;
	private final GeometryFactory factory = new GeometryFactory();

	private GeometryGetter getter;

	public NetworkIndex(Network network, double range) {

		this.range = range;

		for (Link link : network.getLinks().values()) {
			Envelope env = getLinkEnvelope(link);
			index.insert(env, link);
		}

		index.build();
	}

	public NetworkIndex(Network network, double range, GeometryGetter getter) {

		this.range = range;

		for (Link link : network.getLinks().values()) {
			Envelope env = getLinkEnvelope(link);
			index.insert(env, link);
		}

		index.build();

		this.getter = getter;
	}

	@SuppressWarnings("unchecked")
	public List<Link> query(T toMatch) {

		Geometry geometry = getter.createGeometry(toMatch);

		Envelope searchArea = geometry.buffer(this.range).getEnvelopeInternal();

		List<Link> result = index.query(searchArea);

		if (result.isEmpty()) return null;

		return result;
	}

	public void setGetter(GeometryGetter getter) {
		this.getter = getter;
	}

	public Geometry getGeometry(T object) {
		return this.getter.createGeometry(object);
	}

	public Envelope getLinkEnvelope(Link link) {
		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates).getEnvelopeInternal();
	}

	public LineString link2LineString(Link link) {

		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates);
	}

	public void remove(Link link) {
		Envelope env = getLinkEnvelope(link);
		index.remove(env, link);
	}

	@FunctionalInterface
	public interface GeometryGetter<T> {

		Geometry createGeometry(T o);
	}
}
