package org.matsim.prepare.speedlimit;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * by hzoerner
 */

public class ModifyBerlinNetwork {

    private static double freespeed = 30; // dont use SI-Einheiten hier, in kilometres per hour
    private static int freespeedChangedCounter = 0;

    private static final String networkFilePath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Berlin_Scenario_output\\berlin" +
            "-v5.5-1pct.output_network.xml.gz";
    private static final String berlinGesShapeFile = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Bezirk" +
            "e_-_Berlin-shp\\Berlin_Bezirke.shp";
    private static final String shapeFilePath = "C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_2\\Input" +
            "\\Shapefile\\Berlin_S-Bahn-Ring.shp";
    private static final String outputNetworkFileName = "berlin-speedlimit-" + freespeed + ".xml.gz";

    private static final Network network = NetworkUtils.readNetwork(networkFilePath);
    private static final List<Geometry> geometries = getGeometry();

    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:3857");

    public static void main(String[] args) {

        freespeed = 15/3.6; //in metres per second

        {
            var links = network.getLinks();
            if (links.values().isEmpty()) System.out.println("links.value() is empty...");

            for (var link : links.values()) {

                if (link == null) continue;

                setLinkFreespeed(link);
            }
        }

        System.out.println("Changed freespeed " + freespeedChangedCounter + " times to " + freespeed);
        System.out.println("New network can be found as: " + outputNetworkFileName);

        if (freespeedChangedCounter == 0) return;

        NetworkWriter networkWriter = new NetworkWriter(network);
        networkWriter.write("C:\\Users\\ACER\\Desktop\\Uni\\MATSim\\Hausaufgabe_2\\Input\\" + outputNetworkFileName);

    }

    private static void setLinkFreespeed(double freespeed, Link link){
        //method with individual freespeed

        if (!isLinkInGeometry(link)) return; //modify only Links, which are fully covered by Berlins geometry
        if (isLinkPTOnly(link)) return; //don't modify links, which are allowed only for pt, like S-bahn
        if (isLinkMotorway(link)) return; //limit the motorway freespeed to 30 km/hr ist quiet unrealistic

        if (link.getFreespeed()<= freespeed) return;

        link.setFreespeed(freespeed);
    }

    private static void setLinkFreespeed(Link link){
        //method with static property freespeed

        if (!isLinkInGeometry(link)) return; //modify only Links, which are fully covered by Berlins geometry
        if (isLinkPTOnly(link)) return; //don't modify links, which are allowed only for pt, like S-bahn
        if (isLinkMotorway(link)) return; //limit the motorway freespeed to 30 km/hr ist quiet unrealistic

        if (link.getFreespeed()<= freespeed) return;

        link.setFreespeed(freespeed);
        freespeedChangedCounter++;
    }

    private static boolean isLinkPTOnly (Link link){

        String linkID = link.getId().toString();

        return linkID.contains("pt");
    }

    private static boolean isLinkMotorway (Link link){
        //difficult, a link is assumed to be a motorway, if the freespeed is at least 80/3.6 metres per second

        return NetworkUtils.getType(link).contains("motorway");
       // if (link.getFreespeed() >= 80/3.6) return true;
    }

    private static List<Geometry> getGeometry(){
        var features = ShapeFileReader.getAllFeatures(shapeFilePath);
        List<Geometry> res = new ArrayList<>();

        for (var feature: features){

            var geometry = (Geometry) feature.getDefaultGeometry();
            res.add(geometry);
            System.out.println("Geometry geladen, Größe: " + geometry.getArea());
        }

        return res;
    }

    private static Geometry getGeometry(String id){
        var features = ShapeFileReader.getAllFeatures(shapeFilePath);

        for (var feature: features){
            if(feature.getID().equals(id)){

                return (Geometry) feature.getDefaultGeometry();
            }
        }

        return null;
    }

    private static void filterLinksForGeometry(Map<Id<Link>,? extends Link> linkMap, List<Geometry> geometries){

        var linkList = linkMap.values();

        Iterator<? extends Link> iterator = linkList.iterator();

        while (iterator.hasNext()){

            if (!isInGeometry(iterator.next().getCoord())) iterator.remove();
        }
    }

    private static boolean isLinkInGeometry(Link link){

        var fromNodeCoord = link.getFromNode().getCoord();
        var toNodeCoord = link.getToNode().getCoord();

        return isInGeometry(fromNodeCoord) && isInGeometry(toNodeCoord);
    }

    private static boolean isInGeometry(Coord coord, Geometry geometry) {

        var transformed = transformation.transform(coord);
        return geometry.covers(MGC.coord2Point(transformed));
    }

    private static boolean isInGeometry(Coord coord) {
        //selbe Funktion, nur überprüft die Methode gleich alle Geometrien

        for (var geometry: geometries){

            var transformed = transformation.transform(coord);
            if (geometry.covers(MGC.coord2Point(transformed))) return true;
        }

        return false;
    }
}
