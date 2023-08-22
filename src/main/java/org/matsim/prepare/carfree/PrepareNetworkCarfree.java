package org.matsim.prepare.carfree;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrepareNetworkCarfree {

    //TODO copy code from prepareCarfree method in PrepareNetwork in Leipzig scenario
    //TODO if class is short -> convert to inner class in run class

    /**
     * Adapt network to one or more car-free zones. Therefore, a shape file of the wished car-free area is needed.
     */
    public static void prepareCarFree(Network network, ShpOptions shp, String modes) {

        //TODO @GR figure out how to read shp without shpOptions from CR
        Set<String> modesToRemove = new HashSet<>(Arrays.asList(modes.split(",")));

        Geometry carFreeArea = shp.getGeometry();
        GeometryFactory gf = new GeometryFactory();

        for (Link link : network.getLinks().values()) {

            if (!link.getAllowedModes().contains(TransportMode.car)) {
                continue;
            }

            LineString line = gf.createLineString(new Coordinate[]{
                    MGC.coord2Coordinate(link.getFromNode().getCoord()),
                    MGC.coord2Coordinate(link.getToNode().getCoord())
            });

            boolean isInsideCarFreeZone = line.intersects(carFreeArea);

            if (isInsideCarFreeZone) {
                Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

                for (String mode : modesToRemove) {
                    allowedModes.remove(mode);
                }
                link.setAllowedModes(allowedModes);
            }
        }

        MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
        modesToRemove.forEach(m -> multimodalNetworkCleaner.run(Set.of(m)));
    }
}
