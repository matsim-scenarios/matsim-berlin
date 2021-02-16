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
        final String epsgCode = "31468";
        String filename = "hub-100";
        int bufferRadius = 300; // based on maxBeelineWalkConnectionDistance in config
        int hubReach = 100;

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TransitSchedule transitScheduleOld = scenario.getTransitSchedule();

        // Collect all rail stations
        Collection<TransitStopFacility> allStations = transitScheduleOld.getFacilities().values();
        List<TransitStopFacility> railStations = allStations.stream()
                .filter(x -> x.getAttributes().getAsMap().containsKey("stopFilter"))
                .filter(x -> x.getAttributes().getAttribute("stopFilter").equals("station_S/U/RE/RB"))
                .collect(Collectors.toList());

        List<Id<TransitStopFacility>> railStationIds = railStations.stream().map(x -> x.getId()).collect(Collectors.toList());

        // Generate buffer geometry around rail stations
        GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();
        List<Geometry> bufferGeoList = new ArrayList<>();
        List<Geometry> railStopGeoList = new ArrayList<>();
        List<Geometry> busStopGeoList = new ArrayList<>();

        for (TransitStopFacility stop : railStations) {
            double x = stop.getCoord().getX();
            double y = stop.getCoord().getY();
            Coordinate coordinate = new Coordinate(x, y);
            Point point = GEOMETRY_FACTORY.createPoint(coordinate);
            Geometry buffer = buffer(point, bufferRadius);
            bufferGeoList.add(buffer);
        }

        Geometry railBufferGeo = GEOMETRY_FACTORY.buildGeometry(bufferGeoList).union();

        Map<Id<TransitStopFacility>,Coordinate > StopId2CoordMap = new HashMap<>();

        //Generate Geometries for all rail stations and all non-rail stations (assumed to be bus)
        for (TransitStopFacility stop : allStations) {
            Coordinate coordinate = new Coordinate(stop.getCoord().getX(), stop.getCoord().getY());
            Point point = GEOMETRY_FACTORY.createPoint(coordinate);
            StopId2CoordMap.put(stop.getId(),coordinate);
            if (railStationIds.contains(stop.getId())) {
                railStopGeoList.add(point);
            } else {
                busStopGeoList.add(point);
            }
        }

        Geometry busStopsAll = GEOMETRY_FACTORY.buildGeometry(busStopGeoList);
        Geometry railStopsGeo = GEOMETRY_FACTORY.buildGeometry(railStopGeoList);

        // Find bus stop geometries that are within rail buffer
        Geometry busStopsInBuffer = busStopsAll.intersection(railBufferGeo);

        // Find corresponding busStopFacilityIds
        List<Id<TransitStopFacility>> busStopsInBufferIds = new ArrayList<>();
        Coordinate[] coordinates = busStopsInBuffer.getCoordinates();

        for (Coordinate coord : coordinates) {
            busStopsInBufferIds.addAll(StopId2CoordMap.entrySet().stream().filter(x -> x.getValue().equals(coord)).map(x -> x.getKey()).collect(Collectors.toList()));
        }

        // Add hub attribute to bus stops within buffer of rail stops
        for (Id<TransitStopFacility> id : busStopsInBufferIds) {
            scenario.getTransitSchedule().getFacilities().get(id).getAttributes().putAttribute("hub", hubReach);
        }

        { // Write shape files for rail stops, buffers around rail stops, and bus stops within buffer
                    writeGeometryCollection2ShapeFile(outputPath + "railStopGeo-300", ShapeType.POINT,
                            (GeometryCollection) railStopsGeo);

                    writeGeometryCollection2ShapeFile(outputPath +"railBufferGeo-300", ShapeType.POLYGON,
                            (GeometryCollection) railBufferGeo);

                    writeGeometryCollection2ShapeFile(outputPath +"busStopsInBufferGeo-300", ShapeType.POINT,
                            (GeometryCollection) busStopsInBuffer);
        }

        // Split ROutes
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));
        System.out.println("\n Modify Routes: SplitRoute");
        TransitRouteTrimmer transitRouteTrimmer = new TransitRouteTrimmer(scenario.getTransitSchedule(), scenario.getVehicles(), geometries);

        Set<Id<TransitLine>> linesToModify = scenario.getTransitSchedule().getTransitLines().keySet();

        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SplitRoute);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        Vehicles vehiclesNew = transitRouteTrimmer.getVehicles();

        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNew, scenario.getNetwork());
        System.out.println(validationResult.getErrors());

        TransitRouteTrimmerUtils.transitSchedule2ShapeFile(transitScheduleNew, outputPath + "routes-" + filename + ".shp", epsgCode);
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "sched-" + filename + ".xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "vehs-" + filename + ".xml.gz");

    }

    private static void writeGeometryCollection2ShapeFile(String fileName, ShapeType shapeType, GeometryCollection geometryCollection) throws IOException {
        RandomAccessFile shp = new RandomAccessFile(fileName + ".shp", "rw");
        RandomAccessFile shx = new RandomAccessFile(fileName + ".shx", "rw");
        ShapefileWriter writer = new ShapefileWriter(shp.getChannel(), shx.getChannel());
        writer.write(geometryCollection, shapeType);
    }


}

