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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.contrib.roadpricing.RoadPricingWriterXMLv1;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.run.RunBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CreateRoadPricingXml {

    private static String BERLIN_CRS = "EPSG:31468";

    public static void main (String[] args) {
        String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
//        String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-inner-city-area/inner-city-area.shp";
        String zoneShpCRS = "EPSG:31468";

        //determine tolled links
        Network network = NetworkUtils.readNetwork(ConfigUtils.loadConfig("scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml")
                .network().
                        getInputFile());

        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(zoneShpFile));
        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation(BERLIN_CRS, zoneShpCRS);
        Set<Id<Link>> tolledLinks = new HashSet<>();
        for (Link link : network.getLinks().values()) {
            if(!link.getAllowedModes().contains("pt")){ //we wanna skip pt links
                Coord fromNodeTransformed = transformer.transform(link.getFromNode().getCoord());
                Coord toNodeTransformed = transformer.transform(link.getToNode().getCoord());
                if (ShpGeometryUtils.isCoordInPreparedGeometries(fromNodeTransformed, geometries) &&
                        ShpGeometryUtils.isCoordInPreparedGeometries(toNodeTransformed, geometries)) {
                    tolledLinks.add(link.getId());
                }
            }
        }

        for (double ii = 0; ii <= 50; ii += 5){
            double toll = ii; //area toll is in euro
//            double toll = ii / 1_000; //network distance unit is m

            String outputFile = "berlin_area_roadpricing_" + ii + "_euro.xml";
//            String outputFile = "hundekopf_area_roadpricing_" + ii + "_euro.xml";

            //create empty scenario object
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario);

            /* Configure roadpricing scheme. */
            RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_AREA);
//            RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_DISTANCE);

            RoadPricingUtils.setName(scheme, "Berlin_area_toll");
//            RoadPricingUtils.setName(scheme, "Hundekopf_area_toll");
            RoadPricingUtils.setDescription(scheme, "area toll within Berlin border");
//            RoadPricingUtils.setDescription(scheme, "area toll within inner city");

            tolledLinks.forEach(linkId -> RoadPricingUtils.addLink(scheme, linkId));

            RoadPricingUtils.createAndAddGeneralCost(scheme,
                    Time.parseTime("00:00:00"),
                    Time.parseTime("30:00:00"),
                    toll);

            RoadPricingWriterXMLv1 writer = new RoadPricingWriterXMLv1(scheme);
            writer.writeFile(outputFile);
        }

    }

}
