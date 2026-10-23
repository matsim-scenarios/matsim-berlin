package org.matsim.prepare.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.SplittableRandom;

/**
 * Dresden-style draws within inferred SrV bands, stored as Äquivalenzeinkommen.
 * Infers bands from midpoint-based incomes; other positive incomes are retained.
 */
@CommandLine.Command(name = "assign-income")
public final class AssignIncome implements MATSimAppCommand, PersonAlgorithm {

	// SrV household-income representatives, before division by weighted household size.
	private static final int[] REPRESENTATIVES = {250, 700, 1200, 1750, 2300, 2800, 3300, 4100, 5100, 6100};
	private final SplittableRandom rnd = new SplittableRandom(1234);

	@CommandLine.Option(names = "--input", required = true)
	private Path input;
	@CommandLine.Option(names = "--output", required = true)
	private Path output;

	public static void main(String[] args) {
		new AssignIncome().execute(args);
	}

	@Override
	public Integer call() {
		var population = PopulationUtils.readPopulation(input.toString());
		population.getPersons().values().forEach(this::run);
		PopulationUtils.writePopulation(population, output.toString());
		return 0;
	}

	public static void assignIncomeToPersons(Population population) {
		population.getPersons().values().forEach(new AssignIncome()::run);
	}

	@Override
	public void run(Person person) {
		if (!"person".equals(PopulationUtils.getSubpopulation(person)))
			return;

		Double currentIncome = PersonUtils.getIncome(person);
		Object size = person.getAttributes().getAttribute(Attributes.HOUSEHOLD_EQUIVALENT_SIZE);
		if (size == null)
			throw new IllegalArgumentException("Missing household_equivalent_size");
		double householdSize = Double.parseDouble(size.toString());
		if (!Double.isFinite(householdSize) || householdSize <= 0)
			throw new IllegalArgumentException("household_equivalent_size must be positive and finite");

		// Infer the class by reproducing Berlin's rounding and EUR 249 minimum.
		// This is not the original survey response; the first match selects the lower class if ambiguous.
		int incomeGroup = 0;
		for (int i = 0; currentIncome != null && i < REPRESENTATIVES.length; i++) {
			if (currentIncome == Math.max(249, Math.round(REPRESENTATIVES[i] / householdSize))) {
				incomeGroup = i + 1;
				break;
			}
		}
		// Preserve already continuous or otherwise unmatched valid income, including older prepared demand.
		if (incomeGroup == 0 && currentIncome != null && Double.isFinite(currentIncome) && currentIncome > 0)
			return;

		// Uniform draws in equivalent-income bands; the lowest uses (0, upper] to avoid zero income.
		double income = switch (incomeGroup) {
			//darf nicht bis null gehen...
			case 1 -> (1 - rnd.nextDouble()) * (500 / householdSize);
			case 2 -> rnd.nextDouble(500 / householdSize, 900 / householdSize);
			case 3 -> rnd.nextDouble(900 / householdSize, 1500 / householdSize);
			case 4 -> rnd.nextDouble(1500 / householdSize, 2000 / householdSize);
			case 5 -> rnd.nextDouble(2000 / householdSize, 2600 / householdSize);
			case 6 -> rnd.nextDouble(2600 / householdSize, 3000 / householdSize);
			case 7 -> rnd.nextDouble(3000 / householdSize, 3600 / householdSize);
			case 8 -> rnd.nextDouble(3600 / householdSize, 4600 / householdSize);
			case 9 -> rnd.nextDouble(4600 / householdSize, 5600 / householdSize);
			// Dresden's half-normal upper tail, starting at the SrV threshold of EUR 5600.
			case 10 -> (Math.abs(rnd.nextGaussian()) * 1000 + 5600) / householdSize;
			// Missing/invalid income uses Dresden's fallback: 2364 EUR per person/month (2021).
			// Dresden's source comment cites average gross household income of 4734 EUR/month
			// and about 2 persons per household (83.1M persons / 41.5M households).
			// Destatis, accessed 21 Sep. 2021 in the Dresden implementation:
			// https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
			// https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
			// https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html
			default -> 2364;
		};
		// Keep continuous equivalent income: no rounding, additional division or EUR 249 floor.
		PersonUtils.setIncome(person, income);
	}
}
