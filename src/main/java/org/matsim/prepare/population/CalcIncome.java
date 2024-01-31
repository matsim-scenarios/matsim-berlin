package org.matsim.prepare.population;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * Draw income from distribution, according to household size and income group. Based on SrV income groups.
 */
public class CalcIncome implements PersonAlgorithm {

	/**
	 * Income groups in Euro. The last element is the maximum income in the model, which is not known but defined.
	 * The minimum is also defined as well.
	 */
	private static final int[] INCOME_GROUPS = new int[]{400, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, 5600, 8000};

	/**
	 *  Distribution per economic status. See python file extract income.
	 */
	private static final Map<String, double[]> INCOME_DIST = Map.of(
		"very_low", new double[]{0.086, 0.342, 0.343, 0.165, 0.058, 0.004, 0.002, 0.000, 0.000, 0.000},
		"low", new double[]{0.000, 0.000, 0.443, 0.343, 0.123, 0.031, 0.056, 0.005, 0.000, 0.000},
		"medium", new double[]{0.000, 0.000, 0.000, 0.154, 0.324, 0.196, 0.237, 0.084, 0.005, 0.000},
		"high", new double[]{0.000, 0.000, 0.000, 0.000, 0.000, 0.066, 0.069, 0.433, 0.377, 0.055},
		"very_high", new double[]{0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.025, 0.975}
	);

	private final SplittableRandom rnd = new SplittableRandom(1234);

	private final Map<String, EnumeratedIntegerDistribution> dists = new HashMap<>();


	public CalcIncome() {

		for (Map.Entry<String, double[]> e : INCOME_DIST.entrySet()) {
			EnumeratedIntegerDistribution d = new EnumeratedIntegerDistribution(
				new Well19937c(0),
				IntStream.range(0, e.getValue().length).toArray(), e.getValue());
			dists.put(e.getKey(), d);
		}

	}

	@Override
	public void run(Person person) {


		// Only handle persons
		if (!PopulationUtils.getSubpopulation(person).equals("person"))
			return;

		int hh = (int) person.getAttributes().getAttribute(Attributes.HOUSEHOLD_SIZE);
		String economicStatus = (String) person.getAttributes().getAttribute(Attributes.ECONOMIC_STATUS);

		// This is only approximate correct at best, finer grained income data is not available
		// Economic status is normally per household and defined here:
		// https://tu-dresden.de/bu/verkehr/ivs/srv/ressourcen/dateien/SrV2018_Tabellenbericht_Oberzentren_500TEW-_flach.pdf?lang=de
		// page 17

		EnumeratedIntegerDistribution dist = dists.get(economicStatus);

		int idx = dist.sample();

		// income between lower and upper bound is uniformly sampled
		int income = rnd.nextInt(INCOME_GROUPS[idx], INCOME_GROUPS[idx + 1]);

		// Income is divided equally to household
		PersonUtils.setIncome(person, (double) income / hh);
	}

}
