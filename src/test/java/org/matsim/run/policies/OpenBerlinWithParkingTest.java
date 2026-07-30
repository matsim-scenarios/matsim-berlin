package org.matsim.run.policies;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_ON_STREET_SPOTS;

class OpenBerlinWithParkingTest {

    private static final int EXPECTED_BERLIN_PARKING_SPOTS = 1_276_312;
    private static final int EXPECTED_HUNDEKOPF_PARKING_SPOTS = 230_000;
    private static final String BERLIN_SHAPE = "/Users/gregorr/Documents/work/Paper/heartParking/berlin-2582.shp";
    private static final String HUNDEKOPF_SHAPE =
            "input/v6.4/hundekopf-shp/hundekopf-carBanArea-25832.shp";

    @Test
    void assignOnStreetParkingAssignsExpectedNumberOfParkingSpots() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(
                "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/"
                        + "berlin-v6.4/input/berlin-v6.4-network-with-pt.xml.gz"
        );

        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(
                IOUtils.resolveFileOrResource(BERLIN_SHAPE)
        );
        List<PreparedGeometry> hundekopf = ShpGeometryUtils.loadPreparedGeometries(
                IOUtils.resolveFileOrResource(HUNDEKOPF_SHAPE)
        );

        OpenBerlinWithParking openBerlinWithParking = CommandLine.populateCommand(
                new OpenBerlinWithParking(),
                "--parking-supply=unused.csv",
                "--shp-berlin-geometries=" + BERLIN_SHAPE,
                "--shp-hundekopf=" + HUNDEKOPF_SHAPE
        );
        openBerlinWithParking.assignOnStreetParking(scenario);

        int assignedInBerlin = scenario.getNetwork().getLinks().values().stream()
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), berlin))
                .mapToInt(OpenBerlinWithParkingTest::onStreetParkingSpots)
                .sum();

        int assignedInHundekopf = scenario.getNetwork().getLinks().values().stream()
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), hundekopf))
                .mapToInt(OpenBerlinWithParkingTest::onStreetParkingSpots)
                .sum();

        // Capacities are rounded per link, so their sum can differ slightly from the target.
        assertThat(assignedInBerlin)
                .isCloseTo(EXPECTED_BERLIN_PARKING_SPOTS, offset(EXPECTED_BERLIN_PARKING_SPOTS + 100));
        assertThat(assignedInHundekopf)
                .isCloseTo(EXPECTED_HUNDEKOPF_PARKING_SPOTS, offset(EXPECTED_HUNDEKOPF_PARKING_SPOTS + 100));
    }

    private static int onStreetParkingSpots(Link link) {
        Object capacity = link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS);
        return capacity == null ? 0 : (int) capacity;
    }
}
