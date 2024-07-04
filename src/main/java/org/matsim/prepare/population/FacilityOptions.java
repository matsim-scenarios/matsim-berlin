package org.matsim.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Options to work with facility shape file.
 */
public class FacilityOptions {

	private static final Logger log = LogManager.getLogger(FacilityOptions.class);

	@CommandLine.Option(names = "--facilities", description = "Path to facilities shp file", required = true)
	private Path facilityPath;

	@CommandLine.Option(names = "--facilities-attr", description = "Type of facilities to use", defaultValue = "resident")
	private String attr;

	@CommandLine.Option(names = "--facilities-iters", description = "Maximum number of points to generate trying to fit into landuse", defaultValue = "2000")
	private int iters;

	/**
	 * Holds the index of geometries.
	 */
	private ShpOptions.Index index;

	/**
	 * Create an index of landuse shapes.
	 */
	@Nullable
	public synchronized ShpOptions.Index getIndex(String queryCRS) {

		if (index != null)
			return index;


		ShpOptions shp = ShpOptions.ofLayer(facilityPath.toString(), null);

		index = shp.createIndex(queryCRS, attr, ft -> Boolean.TRUE.equals(ft.getAttribute(attr))
			|| Objects.equals(ft.getAttribute(attr), 1));

		log.info("Read {} features for {} facilities", index.size(), attr);

		return index;
	}

	/**
	 * Tries to select a point that lies within one of the geometries of the index.
	 * Will try at least {@link #iters} times, after which the last point is returned even if not within a geometry.
	 * When no landuse is configured the first point is returned.
	 *
	 * @param queryCRS crs of the generated coordinates
	 * @param genCoord function to generate a new point
	 */
	public Coord select(String queryCRS, Supplier<Coord> genCoord) {

		ShpOptions.Index index = getIndex(queryCRS);

		Coord coord;
		int i = 0;
		do {
			coord = genCoord.get();
			i++;

			if (index != null) {
				// if the point is in any of the shapes we keep it
				if (index.contains(coord))
					break;
			}

			// regenerate points if there is an index
		} while (i <= iters && index != null);


		return coord;
	}
}
