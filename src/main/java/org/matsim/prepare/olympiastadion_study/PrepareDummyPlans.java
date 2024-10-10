package org.matsim.prepare.olympiastadion_study;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.Random;

public class PrepareDummyPlans {
	public static void main(String[] args) {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();
		Random random = new Random(1);

		for (int i = 0; i < 100; i++) {
			Person person = factory.createPerson(Id.createPersonId("dummy_" + i));
			Plan plan = factory.createPlan();
			// Olympiastadion
			Activity fromAct = factory.createActivityFromCoord("dummy", new Coord(787944, 5826767));
			// for testing, we use a time when PT supply is good
			fromAct.setEndTime(15 * 3600 + random.nextInt(3600));
			Leg leg = factory.createLeg(TransportMode.pt);
			// Berlin Hbf
			Activity toAct = factory.createActivityFromCoord("dummy", new Coord(796465, 5828241));

			plan.addActivity(fromAct);
			plan.addLeg(leg);
			plan.addActivity(toAct);
			person.addPlan(plan);
			population.addPerson(person);
		}

		new PopulationWriter(population).write("/Users/luchengqi/Documents/MATSimScenarios/Berlin/olympiastadion-study/testing-plans/dummy-plans.xml.gz");
	}
}
