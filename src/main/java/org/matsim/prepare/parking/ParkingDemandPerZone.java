package org.matsim.analysis;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import picocli.CommandLine;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
        name = "analyze-parking-vehicles",
        description = "Analyze parking capacity based on parking vehicles"
)

public class ParkingDemandPerZone implements MATSimAppCommand {

    @CommandLine.Option(names = "--directory", description = "path to matsim output directory", required = true)
    private Path directory;

    @CommandLine.Option(names = "--time-bin", description = "Time bin size in seconds", defaultValue = "900")
    private int timeBinSize;

    @CommandLine.Mixin()
    private final ShpOptions shp = new ShpOptions();

    private static final Map<Id<Link>, Map<Integer, MutableInt>> parkingDemandPerLink = new HashMap<>();
    private static final Map<Id<Link>, Integer> maxNrOfVehiclesPerLink = new HashMap<>();

    private static final Logger log = LogManager.getLogger(ParkingDemandPerZone.class);

    public ParkingDemandPerZone() {
    }

    public static void main(String[] args) { new ParkingDemandPerZone().execute(args); }

    static class ParkedVehiclesCounter implements VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {

        private final Map<Id<Vehicle>, List<VehicleParkingData>> parkingTracker;
        private final Network network;

        ParkedVehiclesCounter(Map<Id<Vehicle>, List<VehicleParkingData>> parkingTracker, Network network) {
            this.parkingTracker = parkingTracker;
            this.network = network;
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            if(!event.getLinkId().toString().contains("pt_") && parkingTracker.containsKey(event.getVehicleId())
                    && network.getLinks().containsKey(event.getLinkId())) {
                VehicleParkingData vehicleParkingData = new VehicleParkingData(event.getLinkId(), event.getTime(), event.getEventType());
                parkingTracker.get(event.getVehicleId()).add(vehicleParkingData);
            }
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            if(!event.getLinkId().toString().contains("pt_") && parkingTracker.containsKey(event.getVehicleId())
                    && network.getLinks().containsKey(event.getLinkId())) {
                VehicleParkingData vehicleParkingData = new VehicleParkingData(event.getLinkId(), event.getTime(), event.getEventType());
                parkingTracker.get(event.getVehicleId()).add(vehicleParkingData);
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        Path networkPath = globFile(directory, "*output_network.*");
        Path eventsPath = globFile(directory, "*output_events.*");
        Path vehiclesPath = globFile(directory, "*output_vehicles.*");
        Path outputFolder = Path.of(directory.toString() + "/analysis-parking");

        int timeBins = 86400 / timeBinSize;

        if (!Files.exists(outputFolder)) {
            Files.createDirectory(outputFolder);
        }

        Network inputNetwork = NetworkUtils.readNetwork(networkPath.toString());
        Network network = NetworkUtils.createNetwork();

        if(shp.isDefined()) {
            Geometry analyzedArea = shp.getGeometry();
            GeometryFactory gf = new GeometryFactory();

            for(Link link : inputNetwork.getLinks().values()) {
                LineString line = gf.createLineString(new Coordinate[]{
                        MGC.coord2Coordinate(link.getFromNode().getCoord()),
                        MGC.coord2Coordinate(link.getToNode().getCoord())
                });
                boolean isInsideArea = line.intersects(analyzedArea);

                if(isInsideArea) {
                    if(!network.getNodes().containsKey(link.getFromNode().getId())) {
                        network.addNode(link.getFromNode());
                    }
                    if(!network.getNodes().containsKey(link.getToNode().getId())) {
                        network.addNode(link.getToNode());
                    }
                    network.addLink(link);
                }
            }

            log.info("Parked vehicles will be analyzed for links inside the given area only. shp-file: " + shp.getShapeFile());
        } else {
            network = inputNetwork;
            log.info("Parked vehicles will be analyzed for all network links.");
        }

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        MatsimVehicleReader vehicleReader = new MatsimVehicleReader(vehicles);
        vehicleReader.readFile(vehiclesPath.toString());

        Map<Id<Vehicle>, List<VehicleParkingData>> parkingTracker = new HashMap<>();

        for(Id<Vehicle> vehicleId : vehicles.getVehicles().keySet()) {
            if(vehicleId.toString().contains("bike")) {
                continue;
            }
            List<VehicleParkingData> dummyList = new ArrayList<>();
            parkingTracker.put(vehicleId, dummyList);
        }


        EventsManager manager = EventsUtils.createEventsManager();

        ParkedVehiclesCounter handler = new ParkedVehiclesCounter(parkingTracker, network);
        manager.addHandler(handler);
        manager.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(manager);
        reader.readFile(eventsPath.toString());
        manager.finishProcessing();

        analyzeParkingCapacityPerLink(network, parkingTracker, timeBins);
        writeParkingCapacityPerLink(network, outputFolder, timeBins);

        return 0;
    }

    private void analyzeParkingCapacityPerLink(Network network, Map<Id<Vehicle>, List<VehicleParkingData>> parkingTracker, int timeBins) {

        for(Id<Link> linkId : network.getLinks().keySet()) {
            if(network.getLinks().get(linkId).toString().contains("pt_")) {
                continue;
            }
            Map<Integer, MutableInt> linkParkingDataMap = new HashMap<>();

            for(int i = 0; i < timeBins; i++) {
                linkParkingDataMap.put(i,new MutableInt(0));
            }
            parkingDemandPerLink.putIfAbsent(linkId, linkParkingDataMap);
            maxNrOfVehiclesPerLink.putIfAbsent(linkId, 0);
        }

        for(Id<Vehicle> vehicleId : parkingTracker.keySet()) {
            for(VehicleParkingData vehicleParkingData : parkingTracker.get(vehicleId)) {
                int timeBin;
                int previousTimeBin;
                if(vehicleParkingData.time >= 86400.) {
                    timeBin = (int) (86400. / timeBinSize - 1);
                } else {
                    timeBin = (int) (vehicleParkingData.time / timeBinSize);
                }

                if(vehicleParkingData.eventType.equals("vehicle enters traffic")) {
                    if(parkingTracker.get(vehicleId).indexOf(vehicleParkingData) > 0) {

                        previousTimeBin = (int) (parkingTracker.get(vehicleId).get(parkingTracker.get(vehicleId).indexOf(vehicleParkingData) - 1).time / timeBinSize);

                        for(int i = previousTimeBin + 1; i <= timeBin; i++) {
                            parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).increment();

                            if(parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).intValue() > maxNrOfVehiclesPerLink.get(vehicleParkingData.linkId)) {
                                maxNrOfVehiclesPerLink.replace(vehicleParkingData.linkId, parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).intValue());
                            }
                        }
                    } else {
                        //decrement parked veh on link in recent timeBin
                        //+ increment on timeBins before because its the first time veh enters traffic = it was parked over night
                        for(int i = 0; i <= timeBin; i++) {
                            parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).increment();

                            if(parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).intValue() > maxNrOfVehiclesPerLink.get(vehicleParkingData.linkId)) {
                                maxNrOfVehiclesPerLink.replace(vehicleParkingData.linkId, parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).intValue());
                            }
                        }
                    }
                    parkingDemandPerLink.get(vehicleParkingData.linkId).get(timeBin).decrement();

