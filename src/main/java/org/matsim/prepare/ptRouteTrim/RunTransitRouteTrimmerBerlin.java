package org.matsim.prepare.ptRouteTrim;

import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.locationtech.jts.precision.EnhancedPrecisionOp.buffer;

public class RunTransitRouteTrimmerBerlin {

    public static void main(String[] args) throws IOException, SchemaException {
        final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
        final String inVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-vehicles.xml.gz";
        final String inNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        final String zoneShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
        final String outputPath = "src/main/java/org/matsim/prepare/ptRouteTrim/output/";
        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);
        config.vehicles().setVehiclesFile(inVehiclesFile);
        final String epsgCode = "31468";//"25832"; //TODO


        Scenario scenario = ScenarioUtils.loadScenario(config);

        TransitSchedule transitScheduleOld = scenario.getTransitSchedule();


        // Collect all rail stations
        Collection<TransitStopFacility> allStations = transitScheduleOld.getFacilities().values();
        List<TransitStopFacility> railStations = allStations.stream()
                .filter(x -> x.getAttributes().getAsMap().containsKey("stopFilter"))
                .filter(x -> x.getAttributes().getAttribute("stopFilter").equals("station_S/U/RE/RB"))
                .collect(Collectors.toList());

        List<Id<TransitStopFacility>> railStationIds = railStations.stream().map(x -> x.getId()).collect(Collectors.toList());

//        List<TransitStopFacility> nonRailStations = allStations.stream().filter(x -> !railStationIds.contains(x.getId())).collect(Collectors.toList());







        GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();

//        GEOMETRY_FACTORY.buildGeometry();




        List<Geometry> geoList = new ArrayList<>();
        List<SimpleFeature> featureList = new ArrayList<>();

        for (TransitStopFacility stop : railStations) {

            Coordinate coordinate = new Coordinate(stop.getCoord().getX(), stop.getCoord().getY());
            Point point = GEOMETRY_FACTORY.createPoint(coordinate);
            Geometry buffer = buffer(point, 400);
            geoList.add(buffer);
        }

        Geometry geometry_full = GEOMETRY_FACTORY.buildGeometry(geoList);

//        Geometry[] geoArray = new Geometry[geoList.size()];
//        geoArray = geoList.toArray(geoArray);
//
//        GeometryCollection geometryCollection = GEOMETRY_FACTORY.createGeometryCollection(geoArray);


        RandomAccessFile shp = new RandomAccessFile("myshape2.shp", "rw");
        RandomAccessFile shx = new RandomAccessFile("myshape2.shx", "rw");
        ShapefileWriter writer = new ShapefileWriter(shp.getChannel(), shx.getChannel());
        writer.write((GeometryCollection) geometry_full, ShapeType.POLYGON);
//        writer.write(geometryCollection, ShapeType.POLYGON);

        //
        //        String shpFileName = "xxxxxxx";
        //        RandomAccessFile shpFile = new RandomAccessFile(shpFileName, "rw");
        //        ShapefileWriter shapefileWriter = new ShapefileWriter(shpFile.getChannel(), shpFile.getChannel());
        //        shapefileWriter.write(geometryCollection, ShapeType.POLYGON);
        //
        //        shapefileWriter.

        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));
        System.out.println("\n Modify Routes: SplitRoute");
        TransitRouteTrimmer transitRouteTrimmer = new TransitRouteTrimmer(scenario.getTransitSchedule(), scenario.getVehicles(), geometries);

        Set<Id<TransitStopFacility>> stopsInZone = transitRouteTrimmer.getStopsInZone();

        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();


        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SplitRoute);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        Vehicles vehiclesNew = transitRouteTrimmer.getVehicles();

        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNew, scenario.getNetwork());
        System.out.println(validationResult.getErrors());


        TransitRouteTrimmerUtils.transitSchedule2ShapeFile(transitScheduleNew, outputPath + "output-trimmed-routes.shp", epsgCode);
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "optimizedSchedule_nonSB-bus-split-at-hubs.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "optimizedVehicles_nonSB-bus-split-at-hubs.xml.gz");


    }


}
