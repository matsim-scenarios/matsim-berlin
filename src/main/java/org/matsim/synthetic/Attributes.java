package org.matsim.synthetic;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.Objects;

public final class Attributes {

	public static final String HOME_X = "home_x";
	public static final String HOME_Y = "home_y";

	/**
	 * Gemeinde code.
	 */
	public static final String GEM = "gem";
	public static final String ARS = "ars";


	/**
	 * LOR for Berlin
	 */
	public static final String LOR = "lor";

	public static final String RegioStaR7 = "RegioStaR7";


	/**
	 * Work / edu commute destination.
	 */
	public static final String COMMUTE = "commute";
	public static final String COMMUTE_KM = "commute_km";


	public static boolean isLinkUnassigned(Id<Link> link) {
		return link != null && Objects.equals(link.toString(), "unassigned");
	}

	/**
	 * Return home coordinate of a person.
	 */
	public static Coord getHomeCoord(Person p) {
		return new Coord((Double) p.getAttributes().getAttribute(HOME_X), (Double) p.getAttributes().getAttribute(HOME_Y));
	}


}
