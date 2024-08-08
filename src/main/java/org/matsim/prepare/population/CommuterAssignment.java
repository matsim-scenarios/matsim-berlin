package org.matsim.prepare.population;

import it.unimi.dsi.fastutil.longs.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.application.options.CsvOptions;
import org.matsim.facilities.ActivityFacility;
import org.geotools.api.feature.simple.SimpleFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Helper class for commuter assignment.
 */
public class CommuterAssignment {

	private static final Logger log = LogManager.getLogger(CommuterAssignment.class);

	private final Map<Long, SimpleFeature> zones;

	/**
	 * Outgoing commuter from ars to ars.
	 */
	private final Long2ObjectMap<Long2DoubleMap> commuter;

	private final CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);
	private final double sample;

	public CommuterAssignment(Long2ObjectMap<SimpleFeature> zones, Path commuterPath, double sample) {
		this.sample = sample;

		// outgoing commuters
		this.commuter = new Long2ObjectOpenHashMap<>();
		this.zones = zones;

		// read commuters
		try (CSVParser parser = csv.createParser(commuterPath)) {
			for (CSVRecord row : parser) {
				long from;
				long to;
				try {
					from = Long.parseLong(row.get("from"));
					to = Long.parseLong(row.get("to"));
				} catch (NumberFormatException e) {
					continue;
				}

				String n = row.get("n");
				commuter.computeIfAbsent(from, k -> Long2DoubleMaps.synchronize(new Long2DoubleOpenHashMap()))
					.mergeDouble(to, Integer.parseInt(n), Double::sum);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	/**
	 * Select and return a commute target.
	 *
	 * @param f   sampler producing target locations
	 * @param ars origin zone
	 */
	public ActivityFacility selectTarget(SplittableRandom rnd, long ars, double dist, Point refPoint, Sampler f) {

		// Commute in same zone
		Long2DoubleMap comms = commuter.get(ars);
		if (!commuter.containsKey(ars) || comms.isEmpty())
			return null;

		LongList entries;
		synchronized (comms) {
			entries = new LongArrayList(comms.keySet());
		}

		while (!entries.isEmpty()) {
			long key = entries.removeLong(rnd.nextInt(entries.size()));

			SimpleFeature ft = zones.get(key);

			// TODO: should maybe not be allowed
			if (ft == null)
				continue;

			Geometry zone = (Geometry) ft.getDefaultGeometry();

			// Zones too far away don't need to be considered
			if (zone.distance(refPoint) > dist * 1.2)
				continue;

			ActivityFacility res = f.sample(zone);

			if (res != null) {
				synchronized (comms) {
					double old = comms.get(key);

					// Check if other thread reduced the counter while computing
					// result needs to be thrown away
					if (old <= 0) {
						comms.remove(key);
						continue;
					}

					// subtract available commuters
					double newValue = old - (1 / sample);
					comms.put(key, newValue);

					if (newValue <= 0)
						comms.remove(key);
				}

				return res;
			}
		}


		return null;
	}

	/**
	 * Sample locations from specific zone.
	 */
	interface Sampler {

		ActivityFacility sample(Geometry zone);

	}

}
