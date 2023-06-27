package org.matsim.prepare.traveltime;

import org.matsim.api.core.v01.Coord;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

/**
 * Interface to calculate route information.
 */
public interface RouteValidator {

	/**
	 * Create departure time for a week day in the future.
	 */
	static ZonedDateTime createDateTime(int hour) {
		LocalDate date = LocalDate.now(ZoneId.systemDefault()).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
		return date.atStartOfDay(ZoneId.systemDefault()).plusHours(hour).withZoneSameInstant(ZoneOffset.UTC);
	}

	/**
	 * Return the name of the validator.
	 */
	String name();

	/**
	 * Calculate route information between two coordinates. Coordinates are always in WGS84.
	 */
	Result calculate(Coord from, Coord to, int hour);

	/**
	 * Result for one query.
	 */
	record Result(int hour, int travelTime, int dist) {
	}

}
