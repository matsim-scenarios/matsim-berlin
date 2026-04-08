package org.matsim.run.policies;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//run --1pct --config:controller.lastIteration 1 --config:controller.overwriteFiles deleteDirectoryIfExists
public class OpenBerlinCarfreeScenario extends OpenBerlinScenario {

	@CommandLine.Option(names = "--ban-area",
		defaultValue = "input/v6.3/hundekopf-shp/hundekopf-carBanArea-25832.shp",
		description = "Path to (single geom) shape file depicting the area where private cars are banned from. If you adjust, think about adjusting the drt area+stops file, as well!")
	private static String SHAPE_FILE;

	public OpenBerlinCarfreeScenario() {
		super();
	}

	public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinCarfreeScenario.class, args);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		ShapeFileReader shpReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shpReader.readFileAndInitialize(SHAPE_FILE);
		Geometry consideredArea = (Geometry) ((SimpleFeatureImpl) ((ArrayList) features).get(0)).getDefaultGeometry();

		// disallow car mode within ring (all links for which the from and to nodes are within the shape file).
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (!link.getAllowedModes().contains("pt") && MGC.coord2Point(link.getFromNode().getCoord()).within(consideredArea) &&
				MGC.coord2Point(link.getToNode().getCoord()).within(consideredArea)) {
				HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.remove(TransportMode.car);
				link.setAllowedModes(allowedModes);
				link.getAttributes().putAttribute("ring", true);
			} else {
				link.getAttributes().putAttribute("ring", false);
			}
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Leg leg : TripStructureUtils.getLegs(plan)) {
					if (leg.getMode().equals(TransportMode.car)) {
						leg.setRoute(null);
					}
				}
			}
		}

		new MultimodalNetworkCleaner(scenario.getNetwork()).run(Set.of(TransportMode.car));

	}

}
