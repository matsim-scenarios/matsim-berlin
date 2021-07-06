package org.matsim.run.ev;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.routing.EvNetworkRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.urbanEV.EVUtils;
import org.matsim.urbanEV.UrbanEVConfigGroup;
import org.matsim.urbanEV.UrbanEVModule;
import org.matsim.urbanEV.UrbanVehicleChargingHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;

class RunUrbanEVBerlinScenario {

    @Inject
    private ChargingInfrastructureSpecification chargingInfrastructureSpecification;

    private static final Logger log = Logger.getLogger(RunBerlinScenario.class);

    public static void main(String[] args) {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[]{"scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-1pct.config-ev-test2.xml"};
        }

        Config config = RunBerlinScenario.prepareConfig(args);
        prepareConfig(config);
        Scenario scenario = RunBerlinScenario.prepareScenario(config);
        RunUrbanEVBerlinScenario.addVehicles(scenario);
        Controler controler = RunBerlinScenario.prepareControler(scenario);

//        config.controler().setOutputDirectory("./output-berlin-v5.5-1pct/evTest/1person");
        config.controler().setOutputDirectory("./output-berlin-v5.5-1pct/EVHomeChargers");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setLastIteration(3);


        //plug in UrbanEVModule
        controler.addOverridingModule(new UrbanEVModule());
        //register EV qsim components
        controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));

        controler.run();

    }


    public static Config prepareConfig(Config config) {

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
        EvConfigGroup evConfigGroup = new EvConfigGroup();
        evConfigGroup.setTimeProfiles(true);
        config.addModule(evConfigGroup);
        // Config config = ConfigUtils.loadConfig("C:/Users/admin/IdeaProjects/matsim-berlin/scenarios/berlin-v5.5-1pct/input/ev/berlin-v5.5-1pct.config-ev-test2.xml", new EvConfigGroup());
        UrbanEVConfigGroup urbanEVConfigGroup = new UrbanEVConfigGroup();
        config.addModule(urbanEVConfigGroup);

        //register charging interaction activities for car
        config.planCalcScore().addActivityParams(
                new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.car + UrbanVehicleChargingHandler.PLUGOUT_INTERACTION)
                        .setScoringThisActivityAtAll(false));
        config.planCalcScore().addActivityParams(
                new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.car + UrbanVehicleChargingHandler.PLUGIN_INTERACTION)
                        .setScoringThisActivityAtAll(false));
        config.qsim().setUsePersonIdForMissingVehicleId(true);
        config.strategy().setFractionOfIterationsToDisableInnovation(0);

        return config;
    }


    public static Scenario addVehicles(Scenario scenario) {

        QSimConfigGroup qSimConfigGroup = scenario.getConfig().qsim();
        VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();


        Set<String> modes = new HashSet<>(qSimConfigGroup.getMainModes());
        modes.add(TransportMode.walk);
        modes.add(TransportMode.pt);
        modes.add("bicycle");
        modes.addAll(qSimConfigGroup.getSeepModes());
        modes.addAll(scenario.getConfig().plansCalcRoute().getNetworkModes());



        for (Person person : scenario.getPopulation().getPersons().values()) {


            Map<String, Id<Vehicle>> mode2VehicleId = new HashMap<>();





            if (person.getId().toString().contains("freight") || PopulationUtils.getPersonAttribute(person, "home-activity-zone").equals("brandenburg")) {

                VehicleType evVehicleType = vehiclesFactory.createVehicleType(Id.create(person.getId().toString(), VehicleType.class));
                //Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, TransportMode.car);
                Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString());
                Vehicle vehicle = vehiclesFactory.createVehicle(vehicleId, evVehicleType);
                scenario.getVehicles().addVehicleType(evVehicleType);
                scenario.getVehicles().addVehicle(vehicle);
                mode2VehicleId.put(TransportMode.car, vehicleId);
            }

            else {


                VehicleType evVehicleType = vehiclesFactory.createVehicleType(Id.create(person.getId().toString(), VehicleType.class));
                //Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, TransportMode.car);
                Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString());
               // VehicleUtils.createVehiclesContainer().addVehicleType(evVehicleType);
                VehicleUtils.setHbefaTechnology(evVehicleType.getEngineInformation(), "electricity");
                VehicleUtils.setEnergyCapacity(evVehicleType.getEngineInformation(), 60);
                EVUtils.setInitialEnergy(evVehicleType.getEngineInformation(), 50);
                EVUtils.setChargerTypes(evVehicleType.getEngineInformation(), Arrays.asList("3.7kW", "11kW", "22kW", "50kW", "150kW", "44kW","43kW", "110kW", "93kW", "39.6kW", "88kW", "28kW", "100kW", "75kW", "45kW", "110kW", "350kW", "320kW", person.getId().toString()));

                Vehicle vehicle = vehiclesFactory.createVehicle(vehicleId, evVehicleType);
                scenario.getVehicles().addVehicleType(evVehicleType);
                scenario.getVehicles().addVehicle(vehicle);
                mode2VehicleId.put(TransportMode.car, vehicleId);
            }

            for (String mode : modes) {
                if (!mode.equals("car")) {

                    VehicleType type = vehiclesFactory.createVehicleType(Id.create(person.getId().toString() + "_" + mode, VehicleType.class));
                    scenario.getVehicles().addVehicleType(type);
                   // VehicleUtils.createVehiclesContainer().addVehicleType(type);
                    //scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class));
                    Vehicle vehicle1 = vehiclesFactory.createVehicle(Id.createVehicleId(person.getId().toString() + "_" + mode), type);
                    scenario.getVehicles().addVehicle(vehicle1);
                    mode2VehicleId.put(mode, vehicle1.getId());
                }

            }

            VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2VehicleId);

        }
        return scenario;

    }
}