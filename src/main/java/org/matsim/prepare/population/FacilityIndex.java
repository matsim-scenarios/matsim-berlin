package org.matsim.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.run.RunOpenBerlinScenario;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spatial index for facilities.
 */
final class FacilityIndex {

	private static final Logger log = LogManager.getLogger(FacilityIndex.class);

	final ActivityFacilities all = FacilitiesUtils.createActivityFacilities();

	/**
	 * Maps activity type to spatial index.
	 */
	final Map<String, STRtree> index = new HashMap<>();

	FacilityIndex(String facilityPath) {

		new MatsimFacilitiesReader(RunOpenBerlinScenario.CRS, RunOpenBerlinScenario.CRS, all)
			.readFile(facilityPath);

		Set<String> activities = all.getFacilities().values().stream()
			.flatMap(a -> a.getActivityOptions().keySet().stream())
			.collect(Collectors.toSet());

		log.info("Found activity types: {}", activities);

		for (String act : activities) {

			NavigableMap<Id<ActivityFacility>, ActivityFacility> afs = all.getFacilitiesForActivityType(act);
			for (ActivityFacility af : afs.values()) {
				STRtree index = this.index.computeIfAbsent(act, k -> new STRtree());
				index.insert(MGC.coord2Point(af.getCoord()).getEnvelopeInternal(), af);
			}
		}

		// Build all trees
		index.values().forEach(STRtree::build);
	}
}
