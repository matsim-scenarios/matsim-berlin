/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.legacy.run.drt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

/**
* @author ikaddoura
 * @deprecated Since class has not been tested since geotools API changes.
*/
@Deprecated
public final class BerlinShpUtils {

	private Map<Integer, Geometry> serviceAreaGeometries;
	private Map<Double, Map<Integer, Geometry>> serviceAreaGeometriesWithBuffer = new HashMap<>();

	public BerlinShpUtils(String drtServiceAreaShapeFile) {
		if (drtServiceAreaShapeFile != null && drtServiceAreaShapeFile != "" && drtServiceAreaShapeFile != "null" ) {
			this.serviceAreaGeometries = loadShapeFile(drtServiceAreaShapeFile);
		}
	}

	private Map<Integer, Geometry> loadShapeFile(String shapeFile) {
		Map<Integer, Geometry> geometries = new HashMap<>();

		Collection<SimpleFeature> features = null;
		if (!shapeFile.startsWith("http")) {
			features = ShapeFileReader.getAllFeatures(shapeFile);
		} else {
			try {
				features = getAllFeatures(new URL(shapeFile));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if (features == null) throw new RuntimeException("Aborting...");
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			geometries.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		return geometries;
	}

	public boolean isCoordInDrtServiceArea(Coord coord) {
		return isCoordInArea(coord, serviceAreaGeometries);
	}

	public boolean isCoordInDrtServiceAreaWithBuffer(Coord coord, double buffer) {
		if (!serviceAreaGeometriesWithBuffer.containsKey(buffer)) {
			serviceAreaGeometriesWithBuffer.put(buffer, prepareAndSaveGeometriesWithBuffer(serviceAreaGeometries, buffer));
		}
		return isCoordInArea(coord, serviceAreaGeometriesWithBuffer.get(buffer));
	}

	private Map<Integer, Geometry> prepareAndSaveGeometriesWithBuffer(Map<Integer, Geometry> geometries, double buffer) {
		Map<Integer, Geometry> geometriesWithBuffer = new HashMap<>();
		for (Map.Entry<Integer, Geometry> entry: geometries.entrySet()) {
			geometriesWithBuffer.put(entry.getKey(), entry.getValue().buffer(buffer));
		}
		return geometriesWithBuffer;
	}

	private boolean isCoordInArea(Coord coord, Map<Integer, Geometry> areaGeometries) {
		boolean coordInArea = false;
		for (Geometry geometry : areaGeometries.values()) {
			Point p = MGC.coord2Point(coord);

			if (p.within(geometry)) {
				coordInArea = true;
			}
		}
		return coordInArea;
	}

	static Collection<SimpleFeature> getAllFeatures(final URL url) {
		try {
			FileDataStore store = FileDataStoreFinder.getDataStore(url);
			SimpleFeatureSource featureSource = store.getFeatureSource();

			SimpleFeatureIterator it = featureSource.getFeatures().features();
			List<SimpleFeature> featureSet = new ArrayList<SimpleFeature>();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				featureSet.add(ft);
			}
			it.close();
			store.dispose();
			return featureSet;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Point getRandomPointInServiceArea(Random random) {
		return getRandomPointInFeature(random, serviceAreaGeometries.get(random.nextInt(serviceAreaGeometries.size())));
	}

	private static Point getRandomPointInFeature(Random rnd, Geometry g)
    {
        Point p = null;
        double x, y;
        do {
            x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
            y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
            p = MGC.xy2Point(x, y);
        }
        while (!g.contains(p));
        return p;
    }

}

