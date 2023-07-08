package org.matsim.prepare.parking;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.*;
import org.matsim.run.RunBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.util.*;

public class PrepareParkingNetwork {

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        final ActivityFacilitiesFactory fac = scenario.getActivityFacilities().getFactory();
        Network network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
        for (Link l : network.getLinks().values()) {
            if (l.getId().toString().contains("pt")) {
                network.removeLink(l.getId());
            }
        }

        NetworkUtils.runNetworkCleaner(network);
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(("/Users/gregorr/Desktop/Test/Uebergabe_Runde_1/TAM_Berlin_2020_Parking_counts_EPSG31468.shp"));
        HashMap<Link, Double> linkParkingCapacity = new HashMap<>();
        NetworkUtils.writeNetwork(network,"carOnlyNetwork.xml");



        double capacity = 0.0;
        for (Iterator<SimpleFeature> iterator = features.iterator(); iterator.hasNext(); ) {
            SimpleFeature simpleFeature = iterator.next();
            Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();
            Coord coord = new Coord(geometry.getCoordinate().getX(), geometry.getCoordinate().getY());
            System.out.println(coord);
            Link l = NetworkUtils.getNearestLinkExactly(network, coord);
            System.out.println(l.getId());

            if (simpleFeature.getAttribute("Count") instanceof Number) {
                capacity = ((Number) simpleFeature.getAttribute("Count")).doubleValue();
            }

            if (!linkParkingCapacity.containsKey(l)) {
                linkParkingCapacity.put(l, capacity);
            }

            if (linkParkingCapacity.containsKey(l)) {
                capacity = linkParkingCapacity.get(l) + capacity;
                linkParkingCapacity.replace(l, capacity);

            }

            capacity = 0;
        }


        
        for (Link link : linkParkingCapacity.keySet()) {

            ActivityFacility parking = fac.createActivityFacility(Id.create(link.getId().toString() + "_curbside", ActivityFacility.class), link.getCoord(), link.getId());
            ActivityOption option = fac.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
            scenario.getActivityFacilities().addActivityFacility(parking);
            option.setCapacity(linkParkingCapacity.get(link));
            parking.addActivityOption(option);
            scenario.getActivityFacilities().addActivityFacility(parking);
        }

        new FacilitiesWriter(scenario.getActivityFacilities()).write("parkingFacilities-poster.xml");
    }
}
