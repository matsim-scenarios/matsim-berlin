package org.matsim.prepare.emission;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

import java.util.Random;

public class CreateEmissionVehicles {
    public static void run() {
        final String workingDirectory="C:\\Users\\anton\\OneDrive\\uni\\MATSim\\HW2\\cluster output\\";
        final String version="nullfall\\";
        final String plansFilePath=workingDirectory+version+"berlin.output_plans.xml";
        final String vehicleOutputPath=workingDirectory+version+"emissionvehicles.xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(plansFilePath);

        Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory factory = vehicles.getFactory();
        //create average car with average emissions
        VehicleType averageCarType = factory.createVehicleType(Id.create("car_average", VehicleType.class));
        averageCarType.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;average;average;averageEND_EMISSIONS");
        vehicles.addVehicleType(averageCarType);
        //TODO: Vehicle Types

        //every person travelling by car is using the average car
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Vehicle vehicle = factory.createVehicle(Id.create(person.getId().toString(), Vehicle.class), averageCarType);
            vehicles.addVehicle(vehicle);
        }
        new MatsimVehicleWriter(vehicles).writeFile(vehicleOutputPath);
    }

    public static void main(String[] args) {
        CreateEmissionVehicles.run();
    }
}
