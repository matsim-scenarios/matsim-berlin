package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.prepare.carfree.PrepareNetworkCarfree;

public class RunBerlinScenarioCarfreeAreas {

    /**
     * Defines which shp file combinations (= carfree area combinations) are simulated
     */
    enum CarfreeAreas {highlySuitableArea, moreSuitableArea, ratherSuitableArea, lessSuitableArea}

    static String highlySuitableAreaPath = "pathToPublicSVN";
    static String moreSuitableAreaPath = "pathToPublicSVN";
    static String ratherSuitableAreaPath = "pathToPublicSVN";
    static String lessSuitableAreaPath = "pathToPublicSVN";

    public static void main(String[] args) {

        Config config = RunBerlinScenario.prepareConfig(args);

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

    private static void adaptNetworkToCarfreeAreaCase(Network network, String[] args) {

        //TODO change 100 to meaningful number
        if (args[100].contains(CarfreeAreas.highlySuitableArea.toString())) {
            PrepareNetworkCarfree.prepareCarFree(network, highlySuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }

        //TODO change 100 to meaningful number
        if (args[100].contains(CarfreeAreas.moreSuitableArea.toString())) {
            PrepareNetworkCarfree.prepareCarFree(network, moreSuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }

        //TODO change 100 to meaningful number
        if (args[100].contains(CarfreeAreas.ratherSuitableArea.toString())) {
            PrepareNetworkCarfree.prepareCarFree(network, ratherSuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }

        //TODO change 100 to meaningful number
        if (args[100].contains(CarfreeAreas.lessSuitableArea.toString())) {
            PrepareNetworkCarfree.prepareCarFree(network, lessSuitableAreaPath, TransportMode.car + "," + TransportMode.ride);
        }
    }
}
