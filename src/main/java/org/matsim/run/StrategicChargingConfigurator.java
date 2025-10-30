package org.matsim.run;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.contrib.ev.strategic.analysis.ChargerTypeAnalysisListener;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters.TariffParameters;
import org.matsim.contrib.ev.strategic.replanning.innovator.RandomChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoringParameters;
import org.matsim.contrib.ev.withinday.WithinDayEvUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class shows how to set up a MATSim scenario for simulation with the
 * Strategic Electric Vehicle Charging (SEVC) extension.
 * 
 * See below for a list of options in the Settings object that needs to be
 * passed to the configurator.
 * 
 * The script is intended to be called directly after loading your scenario
 * on-the-fly in the code, but you can also write out the files via the command
 * line using the main() method of this class.
 * 
 * After all, it is meant to be an example and as a starting point for your own
 * configuration scripts.
 * 
 * The example configurator will take any baseline scenario and do the following
 * steps:
 * 
 * Persons:
 * - Find eligible persons (using the car at least once, having a home acitvity)
 * - Configure those persons to make use of SEVC
 * - Assign a minimum desired SoC during the day and for the end of the day
 * 
 * Vehicles:
 * - Register a new vehicle type (electric)
 * - Create one vehicle for each selected person and assign it as the "car"
 * vehicle
 * - Update the plans to take into account this vehicle instead of the initial
 * one
 * - Assign an initial SoC for each vehicle
 * - Set a maximum SoC for each vehicle up to which the persons would like to
 * charge it
 * 
 * Chargers:
 * - Create a home charger for a specified percentage of the population
 * - Create work chargers dependent on the nubmer of employees performing "work"
 * activities at every facility
 * - Create a specified number of public chargers randomly in the network
 * 
 * Tariffs:
 * - Create a tariff for each of home, work, and public chargers
 * 
 * Configuration:
 * - Update the configuration so that SEVC is taken into account.
 * - Note that the scenario will be configured as "SEVC only", all other
 * replanning strategies will be disabled
 * 
 * What remains for you is then to activate SEVC in the controller, which can be
 * done using
 * 
 * StrategicChargingUtils.configureController(controller)
 * 
 * Furthermore, you need to activate the EV contrib by adding the config group:
 * 
 * config.addModule(new EvConfigGroup());
 * 
 * and adding the module to the controller:
 * 
 * controller.addOverridingModule(new EvModule());
 * 
 */
public class StrategicChargingConfigurator {
    private static final Logger logger = LogManager.getLogger(StrategicChargingConfigurator.class);

    static public class Settings {
        // a random seed to generate varying scenarios
        public int seed = 1000;

        // the rate of persons that have an electric car among the eligible ones
        public double ownershipRate = 0.2;

        // desired minimum soc (uniform across all users)
        public double minimumSoc = 0.2;

        // desired minimum soc at the end of the day (uniform across all users)
        public double minimumEndOfDaySoc = 0.5;

        // battery capacity of the electric vehicles (uniform across all evs)
        public double batteryCapacity_kWh = 75.0;

        // initial soc at the beginning of the day (uniform across all evs)
        public double initialSoc = 0.7;

        // maximum soc until which a vehicle is charged (uniform across all evs)
        public double maximumSoc = 0.9;

        // percentage of ev users that have a charger at home
        public double homeChargerRate = 0.4;

        // charging power of home chargers (uniform overall home chargers)
        public double homePlugPower_kW = 7.0;

        // cost per energy consumption of a home charger
        public double homeCostPerEnergy_kWh = 0.3;

        // minimum number of persons working at a facility so it receives work chargers
        public int workMinimumEmployees = 20;

        // number of plugs generated per number of employees at each eligible facility
        public double workPlugsPerEmployee = 0.2;

        // power of work chargers
        public double workPlugPower_kW = 12.0;

        // number of public chargers to be created randomly in the network
        public int publicChargerCount = 1000;

        // number of plugs per created charger
        public int publicChargerPlugs = 3;

        // power of public chargers (uniform overall public chargers)
        public double publicChargerPower_kW = 18.0;

        // cost per hour charged when using a public charger
        public double publicCostPerDuration_h = 5.0;

