package org.matsim.run;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MosaikRunnerWithRoadPricing {

    private static final Logger log = LogManager.getLogger(MosaikRunnerWithRoadPricing.class);

    public static void main(String[] args) throws IOException {

        var emissionConfig = new EmissionsConfigGroup();
        var rpConfigGroup = new Mosaik2ConfigGroup();
        var config = RunBerlinScenario.prepareConfig(args, emissionConfig, rpConfigGroup);

        log.info("Running with scale factor " + rpConfigGroup.factor);

        config.global().setCoordinateSystem("EPSG:25833");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        var scenario = RunBerlinScenario.prepareScenario(config);

        for (Link link : scenario.getNetwork().getLinks().values()) {

            double freespeed;

            if (link.getFreespeed() <= 13.888889) {
                freespeed = link.getFreespeed() * 2;
                // for non motorway roads, the free speed level was reduced
            } else {
                freespeed = link.getFreespeed();
                // for motorways, the original speed levels seems ok.
            }

            if (freespeed <= 8.333333333) { //30kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
            } else if (freespeed <= 11.111111111) { //40kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
            } else if (freespeed <= 13.888888889) { //50kmh
                double lanes = link.getNumberOfLanes();
                if (lanes <= 1.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
                } else if (lanes <= 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
                } else if (lanes > 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
                } else {
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if (freespeed <= 16.666666667) { //60kmh
                double lanes = link.getNumberOfLanes();
                if (lanes <= 1.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
                } else if (lanes <= 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
                } else if (lanes > 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
                } else {
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if (freespeed <= 19.444444444) { //70kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
            } else if (freespeed <= 22.222222222) { //80kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
            } else if (freespeed > 22.222222222) { //faster
                link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
            } else {
                throw new RuntimeException("Link not considered...");
            }
        }

        Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
        var carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
        Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);
        VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId);

        EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology(carEngineInformation, "average");
        VehicleUtils.setHbefaSizeClass(carEngineInformation, "average");
        VehicleUtils.setHbefaEmissionsConcept(carEngineInformation, "average");

        EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(freightEngineInformation, "average");
        VehicleUtils.setHbefaSizeClass(freightEngineInformation, "average");
        VehicleUtils.setHbefaEmissionsConcept(freightEngineInformation, "average");

        Id<VehicleType> bikeVehicleTypeId = Id.create("bike", VehicleType.class);
        if (scenario.getVehicles().getVehicleTypes().containsKey(bikeVehicleTypeId)) {
            // bikes don't have emissions
            VehicleType bikeVehicleType = scenario.getVehicles().getVehicleTypes().get(bikeVehicleTypeId);
            EngineInformation bikeEngineInformation = bikeVehicleType.getEngineInformation();
            VehicleUtils.setHbefaVehicleCategory(bikeEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
            VehicleUtils.setHbefaTechnology(bikeEngineInformation, "average");
            VehicleUtils.setHbefaSizeClass(bikeEngineInformation, "average");
            VehicleUtils.setHbefaEmissionsConcept(bikeEngineInformation, "average");
        }

        // public transit vehicles should be considered as non-hbefa vehicles
        for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
            EngineInformation engineInformation = type.getEngineInformation();
            VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
            VehicleUtils.setHbefaTechnology(engineInformation, "average");
            VehicleUtils.setHbefaSizeClass(engineInformation, "average");
            VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
        }

        RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario);
        RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
        RoadPricingUtils.setName(scheme, "Toll_from_PALM");
        RoadPricingUtils.setDescription(scheme, "Tolls are calculated from concentrations retrieved from a PALM simulation run.");
        var timeFactors = createTimeFactors();
        var costFactors = createCostFactors();
        var tollLinks = getTollLinks(rpConfigGroup, scenario.getNetwork());
        var emissionPerMeter = createEmissionPerMeter();

        var path = Paths.get(config.controler().getOutputDirectory()).resolve("tolls.csv");
        log.info("writing tolls to: " + path);
        try (var writer = Files.newBufferedWriter(path); var p = CSVFormat.DEFAULT.withFirstRecordAsHeader().withHeader("time", "toll [€/m]").print(writer)) {
            for (var bin : timeFactors.getTimeBins()) {

                var time = bin.getStartTime();
                var tollPerMeter = bin.getValue().entrySet().stream()
                        .mapToDouble(entry -> costFactors.get(entry.getKey()) * emissionPerMeter.get(entry.getKey()) * entry.getValue())
                        .sum();
                p.printRecord(time, tollPerMeter);

                for (var id : tollLinks) {
                    var link = scenario.getNetwork().getLinks().get(id);
                    var toll = link.getLength() * tollPerMeter;
                    RoadPricingUtils.addLinkSpecificCost(scheme, id, time, time + timeFactors.getBinSize(), toll);
                }
            }
        }

        var controler = RunBerlinScenario.prepareControler(scenario);
        controler.addOverridingModule(new RoadPricingModule());

        controler.run();
    }

    /**
     * Creates average emission [g/m]. Values are taken from HBEFA https://www.hbefa.net/e/index.html with Parameters
     * Passenger Car, Regulated, 2020, Emissioncat::Details,Fuel::Aggregated
     * <p>
     * Values are divided by 1000 to konvert [g/Vehkm] into [g/Vehm]
     *
     * @return
     */
    private static Map<Pollutant, Double> createEmissionPerMeter() {
        log.info("Creating emission per meter");
        return Map.of(
                Pollutant.PM, 0.002 / 1000,
                Pollutant.PM_non_exhaust, 0.002 / 1000,
                Pollutant.NOx, 0.338 / 1000
        );
    }

    /**
     * creates cost factors in euro/g according to
     * <a href="https://paperpile.com/app/p/9cd641c8-bb39-0cf0-a9ab-9b4fe386282c">Handbook on the external costs of transport
     * </a>, version 2019 table 14
     */
    private static Map<Pollutant, Double> createCostFactors() {
        log.info("Creating cost factors.");
        return Map.of(
                Pollutant.PM, 39.6 / 1000,
                Pollutant.PM_non_exhaust, 39.6 / 1000,
                Pollutant.NOx, 36.8 / 1000
        );
    }

    /**
     * reads in hourly factors from resources folder.
     */
    private static TimeBinMap<Map<Pollutant, Double>> createTimeFactors() {
        TimeBinMap<Map<Pollutant, Double>> result = new TimeBinMap<>(3600);

        readTable("nox_hourly_factors.csv", record -> {
            var time = Double.parseDouble(record.get("time"));
            var factor = Double.parseDouble(record.get("factor"));
            var bin = result.getTimeBin(time);
            if (!bin.hasValue()) {
                bin.setValue(new HashMap<>());
            }
            bin.getValue().put(Pollutant.NOx, factor);
        });

        readTable("pm10_hourly_factors.csv", record -> {
            var time = Double.parseDouble(record.get("time"));
            var factor = Double.parseDouble(record.get("factor"));
            var bin = result.getTimeBin(time);
            if (!bin.hasValue()) {
                bin.setValue(new HashMap<>());
            }
            bin.getValue().put(Pollutant.PM, factor);
            bin.getValue().put(Pollutant.PM_non_exhaust, factor);
        });

        return result;
    }

    private static Set<Id<Link>> getTollLinks(Mosaik2ConfigGroup config, Network network) {

        if (config.tollArea == null) return network.getLinks().keySet();

        var fact = new PreparedGeometryFactory();
        var geometry = ShapeFileReader.getAllFeatures(config.tollArea).stream()
                .map(f -> (Geometry) f.getDefaultGeometry())
                .map(fact::create)
                .findAny()
                .orElseThrow();

        return network.getLinks().values().parallelStream()
                .filter(l -> geometry.covers(MGC.coord2Point(l.getFromNode().getCoord())) && geometry.covers(MGC.coord2Point(l.getToNode().getCoord())))
                .map(Identifiable::getId)
                .collect(Collectors.toSet());
    }

    public static void readTable(String table, Consumer<CSVRecord> recordConsumer) {

        log.info("Loading table: " + table);
        var csvFormat = CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader();

        try (var is = MosaikRunnerWithRoadPricing.class.getClassLoader().getResourceAsStream(table); var parser = CSVParser.parse(Objects.requireNonNull(is), Charset.defaultCharset(), csvFormat)) {
            for (var record : parser) {
                recordConsumer.accept(record);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Mosaik2ConfigGroup extends ReflectiveConfigGroup {

        public static String GROUP_NAME = "m2";

        private double factor = 1.0;

        private String tollArea;

        @StringSetter("factor")
        public void setFactor(String factor) {
            this.factor = Double.parseDouble(factor);
        }

        @StringGetter("factor")
        public String getFactor() {
            return Double.toString(factor);
        }

        @StringSetter("tollArea")
        public void setTollArea(String value) {
            this.tollArea = value;
        }

        @StringGetter("tollArea")
        public String getTollArea() {
            return tollArea;
        }

        public void setFactor(double factor) {
            this.factor = factor;
        }

        public Mosaik2ConfigGroup() {
            super(GROUP_NAME);
        }
    }
}
