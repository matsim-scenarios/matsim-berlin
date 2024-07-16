package org.matsim.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.run.OpenBerlinScenario;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

@CommandLine.Command(
	name = "fixed-population",
	description = "Create synthetic population for Gartenfeld."
)
public class CreateFixedPopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateFixedPopulation.class);

	@CommandLine.Mixin
	private FacilityOptions facilities = new FacilityOptions();

	@CommandLine.Option(names = "--n", description = "Number of persons to generate", required = true)
	private int n;

	@CommandLine.Option(names = "--prefix", description = "Prefix for person ids", required = true)
	private String prefix;

	@CommandLine.Option(names = "--age-dist", description = "Age distribution for < 18 and 65+", arity = "2", required = true)
	private List<Double> ageDist;

	@CommandLine.Option(names = "--unemployed", description = "Unemployment rate", required = true)
	private double unemployed;

	@CommandLine.Option(names = "--gender-dist", description = "Proportion of women", defaultValue = "0.5")
	private double genderDist;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--sample", description = "Sample size to generate", defaultValue = "0.25")
	private double sample;

	private SplittableRandom rnd;
	private Population population;

	public static void main(String[] args) {
		new CreateFixedPopulation().execute(args);
	}

	@Override
	@SuppressWarnings("IllegalCatch")
	public Integer call() throws Exception {

		rnd = new SplittableRandom(0);
		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		generatePersons();

		log.info("Generated {} persons", population.getPersons().size());

		PopulationUtils.sortPersons(population);

		ProjectionUtils.putCRS(population, OpenBerlinScenario.CRS);
		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	private void generatePersons() {

		double young = ageDist.get(0);
		double old = ageDist.get(1);

		// x women for 100 men
		double quota = genderDist;

		var sex = new EnumeratedAttributeDistribution<>(Map.of("f", quota, "m", 1 - quota));
		var employment = new EnumeratedAttributeDistribution<>(Map.of(true, 1 - unemployed, false, unemployed));
		var ageGroup = new EnumeratedAttributeDistribution<>(Map.of(
			AgeGroup.YOUNG, young,
			AgeGroup.MIDDLE, 1.0 - young - old,
			AgeGroup.OLD, old
		));

		PopulationFactory f = population.getFactory();
		Geometry geom = facilities.getGeometry().convexHull();

		var youngDist = new UniformAttributeDistribution<>(IntStream.range(1, 18).boxed().toList());
		var middleDist = new UniformAttributeDistribution<>(IntStream.range(18, 65).boxed().toList());
		var oldDist = new UniformAttributeDistribution<>(IntStream.range(65, 100).boxed().toList());

		for (int i = 0; i < n * sample; i++) {

			Person person = f.createPerson(CreateBerlinPopulation.generateId(population, prefix, rnd));
			PersonUtils.setSex(person, sex.sample());
			PopulationUtils.putSubpopulation(person, "person");

			AgeGroup group = ageGroup.sample();

			if (group == AgeGroup.MIDDLE) {
				PersonUtils.setAge(person, middleDist.sample());
				PersonUtils.setEmployed(person, employment.sample());
			} else if (group == AgeGroup.YOUNG) {
				PersonUtils.setAge(person, youngDist.sample());
				PersonUtils.setEmployed(person, false);
			} else if (group == AgeGroup.OLD) {
				PersonUtils.setAge(person, oldDist.sample());
				PersonUtils.setEmployed(person, false);
			}

			Coord coord = CreateBerlinPopulation.sampleHomeCoordinate(geom, OpenBerlinScenario.CRS, facilities, rnd);

			person.getAttributes().putAttribute(Attributes.HOME_X, coord.getX());
			person.getAttributes().putAttribute(Attributes.HOME_Y, coord.getY());

			// Currently hard-coded as berlin inhabitants
			person.getAttributes().putAttribute(Attributes.GEM, 11000000);
			person.getAttributes().putAttribute(Attributes.ARS, 110000000000L);
			person.getAttributes().putAttribute(Attributes.RegioStaR7, 1);

			Plan plan = f.createPlan();
			plan.addActivity(f.createActivityFromCoord("home", coord));

			person.addPlan(plan);
			person.setSelectedPlan(plan);

			population.addPerson(person);
		}
	}

	private enum AgeGroup {
		YOUNG,
		MIDDLE,
		OLD
	}

}
