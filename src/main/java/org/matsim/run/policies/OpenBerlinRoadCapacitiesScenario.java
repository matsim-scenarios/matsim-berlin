package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Berlin scenario including the possibility to change road capacities and
 * thus the possibility to model increase/decrease of population size.
 * All necessary configs will be made in this class.
 */
public class OpenBerlinRoadCapacitiesScenario extends OpenBerlinScenario {
	private static final Logger log = LogManager.getLogger(OpenBerlinRoadCapacitiesScenario.class);

	@CommandLine.Option(names = "--capacities-shp", description = "Path to shp file for adaption of link capacities. Should be shape of berlin or related.", required = true)
	private String capacityShp;
	@CommandLine.Option(names = "--capacity-relative-change", description = "provide a value that is bigger than 0.0. Should be < 1.0 for capacity reduction and > 1.0 for increase." +
		"The default is set to 0.5.", defaultValue = "0.5")
	private double relativeCapacityChange;

	@Nullable
	@Override
	public Config prepareConfig(Config config) {
		//		apply all config changes from base scenario class
		super.prepareConfig(config);
		//		we do not want to set changed road capacities via qsim cfg group because this would affect all links.
//		the network of this model includes the whole of Brandenburg.

		return config;
	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//		apply all scenario changes from base scenario class
		super.prepareScenario(scenario);

		changeLinkCapacitiesInScenario(scenario, relativeCapacityChange, capacityShp);
	}

	@Override
	public void prepareControler(Controler controler) {
		//		apply all controller changes from base scenario class
		super.prepareControler(controler);
	}


	/**
	 * apply given link capacity to all affected links.
	 */
	static void changeLinkCapacitiesInScenario(Scenario scenario, double relativeCapacityChange, String capacityShp) {
		if (relativeCapacityChange == 0.0) {
			log.fatal("You tried to set a relative road capacity change of {}. This results in road capacities of 0 veh/h, which is invalid. Aborting!", relativeCapacityChange);
			throw new IllegalStateException("");
		}

		Geometry geom = new ShpOptions(capacityShp, null, null).getGeometry();

		AtomicInteger count = new AtomicInteger(0);

		if (relativeCapacityChange != 1.0) {
			log.info("Link capacity will be set to {} instead of default {} for relevant links (within provided shape file and car as an allowed mode).",
				scenario.getConfig().qsim().getFlowCapFactor() * relativeCapacityChange, scenario.getConfig().qsim().getFlowCapFactor());

//		filter links for links inside of shape and link with car as allowed mode. The latter excludes cycleways and pt links.
			scenario.getNetwork().getLinks().values()
				.stream()
				.filter(l -> MGC.coord2Point(l.getFromNode().getCoord()).within(geom) ||
					MGC.coord2Point(l.getToNode().getCoord()).within(geom))
				.filter(l -> l.getAllowedModes().contains(TransportMode.car))
				.forEach(l -> {
					l.getAttributes().putAttribute("originalCapacity", l.getCapacity());
					l.setCapacity(l.getCapacity() * relativeCapacityChange);
					count.getAndIncrement();
				});

			log.info("For {} network links the link capacity has been adapted by a factor of {}.", count.get(), relativeCapacityChange);
		}
	}
}
