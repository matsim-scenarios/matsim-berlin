package org.matsim.run.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.contrib.parking.parkingsearchparameterization.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.kernel.ConstantKernelDistance;
import org.matsim.core.network.kernel.DefaultKernelFunction;
import org.matsim.core.network.kernel.KernelDistance;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;
import org.wololo.jts2geojson.GeoJSONReader;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.contrib.parking.parkingsearchparameterization.ParkingUtils.LINK_OFF_STREET_SPOTS;
import static org.matsim.contrib.parking.parkingsearchparameterization.ParkingUtils.LINK_ON_STREET_SPOTS;

/*
This class extends the matsim berlin scenario by parking functionality
 */

public class OpenBerlinWithParking extends OpenBerlinScenario {

    @CommandLine.Option(names = "--parking-supply",
            description = "Path to parking supply data", required = true)
    private String parkingSupply;

    @CommandLine.Option(names = "--alpha", description = "Alpha parameter for Belloche parking search time function", defaultValue = "0.217")
    private double alpha;

    @CommandLine.Option(names = "--beta", description = "Beta parameter for Belloche parking search time function", defaultValue = "-7.364")
    private double beta;

    @CommandLine.Option(names = "--noModeChoice", defaultValue = "true")
    private boolean noModeChoice;

    @CommandLine.Option(names = "--on-street-parking-assignment",
            description = "How to assign on-street parking: ${COMPLETION-CANDIDATES}",
            defaultValue = "REGIONAL_TOTALS")
    private OnStreetParkingAssignment onStreetParkingAssignment;

    @CommandLine.Option(names = "--shp-hundekopf",
            description = "Shapefile of Hundekopf; required for REGIONAL_TOTALS")
    private String shpHundekopf;

    @CommandLine.Option(names = "--shp-berlin-geometries",
            description = "Shapefile of Berlin; required for REGIONAL_TOTALS")
    private String shpBerlinGeometries;

    @CommandLine.Option(names = "--parking-inside",
            description = "GeoJSON parking data inside Hundekopf; required for PARKING_DATA")
    private String parkingInside;

    @CommandLine.Option(names = "--parking-outside",
            description = "GeoJSON parking data in the rest of Berlin; required for PARKING_DATA")
    private String parkingOutside;

    private static final Logger log = LogManager.getLogger(OpenBerlinWithParking.class);
    private static final int PARKING_SPOTS_IN_HUNDEKOPF = 230_000;
    private static final int PARKING_SPOTS_IN_BERLIN = 1_276_312;
    private static final double MAX_PARKING_LINK_FREESPEED = 13.89;
    private static final CRSFactory CRS_FACTORY = new CRSFactory();
    private static final CoordinateTransform WGS84_TO_NETWORK_CRS =
            new CoordinateTransformFactory().createTransform(
                    CRS_FACTORY.createFromName("EPSG:4326"),
                    CRS_FACTORY.createFromName("EPSG:25832")
            );

    private enum OnStreetParkingAssignment {
        REGIONAL_TOTALS,
        PARKING_DATA
    }


    public static void main(String[] args) {
        MATSimApplication.run(OpenBerlinWithParking.class, args);
    }

