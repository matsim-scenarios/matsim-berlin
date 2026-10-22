package org.matsim.prepare;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class RunOpenBerlinCalibrationTest {

	@Test
	public void replicatesOnlyCarDrivingNonPersons() {

		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		addAgent(population, "resident", "person", "car", 1);
		Person van = addAgent(population, "van", "commercialPersonTraffic", "car", 3);
		van.setSelectedPlan(van.getPlans().get(1));
		van.getAttributes().putAttribute("vehicleTypes", "vwCaddy");
		van.getAttributes().putAttribute("vehicles", "van_vehicle");
		addAgent(population, "truck", "goodsTraffic", "truck", 2);
		addAgent(population, "longHaul", "freight", "freight", 1);

		RunOpenBerlinCalibration.replicateCarDrivingNonPersons(population, 5);

		assertThat(population.getPersons()).hasSize(4 + 4);

		for (int k = 1; k < 5; k++) {
			Person copy = population.getPersons().get(Id.createPersonId("van_car_factor_" + k));
			assertThat(copy).isNotNull();
			assertThat(PopulationUtils.getSubpopulation(copy)).isEqualTo("commercialPersonTraffic");
			assertThat(copy.getAttributes().getAttribute("vehicleTypes")).isEqualTo("vwCaddy");
			assertThat(copy.getAttributes().getAttribute("vehicles")).isNull();
			assertThat(copy.getPlans()).extracting(Plan::getType).containsExactly("0", "1", "2");
			assertThat(copy.getPlans().indexOf(copy.getSelectedPlan())).isEqualTo(1);
			assertThat(TripStructureUtils.getLegs(copy.getSelectedPlan())).hasSize(1);
		}

		// the original keeps its selection and its vehicle
		assertThat(van.getPlans().indexOf(van.getSelectedPlan())).isEqualTo(1);
		assertThat(van.getAttributes().getAttribute("vehicles")).isEqualTo("van_vehicle");
	}

	private static Person addAgent(Population population, String id, String subpopulation, String mode, int plans) {
		Person person = population.getFactory().createPerson(Id.createPersonId(id));
		PopulationUtils.putSubpopulation(person, subpopulation);
		for (int i = 0; i < plans; i++) {
			Plan plan = population.getFactory().createPlan();
			plan.setType(String.valueOf(i));
			plan.addActivity(population.getFactory().createActivityFromCoord("start", new Coord(0, 0)));
			plan.addLeg(population.getFactory().createLeg(mode));
			plan.addActivity(population.getFactory().createActivityFromCoord("end", new Coord(1000, 0)));
			person.addPlan(plan);
		}
		person.setSelectedPlan(person.getPlans().get(0));
		population.addPerson(person);
		return person;
	}
}