        // number of ev users holding a special tariff subscription for public chargers
        public double subscriptionRate = 0.2;

        // number of public chargers being eligible for special tariff charging
        public double availabilityRate = 0.7;

        // cost per hour for the special charging tariff
        public double specialTariffCost_h = 2.0;
    }

    private final Settings settings;
    private final List<String> report;

    public StrategicChargingConfigurator(Settings settings, List<String> report) {
        this.settings = settings;
        this.report = report;
    }

    public StrategicChargingConfigurator(Settings settings) {
        this.settings = settings;
        this.report = new LinkedList<>();
    }

    /**
     * This is the main method that will configure EV users, the vehicles, create
     * the charging infrastructure and adjust the configuration.
     */
    public ChargingInfrastructureSpecification apply(Scenario scenario) {
        Random random = new Random(settings.seed);

        /*
         * PROCESS PERSONS
         * 
         * - We randomly select persons (that use the car once during the day)
         * - We enable them for electric charging according to the ownershipRate
         * - We configure them so that they are used by SEVC
         */

        Population population = scenario.getPopulation();
        List<Person> persons = findRelevantPersons(population);

        int numberOfUsers = 0;
        for (Person person : persons) {
            if (random.nextDouble() < settings.ownershipRate) { // only select a few
                // activate for dynamic charging
                WithinDayEvUtils.activate(person);

                // set the minimum SoC at any time and the end of the day that are used in
                // scoring per person
                StrategicChargingUtils.setMinimumSoc(person, settings.minimumSoc);
                StrategicChargingUtils.setMinimumEndSoc(person, settings.minimumEndOfDaySoc);

                numberOfUsers++;
            }
        }

        report.add(String.format("Number of electric vehicle users: %d", numberOfUsers));

        /*
         * PROCESS VEHICLES
         * 
         * - We assume here that there is no electric vehicle type in the scenario
         * - We generate a new electric vehicle type
         * - We assign a new electric vehicle to every agent and make it their "car"
         */

        Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory vehiclesFactory = vehicles.getFactory();

        // create vehicle type
        VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.create("sevc:electric", VehicleType.class));
        vehicles.addVehicleType(vehicleType);

        vehicleType.setNetworkMode(TransportMode.car); // these are cars