                    if(parkingDemandPerLink.get(vehicleParkingData.linkId).get(timeBin).intValue() < 0) {
                        log.error("At " + vehicleParkingData.time + " the number of parked vehicles on link " +
                                vehicleParkingData.linkId + " is < 0.");
                        throw new RuntimeException();
                    }

                    if(parkingTracker.get(vehicleId).indexOf(vehicleParkingData) > 0 && vehicleParkingData.linkId !=
                            parkingTracker.get(vehicleId).get(parkingTracker.get(vehicleId).indexOf(vehicleParkingData)-1).linkId) {
                        log.info(vehicleId + " enters traffic at link " + vehicleParkingData.linkId + " but left it on link "
                                + parkingTracker.get(vehicleId).get(parkingTracker.get(vehicleId).indexOf(vehicleParkingData)-1).linkId);
                    }

                } else if(vehicleParkingData.eventType.equals("vehicle leaves traffic")) {
                    if(parkingTracker.get(vehicleId).indexOf(vehicleParkingData) == parkingTracker.get(vehicleId).size() - 1) {
                        //last traffic event of the day -> vehicle does not enter traffic again
                        if(timeBin != (timeBins - 1)) {
                            for(int i = timeBin; i <= (timeBins - 1); i++) {
                                parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).increment();

                                if(parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).intValue() > maxNrOfVehiclesPerLink.get(vehicleParkingData.linkId)) {
                                    maxNrOfVehiclesPerLink.replace(vehicleParkingData.linkId, parkingDemandPerLink.get(vehicleParkingData.linkId).get(i).intValue());
                                }
                            }
                        }
                    } else {
                        //vehicle will enter traffic again
                        VehicleParkingData nextTrafficEvent = parkingTracker.get(vehicleId).get(parkingTracker.get(vehicleId).indexOf(vehicleParkingData) + 1);
                        parkingDemandPerLink.get(nextTrafficEvent.linkId).get(timeBin).increment();
                    }

                } else {
                    log.error("Vehicle " + vehicleId + " records an event of type " + vehicleParkingData.eventType +
                            ". There only should be " + VehicleLeavesTrafficEvent.EVENT_TYPE + " and " + VehicleEntersTrafficEvent.EVENT_TYPE);
                }
            }
        }
    }

    private void writeParkingCapacityPerLink(Network network, Path outputFolder, int timeBins) {

        BufferedWriter timeBinWriter = IOUtils.getBufferedWriter(outputFolder.toString() + "/parking-vehicles-per-link.tsv");
        BufferedWriter capacityWriter = IOUtils.getBufferedWriter(outputFolder + "/parking-capacity-per-link.tsv");
        log.info("Trying to write parking data to " + outputFolder);

        try {
            timeBinWriter.write("linkId");
            for(int i = 0; i <= timeBins; i++) {
                int timeBin = i * timeBinSize;

                String formattedTime = DurationFormatUtils.formatDuration(timeBin * 1000L, "HH:mm:ss", true);
                timeBinWriter.write("\t" + formattedTime);
            }
            timeBinWriter.newLine();

            capacityWriter.write("linkId" + "\t" + "maxParkedVehicles");
            capacityWriter.newLine();

            for(Id<Link> linkId : network.getLinks().keySet()) {
                if(linkId.toString().contains("pt_")) {
                    continue;
                }
                timeBinWriter.write(linkId.toString());
                capacityWriter.write(linkId.toString());

                if(parkingDemandPerLink.containsKey(linkId)) {
                    for(int timeBin : parkingDemandPerLink.get(linkId).keySet()) {
                        timeBinWriter.write("\t" + parkingDemandPerLink.get(linkId).get(timeBin));
                    }
                    capacityWriter.write("\t" + maxNrOfVehiclesPerLink.get(linkId).toString());
                } else {
                    for(int i = 0; i <= timeBins; i++) {
                        timeBinWriter.write("\t" + 0);
                    }
                    capacityWriter.write("\t" + 0);
                }
                timeBinWriter.newLine();
                capacityWriter.newLine();
            }
            timeBinWriter.close();
            capacityWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record VehicleParkingData(Id<Link> linkId, Double time, String eventType) {
    }
}