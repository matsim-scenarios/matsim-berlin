package org.matsim.run;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

public class MosaikRunnerWithRoadPricing {

    private static final Logger log = LogManager.getLogger(MosaikRunnerWithRoadPricing.class);

    public static void main(String[] args) {

        var emissionConfig = new EmissionsConfigGroup();
        var rpConfigGroup = new RoadPricingConfigGroup();
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

        var tollHandler = new EmissionTollHandler(createCostFactors(), createTimeFactors(), rpConfigGroup.factor);

        var controler = RunBerlinScenario.prepareControler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
                addEventHandlerBinding().toInstance(tollHandler);
            }
        });

        controler.run();
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

    private static class EmissionTollHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler {

        private final Map<Pollutant, Double> costFactors;
        private final TimeBinMap<Map<Pollutant, Double>> timeFactors;
        private final double scaleFactor;
        private final Map<Id<Vehicle>, Id<Person>> vehicle2Person = new HashMap<>();
        private final Set<Id<Person>> td = new HashSet<>();

        @Inject
        private EventsManager events;

        private EmissionTollHandler(Map<Pollutant, Double> costFactors, TimeBinMap<Map<Pollutant, Double>> timeFactors, double scaleFactor) {
            this.costFactors = costFactors;
            this.timeFactors = timeFactors;
            this.scaleFactor = scaleFactor;
        }

        @Override
        public void handleEvent(ColdEmissionEvent event) {
            handle(event.getTime(), event.getVehicleId(), event.getColdEmissions());
        }

        @Override
        public void handleEvent(WarmEmissionEvent event) {
            handle(event.getTime(), event.getVehicleId(), event.getWarmEmissions());
        }

        private void handle(double time, Id<Vehicle> vehId, Map<Pollutant, Double> emissions) {

            var personId = vehicle2Person.get(vehId);

            // in case of pt vehicle for example
            if (personId == null) return;

            var amount = emissions.entrySet().stream()
                    .filter(entry -> costFactors.containsKey(entry.getKey()))
                    .mapToDouble(entry -> calculateAmount(time, entry.getKey(), entry.getValue()))
                    .sum();

            events.processEvent(new PersonMoneyEvent(time, personId, -amount, "hot spot mitigation", "government"));
        }

        private double calculateAmount(double time, Pollutant pollutant, double emission) {
            var costFactor = costFactors.get(pollutant);
            var timeFactor = timeFactors.getTimeBin(time).hasValue() ?
                    timeFactors.getTimeBin(time).getValue().get(pollutant) : 1.0;
            return emission * costFactor * timeFactor * scaleFactor;
        }

        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            if (!td.contains(event.getPersonId()))
                vehicle2Person.put(event.getVehicleId(), event.getPersonId());
        }

        @Override
        public void handleEvent(PersonLeavesVehicleEvent event) {
            vehicle2Person.remove(event.getVehicleId());
        }

        @Override
        public void handleEvent(TransitDriverStartsEvent event) {
            td.add(event.getDriverId());
        }
    }

    private static class RoadPricingConfigGroup extends ReflectiveConfigGroup {

        public static String GROUP_NAME = "rp";

        private double factor = 1.0;

        @StringSetter("factor")
        public void setFactor(String factor) {
            this.factor = Double.parseDouble(factor);
        }

        @StringGetter("factor")
        public String getFactor() {
            return Double.toString(factor);
        }

        public void setFactor(double factor) {
            this.factor = factor;
        }

        public RoadPricingConfigGroup() {
            super(GROUP_NAME);
        }
    }
}
