package org.matsim.analysis.speedlimit;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShapeFileAnalyzer {

    private String epsgFrom = "31468";
    private String epsgTo = "3857";

    private CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:" + epsgFrom,
            "EPSG:"+epsgTo);

    private String shapeFilePath;

    private HashMap<String, Geometry> geometries = new HashMap<>();

    public ShapeFileAnalyzer(String shapeFilePath){
        this.shapeFilePath = shapeFilePath;
        getGeometry();
    }

    public ShapeFileAnalyzer(String shapeFilePath, String epsgFrom, String epsgTo){
        this.shapeFilePath = shapeFilePath;
        getGeometry();
        setEpsgFrom(epsgFrom);
        setEpsgTo(epsgTo);
    }

    private void setEpsgFrom(String epsgFrom){
        this.epsgFrom = epsgFrom;
    }

    private void setEpsgTo(String epsgTo){
        this.epsgTo = epsgTo;
    }

    private List<Geometry> getGeometry(){
        var features = ShapeFileReader.getAllFeatures(this.shapeFilePath);
        List<Geometry> res = new ArrayList<>();

        for (var feature: features){

            var geometry = (Geometry) feature.getDefaultGeometry();
            res.add(geometry);
        }

        return res;
    }

    public boolean isLinkInGeometry(Link link){

        var fromNodeCoord = link.getFromNode().getCoord();
        var toNodeCoord = link.getToNode().getCoord();

        return isInGeometry(fromNodeCoord) && isInGeometry(toNodeCoord);
    }

    public boolean isInGeometry(Coord coord, Geometry geometry) {

        var transformed = transformation.transform(coord);
        return geometry.covers(MGC.coord2Point(transformed));
    }

    public boolean isInGeometry(Coord coord) {
        //selbe Funktion, nur überprüft die Methode gleich alle Geometrien

        for (var geometry: this.geometries.values()){

            var transformed = transformation.transform(coord);
            if (geometry.covers(MGC.coord2Point(transformed))) return true;
        }

        return false;
    }
}
