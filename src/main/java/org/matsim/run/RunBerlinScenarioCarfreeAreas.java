package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.prepare.carfree.PrepareNetworkCarfree;

import java.net.MalformedURLException;
import java.nio.file.Path;

public class RunBerlinScenarioCarfreeAreas {

    /**
     * Defines which shp file combinations (= carfree area combinations) are simulated
     */
    enum CarfreeAreas {highlySuitableArea, moreSuitableArea, ratherSuitableArea, lessSuitableArea}

    static String highlySuitableAreaPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-berlin-planungsraum-ohne-hundekopf/shp-berlin-planungsraum-ohne-hundekopf.shp";
    static String moreSuitableAreaPath = "pathToPublicSVN";
    static String ratherSuitableAreaPath = "pathToPublicSVN";
    static String lessSuitableAreaPath = "pathToPublicSVN";

    private static final Logger log = Logger.getLogger(RunBerlinScenarioCarfreeAreas.class );

    public static void main(String[] args) throws MalformedURLException {

        if (args.length == 0) {
            args = new String[]{
                    "scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml",
            };
        }

        for (String arg : args) {
            log.info( arg );
        }


        //TODO args processing...
        CarfreeAreas.valueOf(args[1]);

        String [] configArgs = new String[]{args[0]};

        Config config = RunBerlinScenario.prepareConfig(configArgs);
        config.controler().setLastIteration(0);

        Scenario scenario = RunBerlinScenario.prepareScenario(config);

        for (Facility fac : scenario.getActivityFacilities().getFacilities().values()) {
            FacilitiesUtils.setLinkID(fac, null);
        }

        adaptNetworkToCarfreeAreaCase(scenario.getNetwork(), args);

        Controler controler = RunBerlinScenario.prepareControler(scenario);
// this is not needed right now. we try an approach where linkIds are deleted from the facility. This should have the same effect! -sme0823
//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                bind()
//            }
//        });

        controler.run();
    }

    private static void adaptNetworkToCarfreeAreaCase(Network network, String[] args) throws MalformedURLException {

        //TODO change 100 to meaningful number
        if (args[1].contains(CarfreeAreas.highlySuitableArea.toString())) {
            PrepareNetworkCarfree.prepareCarFree(network, highlySuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }

        //TODO change 100 to meaningful number
        if (args[1].contains(CarfreeAreas.moreSuitableArea.toString())) {
            //PrepareNetworkCarfree.prepareCarFree(network, moreSuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }

        //TODO change 100 to meaningful number
        if (args[1].contains(CarfreeAreas.ratherSuitableArea.toString())) {
            //PrepareNetworkCarfree.prepareCarFree(network, ratherSuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }

        //TODO change 100 to meaningful number
        if (args[1].contains(CarfreeAreas.lessSuitableArea.toString())) {
            //PrepareNetworkCarfree.prepareCarFree(network, lessSuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }
    }
}
