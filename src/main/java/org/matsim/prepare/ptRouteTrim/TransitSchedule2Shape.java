package org.matsim.prepare.ptRouteTrim;

import java.io.*;
import java.util.*;

import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
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
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * This example reads data for point locations and associated attributes from a comma separated text
 * (CSV) file and exports them as a new shapefile. It illustrates how to build a feature type.
 *
 * <p>Note: to keep things simple in the code below the input file should not have additional spaces
 * or tabs between fields.
 */
public class TransitSchedule2Shape {

    public static void main(String[] args) throws Exception {

        // Import stuff
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";//"../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
//        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        File newFile = new File("C:\\Users\\jakob\\projects\\matsim-berlin\\src\\main\\java\\org\\matsim\\prepare\\ptRouteTrim\\output\\output.shp");



        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
//        config.network().setInputFile(inNetworkFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TransitSchedule tS = scenario.getTransitSchedule();



//        /*
//         * We use the DataUtilities class to create a FeatureType that will describe the data in our
//         * shapefile.
//         *
//         * See also the createFeatureType method below for another, more flexible approach.
//         */
        final SimpleFeatureType TYPE =
                DataUtilities.createType(
                        "Link",
                        "the_geom:LineString:srid=4326,"
                                + // <- the geometry attribute: Point type
                                "name:String,"
//                                + // <- a String attribute
//                                "number:Integer" // a number attribute
                );
        System.out.println("TYPE:" + TYPE);
//
//        /*
//         * A list to collect features as we create them.
//         */
//
        List<SimpleFeature> features = new ArrayList<>();


//        /*
//         * GeometryFactory will be used to create the geometry attribute of each feature,
//         * using a Point object for the location.
//         */
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
//
        ShapefileDataStore newDataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

//        /*
//         * TYPE is used as a template to describe the file contents
//         */
        newDataStore.createSchema(TYPE);
//
        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
        /*
         * The Shapefile format has a couple limitations:
         * - "the_geom" is always first, and used for the geometry attribute name
         * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
         * - Attribute names are limited in length
         * - Not all data types are supported (example Timestamp represented as Date)
         *
         * Each data store has different limitations so check the resulting SimpleFeatureType.
         */
        System.out.println("SHAPE:" + SHAPE_TYPE);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            /*
             * SimpleFeatureStore has a method to add features from a
             * SimpleFeatureCollection object, so we use the ListFeatureCollection
             * class to wrap our list of features.
             */
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
            System.exit(0); // success!
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
    }
}