        // make the vehicle type electric
        VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(),
                ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);

        // set the energy capacity in kWh
        VehicleUtils.setEnergyCapacity(vehicleType.getEngineInformation(), settings.batteryCapacity_kWh);

        // now create a vehicle per person
        for (Person person : persons) {
            if (WithinDayEvUtils.isActive(person)) {
                Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId("sevc:" + person.getId().toString()),
                        vehicleType);
                vehicles.addVehicle(vehicle);

                // set the initial SoC of the electric vehicle
                ElectricFleetUtils.setInitialSoc(vehicle, settings.initialSoc);

                // set the maximum SoC up to which the person will charge (this is optional, 1.0
                // is assumed)
                StrategicChargingUtils.setMaximumSoc(vehicle, settings.maximumSoc);

                // make sure this vehicle is used by this person for the transport mode "car"
                setVehicleId(person, vehicle.getId());

                // cleaning up the plans
                updateVehicleInPlans(person, vehicle.getId());
            }
        }

        /*
         * PROCESS HOME CHARGERS
         * 
         * - we give home chargers to persons based on the homeChargerRate
         */

        ChargingInfrastructureSpecificationDefaultImpl infrastructure = new ChargingInfrastructureSpecificationDefaultImpl();

        int numberOfHomeChargers = 0;
        for (Person person : persons) {
            if (WithinDayEvUtils.isActive(person)) {
                if (random.nextDouble() < settings.homeChargerRate) { // person gets a home charger
                    // describe the charger
                    ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                            .id(Id.create("sevc:home:" + person.getId().toString(), Charger.class)) // ,
                            .linkId(getHomeLinkId(person)) //
                            .chargerType("home") // only for analysis, no logical meaning
                            .plugPower(settings.homePlugPower_kW * 1e3) //
                            .plugCount(1) //
                            .build();

                    // reserve this charger for the person
                    StrategicChargingUtils.assignChargerPersons(charger, Collections.singleton(person.getId()));

                    // add it to the infrastructure
                    infrastructure.addChargerSpecification(charger);

                    // for aggregated analysis
                    ChargerTypeAnalysisListener.addAnalysisType(charger, "home");

                    numberOfHomeChargers++;
                }
            }
        }

        report.add(String.format("Number of home chargers: %d", numberOfHomeChargers));

        /*
         * PROCESS WORK CHARGERS
         * 
         * - we count the number of persons working at each facility
         * - we select all facilities that have more than workMinimumEmployees
         * - we assign to each workplace plugsPerEmployee plugs
         */

        Network roadNetwork = NetworkUtils.createNetwork(scenario.getConfig());
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

        ActivityFacilities facilities = scenario.getActivityFacilities();

        int numberOfWorkChargers = 0;
        int maximumNumberOfWorkPlugs = 0;

        for (var entry : countEmployees(population, facilities).entrySet()) {
            int employees = entry.getValue();

            if (employees >= settings.workMinimumEmployees) {
                ActivityFacility facility = facilities.getFacilities().get(entry.getKey());
                int plugs = (int) Math.floor(employees * settings.workPlugsPerEmployee);

                // find the link id
                Id<Link> linkId = facility.getLinkId();

                if (linkId == null) {
                    linkId = NetworkUtils.getNearestLink(roadNetwork, facility.getCoord()).getId();
                }

                // describe the charger
                ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                        .id(Id.create("sevc:work:" + facility.getId().toString(), Charger.class)) // ,
                        .linkId(facility.getLinkId()) //
                        .chargerType("work") // only for analysis, no logical meaning
                        .plugPower(settings.workPlugPower_kW * 1e3) //
                        .plugCount(plugs) // dependent on employee count
                        .build();

                // reserve this charger for activities at the specific facility
                StrategicChargingUtils.assignChargerFacilities(charger, Collections.singleton(facility.getId()));

                // add it to the infrastructure
                infrastructure.addChargerSpecification(charger);

                // for aggregated analysis
                ChargerTypeAnalysisListener.addAnalysisType(charger, "work");

                numberOfWorkChargers++;
                maximumNumberOfWorkPlugs = Math.max(maximumNumberOfWorkPlugs, plugs);
            }
        }

        report.add(String.format("Number of work chargers: %d", numberOfWorkChargers));
        report.add(String.format("Number of plugs at largest work place: %d", maximumNumberOfWorkPlugs));

        /*
         * PROCESS PUBLIC CHARGERS
         * 
         * - we distribute plugChargerCount chargers randomly in the network
         */

        // make a list of all links and shuffle it
        List<Link> links = new LinkedList<>(roadNetwork.getLinks().values());
        Collections.shuffle(links, random);

        for (int k = 0; k < settings.publicChargerCount; k++) { // create N chargers for the first N links
            Link link = links.get(k);

            // describe the charger
            ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                    .id(Id.create("sevc:public:" + k, Charger.class)) // ,
                    .linkId(link.getId()) //
                    .chargerType("public") // only for analysis, no logical meaning
                    .plugPower(settings.publicChargerPower_kW * 1e3) //
                    .plugCount(settings.publicChargerPlugs) //
                    .build();

            // make this a selectable public charger
            StrategicChargingUtils.assignPublic(charger, true);

            // for aggregated analysis
            ChargerTypeAnalysisListener.addAnalysisType(charger, "public");

            // add it to the infrastructure
            infrastructure.addChargerSpecification(charger);
        }

        report.add(String.format("Number of public chargers: %d", settings.publicChargerCount));

        /*
         * PROCESS CONFIGURATION
         * 
         * - we add the relevant config groups
         * - we define the standard charging scoring parameters
         * - we configure random innovation for charging plans
         */

        // add the relevant config groups and update scoring
        // also, it removes any other replanning strategies
        StrategicChargingUtils.configureStandalone(scenario.getConfig());

        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(scenario.getConfig());

        // add standard scoring parameters
        ChargingPlanScoringParameters scoringParameters = new ChargingPlanScoringParameters();
        sevcConfig.addParameterSet(scoringParameters);

        // random innovation strategy
        RandomChargingPlanInnovator.Parameters innovationParameters = new RandomChargingPlanInnovator.Parameters();
        sevcConfig.addParameterSet(innovationParameters);

        /*
         * PROCESS COST STRUCTURES
         * 
         * - cost structures can be set globally (for all chargers), per charger (as
         * attributes) or for groups of chargers using "tariffs"
         * - here, we create one tariff for each type of charger
         */

        TariffBasedChargingCostsParameters tariffs = new TariffBasedChargingCostsParameters();
        sevcConfig.addParameterSet(tariffs);

        // we create the home tariff
        TariffParameters homeTariff = new TariffParameters();
        homeTariff.setTariffName("home");
        homeTariff.setCostPerEnergy_kWh(settings.homeCostPerEnergy_kWh); // cost per kWh
        tariffs.addParameterSet(homeTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("home")) {
                StrategicChargingUtils.addTariff(charger, "home");
            }
        }

        // we create the work tariff
        TariffParameters workTariff = new TariffParameters(); // keep "for free"
        workTariff.setTariffName("work");
        tariffs.addParameterSet(workTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("work")) {
                StrategicChargingUtils.addTariff(charger, "work");
            }
        }

        // we create the public tariff
        TariffParameters publicTariff = new TariffParameters();
        publicTariff.setTariffName("public");
        publicTariff.setCostPerDuration_min(settings.publicCostPerDuration_h / 60.0);
        tariffs.addParameterSet(publicTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("public")) {
                StrategicChargingUtils.addTariff(charger, "public");
            }
        }

        return infrastructure;
    }

    /**
     * This is an optional method that will create a special subscription tariff for
     * certain registered users with a favorable tariff.
     */
    public void applySubscriptions(Scenario scenario, ChargingInfrastructureSpecification infrastructure) {
        Random random = new Random(settings.seed * 55);

        // get the tariffs
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(scenario.getConfig());
        TariffBasedChargingCostsParameters tariffs = (TariffBasedChargingCostsParameters) sevcConfig
                .getCostParameters();

        // we add a new special tariff for public chargers
        TariffParameters specialTariff = new TariffParameters();
        specialTariff.setTariffName("special");
        specialTariff.setCostPerDuration_min(settings.specialTariffCost_h / 60.0);
        specialTariff.setSubscriptions(Set.of("special_subscription")); // only accessible with this subscription
        tariffs.addParameterSet(specialTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("public")) {
                if (random.nextDouble() < settings.availabilityRate) {
                    StrategicChargingUtils.addTariff(charger, "special");
                }
            }
        }

        // we add the subscription to a subset of the persons
        Population population = scenario.getPopulation();

        for (Person person : population.getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                if (random.nextDouble() < settings.subscriptionRate) {
                    // StrategicChargingUtils.addSubscription(person, "special_subscription");
                }
            }
        }
    }

    // THIS IS THE COMMAND LINE SCRIPT

    /*
     * The following parameters can be given to the command line script:
     * 
     * --config-path [path] defines the configuration file that will be updated
     * 
     * --settings-path [path] optionally points to a JSON script that will override
     * the default settings
     * 
     * --plans-path [path] optionally overrides the plans path in the config (in
     * case you want to apply the changes to an output plans file of an already
     * performed simulation)
     * 
     * This script will write out a config file that is prefixed with "sevc_". Other
     * files such as the population will also be prefixed and generated next to the
     * config file.
     */
    static public void main(String[] args)
            throws ConfigurationException, StreamReadException, DatabindException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .allowOptions("settings-path", "plans-path", "prefix") //
                .build();

        // load config and scenario
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));

        if (cmd.hasOption("plans-path")) {
            // in case input plans should be overridden
            config.plans().setInputFile(cmd.getOptionStrict("plans-path"));
        }

        // no need to load certain data sets
        String householdsInputPath = config.households().getInputFile();
        config.households().setInputFile(null);

        String transitScheduleInputPath = config.transit().getTransitScheduleFile();
        config.transit().setTransitScheduleFile(null);

        String transitVehiclesInputPath = config.transit().getVehiclesFile();
        config.transit().setVehiclesFile(null);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // load settings
        Settings settings = new Settings();

        if (cmd.hasOption("settings-path")) {
            // load from JSON if given
            settings = new ObjectMapper().readValue(new File(cmd.getOptionStrict("settings-path")), Settings.class);
        }

        // set up configurator
        List<String> report = new LinkedList<>();
        StrategicChargingConfigurator configurator = new StrategicChargingConfigurator(settings, report);

        // apply changes
        ChargingInfrastructureSpecification infrastructure = configurator.apply(scenario);
        configurator.applySubscriptions(scenario, infrastructure);

        // prepare writing
        String prefix = cmd.getOption("prefix").orElse("sevc");
        File parentPath = new File(cmd.getOptionStrict("config-path")).getParentFile();

        // write assets
        new PopulationWriter(scenario.getPopulation())
                .write(new File(parentPath, prefix + "_plans.xml.gz").toString());
        config.plans().setInputFile(prefix + "_plans.xml.gz");

        new MatsimVehicleWriter(scenario.getVehicles())
                .writeFile(new File(parentPath, prefix + "_vehicles.xml.gz").toString());
        config.vehicles().setVehiclesFile(prefix + "_vehicles.xml.gz");

        new ChargerWriter(infrastructure.getChargerSpecifications().values().stream())
                .write(new File(parentPath, prefix + "_chargers.xml.gz").toString());
        EvConfigGroup.get(config).setChargersFile(prefix + "_chargers.xml.gz");

        // reset ignored files
        config.households().setInputFile(householdsInputPath);
        config.transit().setTransitScheduleFile(transitScheduleInputPath);
        config.transit().setVehiclesFile(transitVehiclesInputPath);

        // write config
        new ConfigWriter(config).write(new File(parentPath, prefix + "_config.xml").toString());

        // reporting
        logger.info("SEVC CONFIGURATION REPORT");

        for (String line : report) {
            logger.info("  " + line);
        }
    }

    // BELOW THIS POINT ONLY HELPER METHODS

    private void setVehicleId(Person person, Id<Vehicle> vehicleId) {
        Map<String, Id<Vehicle>> vehicles = new HashMap<>();
        vehicles.putAll(VehicleUtils.getVehicleIds(person));
        vehicles.put(TransportMode.car, vehicleId);
        VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, vehicles);
    }

    private List<Person> findRelevantPersons(Population population) {
        List<Person> relevant = new LinkedList<>();

        for (Person person : population.getPersons().values()) {
            boolean foundCar = false;

            for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
                if (leg.getMode().equals(TransportMode.car)) {
                    foundCar = true;
                    break;
                }
            }

            boolean foundHome = false;

            for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                    StageActivityHandling.ExcludeStageActivities)) {
                if (activity.getType().startsWith("home")) {
                    foundHome = true;
                    break;
                }
            }

            if (foundCar && foundHome) {
                relevant.add(person);
            }
        }

        return relevant;
    }

    private Id<Link> getHomeLinkId(Person person) {
        for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                StageActivityHandling.ExcludeStageActivities)) {
            if (activity.getType().startsWith("home")) {
                return Objects.requireNonNull(activity.getLinkId());
            }
        }

        throw new IllegalStateException();
    }

    private IdMap<ActivityFacility, Integer> countEmployees(Population population, ActivityFacilities facilities) {
        IdMap<ActivityFacility, Integer> count = new IdMap<>(ActivityFacility.class);

        for (Person person : population.getPersons().values()) {
            for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                    StageActivityHandling.ExcludeStageActivities)) {
                if (activity.getType().startsWith("work")) {
                    Id<ActivityFacility> facilityId = activity.getFacilityId();

                    if (facilityId != null) {
                        count.compute(facilityId, (key, value) -> value == null ? 1 : value + 1);
                    }
                }
            }
        }

        return count;
    }

    private void updateVehicleInPlans(Person person, Id<Vehicle> vehicleId) {
        for (Plan plan : person.getPlans()) {
            for (Leg leg : TripStructureUtils.getLegs(plan)) {
                if (leg.getMode().equals(TransportMode.car)) {
                    ((NetworkRoute) leg.getRoute()).setVehicleId(vehicleId);
                }
            }
        }
    }
}
