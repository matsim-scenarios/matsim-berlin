package org.matsim.prepare.ptRouteTrim;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetLinksInArea {

    public static void main(String[] args) throws MalformedURLException {
        Config config = ConfigUtils.loadConfig("scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml");
        Scenario scenario =  ScenarioUtils.loadScenario(config);
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-hundekopf-areas/berlin_hundekopf.shp"));
        Set<Id<Link>> linksInArea = new HashSet<>();
        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries)) {
                linksInArea.add(link.getId());
            }
        }
    }

}