    @Override
    protected Config prepareConfig(Config config) {
        Config preparedConfig = super.prepareConfig(config);
        preparedConfig.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        if (noModeChoice) {
            // no mode choice if we simulate parking
            for (ReplanningConfigGroup.StrategySettings strategySettings : config.replanning().getStrategySettings()) {
                if (strategySettings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)) {
                    strategySettings.setWeight(0.0);
                }
            }
        }

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);


        return preparedConfig;
    }

    @Override
    protected void prepareScenario(Scenario scenario) {
        super.prepareScenario(scenario);

        // Store full-population parking supply on the network. The configured
        // ParkingCapacityInitializer scales it with qsim.storageCapFactor.
        assignOnStreetParking(scenario);
        // Read parking supply data for off street parking from CSV
        Map<Id<Link>, Integer> parkingSpotsPerLink = readCSV(parkingSupply);
        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (parkingSpotsPerLink.containsKey(link.getId())) {
                //log.info("Parking spots for " + link.getId() + ": on-street=" + parkingSpotsPerLink.get(link.getId()).onstreetSpots + ", off-street=" + parkingSpotsPerLink.get(link.getId()).offstreetSpots);
                link.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, parkingSpotsPerLink.get(link.getId()));
            }
        }


        int totalOffStreetSpots = scenario.getNetwork().getLinks().values().stream()
                .mapToInt(link -> {
                    Object attr = link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS);
                    return attr == null ? 0 : (int) attr;
                })
                .sum();


        log.info("Total number of off-street parking spots assigned: " + totalOffStreetSpots);
        NetworkUtils.writeNetwork(scenario.getNetwork(), "network-v6.4-with-parking.xml.gz");
    }

    @Override
    protected void prepareControler(Controler controler) {
        super.prepareControler(controler);
        controler.addOverridingQSimModule(new AbstractQSimModule() {
            @Override
            protected void configureQSim() {
                addQSimComponentBinding("ParkingOccupancyOberserver").to(ParkingOccupancyObserver.class);
                addMobsimScopeEventHandlerBinding().to(ParkingOccupancyObserver.class);
                addVehicleHandlerBinding().to(ParkingVehicleHandler.class);
                bind(ParkingOccupancyObservingSearchTimeCalculator.class).in(Singleton.class);
                addParkingSearchTimeCalculatorBinding().to(ParkingOccupancyObservingSearchTimeCalculator.class);
            }
        });

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ParkingOccupancyObserver.class).in(Singleton.class);
                bind(ParkingCapacityInitializer.class).to(PlanBasedParkingCapacityInitializerBerlin.class);
                bind(NetworkKernelFunction.class).to(DefaultKernelFunction.class);
                bind(KernelDistance.class).toInstance(new ConstantKernelDistance(500));
                // Use Belloche's complete-sample exponential congestion model:
                // t = 0.217 * exp(7.364 * T_c); https://doi.org/10.1016/j.trpro.2015.03.024
                bind(ParkingSearchTimeFunction.class).toInstance(new BellochePenaltyFunction(alpha, beta));
                addControlerListenerBinding().to(ParkingOccupancyObserver.class);
                addMobsimListenerBinding().to(ParkingOccupancyObserver.class);
            }
        });
    }


	/*
	Read parking supply data from a CSV file and return a map of Link IDs to ParkingSpots objects.
	 */

    public static Map<Id<Link>, Integer> readCSV(String filePath) {
        Map<Id<Link>, Integer> parkingMap = new HashMap<>();
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip the header line
            br.readLine();

            // Read each subsequent line in the CSV
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                // Assuming the order: id, onstreet_spots, offstreet_spots
                Id<Link> id = Id.createLinkId(values[0]);
                //int onstreetSpots = parseSpotValue(values[1]);
                int offstreetSpots = parseSpotValue(values[1]);

                // Create a ParkingData object and store it in the map
                //ParkingSpots parkingSpots = new ParkingSpots(0, offstreetSpots);
                parkingMap.put(id, offstreetSpots);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid number format.");
        }

        return parkingMap;
    }

    // Helper method to handle NA and parse the number
    private static int parseSpotValue(String value) {
        if (value.equalsIgnoreCase("NA")) {
            return 0; // Treat "NA" as zero
        }
        return Integer.parseInt(value); // Otherwise, parse the integer
    }

    public List<ParkingAssignmentStats> assignOnStreetParking(Scenario scenario) {
        return switch (onStreetParkingAssignment) {
            case REGIONAL_TOTALS -> {
                assignOnStreetParkingFromRegionalTotals(scenario);
                yield List.of();
            }
            case PARKING_DATA -> assignOnStreetParkingFromParkingData(scenario);
        };
    }

    private void assignOnStreetParkingFromRegionalTotals(Scenario scenario) {
        requireOption(shpHundekopf, "--shp-hundekopf", OnStreetParkingAssignment.REGIONAL_TOTALS);
        requireOption(shpBerlinGeometries, "--shp-berlin-geometries", OnStreetParkingAssignment.REGIONAL_TOTALS);

        List<PreparedGeometry> hundekopf = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpHundekopf)));
        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(String.valueOf(shpBerlinGeometries)));

        // Parking is assigned only to car links below 50 km/h. This excludes
        // motorways and other high-speed roads where on-street parking is implausible.
        List<? extends Link> eligibleLinks = scenario.getNetwork().getLinks().values().stream()
                .filter(OpenBerlinWithParking::isEligibleForOnStreetParking)
                .toList();

        // Classify links by their coordinate. Hundekopf is handled separately
        // because its parking density is derived from its own known supply.
        List<? extends Link> hundekopfLinks = eligibleLinks.stream()
                .filter(link -> isInArea(link, hundekopf))
                .toList();
        List<? extends Link> restOfBerlinLinks = eligibleLinks.stream()
                .filter(link -> isInArea(link, berlin))
                .filter(link -> !isInArea(link, hundekopf))
                .toList();

        // Distribute the known Hundekopf supply proportionally to link length.
        // Capacities remain full-population values and are scaled later by the
        // configured ParkingCapacityInitializer.
        double totalNetworkLengthInsideHundekopf = totalLength(hundekopfLinks);
        double spotsPerMeterInsideHundekopf = PARKING_SPOTS_IN_HUNDEKOPF / totalNetworkLengthInsideHundekopf;
        int assignedParkingSpotsInsideHundekopf = assignParkingSpots(hundekopfLinks, spotsPerMeterInsideHundekopf);

        log.info("Total network length inside Hundekopf: " + totalNetworkLengthInsideHundekopf);
        log.info("Parking spots per meter " + spotsPerMeterInsideHundekopf);
        log.info("Meters per parking spot " + (totalNetworkLengthInsideHundekopf / PARKING_SPOTS_IN_HUNDEKOPF));
        log.info("Assigned " + assignedParkingSpotsInsideHundekopf + " full-population on street parking spots inside Hundekopf based on a density of " + spotsPerMeterInsideHundekopf + " spots per meter.");

        // The Berlin total includes Hundekopf. Therefore only the remaining
        // supply is distributed across the rest of the city.
        int parkingSpotsRestOfBerlin = PARKING_SPOTS_IN_BERLIN - PARKING_SPOTS_IN_HUNDEKOPF;
        double totalNetworkLengthBerlinWithoutHundekopf = totalLength(restOfBerlinLinks);
        double spotsPerMeterRestOfBerlin = parkingSpotsRestOfBerlin / totalNetworkLengthBerlinWithoutHundekopf;
        int assignedParkingSpotsRestOfBerlin = assignParkingSpots(restOfBerlinLinks, spotsPerMeterRestOfBerlin);

        log.info("Total network length in Berlin excluding Hundekopf: " + totalNetworkLengthBerlinWithoutHundekopf);
        log.info("Parking spots per meter in Berlin excluding Hundekopf: " + spotsPerMeterRestOfBerlin);
        log.info("Meters per parking spot in Berlin excluding Hundekopf: " + (totalNetworkLengthBerlinWithoutHundekopf / parkingSpotsRestOfBerlin));
        log.info("Assigned " + assignedParkingSpotsRestOfBerlin + " full-population on street parking spots to the rest of Berlin based on a density of " + spotsPerMeterRestOfBerlin + " spots per meter.");
    }

    private List<ParkingAssignmentStats> assignOnStreetParkingFromParkingData(Scenario scenario) {
        requireOption(parkingInside, "--parking-inside", OnStreetParkingAssignment.PARKING_DATA);
        requireOption(parkingOutside, "--parking-outside", OnStreetParkingAssignment.PARKING_DATA);

        List<? extends Link> eligibleLinks = scenario.getNetwork().getLinks().values().stream()
                .filter(OpenBerlinWithParking::isEligibleForOnStreetParking)
                .toList();
        QuadTree<Link> linkIndex = createLinkIndex(eligibleLinks);

        return List.of(
                assignParkingData(parkingInside, "errechnete_anzahl_parkplaetze", linkIndex, "inside Hundekopf"),
                assignParkingData(parkingOutside, "anzahl_parkplaetze", linkIndex, "in the rest of Berlin")
        );
    }

    private static ParkingAssignmentStats assignParkingData(
            String file,
            String capacityField,
            QuadTree<Link> linkIndex,
            String area
    ) {
        List<ParkingPolygon> parkingPolygons = readParkingGeoJson(file, capacityField);
        int assignedParkingSpots = 0;
        double totalDistance = 0;
        double maximumDistance = 0;
        double[] matchingDistances = new double[parkingPolygons.size()];
        int polygonIndex = 0;

        for (ParkingPolygon parking : parkingPolygons) {
            Coordinate centroid = parking.geometry().getCentroid().getCoordinate();
            ProjCoordinate transformed = new ProjCoordinate();
            WGS84_TO_NETWORK_CRS.transform(new ProjCoordinate(centroid.x, centroid.y), transformed);
            Coord transformedCentroid = new Coord(transformed.x, transformed.y);
            Link nearestLink = linkIndex.getClosest(transformedCentroid.getX(), transformedCentroid.getY());
            double distance = distance(transformedCentroid, nearestLink.getCoord());

            Object existing = nearestLink.getAttributes().getAttribute(LINK_ON_STREET_SPOTS);
            int currentCapacity = existing == null ? 0 : ((Number) existing).intValue();
            nearestLink.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, currentCapacity + parking.capacity());

            assignedParkingSpots += parking.capacity();
            totalDistance += distance;
            maximumDistance = Math.max(maximumDistance, distance);
            matchingDistances[polygonIndex++] = distance;
        }

        double meanDistance = parkingPolygons.isEmpty() ? 0 : totalDistance / parkingPolygons.size();
        Arrays.sort(matchingDistances);
        ParkingAssignmentStats stats = new ParkingAssignmentStats(
                area,
                assignedParkingSpots,
                parkingPolygons.size(),
                meanDistance,
                percentile(matchingDistances, 0.50),
                percentile(matchingDistances, 0.90),
                percentile(matchingDistances, 0.95),
                maximumDistance
        );

        log.info("Assigned {} full-population on-street parking spots from {} polygons {}. "
                        + "Matching distance in meters: mean={}, median={}, p90={}, p95={}, maximum={}.",
                stats.assignedParkingSpots(), stats.polygonCount(), stats.area(),
                stats.meanDistance(), stats.medianDistance(), stats.p90Distance(),
                stats.p95Distance(), stats.maximumDistance());
        return stats;
    }

    private static double percentile(double[] sortedValues, double percentile) {
        if (sortedValues.length == 0) {
            return 0;
        }
        // Use the same nearest-rank convention as the original matching-quality report.
        return sortedValues[Math.min((int) (percentile * sortedValues.length), sortedValues.length - 1)];
    }

    private static List<ParkingPolygon> readParkingGeoJson(String file, String capacityField) {
        try {
            JsonNode features = new ObjectMapper().readTree(new File(file)).path("features");
            if (!features.isArray()) {
                throw new IllegalArgumentException("GeoJSON contains no feature array: " + file);
            }

            GeoJSONReader geoJsonReader = new GeoJSONReader();
            List<ParkingPolygon> result = new ArrayList<>();
            for (JsonNode feature : features) {
                JsonNode geometry = feature.get("geometry");
                if (geometry == null || geometry.isNull()) {
                    continue;
                }

                int capacity = feature.path("properties").path(capacityField).asInt(0);
                result.add(new ParkingPolygon(geoJsonReader.read(geometry.toString()), capacity));
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Could not read parking data from " + file, e);
        }
    }

    private static QuadTree<Link> createLinkIndex(List<? extends Link> links) {
        if (links.isEmpty()) {
            throw new IllegalArgumentException("The network contains no links eligible for on-street parking");
        }

        double minX = links.stream().mapToDouble(link -> link.getCoord().getX()).min().orElseThrow();
        double minY = links.stream().mapToDouble(link -> link.getCoord().getY()).min().orElseThrow();
        double maxX = links.stream().mapToDouble(link -> link.getCoord().getX()).max().orElseThrow();
        double maxY = links.stream().mapToDouble(link -> link.getCoord().getY()).max().orElseThrow();
        QuadTree<Link> index = new QuadTree<>(minX, minY, maxX, maxY);
        links.forEach(link -> index.put(link.getCoord().getX(), link.getCoord().getY(), link));
        return index;
    }

    private static double distance(Coord first, Coord second) {
        return Math.hypot(first.getX() - second.getX(), first.getY() - second.getY());
    }

    private static void requireOption(String value, String option, OnStreetParkingAssignment assignment) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(option + " is required for " + assignment);
        }
    }

    private static boolean isEligibleForOnStreetParking(Link link) {
        return link.getAllowedModes().contains(TransportMode.car)
                && link.getFreespeed() < MAX_PARKING_LINK_FREESPEED;
    }

    private static boolean isInArea(Link link, List<PreparedGeometry> area) {
        return ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), area);
    }

    private static double totalLength(List<? extends Link> links) {
        return links.stream().mapToDouble(Link::getLength).sum();
    }

    private static int assignParkingSpots(List<? extends Link> links, double spotsPerMeter) {
        int assignedParkingSpots = 0;
        for (Link link : links) {
            // Round per link because the network attribute stores whole spaces.
            // Consequently, the summed result may differ slightly from the target.
            int onStreetSpots = (int) Math.round(link.getLength() * spotsPerMeter);
            link.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, onStreetSpots);
            assignedParkingSpots += onStreetSpots;
        }
        return assignedParkingSpots;
    }

    private record ParkingPolygon(Geometry geometry, int capacity) {
    }

    public record ParkingAssignmentStats(
            String area,
            int assignedParkingSpots,
            int polygonCount,
            double meanDistance,
            double medianDistance,
            double p90Distance,
            double p95Distance,
            double maximumDistance
    ) {
    }

}
