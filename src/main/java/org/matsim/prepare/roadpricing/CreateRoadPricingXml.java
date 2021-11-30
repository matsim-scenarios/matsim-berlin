/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.prepare.roadpricing;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.contrib.roadpricing.RoadPricingWriterXMLv1;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.run.RunBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.List;

public class CreateRoadPricingXml {
    public static void main (String[] args) {
//        String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-bezirksregionen/berlin-bezirksregion_GK4_fixed.shp";
        String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-inner-city-area/inner-city-area.shp";
        String zoneShpCRS = "EPSG:31468";
//        String outputFile = "berlin_distance_roadpricing.xml";
        String outputFile = "berlin_cordon_roadpricing.xml";
        if (args.length == 0) {
            args = new String[]{"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
        }
        Config config =  RunBerlinScenario.prepareConfig( args ) ;
        Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;

//        createDistanceCostRoadPricingXml(scenario, zoneShpFile, zoneShpCRS, outputFile);
        createCordonRoadPricingXml(scenario, zoneShpFile, zoneShpCRS, outputFile);
    }

    static void createDistanceCostRoadPricingXml(Scenario scenario, String zoneShpFile, String shapeFileCRS, String outputFile) {
        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation(scenario.getConfig().global().getCoordinateSystem(), shapeFileCRS);
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(zoneShpFile));

        RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );

        /* Configure roadpricing scheme. */
        RoadPricingUtils.setName(scheme, "Berlin_distance_toll");
        RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_DISTANCE);
        RoadPricingUtils.setDescription(scheme, "distance toll within Berlin city border");

        /* Add general toll. */
        for (Link link : scenario.getNetwork().getLinks().values()) {
            Coord fromNodeTransformed = transformer.transform(link.getFromNode().getCoord());
            Coord toNodeTransformed = transformer.transform(link.getToNode().getCoord());
            if (ShpGeometryUtils.isCoordInPreparedGeometries(fromNodeTransformed, geometries) &&
                    ShpGeometryUtils.isCoordInPreparedGeometries(toNodeTransformed, geometries)) {
                RoadPricingUtils.addLink(scheme, link.getId());
            }
        }

        RoadPricingUtils.createAndAddGeneralCost(scheme,
                Time.parseTime("00:00:00"),
                Time.parseTime("30:00:00"),
                0.00005);

        RoadPricingWriterXMLv1 writer = new RoadPricingWriterXMLv1(scheme);
        writer.writeFile(outputFile);
    }

    static void createCordonRoadPricingXml(Scenario scenario, String zoneShpFile, String shapeFileCRS, String outputFile) {
        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation(scenario.getConfig().global().getCoordinateSystem(), shapeFileCRS);
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(zoneShpFile));

        RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );

        /* Configure roadpricing scheme. */
        RoadPricingUtils.setName(scheme, "Berlin_distance_toll");
        RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
        RoadPricingUtils.setDescription(scheme, "cordon toll within Berlin inner city border");

        /* Add general toll. */
        for (Link link : scenario.getNetwork().getLinks().values()) {
            Coord fromNodeTransformed = transformer.transform(link.getFromNode().getCoord());
            Coord toNodeTransformed = transformer.transform(link.getToNode().getCoord());
            if (!ShpGeometryUtils.isCoordInPreparedGeometries(fromNodeTransformed, geometries) &&
                    ShpGeometryUtils.isCoordInPreparedGeometries(toNodeTransformed, geometries)) {
                RoadPricingUtils.addLink(scheme, link.getId());
            }
        }

        RoadPricingUtils.createAndAddGeneralCost(scheme,
                Time.parseTime("00:00:00"),
                Time.parseTime("30:00:00"),
                5.0);

        RoadPricingWriterXMLv1 writer = new RoadPricingWriterXMLv1(scheme);
        writer.writeFile(outputFile);
    }
}
