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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit, ihab
 */

public class GenerateAirPollutionSpatialPlots {
	private static final Logger log = Logger.getLogger(GenerateAirPollutionSpatialPlots.class);

    private final double countScaleFactor;
    private final double gridSize;
    private final double smoothingRadius;
    
    private static final double xMin = 4565039. - 125.;
	private static final double xMax = 4632739. + 125.; 
	private static final double yMin = 5801108. - 125.;
	private static final double yMax = 5845708. + 125.;

    private GenerateAirPollutionSpatialPlots(final double gridSize, final double smoothingRadius, final double countScaleFactor) {
        this.gridSize = gridSize;
        this.smoothingRadius = smoothingRadius;
        this.countScaleFactor = countScaleFactor;
    }

	public static void main(String[] args) {

		String rootDirectory;
		
		if (args.length == 1) {
			rootDirectory = args[0];
		} else {
			throw new RuntimeException("Please set the root directory. Aborting...");
		}
        
        final double gridSize = 100.;
        final double smoothingRadius = 500.;
        final double scaleFactor = 100.;
        
        final String runDir = rootDirectory + "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
    	final String runId = "berlin-v5.4-1pct";

        GenerateAirPollutionSpatialPlots plots = new GenerateAirPollutionSpatialPlots(gridSize, smoothingRadius, scaleFactor);
        
        final String configFile = runDir + runId + ".output_config.xml";
		final String events = runDir + runId + ".emission.events.offline.xml.gz";

		plots.writeEmissions(configFile , events, runDir, runId);
    }

    private void writeEmissions(String configPath, String eventsPath, String runDir, String runId) {

		Config config = ConfigUtils.loadConfig(configPath);
		config.plans().setInputFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.vehicles().setVehiclesFile(null);
		config.network().setInputFile(runDir + runId + ".output_network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        double binSize = 3600. * 3; // for daily numbers: make the bin size bigger than the scenario has seconds

        Network network = scenario.getNetwork();

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(gridSize)
                .withTimeBinSize(binSize)
                .withNetwork(network)
                .withBounds(createBoundingBox())
                .withSmoothingRadius(smoothingRadius)
                .withCountScaleFactor(countScaleFactor)
                .withGridType(EmissionGridAnalyzer.GridType.Square)
                .build();

		TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(eventsPath );
		analyzer.processToJsonFile(eventsPath, runDir + runId + ".emissions.json");
		
		Set<Pollutant> pollutants = new HashSet<>();
		for (TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin : timeBins.getTimeBins()) {
            for (Grid.Cell<Map<Pollutant, Double>> cell : bin.getValue().getCells()) {
                for (Pollutant pollutentInCell : cell.getValue().keySet()) {
                	if (!pollutants.contains(pollutentInCell)) pollutants.add(pollutentInCell);
                }
			}
		}
		for (Pollutant pollutant : pollutants) {
			log.info("Writing data to csv file: " + pollutant);
	        writeGridToCSV(timeBins, pollutant, runDir + runId + ".emissions." + pollutant + ".csv");
		}
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, Pollutant pollutant, String outputPath) {

		try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputPath), CSVFormat.TDF)) {
            printer.printRecord("timeBinStartTime", "centroidX", "centroidY", "weight");

            for (TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin : bins.getTimeBins()) {
                final double timeBinStartTime = bin.getStartTime();
                for (Grid.Cell<Map<Pollutant, Double>> cell : bin.getValue().getCells()) {
                    double weight = cell.getValue().containsKey(pollutant) ? cell.getValue().get(pollutant) : 0;
                    printer.printRecord(timeBinStartTime, cell.getCoordinate().x, cell.getCoordinate().y, weight);
				}
			}
        } catch (IOException e) {
            e.printStackTrace();
		}
	}

    private Geometry createBoundingBox() {
        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });
    }
}
