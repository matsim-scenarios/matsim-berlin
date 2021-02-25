package org.matsim.run.dynamicShutdown;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunModeChoiceCoverageControlerListenerTest {

    static int bike;
    static int car;
    static int pt;
    static int other;
    static int non_network_walk;
    static int ride;
    static int walk;

    HashMap<String, Integer> person1modes = new HashMap<String, Integer>();
    //    HashMap<String, Integer> person2modes = new HashMap<String, Integer>();
    //    HashMap<String, Integer> person3modes = new HashMap<String, Integer>();


    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testModeStatsControlerListener() {

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        final List<PlanElement> planElem = new ArrayList<PlanElement>();
        PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
        TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();
        PlanCalcScoreConfigGroup.ModeParams modeParam1 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.walk);
        PlanCalcScoreConfigGroup.ModeParams modeParam2 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.car);
        PlanCalcScoreConfigGroup.ModeParams modeParam3 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.pt);
        PlanCalcScoreConfigGroup.ModeParams modeParam4 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.non_network_walk);
        PlanCalcScoreConfigGroup.ModeParams modeParam5 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.ride);
        PlanCalcScoreConfigGroup.ModeParams modeParam6 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.other);
        PlanCalcScoreConfigGroup.ModeParams modeParam7 = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike);
        scoreConfig.addModeParams(modeParam1);
        scoreConfig.addModeParams(modeParam2);
        scoreConfig.addModeParams(modeParam3);
        scoreConfig.addModeParams(modeParam4);
        scoreConfig.addModeParams(modeParam5);
        scoreConfig.addModeParams(modeParam6);
        scoreConfig.addModeParams(modeParam7);

        /* ########Person 1######### --- creating person 1*/
        final Plan plan = PopulationUtils
                .createPlan(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));
        Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
        person1modes.put(TransportMode.walk, 0);
        person1modes.put(TransportMode.car, 0);
        person1modes.put(TransportMode.pt, 0);
        person1modes.put(TransportMode.non_network_walk, 0);
        person1modes.put(TransportMode.other, 0);
        person1modes.put(TransportMode.bike, 0);
        person1modes.put(TransportMode.ride, 0);

        final Id<Link> link1 = Id.create(10723, Link.class);
        final Id<Link> link2 = Id.create(123160, Link.class);
        final Id<Link> link3 = Id.create(130181, Link.class);
        final Id<Link> link4 = Id.create(139117, Link.class);
        final Id<Link> link5 = Id.create(139100, Link.class);

        Activity act1 = PopulationUtils.createActivityFromLinkId("home", link1);
        planElem.add(act1);
        plan.addActivity(act1);
        Leg leg1 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg1);
        plan.addLeg(leg1);
        person1modes.put(TransportMode.walk, person1modes.get(TransportMode.walk) + 1);
        Activity act2 = PopulationUtils.createActivityFromLinkId("leisure", link1);// main mode walk
        planElem.add(act2);
        plan.addActivity(act2);
        Leg leg2 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg2);
        plan.addLeg(leg2);
        Activity act3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
        planElem.add(act3);
        plan.addActivity(act3);
        Leg leg3 = PopulationUtils.createLeg(TransportMode.car);
        planElem.add(leg3);
        plan.addLeg(leg3);
        Activity act4 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
        planElem.add(act4);
        plan.addActivity(act4);
        Leg leg4 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg4);
        plan.addLeg(leg4);
        person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
        Activity act5 = PopulationUtils.createActivityFromLinkId("work", link2);// main mode car
        planElem.add(act5);
        plan.addActivity(act5);
        Leg leg5 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg5);
        plan.addLeg(leg5);
        Activity act6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
        planElem.add(act6);
        plan.addActivity(act6);
        Leg leg6 = PopulationUtils.createLeg(TransportMode.car);
        planElem.add(leg6);
        plan.addLeg(leg6);
        Activity act7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
        planElem.add(act7);
        plan.addActivity(act7);
        Leg leg7 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg7);
        plan.addLeg(leg7);
        person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
        Activity act8 = PopulationUtils.createActivityFromLinkId("leisure", link3);// main mode car
        planElem.add(act8);
        plan.addActivity(act8);
        Leg leg8 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg8);
        plan.addLeg(leg8);
        Activity act9 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
        planElem.add(act9);
        plan.addActivity(act9);
        Leg leg9 = PopulationUtils.createLeg(TransportMode.car);
        planElem.add(leg9);
        plan.addLeg(leg9);
        Activity act10 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
        planElem.add(act10);
        plan.addActivity(act10);
        Leg leg10 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg10);
        plan.addLeg(leg10);
        person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
        Activity act11 = PopulationUtils.createActivityFromLinkId("shopping", link4);// main mode car
        planElem.add(act11);
        plan.addActivity(act11);
        Leg leg11 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg11);
        plan.addLeg(leg11);
        Activity act12 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
        planElem.add(act12);
        plan.addActivity(act12);
        Leg leg12 = PopulationUtils.createLeg(TransportMode.pt);
        planElem.add(leg12);
        plan.addLeg(leg12);
        Activity act13 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
        planElem.add(act13);
        plan.addActivity(act13);
        Leg leg13 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg13);
        plan.addLeg(leg13);
        person1modes.put(TransportMode.pt, person1modes.get(TransportMode.pt) + 1);
        Activity act14 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode pt
        planElem.add(act14);
        plan.addActivity(act14);
        Leg leg14 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg14);
        plan.addLeg(leg14);
        Activity act15 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
        planElem.add(act15);
        plan.addActivity(act15);
        Leg leg15 = PopulationUtils.createLeg(TransportMode.pt);
        planElem.add(leg15);
        plan.addLeg(leg15);
        Activity act16 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
        planElem.add(act16);
        plan.addActivity(act16);
        Leg leg16 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg16);
        plan.addLeg(leg16);
        Activity act17 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
        planElem.add(act17);
        plan.addActivity(act17);
        Leg leg17 = PopulationUtils.createLeg(TransportMode.car);
        planElem.add(leg17);
        plan.addLeg(leg17);
        Activity act18 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
        planElem.add(act18);
        plan.addActivity(act18);
        Leg leg18 = PopulationUtils.createLeg(TransportMode.walk);
        planElem.add(leg18);
        plan.addLeg(leg18);
        person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
        Activity act19 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
        planElem.add(act19);
        plan.addActivity(act19);

        person.addPlan(plan);
        population.addPerson(person);

        //

        ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
        String outputDirectory = utils.getOutputDirectory() + "/ModeStatsControlerListener";
        OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(outputDirectory,
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles, ControlerConfigGroup.CompressionType.gzip);
        controlerConfigGroup.setCreateGraphs(true);
        controlerConfigGroup.setFirstIteration(0);

        ModeChoiceCoverageControlerListener mccListener = new ModeChoiceCoverageControlerListener(controlerConfigGroup, population,
                controlerIO, scoreConfig, transportId);

        mccListener.notifyStartup(new StartupEvent(null));

        HashMap<String, Integer> modesIter0 = new HashMap<String, Integer>();

        IterationEndsEvent event0 = new IterationEndsEvent(null, 0, false);
        mccListener.notifyIterationEnds(event0);

        person1modes.forEach((k, v) -> modesIter0.merge(k, v, Integer::sum));

        Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory = mccListener.getModeChoiceCoverageHistory();

        return;
    }
}
