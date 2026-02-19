package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Berlin scenario including speed reductions/increases on network links of all types except motorways.
 * For speed reduction (relativeSpeedChange < 1.0), links of the main and secondary road network are adapted equally to keep the network's hierarchy.
 * For speed increase (relativeSpeedChange > 1.0), only links of the main network are adapted.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinRoadSpeedScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinRoadSpeedScenario.class);

	@CommandLine.Option(names = "--speed-shp", description = "Path to shp file for adaption of link speeds.", defaultValue = "TODO")
	private String speedAreaShp;
	@CommandLine.Option(names = "--speed-relative-change", description = "provide a value that is bigger than 0.0. Should be < 1.0 for speed reduction and > 1.0 for increase." +
		"The default is set to 0.6, such that roads with an allowed speed of 50kmh are reduced to 30kmh.", defaultValue = "0.6")
	private double relativeSpeedChange;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		applyRelativeSpeedChangeToLinksInScenario(scenario, relativeSpeedChange, speedAreaShp);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}

	/**
	 * applies the given change of freespeed to all affected links.
	 */
	static void applyRelativeSpeedChangeToLinksInScenario(Scenario scenario, double relativeSpeedChange, String speedAreaShp) {
		if (relativeSpeedChange == 0.0) {
			log.fatal("You tried to set a relative freespeed change of {}. This results in freespeeds of 0 km/h on affected links, which is invalid. Aborting!", relativeSpeedChange);
			throw new IllegalStateException("");
		}

		List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(IOUtils.extendUrl(scenario.getConfig().getContext(), speedAreaShp));

		Set<? extends Link> carLinksInArea = scenario.getNetwork().getLinks().values().stream()
			//filter car links
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			//spatial filter
			.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
			//we won't change motorways and motorway_links
			.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))
			.collect(Collectors.toSet());

		if (relativeSpeedChange > 0.0) {
			log.info("adapt speed relatively by a factor of: {}", relativeSpeedChange);

			//apply speed reduction to all roads but motorways
			if (relativeSpeedChange > 1.0) {
//				only touch main roads
				Set<String> roadTypes = Set.of("highway.primary", "highway.primary_link", "highway.secondary", "highway.secondary_link", "highway.tertiary");
				carLinksInArea.stream()
					.filter(link -> roadTypes.contains(link.getAttributes().getAttribute("type")))
					.forEach(l -> l.setFreespeed(l.getFreespeed() * relativeSpeedChange));
			} else {
				carLinksInArea.forEach(link -> link.setFreespeed(link.getFreespeed() * relativeSpeedChange));
			}

		} else {
			log.fatal("Speed reduction value of {} is invalid. Please put a 0.0 <= value < 1.0", relativeSpeedChange);
			throw new IllegalArgumentException("");
		}
	}
}
