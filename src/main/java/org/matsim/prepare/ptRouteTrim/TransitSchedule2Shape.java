package org.matsim.prepare.ptRouteTrim;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
//import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.util.URLs;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * This tool creates a LineString for each route in a TransitSchedule, based on the coordinates of the StopFacilities.
 * The collection of LineStrings is then exported to a ESRI shape file.
 *
 * @author jakobrehmann
 */
public class TransitSchedule2Shape {

    public static void main(String[] args) throws Exception {

        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
        String newFile = "C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\output\\output.shp";

        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TransitSchedule tS = scenario.getTransitSchedule();

        createShpFile(tS, newFile, "31468");

    }


    public static void createShpFile(TransitSchedule tS, String outputFilename, String epsgCode) throws SchemaException, IOException {

        File newFile = new File(outputFilename);

        final SimpleFeatureType TYPE =
                DataUtilities.createType(
                        "Link",
                        "the_geom:LineString:srid=" + epsgCode + ","
                                + // <- the geometry attribute: Point type
                                "name:String,"
//                                + // <- a String attribute
//                                "number:Integer" // a number attribute
                );
        System.out.println("TYPE:" + TYPE);

        List<SimpleFeature> features = new ArrayList<>();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

        for(TransitLine line : tS.getTransitLines().values()){
            for (TransitRoute route : line.getRoutes().values()) {

                List<TransitRouteStop> stops = route.getStops();
                Coordinate[] coordinates = new Coordinate[stops.size()] ;
                for (int i = 0; i < stops.size(); i++) {
                    TransitRouteStop stop = stops.get(i);
                    Coord coord = stop.getStopFacility().getCoord();
                    Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
                    coordinates[i]=coordinate;
                }
                if (coordinates.length == 1) {
                    continue;
                }
                LineString routeString = geometryFactory.createLineString(coordinates);
                String routeName = route.getId().toString();
                featureBuilder.add(routeString);
                featureBuilder.add(routeName);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
        }


        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);


        newDataStore.createSchema(TYPE);

        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();

        System.out.println("SHAPE:" + SHAPE_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        }
    }
}