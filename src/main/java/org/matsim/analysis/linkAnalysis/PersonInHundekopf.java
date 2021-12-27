package org.matsim.analysis.linkAnalysis;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zmeng
 */

public class PersonInHundekopf {
private static final Logger log =Logger.getLogger(PersonInHundekopf.class);
private List<PreparedGeometry> preparedGeometries;
private Map<Person, Coord> person2homeLocation = new HashMap<>();

private Scenario scenario;


public static void main(String[] args) throws IOException {
        String plansFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";
        String outputFile = "/net/ils/kreuschner/personInsideHundekopf.csv";
        final String shapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/berlin-planungsraum-hundekopf/berlin-hundekopf-based-on-planungsraum.shp";
        PersonInHundekopf generatePersonHomeLocation = new PersonInHundekopf(plansFile, shapeFile);
        generatePersonHomeLocation.generate();
        generatePersonHomeLocation.write(outputFile,",");

        }


    public PersonInHundekopf(String plansFile, String shapeFile) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:31468");
        config.plans().setInputCRS("EPSG:31468");
        config.plans().setInputFile(plansFile);
        this.scenario = ScenarioUtils.loadScenario(config);
        this.preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shapeFile));
    }

    private void generate() {
        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            var activities = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
            for (Activity act :
                    activities) {
                if (act.getType().contains("home") && ShpGeometryUtils.isCoordInPreparedGeometries(act.getCoord(), this.preparedGeometries)){
                    this.person2homeLocation.put(person, act.getCoord());
                    break;
                }
            }
        }
    }

    private void write(String outputFile, String splitSymbol) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(outputStreamWriter);
        bw.write("person" + splitSymbol + "home_x" + splitSymbol + "home_y");

        this.person2homeLocation.forEach((person, coord) -> {
            try {
                bw.newLine();
                bw.write(person.getId().toString() + splitSymbol + coord.getX() + splitSymbol + coord.getY());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        bw.close();
    }
}