package org.matsim.prepare.ptRouteTrim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransitUtilsJR {
    private static final Logger log = Logger.getLogger(TransitRouteTrimmer.class);

    public static Vehicles removeUnusedVehicles(Vehicles vehicles, TransitSchedule tS) {

        Scenario scenarioNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Vehicles vehiclesNew = scenarioNew.getVehicles();
        Set<Id<Vehicle>> vehIds = tS.getTransitLines().values().stream().
                flatMap(line -> line.getRoutes().values().stream().
                        flatMap(route -> route.getDepartures().values().stream()
                                .map(Departure::getVehicleId)))
                                .collect(Collectors.toSet());


        // Add all Vehicle Types
        for ( VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
            vehiclesNew.addVehicleType(vehicleType);
        }

        // Add only vehicles that are in use
        int vehicleDeleteCount = 0;
        for (Map.Entry<Id<Vehicle>, Vehicle> veh : vehicles.getVehicles().entrySet()) {
            if (vehIds.contains(veh.getKey())) {
                vehiclesNew.addVehicle(veh.getValue());
            } else {
                vehicleDeleteCount++;
            }
        }

        log.info(vehicleDeleteCount + " vehicles were removed");
        return vehicles;

    }

}
