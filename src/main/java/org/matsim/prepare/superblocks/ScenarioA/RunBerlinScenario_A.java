package org.matsim.prepare.superblocks.ScenarioA;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.analysis.RunPersonTripAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.prepare.superblocks.ScenarioA.RunBerlinScenario_A;
import org.matsim.run.BerlinExperimentalConfigGroup;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

public class RunBerlinScenario_A {

    private static final Logger log = Logger.getLogger(RunBerlinScenario_A.class );

    public static void main(String[] args) {

        for (String arg : args) {
            log.info( arg );
        }

        if ( args.length==0 ) {
            //args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config-subpop.xml"}  ;

            //******changes******

            args = new String[] {"/Users/moritzkreuschner/Desktop/Master Thesis/B_Coding/Coding/git/matsim-berlin-kreuschner/superblock_input_data/Input_C100/berlin-v5.5-1pct.config-subpop_C100.xml"}  ;
            //******changes******

        }

        Config config = prepareConfig( args ) ;
        Scenario scenario = prepareScenario( config ) ;
        Controler controler = prepareControler( scenario ) ;
        controler.run() ;

    }

    public static Controler prepareControler( Scenario scenario ) {
        // note that for something like signals, and presumably drt, one needs the controler object

        Gbl.assertNotNull(scenario);

        final Controler controler = new Controler( scenario );

        if (controler.getConfig().transit().isUseTransit()) {
            // use the sbb pt raptor router
            controler.addOverridingModule( new AbstractModule() {
                @Override
                public void install() {
                    install( new SwissRailRaptorModule() );
                }
            } );
        } else {
            log.warn("Public transit will be teleported and not simulated in the mobsim! "
                    + "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
                    + "Should only be used for testing or car-focused studies with a fixed modal split.  ");
        }



        // use the (congested) car travel time for the teleported ride mode
        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
                addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
                //bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);

                addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
                addPlanStrategyBinding("ChangeSingleTripModeAndRoute").toProvider(ChangeSingleTripModeAndRoute.class);

                bind(RaptorIntermodalAccessEgress.class).to(BerlinRaptorIntermodalAccessEgress.class);
            }
        } );


        for (int i = 1; i < 160; i++) {

            // Add new plan strategy module
            controler.addOverridingModule( new AbstractModule(){
                @Override
                public void install(){
                    // define second subtour mode choice strategy:
                    this.addPlanStrategyBinding( "SubtourModeChoiceInternal"+i ).toProvider( new Provider<PlanStrategy>(){
                        @Inject
                        private Provider<TripRouter> tripRouterProvider;
                        @Inject private GlobalConfigGroup globalConfigGroup;
                        @Inject private ActivityFacilities facilities;
                        @Override public PlanStrategy get() {
                            PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder( new RandomPlanSelector<>() ) ;
                            SubtourModeChoiceConfigGroup modeChoiceConfig = new SubtourModeChoiceConfigGroup() ;
                            modeChoiceConfig.setModes( new String[] {TransportMode.walk, TransportMode.pt, "carSuperblock", "bicycle"} );
                            modeChoiceConfig.setChainBasedModes( new String[] {"carSuperblock", "bicycle"});
                            builder.addStrategyModule(new SubtourModeChoice(tripRouterProvider, globalConfigGroup, modeChoiceConfig) );
                            builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup) );
                            return builder.build() ;
                        }
                    } ) ;
                }
            } ) ;
        }
        return controler;
    }

    public static Scenario prepareScenario( Config config ) {
        Gbl.assertNotNull( config );

        // note that the path for this is different when run from GUI (path of original config) vs.
        // when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
        // yy Does this comment still apply?  kai, jul'19

        /*
         * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
         * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
         * class is also used for runs without drt.
         */
        final Scenario scenario = ScenarioUtils.createScenario( config );


        for (int i = 1; i < 160; i++) {
            // Add carInternal vehicle type
            VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();
            VehicleType carSuperblockVehicleType = vehiclesFactory.createVehicleType(Id.create("carSuperblock"+i, VehicleType.class));
            scenario.getVehicles().addVehicleType(carSuperblockVehicleType);
        }
        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        ScenarioUtils.loadScenario(scenario);

        // Delete all links and routes
        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Activity) {
                        ((Activity) pe).setLinkId(null);
                    } else if (pe instanceof Leg) {
                        ((Leg) pe).setRoute(null);
                    } else {
                        throw new RuntimeException("Plan element can either be activity or leg.");
                    }
                }
            }
        }

        BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
        if (berlinCfg.getPopulationDownsampleFactor() != 1.0) {
            downsample(scenario.getPopulation().getPersons(), berlinCfg.getPopulationDownsampleFactor());
        }

        return scenario;
    }

    public static Config prepareConfig( String [] args, ConfigGroup... customModules ){
        return prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation.none, args, customModules ) ;
    }
    public static Config prepareConfig(RunDrtOpenBerlinScenario.AdditionalInformation additionalInformation, String [] args,
                                       ConfigGroup... customModules ) {
        OutputDirectoryLogging.catchLogEntries();

        String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );

        ConfigGroup[] customModulesToAdd = null ;
        if ( additionalInformation== RunDrtOpenBerlinScenario.AdditionalInformation.acceptUnknownParamsBerlinConfig ) {
            customModulesToAdd = new ConfigGroup[]{ new BerlinExperimentalConfigGroup(true) };
        } else {
            customModulesToAdd = new ConfigGroup[]{ new BerlinExperimentalConfigGroup(false) };
        }
        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
        config.controler().setLastIteration(1);

        config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );

        config.plansCalcRoute().setRoutingRandomness( 3. );
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
        config.plansCalcRoute().removeModeRoutingParams("undefined");

        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );

        // vsp defaults
        config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
        config.plansCalcRoute().setInsertingAccessEgressWalk( true );
        config.qsim().setUsingTravelTimeCheckInTeleportation( true );
        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );

        // activities:
        for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
            config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
            config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
            config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
            config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
            config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
        }
        config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );

        ConfigUtils.applyCommandline( config, typedArgs ) ;

        return config ;
    }

    public static void runAnalysis(Controler controler) {
        Config config = controler.getConfig();

        String modesString = "";
        for (String mode: config.planCalcScore().getAllModes()) {
            modesString = modesString + mode + ",";
        }
        // remove last ","
        if (modesString.length() < 2) {
            log.error("no valid mode found");
            modesString = null;
        } else {
            modesString = modesString.substring(0, modesString.length() - 1);
        }

        String[] args = new String[] {
                config.controler().getOutputDirectory(),
                config.controler().getRunId(),
                "null", // TODO: reference run, hard to automate
                "null", // TODO: reference run, hard to automate
                config.global().getCoordinateSystem(),
                "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-bezirke/bezirke_berlin.shp",
                TransformationFactory.DHDN_GK4,
                "SCHLUESSEL",
                "home",
                "10", // TODO: scaling factor, should be 10 for 10pct scenario and 100 for 1pct scenario
                "null", // visualizationScriptInputDirectory
                modesString
        };

        try {
            RunPersonTripAnalysis.main(args);
        } catch (IOException e) {
            log.error(e.getStackTrace());
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void downsample(final Map<Id<Person>, ? extends Person> map, final double sample ) {
        final Random rnd = MatsimRandom.getLocalInstance();
        log.warn( "Population downsampled from " + map.size() + " agents." ) ;
        map.values().removeIf( person -> rnd.nextDouble() > sample ) ;
        log.warn( "Population downsampled to " + map.size() + " agents." ) ;
    }
}
