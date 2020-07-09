/**
 *
 */
package org.matsim.run.dynamicShutdown;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.population.PopulationUtils;
//import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

/**
 *
 * @author jakobrehmann
 * adapted from Aravind "ModeStatsControlerListenerTest"
 *
 * //TODO: What happens when activities/trips are added or deleted due to innovation?
 */
public class ModeChoiceCoverageStatsControlerListenerTest {

//	@Rule
//	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	public void testChangePlanModes() {

		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
		TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();

		ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy("/ModeChoiceCoverageControlerListener",
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);

		ModeChoiceCoverageControlerListener modeCC = new ModeChoiceCoverageControlerListener(controlerConfigGroup, population, controlerIO, scoreConfig, transportId);
		modeCC.notifyStartup(new StartupEvent(null));

		// Iteration 0: walk - walk
		Plan plan1 = makePlan(person, TransportMode.walk, TransportMode.walk);
		person.addPlan(plan1);
		person.setSelectedPlan(plan1);

		modeCC.notifyIterationEnds(new IterationEndsEvent(null, 0));
		Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory = modeCC.getModeChoiceCoverageHistory();
		Assert.assertEquals( (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(0));
		Assert.assertEquals( (Double) 0.0, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(0));

		// Iteration 1: walk - bike
		Plan plan2 = makePlan(person, TransportMode.walk, TransportMode.bike);
		person.addPlan(plan2);
		person.setSelectedPlan(plan2);

		modeCC.notifyIterationEnds(new IterationEndsEvent(null, 1));
		Assert.assertEquals( (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(1));
		Assert.assertEquals( (Double) 0.5, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(1));

		//O Iteration 3: bike - walk
		Plan plan3 = makePlan(person, TransportMode.bike, TransportMode.walk);
		person.addPlan(plan3);
		person.setSelectedPlan(plan3);

		modeCC.notifyIterationEnds(new IterationEndsEvent(null, 2));
		Assert.assertEquals( (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(2));
		Assert.assertEquals( (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(2));
	}

	@Test
	public void testDifferentLevels() {

		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
		TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();

		ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy("/ModeChoiceCoverageControlerListener",
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);

		ModeChoiceCoverageControlerListener modeCC = new ModeChoiceCoverageControlerListener(controlerConfigGroup, population, controlerIO, scoreConfig, transportId);
		modeCC.notifyStartup(new StartupEvent(null));

		// After 1 iteration
		Plan plan1 = makePlan(person, TransportMode.walk, TransportMode.walk);
		person.addPlan(plan1);
		person.setSelectedPlan(plan1);

		modeCC.notifyIterationEnds(new IterationEndsEvent(null, 0));
		Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory = modeCC.getModeChoiceCoverageHistory();
		Assert.assertEquals((Double) 1.0, modeChoiceCoverageHistory.get(1).get("walk").get(0));
		Assert.assertEquals((Double) 0.0, modeChoiceCoverageHistory.get(5).get("walk").get(0));
		Assert.assertEquals((Double) 0.0, modeChoiceCoverageHistory.get(10).get("walk").get(0));


		// After 5 iterations
		for (int i = 1; i < 5; i++) {
			modeCC.notifyIterationEnds(new IterationEndsEvent(null, i));
		}

		Assert.assertEquals((Double) 1.0, modeChoiceCoverageHistory.get(1).get("walk").get(4));
		Assert.assertEquals((Double) 1.0, modeChoiceCoverageHistory.get(5).get("walk").get(4));
		Assert.assertEquals((Double) 0.0, modeChoiceCoverageHistory.get(10).get("walk").get(4));

		// After 10 iterations
		for (int i = 5; i < 10; i++) {
			modeCC.notifyIterationEnds(new IterationEndsEvent(null, i));
		}

		Assert.assertEquals((Double) 1.0, modeChoiceCoverageHistory.get(1).get("walk").get(9));
		Assert.assertEquals((Double) 1.0, modeChoiceCoverageHistory.get(5).get("walk").get(9));
		Assert.assertEquals((Double) 1.0, modeChoiceCoverageHistory.get(10).get("walk").get(9));

	}


	private Plan makePlan( Person person, String modeLeg1, String modeLeg2) {
		Plan plan = PopulationUtils.createPlan(person);

		final Id<Link> link1 = Id.create(10723, Link.class);
		final Id<Link> link2 = Id.create(123160, Link.class);

		Activity act1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan.addActivity(act1);

		Leg leg1 = PopulationUtils.createLeg(modeLeg1);
		plan.addLeg(leg1);

		Activity act2 = PopulationUtils.createActivityFromLinkId("work", link2);
		plan.addActivity(act2);

		Leg leg2 = PopulationUtils.createLeg(modeLeg2);
		plan.addLeg(leg2);

		Activity act3 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan.addActivity(act3);
		return plan;
	}
}