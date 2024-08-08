package org.matsim.run;


import org.locationtech.jts.geom.Geometry;
import org.matsim.application.MATSimApplication;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Run the {@link OpenBerlinScenario} with default configuration.
 */
public final class RunOpenBerlinScenario {

	private RunOpenBerlinScenario() {




	}

	public static void main(String[] args) {
		MATSimApplication.runWithDefaults(OpenBerlinScenario.class, args);
	}

}
