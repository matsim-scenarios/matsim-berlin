/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * @author amit, ihab, janek
 */

public abstract class GenerateAirPollutionSpatialPlots {
	private static final Logger log = Logger.getLogger(GenerateAirPollutionSpatialPlots.class);
    
    private static final double xMin = 4565039. - 125.;
	private static final double xMax = 4632739. + 125.; 
	private static final double yMin = 5801108. - 125.;
	private static final double yMax = 5845708. + 125.;


	public static void main(String[] args) {

		String rootDirectory;
		
		if (args.length == 1) {
			rootDirectory = args[0];
		} else {
			throw new RuntimeException("Please set the root directory. Aborting...");
		}
        
        final double gridSize = 100.;
        final double scaleFactor = 100.;
        final String runDir = rootDirectory + "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
        final String runId = "berlin-v5.4-1pct";
        final String events = runDir + runId + ".emission.events.offline.xml.gz";
        final String networkFile = runDir + runId + "output_network.xml.gz";
        final String outputFile = runDir + runId + ".emissions." + Pollutant.NOx + ".csv";

        // filter the network onto the bounding box. This way only the links within the bounding box will collect emissions
        var boundingBox = createBoundingBox();
        var filteredNetwork = NetworkUtils.readNetwork(networkFile).getLinks().values().parallelStream()
                .filter(link -> boundingBox.covers(MGC.coord2Point(link.getFromNode().getCoord())) || boundingBox.covers(MGC.coord2Point(link.getToNode().getCoord())))
                .collect(NetworkUtils.getCollector());

        // do the actual rastering. Reducing the radius will lead to less smoothed emissions
        // reduce to 0, to only draw emissions onto cells which are covered by a link.
        var rasterMap = FastEmissionGridAnalyzer.processEventsFile(events, filteredNetwork, gridSize, 20);

        // write the raster for nox
        var noxRaster = rasterMap.get(Pollutant.NOx);
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.TDF)) {

            // write header
            printer.printRecord("x", "y", Pollutant.NOx);

            // write values
            noxRaster.forEachCoordinate((x, y, value) -> {

                // skip values smaller than 0.1g/ha to reduce file size
                if (value < 0.1)  return;

                // unfortunately one has to wrap this in try/catch because it is inside a lambda function
                try {
                    printer.printRecord(x, y, value * scaleFactor);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Geometry createBoundingBox() {
        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });
    }
}
