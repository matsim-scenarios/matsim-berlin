package org.matsim.prepare.ptRouteTrim;

import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RunTransitRouteTrimmer {
    public static void main(String[] args) throws IOException, SchemaException {

        final String inScheduleFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedSchedule.xml.gz";
        final String inVehiclesFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedVehicles.xml.gz";
        final String inNetworkFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedNetwork.xml.gz";
        final String zoneShpFile = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/shp-files/Gladbeck_area_b_en_detail_bus_hubs_Schnellbus_cut_out.shp";
        final String outputPath = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/";
        final String epsgCode = "25832";


        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(inScheduleFile);
        config.network().setInputFile(inNetworkFile);
        config.vehicles().setVehiclesFile(inVehiclesFile);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        Set<String> modes2Trim = new HashSet<>();
        modes2Trim.add("bus");
        Set<Id<TransitLine>> linesToModify = TransitRouteTrimmerUtils.filterTransitLinesForMode(transitSchedule.getTransitLines().values(), modes2Trim);

//        Set<Id<TransitLine>> linesToModify = new HashSet<>(transitSchedule.getTransitLines().keySet());

        Set<Id<TransitLine>> linesSB = transitSchedule.getTransitLines().values().stream()
                .filter(v -> v.getId().toString().contains("SB"))
                .map(v -> v.getId())
                .collect(Collectors.toSet()
                );

        linesToModify.removeAll(linesSB);

        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new File(zoneShpFile).toURI().toURL());

        System.out.println("\n Modify Routes: SplitRoute");
        TransitRouteTrimmer transitRouteTrimmer = new TransitRouteTrimmer(scenario.getTransitSchedule(), scenario.getVehicles(), geometries);
        transitRouteTrimmer.modifyTransitLinesFromTransitSchedule(linesToModify, TransitRouteTrimmer.modMethod.SplitRoute);
        TransitSchedule transitScheduleNew = transitRouteTrimmer.getTransitScheduleNew();
        Vehicles vehiclesNew = transitRouteTrimmer.getVehicles();

//        TransitSchedule tScleaned = TransitScheduleCleaner.removeStopsNotUsed(transitScheduleNew);
        TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(transitScheduleNew, scenario.getNetwork());
        System.out.println(validationResult.getErrors());


        TransitRouteTrimmerUtils.transitSchedule2ShapeFile(transitScheduleNew, outputPath + "output-trimmed-routes.shp",epsgCode);
        new TransitScheduleWriter(transitScheduleNew).writeFile(outputPath + "optimizedSchedule_nonSB-bus-split-at-hubs.xml.gz");
        new MatsimVehicleWriter(vehiclesNew).writeFile(outputPath + "optimizedVehicles_nonSB-bus-split-at-hubs.xml.gz");

    }
}
